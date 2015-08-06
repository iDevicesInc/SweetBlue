package com.idevicesinc.sweetblue;

import android.os.Handler;

/**
 * 
 * 
 *
 */
class P_WrappingUhOhListener extends PA_CallbackWrapper implements BleManager.UhOhListener
{
	private final BleManager.UhOhListener m_listener;
	
	P_WrappingUhOhListener(BleManager.UhOhListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
	}

	@Override public void onEvent(final UhOhEvent event)
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
