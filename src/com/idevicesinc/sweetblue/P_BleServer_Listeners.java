package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Response;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Result;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Status;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Target;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Type;
import com.idevicesinc.sweetblue.utils.UpdateLoop;

public class P_BleServer_Listeners extends BluetoothGattServerCallback {

	private final BleServer m_server;
	private final P_Logger m_logger;
	private final P_TaskQueue m_queue;
	
	abstract class SynchronizedRunnable implements Runnable
	{
		@Override public void run()
		{
			synchronized (m_server.m_threadLock)
			{
				run_nested();
			}
		}
		
		public abstract void run_nested();
	}
	final PA_Task.I_StateListener m_taskStateListener = new PA_Task.I_StateListener()
	{
		@Override public void onStateChange(PA_Task task, PE_TaskState state)
		{
			synchronized (m_server.m_threadLock)
			{
				onStateChange_synchronized(task, state);
			}
		}
		
		private void onStateChange_synchronized(PA_Task task, PE_TaskState state)
		{
			if (task.getClass() == P_Task_ServerDisconnect.class)
			{
				if (state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT)
				{
					P_Task_Disconnect task_cast = (P_Task_Disconnect) task;

					m_server.onNativeDisconnect(task_cast.isExplicit());
				}
			}
		}
	};
	public P_BleServer_Listeners( BleServer server ) {
		m_server = server;
		m_logger = m_server.getManager().getLogger();
		m_queue = m_server.getTaskQueue();
	}

	/**
     * Callback indicating when a remote device has been connected or disconnected.
     *
     * @param device Remote device that has been connected or disconnected.
     * @param status Status of the connect or disconnect operation.
     * @param newState Returns the new connection state. Can be one of
     *                  {@link BluetoothProfile#STATE_DISCONNECTED} or
     *                  {@link BluetoothProfile#STATE_CONNECTED}
     */
    public void onConnectionStateChange(final BluetoothDevice device, final int status, final int newState) {
    	m_logger.log_status(status, m_logger.gattConn(newState));
		
		UpdateLoop updater = m_server.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				onConnectionStateChange_synchronized(device, status, newState);
			}
		});
    }

    private void onConnectionStateChange_synchronized(BluetoothDevice device, int status, int newState)
	{
    	if (newState == BluetoothProfile.STATE_CONNECTED ) {
    		m_server.setNativeDevice( device );
    	}
    	else if (newState == BluetoothProfile.STATE_DISCONNECTED )
		{
			m_server.m_nativeWrapper.updateNativeConnectionState(device, newState);

			if (!m_queue.succeed(P_Task_ServerDisconnect.class, m_server))
			{
				m_server.onNativeDisconnect(/*explicit=*/false);
			}
		}
	}
    /**
     * Indicates whether a local service has been added successfully.
     *
     * @param status Returns {@link BluetoothGatt#GATT_SUCCESS} if the service
     *               was added successfully.
     * @param service The service that has been added
     */
    public void onServiceAdded(int status, BluetoothGattService service) {
    }

    /**
     * A remote client has requested to read a local characteristic.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param characteristic Characteristic to be read
     */
    public void onCharacteristicReadRequest(final BluetoothDevice device, final int requestId, final int offset, final BluetoothGattCharacteristic characteristic) {
    	try { 
        	final UUID uuid = characteristic.getUuid();
    		final ReadOrWriteRequestListener listener = m_server.getReadOrWriteRequestListener();;
    		m_logger.i(m_logger.charName(uuid));
    		m_logger.log_status( BluetoothGatt.GATT_SUCCESS );
    		
    		UpdateLoop updater = m_server.getManager().getUpdateLoop();
    		
    		updater.postIfNeeded(new SynchronizedRunnable()
    		{
    			@Override public void run_nested()
    			{
    				Response response = listener.onReadOrWriteRequest( new Result( m_server, device, characteristic.getUuid(), null, Type.READ, Target.CHARACTERISTIC, null, Status.SUCCESS, false, requestId, offset ) );
    				m_server.sendResponse( response );
    			}
    		});
    	}
    	catch( Exception e ) {
    		e.printStackTrace();
    	}
    }

    /**
     * A remote client has requested to write to a local characteristic.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device The remote device that has requested the write operation
     * @param requestId The Id of the request
     * @param characteristic Characteristic to be written to.
     * @param preparedWrite true, if this write operation should be queued for
     *                      later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset The offset given for the value
     * @param value The value the client wants to assign to the characteristic
     */
    public void onCharacteristicWriteRequest(final BluetoothDevice device, final int requestId,
                                             final BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, final boolean responseNeeded,
                                             final int offset, final byte[] value) {
    	try { 
        	final UUID uuid = characteristic.getUuid();
        	final ReadOrWriteRequestListener listener = m_server.getReadOrWriteRequestListener();
    		m_logger.i(m_logger.charName(uuid));
    		
    		UpdateLoop updater = m_server.getManager().getUpdateLoop();
    		
    		updater.postIfNeeded(new SynchronizedRunnable()
    		{
    			@Override public void run_nested()
    			{
    				if ( responseNeeded ) {
    					Response response = listener.onReadOrWriteRequest( new Result( m_server, device, characteristic.getUuid(), null, Type.WRITE, Target.CHARACTERISTIC, value, Status.SUCCESS, responseNeeded, requestId, offset ) );
    					m_server.sendResponse( response );
    				}
    			}
    		});
    	}
    	catch( Exception e ) {
    		e.printStackTrace();
    	}
    }

    /**
     * A remote client has requested to read a local descriptor.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param descriptor Descriptor to be read
     */
    public void onDescriptorReadRequest(final BluetoothDevice device, final int requestId,
                                        final int offset, final BluetoothGattDescriptor descriptor) {
    	try { 
        	final UUID uuid = descriptor.getUuid();
    		final ReadOrWriteRequestListener listener = m_server.getReadOrWriteRequestListener();;
    		m_logger.i(m_logger.charName(uuid));
    		m_logger.log_status( BluetoothGatt.GATT_SUCCESS );
    		
    		UpdateLoop updater = m_server.getManager().getUpdateLoop();
    		
    		updater.postIfNeeded(new SynchronizedRunnable()
    		{
    			@Override public void run_nested()
    			{
    				Response response = listener.onReadOrWriteRequest( new Result( m_server, device, null, descriptor.getUuid(), Type.READ, Target.DESCRIPTOR, null, Status.SUCCESS, false, requestId, offset ) );
    				m_server.sendResponse( response );
    			}
    		});
    	}
    	catch( Exception e ) {
    		e.printStackTrace();
    	}
    }

    /**
     * A remote client has requested to write to a local descriptor.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device The remote device that has requested the write operation
     * @param requestId The Id of the request
     * @param descriptor Descriptor to be written to.
     * @param preparedWrite true, if this write operation should be queued for
     *                      later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset The offset given for the value
     * @param value The value the client wants to assign to the descriptor
     */
    public void onDescriptorWriteRequest( final BluetoothDevice device, final int requestId,
                                          final BluetoothGattDescriptor descriptor,
                                          final boolean preparedWrite, final boolean responseNeeded,
                                          final int offset, final byte[] value) {
    	try { 
        	final UUID uuid = descriptor.getUuid();
        	final ReadOrWriteRequestListener listener = m_server.getReadOrWriteRequestListener();;
    		m_logger.i(m_logger.charName(uuid));
    		
    		UpdateLoop updater = m_server.getManager().getUpdateLoop();
    		
    		updater.postIfNeeded(new SynchronizedRunnable()
    		{
    			@Override public void run_nested()
    			{
    				if ( responseNeeded ) {
    					Response response = listener.onReadOrWriteRequest( new Result( m_server, device, null, descriptor.getUuid(), Type.WRITE, Target.DESCRIPTOR, value, Status.SUCCESS, responseNeeded, requestId, offset ) );
    					m_server.sendResponse( response );
    				}
    			}
    		});
    	}
    	catch( Exception e ) {
    		e.printStackTrace();
    	}
    }

    /**
     * Execute all pending write operations for this device.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device The remote device that has requested the write operations
     * @param requestId The Id of the request
     * @param execute Whether the pending writes should be executed (true) or
     *                cancelled (false)
     */
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
    }
    
    @Override
    public void onNotificationSent( final BluetoothDevice device, final int status ) {

		UpdateLoop updater = m_server.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				Status aStatus = Status.SUCCESS;
				final P_Task_Notify task = m_server.getTaskQueue().getCurrent( P_Task_Notify.class, m_server );
				final ReadOrWriteRequestListener listener = task.getListener(); 
		    	if ( status != BluetoothGatt.GATT_SUCCESS ) {
		    		aStatus = Status.FAILED_TO_SEND_OUT;
		    	}
				listener.onNotificationSent( new Result( m_server, device, null, null, Type.NOTIFICATION, Target.CHARACTERISTIC, null, aStatus, false, 0, 0 ) );
			}
		});
    }
}
