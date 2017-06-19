package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_Task_RequestMtu extends PA_Task_Transactionable implements PA_Task.I_StateListener
{
	protected final BleDevice.ReadWriteListener m_readWriteListener;
	private final int m_mtu;

	public P_Task_RequestMtu(BleDevice device, BleDevice.ReadWriteListener readWriteListener, BleTransaction txn_nullable, PE_TaskPriority priority, final int mtu)
	{
		super(device, txn_nullable, false, priority);
		
		m_readWriteListener = readWriteListener;
		m_mtu = mtu;
	}
	
	private ReadWriteEvent newEvent(Status status, int gattStatus, int mtu)
	{
		return new ReadWriteEvent(getDevice(), /*mtu=*/mtu, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
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
		if( Utils.isLollipop() )
		{
			if( false == getDevice().layerManager().requestMtu(m_mtu))
			{
				fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
			}
			else
			{
				// SUCCESS, so far...
			}
		}
		else
		{
			//--- DRK > Should be checked for before the task is even created but just being anal.
			fail(Status.ANDROID_VERSION_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
	}
	
	private void succeed(int gattStatus, int mtu)
	{
		super.succeed();

		final ReadWriteEvent event = newEvent(Status.SUCCESS, gattStatus, mtu);
		
		getDevice().invokeReadWriteCallback(m_readWriteListener, event);
	}
	
	public void onMtuChanged(BluetoothGatt gatt, int mtu, int gattStatus)
	{
		getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));
		
		if( Utils.isSuccess(gattStatus) )
		{
			succeed(gattStatus, mtu);
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, gattStatus);
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
		return BleTask.SET_MTU;
	}
}
