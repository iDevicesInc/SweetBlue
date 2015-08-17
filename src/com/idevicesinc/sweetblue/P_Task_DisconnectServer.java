package com.idevicesinc.sweetblue;

class P_Task_DisconnectServer extends PA_Task_RequiresBleOn
{
	final String m_macAddress;

	public P_Task_DisconnectServer(final BleServer server, final String macAddress, final I_StateListener listener)
	{
		super( server, listener );

		m_macAddress = macAddress;
	}

	 @Override void execute()
	 {

	 }

	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.DISCONNECT_SERVER;
	}
}
