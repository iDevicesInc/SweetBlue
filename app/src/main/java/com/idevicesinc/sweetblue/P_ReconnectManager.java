package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.BleNodeConfig.*;

final class P_ReconnectManager
{
	private static final double NOT_RUNNING = -1.0;
	private final BleDevice m_device;
	
	private double m_totalTime;
	private int m_attemptCount;
	private double m_delay = 0.0;
	private double m_timeTracker = NOT_RUNNING;
	
	private int m_gattStatusOfOriginalDisconnect = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	
	private ConnectionFailListener.ConnectionFailEvent m_connectionFailEvent;
	
	private final boolean m_isShortTerm;
	
	private static final BleNodeConfig.ReconnectFilter.ReconnectEvent EVENT = new BleNodeConfig.ReconnectFilter.ReconnectEvent();
	
	P_ReconnectManager(final BleDevice device, final boolean isShortTerm)
	{
		m_device = device;
		
		m_isShortTerm = isShortTerm;
		
		m_connectionFailEvent = m_device.NULL_CONNECTIONFAIL_INFO();
	}
	
	void attemptStart(final int gattStatusOfDisconnect)
	{
		m_totalTime = 0.0;
		m_attemptCount = 0;
		m_connectionFailEvent = m_device.NULL_CONNECTIONFAIL_INFO();
		
		m_delay = getNextTime(m_device.NULL_CONNECTIONFAIL_INFO());
		
		if( m_delay < 0.0 )
		{
			m_timeTracker = NOT_RUNNING;
			m_gattStatusOfOriginalDisconnect = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
		}
		else
		{
			if( !isRunning() )
			{
				m_device.getManager().pushWakeLock();
			}
			
			m_timeTracker = 0.0;
			m_gattStatusOfOriginalDisconnect = gattStatusOfDisconnect;
		}
		
		//--- DRK > If delay is zero we still wait until the first time step to actually attempt first (re)connect.
		//---		May change in future for API-consistency's sake. 
	}
	
	int gattStatusOfOriginalDisconnect()
	{
		return m_gattStatusOfOriginalDisconnect;
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
	
	private ReconnectFilter getFilter()
	{
		final ReconnectFilter filter = m_device.conf_device().reconnectFilter;

		return filter != null ? filter : m_device.conf_mngr().reconnectFilter;
	}
	
	private double getNextTime(ConnectionFailListener.ConnectionFailEvent connectionFailInfo)
	{
		final ReconnectFilter filter = getFilter();
		
		if( filter == null )
		{
			return Interval.DISABLED.secs();
		}
		else
		{
			final ReconnectFilter.Type type = m_isShortTerm ? ReconnectFilter.Type.SHORT_TERM__SHOULD_TRY_AGAIN : ReconnectFilter.Type.LONG_TERM__SHOULD_TRY_AGAIN;

			EVENT.init(m_device, m_device.getMacAddress(), m_attemptCount, Interval.secs(m_totalTime), Interval.secs(m_delay), connectionFailInfo, type);
			final ReconnectFilter.Please please = filter.onEvent(EVENT);
			
			m_device.getManager().getLogger().checkPlease(please, ReconnectFilter.Please.class);

			if( false == please.shouldPersist() )
			{
				return Interval.DISABLED.secs();
			}
			else
			{
				final Interval delay = please != null ? please.interval() : null;

				return delay != null ? delay.secs() : Interval.DISABLED.secs();
			}
		}
	}
	
	void onConnectionFailed(final ConnectionFailListener.ConnectionFailEvent connectionFailInfo)
	{
		if( !isRunning() )
		{
			return;
		}
		
		m_attemptCount++;
		m_timeTracker = 0.0;
		
		final double delay = getNextTime(connectionFailInfo);
		
		if( delay < 0.0 )
		{
			stop();
		}
		else
		{
			m_connectionFailEvent = connectionFailInfo;
			m_delay = delay;
			m_timeTracker = 0.0;
		}
	}
	
	void update(double timeStep)
	{
		if( !isRunning() )  return;

		m_totalTime += timeStep;
		
		if( !m_isShortTerm && !m_device.is(BleDeviceState.RECONNECTING_LONG_TERM) )  return;
		if( m_isShortTerm && !m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )  return;
		
		m_timeTracker += timeStep;
		
		doPersistCheck();
		
		if( !/*still*/isRunning() )  return;
		
		if( m_timeTracker >= m_delay )
		{
			if( !m_device.is_internal(BleDeviceState.CONNECTING_OVERALL) )
			{
				m_device.attemptReconnect();
			}
		}
	}
	
	private void doPersistCheck()
	{
		ReconnectFilter persistFilter = getFilter();
		
		if( persistFilter == null )  return;

		final ReconnectFilter.Type type = m_isShortTerm ? ReconnectFilter.Type.SHORT_TERM__SHOULD_CONTINUE : ReconnectFilter.Type.LONG_TERM__SHOULD_CONTINUE;
		
		EVENT.init(m_device, m_device.getMacAddress(), m_attemptCount, Interval.secs(m_totalTime), Interval.secs(m_delay), m_connectionFailEvent, type);
		
		final ReconnectFilter.Please please = persistFilter.onEvent(EVENT);
		
		m_device.getManager().getLogger().checkPlease(please, ReconnectFilter.Please.class);
		
		if( please == null || false == please.shouldPersist() )
		{
			final int gattStatusOfOriginalDisconnect = gattStatusOfOriginalDisconnect();
			
			stop();
			
			if( m_isShortTerm )
			{
				m_device.onNativeDisconnect(/*wasExplicit=*/false, gattStatusOfOriginalDisconnect, /*doShortTermReconnect=*/false, /*saveLastDisconnect=*/true);
			}
			else
			{
				m_device.stateTracker_main().update(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, RECONNECTING_LONG_TERM, false);
				m_device.onLongTermReconnectTimeOut();
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
		m_connectionFailEvent = m_device.NULL_CONNECTIONFAIL_INFO();
		m_gattStatusOfOriginalDisconnect = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	}
}
