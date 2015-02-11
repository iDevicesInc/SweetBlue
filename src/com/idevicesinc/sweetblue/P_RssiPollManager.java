package com.idevicesinc.sweetblue;

import android.os.Handler;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;

/**
 * 
 * 
 */
class P_RssiPollManager
{
	private static class CustomListener extends P_WrappingReadWriteListener
	{
		private final P_RssiPollManager m_thisMngr;
		
		CustomListener(P_RssiPollManager thisMngr, ReadWriteListener listener, Handler handler, boolean postToMain)
		{
			super(listener, handler, postToMain);
			
			m_thisMngr = thisMngr;
		}
		
		@Override public void onResult(final Result result)
		{
			m_thisMngr.m_waitingOnResponse = false;
			
			if( m_thisMngr.m_timeTracker >= ENABLE_TIMER )
			{
				m_thisMngr.m_timeTracker = ENABLE_TIMER;
			}
			
			super.onResult(result);
		}
	}
	
	private static final double DISABLE_TIMER = -1.0;
	private static final double ENABLE_TIMER = 0.0;
	
	private final BleDevice m_device;
	private double m_timeTracker = DISABLE_TIMER;
	private double m_interval = 0.0;
	private boolean m_waitingOnResponse = false;
	
	private P_WrappingReadWriteListener m_listener;
	
	P_RssiPollManager(BleDevice device)
	{
		m_device = device;
		
		stop();
	}
	
	void start(double interval, ReadWriteListener listener_nullable)
	{
		if( interval <= 0.0 )
		{
			return;
		}
		
		m_timeTracker = ENABLE_TIMER;
		m_interval = interval;
		m_listener = new CustomListener(this, listener_nullable, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
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
		if( m_timeTracker == DISABLE_TIMER )  return;
		
		m_timeTracker += timestep;
		
		if( m_timeTracker >= m_interval && !m_waitingOnResponse)
		{
			if( m_device.is(BleDeviceState.CONNECTED) )
			{
				m_waitingOnResponse = true;
				m_device.readRssi_internal(Type.POLL, m_listener);	
			}
		}
	}
}
