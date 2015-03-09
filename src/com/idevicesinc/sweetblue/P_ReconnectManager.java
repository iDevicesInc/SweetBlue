package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Timing;
import com.idevicesinc.sweetblue.BleDeviceConfig.ReconnectPersistFilter;
import com.idevicesinc.sweetblue.BleDeviceConfig.ReconnectPersistFilter.ReconnectPersistEvent;
import com.idevicesinc.sweetblue.BleDeviceConfig.ReconnectRequestFilter;
import com.idevicesinc.sweetblue.BleDeviceConfig.ReconnectRequestFilter.Please;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;

class P_ReconnectManager
{
	private static final double NOT_RUNNING = -1.0;
	private final BleDevice m_device;
	
	private double m_totalTime;
	private int m_attemptCount;
	private double m_delay = 0.0;
	private double m_timeTracker = NOT_RUNNING;
	
	private ConnectionFailListener.ConnectionFailEvent m_connectionFailInfo;
	
	private final boolean m_isShortTerm;
	
	private static final ReconnectPersistEvent PERSIST_EVENT = new ReconnectPersistEvent();
	
	P_ReconnectManager(final BleDevice device, final boolean isShortTerm)
	{
		m_device = device;
		
		m_isShortTerm = isShortTerm;
		
		m_connectionFailInfo = m_device.NULL_CONNECTIONFAIL_INFO();
	}
	
	void attemptStart()
	{
		if( !isRunning() )
		{
			m_device.getManager().pushWakeLock();
		}
		
		m_totalTime = 0.0;
		m_attemptCount = 0;
		m_delay = 0.0;
		m_timeTracker = 0.0;
		
		m_delay = getNextTime(m_device.NULL_CONNECTIONFAIL_INFO());
		
		if( m_delay < 0.0 )
		{
			m_timeTracker = NOT_RUNNING;
		}
		
		//--- DRK > If delay is zero we still wait until the first time step to actually attempt first (re)connect.
		//---		May change in future for API-consistency's sake. 
	}
	
	boolean isRunning()
	{
		if( m_timeTracker >= 0.0 )
		{
//			m_device.getManager().ASSERT(m_device.is(E_DeviceState.ATTEMPTING_RECONNECT));
			
			return true;
		}
		else
		{
//			m_device.getManager().ASSERT(!m_device.is(E_DeviceState.ATTEMPTING_RECONNECT));
			
			return false;
		}
	}
	
	private ReconnectRequestFilter getRequestFilter()
	{
		if( m_isShortTerm )
		{
			final BleDeviceConfig.ReconnectRequestFilter filter = m_device.conf_device().reconnectRequestFilter_shortTerm;
			return filter != null ? filter : m_device.conf_mngr().reconnectRequestFilter_shortTerm;
		}
		else
		{
			final BleDeviceConfig.ReconnectRequestFilter filter = m_device.conf_device().reconnectRequestFilter_longTerm;
			return filter != null ? filter : m_device.conf_mngr().reconnectRequestFilter_longTerm;
		}
	}
	
	private ReconnectPersistFilter getPersistFilter()
	{
		if( m_isShortTerm )
		{
			final BleDeviceConfig.ReconnectPersistFilter filter = m_device.conf_device().reconnectPersistFilter_shortTerm;
			return filter != null ? filter : m_device.conf_mngr().reconnectPersistFilter_shortTerm;
		}
		else
		{
			final BleDeviceConfig.ReconnectPersistFilter filter = m_device.conf_device().reconnectPersistFilter_longTerm;
			return filter != null ? filter : m_device.conf_mngr().reconnectPersistFilter_longTerm;
		}
	}
	
	private double getNextTime(ConnectionFailListener.ConnectionFailEvent connectionFailInfo)
	{
		final BleDeviceConfig.ReconnectRequestFilter filter = getRequestFilter();
		
		if( filter == null )
		{
			return BleManagerConfig.ReconnectRequestFilter.Please.STOP.secs();
		}
		else
		{
			ReconnectRequestFilter.ReconnectRequestEvent info = new ReconnectRequestFilter.ReconnectRequestEvent(m_device, m_attemptCount, Interval.secs(m_totalTime), Interval.secs(m_delay), connectionFailInfo);
			Please please = filter.onEvent(info);
			
			Interval delay = please != null ? please.getInterval() : null;
			delay = delay != null ? delay : BleManagerConfig.ReconnectRequestFilter.Please.STOP;
			
			return delay.secs();
		}
	}
	
	boolean onConnectionFailed(final ConnectionFailListener.ConnectionFailEvent connectionFailInfo)
	{
		if( !isRunning() )
		{
			return false;
		}
		
		m_attemptCount++;

		m_timeTracker = 0.0;
		
		double delay = getNextTime(connectionFailInfo);
		
		if( delay < 0.0 )
		{
			stop();
			
			return false;
		}
		else
		{
			m_connectionFailInfo = connectionFailInfo;
			m_delay = delay;
			m_timeTracker = 0.0;
			
			return true;
		}
	}
	
	void update(double timeStep)
	{
		if( !isRunning() )  return;
		
		m_totalTime += timeStep;
		
		if( !m_device.is(BleDeviceState.DISCONNECTED) )  return;
		
		m_timeTracker += timeStep;
		
		doPersistCheck();
		
		if( !/*still*/isRunning() )  return;
		
		if( m_timeTracker >= m_delay )
		{
			if( !m_device.is(BleDeviceState.CONNECTING_OVERALL) )
			{
				m_device.attemptReconnect();
			}
		}
	}
	
	private void doPersistCheck()
	{
		ReconnectPersistFilter persistFilter = getPersistFilter();
		
		if( persistFilter == null )  return;
		
		PERSIST_EVENT.init(m_device, m_attemptCount, Interval.secs(m_totalTime), Interval.secs(m_delay), m_connectionFailInfo);
		
		ReconnectPersistFilter.Please please = persistFilter.onEvent(PERSIST_EVENT);
		
		if( please == null || !please.shouldPersist() )
		{
			stop();
			
			if( m_isShortTerm )
			{
				m_device.disconnectWithReason(/*priority=*/null, Status.EXPLICIT_DISCONNECT, Timing.NOT_APPLICABLE, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE, BleDeviceConfig.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_RESULT());
			}
			else
			{
				m_device.stateTracker_main().update(E_Intent.UNINTENTIONAL, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE, RECONNECTING_LONG_TERM, false);
			}
		}
	}
	
	void stop()
	{
		if( isRunning() )
		{
			m_device.getManager().popWakeLock();
		}
		
		m_timeTracker = NOT_RUNNING;
		m_attemptCount = 0;
		m_totalTime = 0.0;
		m_connectionFailInfo = m_device.NULL_CONNECTIONFAIL_INFO();
	}
}
