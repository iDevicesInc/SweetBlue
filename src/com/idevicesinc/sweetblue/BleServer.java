/**
 * 
 */
package com.idevicesinc.sweetblue;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Response;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Type;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * @author johnkim
 *
 */
public class BleServer {

	static final byte[]				EMPTY_BYTE_ARRAY	= new byte[0];
	
			final Object m_threadLock = new Object();
	private final P_ServerStateTracker m_stateTracker;
	private final BleManager m_mngr;
	private final P_TaskQueue m_queue;
			final P_BleServer_Listeners m_listeners;
			final BluetoothManager m_btMngr;
				  BluetoothGattServer m_serverNative;
	private final PA_Task.I_StateListener m_taskStateListener;
			final P_NativeServerWrapper m_nativeWrapper;
	private		  ReadOrWriteRequestListener m_readWriteListener;
	private		  BleDevice m_device;
	/**
	 * 
	 */
	public static interface ReadOrWriteRequestListener
	{
		public static class Response 
		{			
			public Response( BluetoothDevice device, UUID uuid, int requestId, int status, int offset, byte[] value, boolean preparedWrite ) {
				this.device = device;
				this.requestId = requestId;
				this.status = status;
				this.offset = offset;
				this.data = value;
				this.preparedWrite = preparedWrite;
				this.uuid = uuid;
			}
			public final BluetoothDevice device; 
			public final int requestId;
			public final int status;
			public final int offset;
			public final byte[] data;
			public final boolean preparedWrite;
			public final UUID uuid;
		}
		/**
		 * Provides a bunch of information about a completed read, write, or notification.
		 */
		public static class Result
		{
			/**
			 * Value used in place of <code>null</code>, either indicating that {@link #descUuid}
			 * isn't used for the {@link Result} because {@link #target} is {@link Target#CHARACTERISTIC},
			 * or that both {@link #descUuid} and {@link #charUuid} aren't applicable because {@link #target}
			 * is {@link Target#RSSI}.
			 */
			public static final UUID NON_APPLICABLE_UUID = Uuids.INVALID;
			
			/**
			 * Status code used for {@link #gattStatus} when the operation didn't get to a point where a
			 * gatt status from the underlying stack is provided.
			 */
			public static final int GATT_STATUS_NOT_APPLICABLE = -1;
			
			/**
			 * The {@link BleServer} this {@link Result} is for.
			 */
			public final BleServer server;
			
			/**
			 * The type of operation, read, write, etc.
			 */
			public final Type type;
			
			/**
			 * The type of GATT object this {@link Result} is for, characteristic or descriptor.
			 */
			public final Target target;
			
			/**
			 * The {@link UUID} of the characteristic associated with this {@link Result}. This will always be
			 * a valid {@link UUID}, even if {@link #target} is {@link Target#DESCRIPTOR}.
			 */
			public final UUID charUuid;
			
			/**
			 * The {@link UUID} of the descriptor associated with this {@link Result}. If {@link #target} is
			 * {@link Target#CHARACTERISTIC} then this will be referentially equal (i.e. you can use == to compare)
			 * to {@link #NON_APPLICABLE_UUID}.
			 */
			public final UUID descUuid;
			
			/**
			 * The data sent to the peripheral if {@link Result#type} is {@link Type#WRITE},
			 * otherwise the data received from the peripheral if {@link Result#type} {@link Type#isRead()}.
			 * This will never be null. For error statuses it will be a zero-length array.
			 */
			public final byte[] data;
			
			/**
			 * Indicates either success or the type of failure. Some values of {@link Status} are not
			 * used for certain values of {@link Type}. For example a {@link Type#NOTIFICATION}
			 * cannot fail with {@link Status#TIMED_OUT}.
			 */
			public final Status status;
			
			/**
			 * The native gatt status returned from the stack, if applicable. If the {@link #status} returned is,
			 * for example, {@link Status#NO_MATCHING_TARGET}, then the operation didn't even reach the point
			 * where a gatt status is provided, in which case this member is set to {@link #GATT_STATUS_NOT_APPLICABLE}
			 * (value of {@value #GATT_STATUS_NOT_APPLICABLE}). Otherwise it will be <code>0</code> for success or greater
			 * than <code>0</code> when there's an issue. <i>Generally</i> this value will only be meaningful when {@link #status}
			 * is {@link Status#SUCCESS} or {@link Status#REMOTE_GATT_FAILURE}. There are also some cases where this will be 0 for
			 * success but {@link #status} is for example {@link Status#NULL_DATA} - in other words the underlying stack deemed the 
			 * operation a success but SweetBlue disagreed. For this reason it's recommended to treat this value as a debugging tool
			 * and use {@link #status} for actual application logic if possible.
			 * <br><br>
			 * See {@link BluetoothGatt} for its static <code>GATT_*</code> status code members.
			 * Also see {@link PS_GattStatus} for SweetBlue's more comprehensive internal reference list of gatt status values.
			 * This list may not be totally accurate or up-to-date.
			 */
			public final BluetoothDevice device;
			
			public final int requestId;
			
			public final boolean preparedWrite;
			
			public final int offset;
			
			Result(BleServer server, BluetoothDevice device, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_in, Status status_in, boolean preparedWrite, int requestId, int offset )
			{
				this.server = server;
				this.device = device;
				this.charUuid = charUuid_in != null ? charUuid_in : NON_APPLICABLE_UUID;;
				this.descUuid = descUuid_in != null ? descUuid_in : NON_APPLICABLE_UUID;
				this.type = type_in;
				this.target = target_in;
				this.status = status_in;
				this.preparedWrite = preparedWrite;
				this.requestId = requestId;
				this.offset = offset;
						
				this.data = data_in != null ? data_in : EMPTY_BYTE_ARRAY;
			}
			
			Result(BleServer server, BluetoothDevice device, Type type_in, Status status_in, int gattStatus_in, double totalTime, double transitTime, boolean preparedWrite, int requestId, int offset )
			{
				this.server = server;
				this.device = device;
				this.charUuid = NON_APPLICABLE_UUID;
				this.descUuid = NON_APPLICABLE_UUID;
				this.type = type_in;
				this.target = Target.CHARACTERISTIC;
				this.status = status_in;
				this.requestId = requestId;
				this.preparedWrite = preparedWrite;
				this.offset = offset;
				
				this.data = EMPTY_BYTE_ARRAY;
			}
			
			/**
			 * Convenience method for checking if {@link Result#status} equals {@link Status#SUCCESS}.
			 */
			public boolean wasSuccess()
			{
				return status == Status.SUCCESS;
			}
			
			@Override public String toString()
			{
				return "status="+status+" type="+type+" target="+target+" charUuid="+charUuid +" data="+data;
			}
		}
		/**
		 * A value returned to {@link ResponseNotifyListener#onResponseOrNotifyComplete(Result)} by way of
		 * {@link Result#status} that indicates success of the operation or the reason for its failure.
		 * This enum is <i>not</i> meant to match up with {@link BluetoothGatt}.GATT_* values in any way.
		 * 
		 * @see Result#status
		 */
		public static enum Status
		{
			/**
			 * If {@link Result#type} {@link Type#isRead()} then {@link Result#data} will contain
			 * some data returned from the device. If type is {@link Type#WRITE} then {@link Result#data}
			 * was sent to the device.
			 */
			SUCCESS,
			
			/**
			 * Device is {@link BleDeviceState#CONNECTED}.
			 */
			CONNECTED,
			
			/**
			 * Device is not {@link BleDeviceState#CONNECTED}.
			 */
			NOT_CONNECTED,
			
			/**
			 * Couldn't find a matching {@link Result#target} for the {@link Result#charUuid} (or {@link Result#descUuid} if {@link Result#target} is
			 * {@link Target#DESCRIPTOR}) which was given to {@link BleDevice#read(UUID, ReadWriteListener)},
			 * {@link BleDevice#write(UUID, byte[])}, etc. This most likely means that the internal call to {@link BluetoothGatt#discoverServices()}
			 * didn't find any {@link BluetoothGattService} that contained a {@link BluetoothGattCharacteristic} for {@link Result#charUuid}.
			 */
			NO_MATCHING_TARGET,
			
			/**
			 * You tried to do a read on a characteristic that is write-only, or vice-versa, or tried to read a notify-only characteristic, etc., etc. 
			 */
			OPERATION_NOT_SUPPORTED,
			
			/**
			 * {@link BluetoothGattCharacteristic#setValue(byte[])} (or one of its overloads) or
			 * {@link BluetoothGattDescriptor#setValue(byte[])} (or one of its overloads) returned false.
			 */
			FAILED_TO_WRITE_VALUE_TO_TARGET,
			
			/**
			 * The call to {@link BluetoothGatt#readCharacteristic(BluetoothGattCharacteristic)} or {@link BluetoothGatt#writeCharacteristic(BluetoothGattCharacteristic)}
			 * or etc. returned {@link Boolean#false} and thus failed immediately for unknown reasons. No good remedy for this...perhaps try {@link BleManager#dropTacticalNuke()}.
			 */
			FAILED_TO_SEND_OUT,
			
			/**
			 * The operation was cancelled either by the device becoming {@link BleDeviceState#DISCONNECTED} or {@link BleManager} turning {@link BleState#OFF}.
			 */
			CANCELLED,
			
			/**
			 * Used either when {@link Result#type} {@link Type#isRead()} and the stack returned a null value for {@link BluetoothGattCharacteristic#getValue()} despite
			 * the operation being otherwise "successful", <i>or</i> {@link BleDevice#write(UUID, byte[])} (or overload(s) ) were called with a null data parameter.
			 * For the read case, the library will throw an {@link UhOh#READ_RETURNED_NULL}, but hopefully it was just a temporary glitch.
			 * If the problem persists try {@link BleManager#dropTacticalNuke()}.
			 */
			NULL_DATA,
			
			/**
			 * Used either when {@link Result#type} {@link Type#isRead()} and the operation was "successful" but returned a zero-length array for {@link Result#data}.
			 * <i>or</i> {@link BleDevice#write(UUID, byte[])} (or overload(s) ) were called with a non-null but zero-length data parameter.
			 * Note that {@link Result#data} will be a zero-length array for all other statuses as well, for example {@link #NO_MATCHING_TARGET}, {@link #NOT_CONNECTED}, etc.
			 * In other words it's never null.
			 */
			EMPTY_DATA,
			
			/**
			 * The operation failed in a "normal" fashion, at least relative to all the other strange ways an operation can fail. This means
			 * for example that {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)} returned
			 * a status code that was not zero. This could mean the device went out of range, was turned off, signal was disrupted, whatever.
			 */
			REMOTE_GATT_FAILURE,
			
			/**
			 * Operation took longer than {@link BleManagerConfig#DEFAULT_TASK_TIMEOUT} seconds so we cut it loose.
			 */
			TIMED_OUT;
		}
		
		/**
		 * The type of operation for a {@link Result} - read, write, poll, etc.
		 */
		public static enum Type
		{
			/**
			 * Associated with {@link BleDevice#read(UUID, ReadWriteListener)} or {@link BleDevice#readRssi(ReadWriteListener)}.
			 */
			READ,
			
			/**
			 * Associated with {@link BleDevice#write(UUID, byte[])} or {@link BleDevice#write(UUID, byte[], ReadWriteListener)}.
			 */
			WRITE,
			
			/**
			 * Associated with {@link BleDevice#enableNotify(UUID, ReadWriteListener)} when we actually get a notification.
			 */
			NOTIFICATION,
			
			/**
			 * Similar to {@link #NOTIFICATION}, kicked off from {@link BleDevice#enableNotify(UUID, ReadWriteListener)},
			 * but under the hood this is treated slightly differently.
			 */
			INDICATION;
			
			public boolean isRead()
			{
				return this != WRITE && this != NOTIFICATION;
			}
			
			/**
			 * Returns true if <code>this</code> is {@link #NOTIFICATION}, {@link #PSUEDO_NOTIFICATION}, or {@link #INDICATION}.
			 */
			public boolean isNotification()
			{
				return this == NOTIFICATION || this == INDICATION;
			}
		}
		
		/**
		 * The type of GATT object, provided by {@link Result#target}.
		 */
		public static enum Target
		{
			/**
			 * The {@link Result} returned has to do with a {@link BluetoothGattCharacteristic} under the hood.
			 */
			CHARACTERISTIC,
			
			/**
			 * The {@link Result} returned has to do with a {@link BluetoothGattDescriptor} under the hood.
			 */
			DESCRIPTOR;
		}
		
		/**
		 * Called when a read or write is complete or when a notification comes in.
		 */
		public Response onReadOrWriteRequest(Result result);
		
		public void onNotificationSent( Result result );
	}
	/**
	 * Provide an implementation to {@link BleDevice#setListener_State(StateListener)} and/or
	 * {@link BleManager#setListener_DeviceState(BleDevice.StateListener)} to receive state change events.
	 * 
	 * @see BleDeviceState
	 * @see BleDevice#setListener_State(StateListener)
	 */
	public static interface StateListener
	{
		/**
		 * Called when a device's bitwise {@link BleDeviceState} changes. As many bits as possible are flipped at the same time.
		 *  
		 * @param oldStateBits The previous bitwise representation of {@link BleDeviceState}.
		 * @param newStateBits The new and now current bitwise representation of {@link BleDeviceState}. Will be the same as {@link BleDevice#getStateMask()}.
		 */
		void onStateChange(BleServer server, int oldStateBits, int newStateBits);
	}
	public BleServer( BleManager bleManager ) {
		m_mngr = bleManager;
		m_btMngr = bleManager.getNative();
		m_queue = m_mngr.getTaskQueue();
		m_stateTracker = new P_ServerStateTracker(this);
		m_listeners = new P_BleServer_Listeners(this);
		m_taskStateListener = m_listeners.m_taskStateListener;		
		m_nativeWrapper = new P_NativeServerWrapper(this);
//		m_taskStateListener = m_listeners.m_taskStateListener;
	}
	
	/**
	 * Set a listener here to be notified whenever this device's state changes.
	 */
	public void setListener_State(BleServer.StateListener listener)
	{
		m_stateTracker.setListener(listener);
	}
	
	P_TaskQueue getTaskQueue(){						return m_queue;								}
	ReadOrWriteRequestListener getReadOrWriteRequestListener() { return m_readWriteListener; }
	
	public BluetoothGattServer openGattServer( final Context context, final List<BluetoothGattService> gattServices, ReadOrWriteRequestListener listener ) {
		m_readWriteListener = listener;
		m_serverNative = m_btMngr.openGattServer( context, m_listeners );
		if ( null != gattServices ) {
    		for ( BluetoothGattService service : gattServices ) {
    			m_serverNative.addService( service );
    		}
		}
		m_nativeWrapper.setNative( m_serverNative );
		return m_serverNative;
	}
	public P_BleServer_Listeners getListeners() {
		return m_listeners;
	}
	void sendResponse( Response response ) {
		//TODO
		m_queue.add( new P_Task_ReadWriteResponse( this, m_taskStateListener, response ) );
	}
	public void notify( BluetoothDevice device, UUID serviceUuid, UUID charaUuid, byte[] data ) {		
		m_queue.add( new P_Task_Notify( this, device, m_serverNative.getService( serviceUuid ).getCharacteristic( charaUuid ), data, m_readWriteListener, m_taskStateListener, false ) );
	}
	public void notify( BluetoothDevice device, UUID serviceUuid, UUID charaUuid, byte [] data, ReadOrWriteRequestListener listener ) {
		m_queue.add( new P_Task_Notify( this, device, m_serverNative.getService( serviceUuid ).getCharacteristic( charaUuid ), data, listener, m_taskStateListener, true ) );
	}
	public UUID getUuid() {
		UUID uuid = null;
		List<BluetoothGattService> gattServices = m_serverNative.getServices();
		if ( null != gattServices && gattServices.size() > 0 ) {
			uuid = gattServices.get( 0 ).getUuid();
		}
		return uuid;
	}
	public BluetoothGattServer getNative() {
		return m_serverNative;
	}
	public BleDevice getDevice() {
		return m_device;
	}
	/**
	 * Returns whether the device is in any of the provided states.
	 * 
	 * @see #is(BleDeviceState)
	 */
	public boolean isAny(BleDeviceState ... states)
	{
		for( int i = 0; i < states.length; i++ )
		{
			if( is(states[i]) )  return true;
		}
		
		return false;
	}
	/**
	 * Returns whether the device is in the provided state.
	 * 
	 * @see #isAny(BleDeviceState...)
	 */
	public boolean is(BleDeviceState state)
	{
		return state.overlaps(getStateMask());
	}
	/**
	 * Returns the bitwise state mask representation of {@link BleDeviceState} for this device.
	 * 
	 * @see BleDeviceState
	 */
	public int getStateMask()
	{
		return m_stateTracker.getState();
	}
	/**
	 * Returns this server's manager.
	 */
	public BleManager getManager()
	{
		return m_mngr;
	}
	public void disconnect( BleServer.StateListener stateListener ) {
		if( stateListener != null )
		{
			setListener_State(stateListener);
		}
		m_queue.add( new P_Task_ServerDisconnect( this, m_taskStateListener ) );
	}

	void onNativeConnect() {
		
	}
	void onNativeDisconnect( boolean explicit ) {
		m_serverNative.close();
	}
	void setNativeDevice( BluetoothDevice device ) {
		m_device = m_mngr.newDevice(device.getAddress());
	}
	
}
