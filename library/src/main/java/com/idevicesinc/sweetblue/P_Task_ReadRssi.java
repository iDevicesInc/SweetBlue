package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * 
 * 
 */
final class P_Task_ReadRssi extends PA_Task_Transactionable implements PA_Task.I_StateListener
{	
	protected final BleDevice.ReadWriteListener m_readWriteListener;
	private final Type m_type;
	
	public P_Task_ReadRssi(BleDevice device, BleDevice.ReadWriteListener readWriteListener, BleTransaction txn_nullable, PE_TaskPriority priority, Type type)
	{
		super(device, txn_nullable, false, priority);
		
		m_readWriteListener = readWriteListener;

		m_type = type;
	}
	
	private ReadWriteEvent newEvent(Status status, int gattStatus, int rssi)
	{
		return new ReadWriteEvent(getDevice(), m_type, /*rssi=*/rssi, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override protected void onNotExecutable()
	{
		super.onNotExecutable();

		getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0));
	}
	
	private void fail(Status status, int gattStatus)
	{
		this.fail();

		getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(status, gattStatus, 0));
	}

	@Override public void execute()
	{
		if( false == getDevice().layerManager().readRemoteRssi() )
		{
			fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else
		{
			// SUCCESS, so far...
		}
	}
	
	private void succeed(int gattStatus, int rssi)
	{
		super.succeed();

		final ReadWriteEvent event = newEvent(Status.SUCCESS, gattStatus, rssi);
		
		getDevice().invokeReadWriteCallback(m_readWriteListener, event);
	}
	
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
	{
		getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));
		
		if( Utils.isSuccess(status) )
		{
			succeed(status, rssi);
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, status);
		}
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0));
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0));
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.READ_RSSI;
	}
}
