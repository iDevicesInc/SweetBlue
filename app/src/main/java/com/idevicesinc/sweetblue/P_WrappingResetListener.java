package com.idevicesinc.sweetblue;

import java.util.ArrayList;

import android.os.Handler;

/**
 * 
 * 
 *
 */
final class P_WrappingResetListener extends PA_CallbackWrapper implements BleManager.ResetListener
{
	private final ArrayList<BleManager.ResetListener> m_listeners = new ArrayList<BleManager.ResetListener>();
	
	P_WrappingResetListener(BleManager.ResetListener listener, P_SweetHandler handler, boolean postToMain)
	{
		super(handler, postToMain);

		m_listeners.add(listener);
	}
	
	public void addListener(BleManager.ResetListener listener)
	{
		m_listeners.add(listener);
	}

	@Override public void onEvent(final ResetEvent event)
	{
		final Runnable runnable = new Runnable()
		{
			@Override public void run()
			{
				for( int i = 0; i < m_listeners.size(); i++ )
				{
					m_listeners.get(i).onEvent(event);
				}
			}
		};
		
		if( postToMain() )
		{
			m_handler.post(runnable);
		}
		else
		{
			runnable.run();
		}
	}
}
