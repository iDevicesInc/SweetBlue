package com.idevicesinc.sweetblue;

import java.util.List;
import java.util.UUID;

import android.os.Handler;

/**
 * 
 * @author dougkoellmer
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

	@Override public void onUhOh(final BleManager manager, final UhOh reason)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_listener.onUhOh(manager, reason);
				}
			});
		}
		else
		{
			m_listener.onUhOh(manager, reason);
		}
	}
}
