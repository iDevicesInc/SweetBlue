package com.idevicesinc.sweetblue;

public class P_Task_ServerDisconnect extends PA_Task_RequiresBleOn {
	
	public P_Task_ServerDisconnect( BleServer server, I_StateListener listener ) {
		super( server, listener );
		
	}

	@Override
	void execute() {
		
		if( getServer() == null )
		{
			m_logger.w("Already disconnected and server==null!");
			
			redundant();
			
			return;
		}
		
		if( getDevice().m_nativeWrapper./*already*/isNativelyDisconnecting() )
		{
			// nothing to do
			
			return;
		}
		getServer().getNative().cancelConnection( getServer().getDevice().getNative() );
	}

	@Override
	public PE_TaskPriority getPriority() {
		return PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING;
	}

}
