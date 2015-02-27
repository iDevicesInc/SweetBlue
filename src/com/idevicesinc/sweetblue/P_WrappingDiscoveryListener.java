package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.State;

import android.os.Handler;

/**
 * 
 * 
 *
 */
class P_WrappingDiscoveryListener extends PA_CallbackWrapper implements BleManager.DiscoveryListener
{
	final BleManager.DiscoveryListener m_listener;
	
	P_WrappingDiscoveryListener(BleManager.DiscoveryListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
	}
	
	@Override public void onEvent(final DiscoveryEvent event)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_listener.onEvent(event);
				}
			});
		}
		else
		{
			m_listener.onEvent(event);
		}
	}
}
