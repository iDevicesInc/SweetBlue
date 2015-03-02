package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.ATTEMPTING_RECONNECT;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.AutoConnectUsage;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.ConnectionFailEvent;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Please.PE_Please;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Please;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 */
class P_ConnectionFailManager
{
	private final BleDevice m_device;
	private final P_ReconnectManager m_reconnectMngr;
	
	private ConnectionFailListener m_connectionFailListener = BleDevice.DEFAULT_CONNECTION_FAIL_LISTENER;
	
	private int m_failCount = 0;
	private BleDeviceState m_highestStateReached_total = null;
	
	private Long m_timeOfFirstConnect = null;
	private Long m_timeOfLastConnectFail = null;
	
	P_ConnectionFailManager(BleDevice device, P_ReconnectManager reconnectMngr)
	{
		m_device = device;
		m_reconnectMngr = reconnectMngr;
		
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
	}
	
	int getRetryCount()
	{
		int retryCount = m_failCount;
		
		return retryCount;
	}
	
	PE_Please onConnectionFailed(ConnectionFailListener.Status reason_nullable, ConnectionFailListener.Timing timing, boolean isAttemptingReconnect, int gattStatus, int bondFailReason, BleDeviceState highestStateReached, AutoConnectUsage autoConnectUsage, ReadWriteListener.ReadWriteEvent txnFailReason)
	{
		if( reason_nullable == null )  return PE_Please.DO_NOT_RETRY;
		
		long currentTime = System.currentTimeMillis();
		
		//--- DRK > Can be null if this is a spontaneous connect (can happen with autoConnect sometimes for example).
		m_timeOfFirstConnect = m_timeOfFirstConnect != null ? m_timeOfFirstConnect : currentTime;
		Long timeOfLastConnectFail = m_timeOfLastConnectFail != null ? m_timeOfLastConnectFail : m_timeOfFirstConnect;
		Interval attemptTime_latest = Interval.delta(timeOfLastConnectFail, currentTime);
		Interval attemptTime_total = Interval.delta(m_timeOfFirstConnect, currentTime);
		
		m_device.getManager().getLogger().w(reason_nullable+"");
		
		if( isAttemptingReconnect )
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
			highestStateReached, m_highestStateReached_total, autoConnectUsage, bondFailReason, txnFailReason
		);
		
		PE_Please retryChoice = invokeCallback(moreInfo);
		retryChoice = !isAttemptingReconnect ? retryChoice : PE_Please.DO_NOT_RETRY;
		
		if( reason_nullable != null && reason_nullable.wasCancelled() )
		{
			retryChoice = PE_Please.DO_NOT_RETRY;
		}
		else
		{
			if( !m_reconnectMngr.onConnectionFailed(moreInfo) )
			{
				//--- DRK > State change may be redundant.
				m_device.getStateTracker().update(E_Intent.UNINTENTIONAL, gattStatus, ATTEMPTING_RECONNECT, false);
			}
			
			m_device.getManager().onConnectionFailed();
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
			Please please = m_connectionFailListener.onEvent(moreInfo);
			retryChoice = please != null ? please.please() : null;
		}
		else if( m_device.getManager().m_defaultConnectionFailListener != null )
		{
			Please please = m_device.getManager().m_defaultConnectionFailListener.onEvent(moreInfo);
			retryChoice = please != null ? please.please() : null;
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
