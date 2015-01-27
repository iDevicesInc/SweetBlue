package com.idevicesinc.sweetblue;


/**
 * 
 * 
 *
 */
class P_StateTracker extends PA_StateTracker
{
	private BleManager.StateListener m_stateListener;
	private final BleManager m_mngr;
	
	P_StateTracker(BleManager mngr)
	{
		super(mngr.getLogger(), BleState.values());
		
		m_mngr = mngr;
	}
	
	public void setListener(BleManager.StateListener listener)
	{
		if( listener != null )
		{
			m_stateListener = new P_WrappingBleStateListener(listener, m_mngr.m_mainThreadHandler, m_mngr.m_config.postCallbacksToMainThread);
		}
		else
		{
			m_stateListener = null;
		}
	}

	@Override protected void onStateChange(int oldStateBits, int newStateBits, int intentMask)
	{
		if( m_stateListener != null )
		{
			m_stateListener.onBleStateChange(m_mngr, oldStateBits, newStateBits, intentMask);
		}
	}
	
	@Override public String toString()
	{
		return super.toString(BleState.values());
	}
}
