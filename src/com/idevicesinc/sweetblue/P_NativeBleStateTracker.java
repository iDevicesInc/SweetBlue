package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleManager.NativeStateListener.ChangeEvent;


/**
 * 
 * 
 *
 */
class P_NativeBleStateTracker extends PA_StateTracker
{
	private BleManager.NativeStateListener m_stateListener;
	private final BleManager m_mngr;
	
	P_NativeBleStateTracker(BleManager mngr)
	{
		super(mngr.getLogger(), BleManagerState.values());
		
		m_mngr = mngr;
	}
	
	public void setListener(BleManager.NativeStateListener listener)
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
			final ChangeEvent event = new ChangeEvent(m_mngr, oldStateBits, newStateBits, intentMask);
			m_stateListener.onNativeStateChange(event);
		}
	}
}
