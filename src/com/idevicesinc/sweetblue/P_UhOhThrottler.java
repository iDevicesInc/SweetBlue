package com.idevicesinc.sweetblue;

import java.util.HashMap;

import com.idevicesinc.sweetblue.BleManager.UhOhListener;


/**
 * 
 * 
 *
 */
class P_UhOhThrottler
{
	private final HashMap<UhOh, Double> m_lastTimesCalled = new HashMap<UhOh, Double>();
	private UhOhListener m_uhOhListener;
	private final double m_throttle;
	private final BleManager m_mngr;
	private double m_timeTracker = 0.0;
	
	public P_UhOhThrottler(BleManager mngr, double throttle)
	{
		m_mngr = mngr;
		m_throttle = throttle;
	}
	
	public synchronized void setListener(UhOhListener listener)
	{
		if( listener != null )
		{
			m_uhOhListener = new P_WrappingUhOhListener(listener, m_mngr.m_mainThreadHandler, m_mngr.m_config.postCallbacksToMainThread);
		}
		else
		{
			m_uhOhListener = null;
		}
	}
	
	void uhOh(UhOh reason)
	{
		uhOh(reason, m_throttle);
	}
	
	synchronized void uhOh(UhOh reason, double throttle)
	{
		m_mngr.getLogger().w(reason+"");
		
		if( throttle > 0.0 )
		{
			Double lastTimeCalled = m_lastTimesCalled.get(reason);
			
			if( lastTimeCalled != null )
			{
				if( m_timeTracker - lastTimeCalled < throttle )
				{
					return;
				}
			}
		}
		
		if( m_uhOhListener != null ) 
		{
			m_lastTimesCalled.put(reason, m_timeTracker);
			m_uhOhListener.onUhOh(m_mngr, reason);
		}
	}
	
	synchronized void update(double timeStep)
	{
		m_timeTracker += timeStep;
	}
}
