package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleManager.StateListener.ChangeEvent;

import android.transition.ChangeBounds;


/**
 * 
 * 
 *
 */
class P_BleStateTracker extends PA_StateTracker
{
	private BleManager.StateListener m_stateListener;
	private final BleManager m_mngr;
	
	P_BleStateTracker(BleManager mngr)
	{
		super(mngr.getLogger(), BleManagerState.values());
		
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

	@Override protected void onStateChange(int oldStateBits, int newStateBits, int intentMask, int status)
	{
		if( m_stateListener != null )
		{
			final ChangeEvent event = new ChangeEvent(m_mngr, oldStateBits, newStateBits, intentMask);
			m_stateListener.onStateChange(event);
		}
	}
	
	@Override public String toString()
	{
		return super.toString(BleManagerState.values());
	}
}
