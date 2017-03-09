package com.idevicesinc.sweetblue;

import java.util.HashMap;

import com.idevicesinc.sweetblue.BleManager.UhOhListener;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOhEvent;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;


/**
 * 
 * 
 *
 */
final class P_UhOhThrottler
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
		m_uhOhListener = listener;
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
			UhOhEvent event = new UhOhEvent(m_mngr, reason);
			m_uhOhListener.onEvent(event);
		}
	}
	
	void update(double timeStep)
	{
		m_timeTracker += timeStep;
	}
}
