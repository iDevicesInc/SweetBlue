package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.utils.Utils;

final class P_Task_ExecuteReliableWrite extends PA_Task_RequiresConnection implements PA_Task.I_StateListener
{
	private final PE_TaskPriority m_priority;
	private final ReadWriteListener m_listener;

	public P_Task_ExecuteReliableWrite(BleDevice device, ReadWriteListener listener, PE_TaskPriority priority)
	{
		super(device, null);

		m_priority = priority;
		m_listener = listener;
	}

	private ReadWriteListener.ReadWriteEvent newEvent(final ReadWriteListener.Status status, final int gattStatus, final boolean solicited)
	{
		return getDevice().m_reliableWriteMngr.newEvent(status, gattStatus, solicited);
	}

	private void invokeListeners(final ReadWriteListener.Status status, final int gattStatus)
	{
		final ReadWriteListener.ReadWriteEvent event = newEvent(ReadWriteListener.Status.NOT_CONNECTED, gattStatus, /*solicited=*/true);

		getDevice().invokeReadWriteCallback(m_listener, event);
	}

	@Override protected void onNotExecutable()
	{
		super.onNotExecutable();

		invokeListeners(ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.RELIABLE_WRITE;
	}

	@Override void execute()
	{
		if( false == getDevice().layerManager().executeReliableWrite() )
		{
			fail(ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
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

			invokeListeners(ReadWriteListener.Status.SUCCESS, gattStatus);
		}
		else
		{
			fail(ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus);
		}
	}

	private void fail(final ReadWriteListener.Status status, final int gattStatus)
	{
		super.fail();

		invokeListeners(ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus);
	}

	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}

	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			getDevice().invokeReadWriteCallback(m_listener, newEvent(ReadWriteListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true));
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_listener, newEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true));
		}
	}
}
