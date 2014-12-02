package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;

/**
 * 
 * @author dougkoellmer
 */
class P_RssiPollManager
{
	private static final double DISABLE_TIMER = -1.0;
	private static final double ENABLE_TIMER = 0.0;
	
	private final BleDevice m_device;
	private double m_timeTracker = DISABLE_TIMER;
	private double m_interval = 0.0;
	
	private P_WrappingReadWriteListener m_listener;
	
	P_RssiPollManager(BleDevice device)
	{
		m_device = device;
		
		stop();
	}
	
	void start(double interval, P_WrappingReadWriteListener listener_nullable)
	{
		if( interval <= 0.0 )
		{
			return;
		}
		
		m_timeTracker = ENABLE_TIMER;
		m_interval = interval;
		m_listener = listener_nullable;
	}
	
	void stop()
	{
		m_listener = null;
		m_interval = DISABLE_TIMER;
		m_timeTracker = DISABLE_TIMER;
	}
	
	void update(double timestep)
	{
		if( m_timeTracker == DISABLE_TIMER )  return;
		
		m_timeTracker += timestep;
		
		if( m_timeTracker >= m_interval )
		{
			if( m_device.is(BleDeviceState.CONNECTED) )
			{
				m_device.readRssi_internal(Type.POLL, m_listener);	
			}
				
			m_timeTracker = ENABLE_TIMER;
		}
	}
}
