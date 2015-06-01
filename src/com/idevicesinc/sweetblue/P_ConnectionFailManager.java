package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;

import java.util.ArrayList;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.AutoConnectUsage;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.ConnectionFailEvent;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Please.PE_Please;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Please;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;

class P_ConnectionFailManager
{
	private final BleDevice m_device;
	
	private ConnectionFailListener m_connectionFailListener = BleDevice.DEFAULT_CONNECTION_FAIL_LISTENER;
	
	private int m_failCount = 0;
	private BleDeviceState m_highestStateReached_total = null;
	
	private Long m_timeOfFirstConnect = null;
	private Long m_timeOfLastConnectFail = null;
	
	private final ArrayList<ConnectionFailEvent> m_history = new ArrayList<ConnectionFailEvent>();
	
	P_ConnectionFailManager(BleDevice device)
	{
		m_device = device;
		
		resetFailCount();
	}
	
	void onExplicitDisconnect()
	{
		resetFailCount();
	}
	
	void onFullyInitialized()
	{
		resetFailCount();
	}
	
	void onExplicitConnectionStarted()
	{
		resetFailCount();
		
		m_timeOfFirstConnect = System.currentTimeMillis();
	}
	
	private void resetFailCount()
	{
		m_failCount = 0;
		m_highestStateReached_total = null;
		m_timeOfFirstConnect = m_timeOfLastConnectFail = null;
		m_history.clear();
	}
	
	int getRetryCount()
	{
		int retryCount = m_failCount;
		
		return retryCount;
	}
	
	PE_Please onConnectionFailed(ConnectionFailListener.Status reason_nullable, ConnectionFailListener.Timing timing, boolean isAttemptingReconnect_longTerm, int gattStatus, int bondFailReason, BleDeviceState highestStateReached, AutoConnectUsage autoConnectUsage, ReadWriteListener.ReadWriteEvent txnFailReason)
	{
		if( reason_nullable == null )  return PE_Please.DO_NOT_RETRY;
		
		long currentTime = System.currentTimeMillis();
		
		//--- DRK > Can be null if this is a spontaneous connect (can happen with autoConnect sometimes for example).
		m_timeOfFirstConnect = m_timeOfFirstConnect != null ? m_timeOfFirstConnect : currentTime;
		Long timeOfLastConnectFail = m_timeOfLastConnectFail != null ? m_timeOfLastConnectFail : m_timeOfFirstConnect;
		Interval attemptTime_latest = Interval.delta(timeOfLastConnectFail, currentTime);
		Interval attemptTime_total = Interval.delta(m_timeOfFirstConnect, currentTime);
		
		m_device.getManager().getLogger().w(reason_nullable+", timing="+timing);
		
		if( isAttemptingReconnect_longTerm )
		{
			m_failCount = 1;
		}
		else
		{
			m_failCount++;
		}		
		
		if( m_highestStateReached_total == null )
		{
			m_highestStateReached_total = highestStateReached;
		}
		else
		{
			if( highestStateReached != null && highestStateReached.getConnectionOrdinal() > m_highestStateReached_total.getConnectionOrdinal() )
			{
				m_highestStateReached_total = highestStateReached;
			}
		}
		
		final ConnectionFailEvent moreInfo = new ConnectionFailEvent
		(
			m_device, reason_nullable, timing, m_failCount, attemptTime_latest, attemptTime_total, gattStatus,
			highestStateReached, m_highestStateReached_total, autoConnectUsage, bondFailReason, txnFailReason,
			m_history
		);
		
		m_history.add(moreInfo);
		
		//--- DRK > Not invoking callback if we're attempting short-term reconnect.
		PE_Please retryChoice = m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) ? PE_Please.DO_NOT_RETRY : invokeCallback(moreInfo);
		
		//--- DRK > Retry choice doesn't matter if we're attempting reconnect.
		retryChoice = !isAttemptingReconnect_longTerm ? retryChoice : PE_Please.DO_NOT_RETRY;
		
		//--- DRK > Disabling retry if app-land decided to call connect() themselves in fail callback...hopefully fringe but must check for now.
		retryChoice = m_device.is_internal(BleDeviceState.CONNECTING_OVERALL) ? PE_Please.DO_NOT_RETRY : retryChoice;
		
		if( reason_nullable != null && reason_nullable.wasCancelled() )
		{
			retryChoice = PE_Please.DO_NOT_RETRY;
		}
		else
		{
			final P_ReconnectManager reconnectMngr = m_device.reconnectMngr();
			final int gattStatusOfOriginalDisconnect = reconnectMngr.gattStatusOfOriginalDisconnect();
			final boolean wasRunning = reconnectMngr.isRunning();
			
			reconnectMngr.onConnectionFailed(moreInfo);
			
			if( wasRunning && !reconnectMngr.isRunning() )
			{
				if( m_device.is(RECONNECTING_LONG_TERM) )
				{
					//--- DRK > State change may be redundant.
					m_device.stateTracker_main().update(E_Intent.UNINTENTIONAL, gattStatus, RECONNECTING_LONG_TERM, false);
				}
				else if( m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )
				{
					retryChoice = PE_Please.DO_NOT_RETRY;
					m_device.onNativeDisconnect(/*wasExplicit=*/false, gattStatusOfOriginalDisconnect, /*doShortTermReconnect=*/false, /*saveLastDisconnect=*/true);
				}
			}
		}
		
		if( retryChoice != null && retryChoice.isRetry() )
		{
			m_device.attemptReconnect();
		}
		else
		{
			m_failCount = 0;
		}
		
		return retryChoice;
	}
	
	PE_Please invokeCallback(final ConnectionFailEvent moreInfo)
	{
		PE_Please retryChoice = null;
		
		if( m_connectionFailListener != null )
		{
			final Please please = m_connectionFailListener.onEvent(moreInfo);
			retryChoice = please != null ? please.please() : null;
			
			m_device.getManager().getLogger().checkPlease(please, Please.class);
		}
		else if( m_device.getManager().m_defaultConnectionFailListener != null )
		{
			final Please please = m_device.getManager().m_defaultConnectionFailListener.onEvent(moreInfo);
			retryChoice = please != null ? please.please() : null;
			
			m_device.getManager().getLogger().checkPlease(please, Please.class);
		}
		
		retryChoice = retryChoice != null ? retryChoice : PE_Please.DO_NOT_RETRY;
		
		return retryChoice;
	}
	
	public void setListener(ConnectionFailListener listener)
	{
		synchronized (m_device.m_threadLock)
		{
			if( listener != null )
			{
				m_connectionFailListener = new P_WrappingDeviceStateListener(listener, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
			}
			else
			{
				m_connectionFailListener = null;
			}
		}
	}
}
