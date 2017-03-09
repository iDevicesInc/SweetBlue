package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.utils.Utils;

final class P_Task_ExecuteReliableWrite extends PA_Task_RequiresConnection implements PA_Task.I_StateListener
{
	private final PE_TaskPriority m_priority;
	private final BleDevice.ReadWriteListener m_listener;

	public P_Task_ExecuteReliableWrite(BleDevice device, BleDevice.ReadWriteListener listener, PE_TaskPriority priority)
	{
		super(device, null);

		m_priority = priority;
		m_listener = listener;
	}

	private BleDevice.ReadWriteListener.ReadWriteEvent newEvent(final BleDevice.ReadWriteListener.Status status, final int gattStatus, final boolean solicited)
	{
		return getDevice().m_reliableWriteMngr.newEvent(status, gattStatus, solicited);
	}

	private void invokeListeners(final BleDevice.ReadWriteListener.Status status, final int gattStatus)
	{
		final BleDevice.ReadWriteListener.ReadWriteEvent event = newEvent(BleDevice.ReadWriteListener.Status.NOT_CONNECTED, gattStatus, /*solicited=*/true);

		getDevice().invokeReadWriteCallback(m_listener, event);
	}

	@Override protected void onNotExecutable()
	{
		super.onNotExecutable();

		invokeListeners(BleDevice.ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.RELIABLE_WRITE;
	}

	@Override void execute()
	{
		if( false == getDevice().layerManager().executeReliableWrite() )
		{
			fail(BleDevice.ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else
		{
			// SUCCESS, so far
		}
	}

	public void onReliableWriteCompleted(final BluetoothGatt gatt, final int gattStatus)
	{
		if( Utils.isSuccess(gattStatus) )
		{
			succeed();

			invokeListeners(BleDevice.ReadWriteListener.Status.SUCCESS, gattStatus);
		}
		else
		{
			fail(BleDevice.ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus);
		}
	}

	private void fail(final BleDevice.ReadWriteListener.Status status, final int gattStatus)
	{
		super.fail();

		invokeListeners(BleDevice.ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus);
	}

	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}

	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			getDevice().invokeReadWriteCallback(m_listener, newEvent(BleDevice.ReadWriteListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true));
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_listener, newEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true));
		}
	}
}
