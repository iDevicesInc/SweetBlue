package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleManager.StateListener.StateEvent;


final class P_BleStateTracker extends PA_StateTracker
{
	private ManagerStateListener m_stateListener;
	private final BleManager m_mngr;
	
	P_BleStateTracker(BleManager mngr)
	{
		super(BleManagerState.VALUES());
		
		m_mngr = mngr;
	}
	
	public void setListener(ManagerStateListener listener)
	{
		m_stateListener = listener;
	}

	@Override protected void onStateChange(final int oldStateBits, final int newStateBits, final int intentMask, final int status)
	{
		if( m_stateListener != null )
		{
			final StateEvent event = new StateEvent(m_mngr, oldStateBits, newStateBits, intentMask);
			m_mngr.postEvent(m_stateListener, event);
		}
	}
	
	@Override public String toString()
	{
		return super.toString(BleManagerState.VALUES());
	}
}
