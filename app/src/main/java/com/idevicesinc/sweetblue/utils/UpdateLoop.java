package com.idevicesinc.sweetblue.utils;

import android.os.Handler;
import android.os.Looper;

import com.idevicesinc.sweetblue.PI_UpdateLoop;

/**
 * Zero-dependency (besides Android) utility class for creating an update loop.
 */
@Deprecated
public class UpdateLoop implements PI_UpdateLoop
{
	
	private final Runnable m_autoUpdateRunnable = new Runnable()
	{
		@Override public void run()
		{
			long currentTime = System.currentTimeMillis();
			double timeStep = ((double) currentTime - m_lastAutoUpdateTime)/1000.0;
			
			timeStep = timeStep <= 0.0 ? .00001 : timeStep;
			timeStep = timeStep > 1.0 ? 1.0 : timeStep;
			
			m_callback.onUpdate(timeStep);
			
			m_lastAutoUpdateTime = currentTime;

			postUpdate();
		}
	};
	
	private boolean m_isRunning = false;
	private long m_lastAutoUpdateTime = 0;
	private long m_autoUpdateRate = 0;
	private Handler m_handler;
	private final Callback m_callback;

	
	public static UpdateLoop newMainThreadLoop(Callback callback)
	{
		return new UpdateLoop(callback, true);
	}
	
	public static UpdateLoop newAnonThreadLoop(Callback callback)
	{
		return new UpdateLoop(callback, false);
	}

	public static UpdateLoop newAnonThreadLoop()
	{
		return new UpdateLoop(new Callback()
		{
			@Override public void onUpdate(double timestep_seconds)
			{

			}
		}, false);
	}
	
	private UpdateLoop(Callback callback, boolean runOnMainThread)
	{
		m_callback = callback;
		
		initHandler(runOnMainThread);
	}

	@Override
	public boolean isRunning()
	{
		return m_isRunning;
	}
	
	private void initHandler(boolean runOnMainThread)
	{
		if( runOnMainThread )
		{
			m_handler = new Handler(Looper.getMainLooper());
		}
		else
		{
			final Thread thread = new Thread()
			{
				@Override public void run()
				{
					Looper.prepare();
					m_handler = new Handler(Looper.myLooper());
					
					if( m_isRunning )
					{
						postUpdate();
					}
					
					Looper.loop();
				}
			};
			
			thread.start();
		}
	}
	
	private void postUpdate()
	{
		if( m_handler != null )
		{
			m_handler.postDelayed(m_autoUpdateRunnable, m_autoUpdateRate);
		}
	}

	@Override
	public void start(double updateRate)
	{
		if( updateRate == 0.0 )  return;
		
		if( /*already*/m_isRunning )
		{
			stop();
		}
		
		m_isRunning = true;
		
		m_autoUpdateRate = (long) (updateRate * 1000);
		m_lastAutoUpdateTime = System.currentTimeMillis();

		postUpdate();

	}

	@Override
	public void stop()
	{
		if( !m_isRunning )  return;
		
		if( m_handler != null )
		{
			m_handler.removeCallbacks(m_autoUpdateRunnable);
		}

		m_isRunning = false;
	}
	
	private void waitForHandler()
	{
		//--- DRK > This can technically take a little time to initialize after 
		//---		this class is constructed so wait for it if needed.
		while(m_handler == null) {}
	}

	@Override
	public void forcePost(Runnable runnable)
	{
		waitForHandler();

		m_handler.postDelayed(runnable, 1);

	}

	@Override
	public Handler getHandler()
	{
		waitForHandler();

		return m_handler;
	}

	@Override
	public boolean postNeeded()
	{
		return m_handler.getLooper().getThread() != Thread.currentThread();
	}

	@Override
	public void postIfNeeded(Runnable runnable)
	{
		waitForHandler();

		m_handler.post(runnable);
	}
}
