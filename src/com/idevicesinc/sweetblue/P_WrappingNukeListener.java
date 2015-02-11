package com.idevicesinc.sweetblue;

import java.util.ArrayList;

import android.os.Handler;

/**
 * 
 * 
 *
 */
class P_WrappingNukeListener extends PA_CallbackWrapper implements BleManager.NukeListener
{
	private final ArrayList<BleManager.NukeListener> m_listeners = new ArrayList<BleManager.NukeListener>();
	
	P_WrappingNukeListener(BleManager.NukeListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);

		m_listeners.add(listener);
	}
	
	public void addListener(BleManager.NukeListener listener)
	{
		m_listeners.add(listener);
	}

	@Override public void onNukeEvent(final NukeEvent event)
	{
		final Runnable runnable = new Runnable()
		{
			@Override public void run()
			{
				for( int i = 0; i < m_listeners.size(); i++ )
				{
					m_listeners.get(i).onNukeEvent(event);
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
