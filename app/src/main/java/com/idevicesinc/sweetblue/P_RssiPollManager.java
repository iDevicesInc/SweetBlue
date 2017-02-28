package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;


final class P_RssiPollManager
{
	private static class CustomListener extends P_WrappingReadWriteListener
	{
		private final P_RssiPollManager m_pollMngr;
		
		CustomListener(P_RssiPollManager thisMngr, ReadWriteListener listener, P_SweetHandler handler, boolean postToMain)
		{
			super(listener, handler, postToMain);
			
			m_pollMngr = thisMngr;
		}
		
		@Override public void onEvent(final ReadWriteEvent event)
		{
			m_pollMngr.m_waitingOnResponse = false;
			
			if( m_pollMngr.m_timeTracker >= ENABLE_TIMER )
			{
				m_pollMngr.m_timeTracker = ENABLE_TIMER;
			}
			
			super.onEvent(event);
		}
	}
	
	private static final double DISABLE_TIMER = -1.0;
	private static final double ENABLE_TIMER = 0.0;
	
	private final BleDevice m_device;
	private double m_timeTracker = DISABLE_TIMER;
	private double m_interval = 0.0;
	private boolean m_waitingOnResponse = false;
	
	private ReadWriteListener m_listener;
	
	P_RssiPollManager(BleDevice device)
	{
		m_device = device;
		
		stop();
	}
	
	void start(double interval, ReadWriteListener listener_nullable)
	{
		if( interval > 0.0 )
		{
			m_timeTracker = ENABLE_TIMER;
			m_interval = interval;
			m_listener = new CustomListener(this, listener_nullable, m_device.getManager().getPostManager().getUIHandler(), m_device.conf_mngr().postCallbacksToMainThread);
		}
	}
	
	boolean isRunning()
	{
		return m_timeTracker != DISABLE_TIMER;
	}
	
	void stop()
	{
		m_listener = null;
		m_interval = DISABLE_TIMER;
		m_timeTracker = DISABLE_TIMER;
		m_waitingOnResponse = false;
	}
	
	void update(double timestep)
	{
		if( m_timeTracker != DISABLE_TIMER )
		{
			m_timeTracker += timestep;

			if( m_timeTracker >= m_interval && !m_waitingOnResponse )
			{
				if( m_device.is(BleDeviceState.INITIALIZED) )
				{
					m_waitingOnResponse = true;
					m_device.readRssi_internal(Type.POLL, m_listener);
				}
			}
		}
	}
}
