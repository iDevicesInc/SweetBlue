package com.idevicesinc.sweetblue;

import java.util.ArrayList;

import android.os.Handler;

/**
 * 
 * 
 *
 */
class P_WrappingNukeListener extends PA_CallbackWrapper implements BleManager.NukeEndListener
{
	private final ArrayList<BleManager.NukeEndListener> m_listeners = new ArrayList<BleManager.NukeEndListener>();
	
	P_WrappingNukeListener(BleManager.NukeEndListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);

		m_listeners.add(listener);
	}
	
	public void addListener(BleManager.NukeEndListener listener)
	{
		m_listeners.add(listener);
	}

	@Override public void onNukeEnded(final BleManager manager)
	{
		final Runnable runnable = new Runnable()
		{
			@Override public void run()
			{
				for( int i = 0; i < m_listeners.size(); i++ )
				{
					m_listeners.get(i).onNukeEnded(manager);
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
