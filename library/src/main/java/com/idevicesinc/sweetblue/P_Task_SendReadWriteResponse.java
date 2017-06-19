package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.PA_Task.I_StateListener;
import com.idevicesinc.sweetblue.utils.P_Const;

final class P_Task_SendReadWriteResponse extends PA_Task_RequiresServerConnection implements I_StateListener
{
	private final BleServer.IncomingListener.IncomingEvent m_requestEvent;
	private final BleServer.IncomingListener.Please m_please;

	private byte[] m_data_sent = null;

	public P_Task_SendReadWriteResponse(BleServer server, final BleServer.IncomingListener.IncomingEvent requestEvent, BleServer.IncomingListener.Please please)
	{
		super( server, requestEvent.macAddress());

		m_requestEvent = requestEvent;
		m_please = please;
	}

	private byte[] data_sent()
	{
		if( m_data_sent == null )
		{
			m_data_sent = m_please.m_futureData != null ? m_please.m_futureData.getData() : P_Const.EMPTY_BYTE_ARRAY;
			m_data_sent = m_data_sent != null ? m_data_sent : P_Const.EMPTY_BYTE_ARRAY;
		}

		return m_data_sent;
	}

	@Override public void onStateChange( PA_Task task, PE_TaskState state )
	{
		if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			invokeFailCallback(getCancelStatusType());
		}
		else if( state == PE_TaskState.TIMED_OUT )
		{
			invokeFailCallback(BleServer.OutgoingListener.Status.TIMED_OUT);
		}
	}

	private void fail(final BleServer.OutgoingListener.Status status)
	{
		super.fail();

		invokeFailCallback(status);
	}

	private void invokeFailCallback(BleServer.OutgoingListener.Status status)
	{
		final BleServer.OutgoingListener.OutgoingEvent e = new BleServer.OutgoingListener.OutgoingEvent(m_requestEvent, data_sent(), status, m_please.m_gattStatus, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		getServer().invokeOutgoingListeners(e, m_please.m_outgoingListener);
	}

	@Override protected void succeed()
	{
		super.succeed();

		final BleServer.OutgoingListener.OutgoingEvent e = new BleServer.OutgoingListener.OutgoingEvent(m_requestEvent, data_sent(), BleServer.OutgoingListener.Status.SUCCESS, m_please.m_gattStatus, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		getServer().invokeOutgoingListeners(e, m_please.m_outgoingListener);
	}

	@Override void execute()
	{
		if( false == getServer().getNativeLayer().sendResponse(m_requestEvent.nativeDevice(), m_requestEvent.requestId(), m_please.m_gattStatus, m_requestEvent.offset(), data_sent()) )
		{
			fail(BleServer.OutgoingListener.Status.FAILED_TO_SEND_OUT);
		}
		else
		{
			// SUCCESS, and we'll wait a half second or so (for now hardcoded) until actually succeeding, cause there's no native callback for this one.
		}
	}

	@Override protected void onNotExecutable()
	{
		fail(BleServer.OutgoingListener.Status.NOT_CONNECTED);
	}

	@Override protected void update(double timeStep)
	{
		final double timeToSuccess = .25d; //TODO

		if( getState() == PE_TaskState.EXECUTING && getTotalTimeExecuting() >= timeToSuccess )
		{
			succeed();
		}
	}

	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_NORMAL_READS_WRITES;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.SEND_READ_WRITE_RESPONSE;
	}
}
