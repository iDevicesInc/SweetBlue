package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING_OVERALL;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;

import java.util.ArrayList;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.ConnectionFailEvent;
import com.idevicesinc.sweetblue.BleNode.ConnectionFailListener.Please;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;

final class P_ConnectionFailManager
{
	private final BleDevice m_device;
	
	private ConnectionFailListener m_connectionFailListener = null;
	
	private int m_failCount = 0;
	private BleDeviceState m_highestStateReached_total = null;
	
	private Long m_timeOfFirstConnect = null;
	private Long m_timeOfLastConnectFail = null;
	private Integer m_pendingConnectionRetry = null;

	// This boolean is here to prevent trying to reconnect when we've fallen out of reconnecting long term
	private boolean m_triedReconnectingLongTerm = false;

	private final ArrayList<ConnectionFailEvent> m_history = new ArrayList<ConnectionFailEvent>();
	
	P_ConnectionFailManager(BleDevice device)
	{
		m_device = device;
		
		resetFailCount();
	}

	void onLongTermTimedOut()
	{
		m_triedReconnectingLongTerm = true;
	}
	
	void onExplicitDisconnect()
	{
		resetFailCount();
	}

	boolean hasPendingConnectionFailEvent()
	{
		return m_pendingConnectionRetry != null;
	}

	int getPendingConnectionFailRetry()
	{
		return m_pendingConnectionRetry;
	}

	void clearPendingRetry()
	{
		m_pendingConnectionRetry = null;
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
		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				m_failCount = 0;
				m_highestStateReached_total = null;
				m_timeOfFirstConnect = m_timeOfLastConnectFail = null;
				m_history.clear();
				m_triedReconnectingLongTerm = false;
			}
		});
	}
	
	int getRetryCount()
	{
		int retryCount = m_failCount;
		
		return retryCount;
	}

	int/*__PE_Please*/ onConnectionFailed(ConnectionFailListener.Status reason_nullable, ConnectionFailListener.Timing timing, boolean isAttemptingReconnect_longTerm, int gattStatus, int bondFailReason, BleDeviceState highestStateReached, ConnectionFailListener.AutoConnectUsage autoConnectUsage, ReadWriteListener.ReadWriteEvent txnFailReason)
	{
		if( reason_nullable == null )  return ConnectionFailListener.Please.PE_Please_DO_NOT_RETRY;
		
		final long currentTime = System.currentTimeMillis();
		
		//--- DRK > Can be null if this is a spontaneous connect (can happen with autoConnect sometimes for example).
		m_timeOfFirstConnect = m_timeOfFirstConnect != null ? m_timeOfFirstConnect : currentTime;
		final Long timeOfLastConnectFail = m_timeOfLastConnectFail != null ? m_timeOfLastConnectFail : m_timeOfFirstConnect;
		final Interval attemptTime_latest = Interval.delta(timeOfLastConnectFail, currentTime);
		final Interval attemptTime_total = Interval.delta(m_timeOfFirstConnect, currentTime);
		
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
			highestStateReached, m_highestStateReached_total, autoConnectUsage, bondFailReason, txnFailReason
		);

		addToHistory(moreInfo);
		
		//--- DRK > Not invoking callback if we're attempting short-term reconnect.
		int retryChoice__PE_Please = m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) ? Please.PE_Please_DO_NOT_RETRY : invokeCallback(moreInfo);
		
		//--- DRK > Retry choice doesn't matter if we're attempting reconnect.
		retryChoice__PE_Please = !isAttemptingReconnect_longTerm ? retryChoice__PE_Please : Please.PE_Please_DO_NOT_RETRY;
		
		//--- DRK > Disabling retry if app-land decided to call connect() themselves in fail callback...hopefully fringe but must check for now.
		//--- RB > Commenting this out for now. If the user calls connect in the fail callback, it gets posted to the update thread, so this shouldn't
		// be an issue anymore. Right now with the new changes to threading, this is causing issues (so a reconnect attempt doesn't happen when it should)
//		retryChoice__PE_Please = m_device.is_internal(BleDeviceState.CONNECTING_OVERALL) ? Please.PE_Please_DO_NOT_RETRY : retryChoice__PE_Please;
		
		if( reason_nullable != null && reason_nullable.wasCancelled() )
		{
			retryChoice__PE_Please = Please.PE_Please_DO_NOT_RETRY;
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
					m_triedReconnectingLongTerm = true;
				}
				else if( m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )
				{
					retryChoice__PE_Please = Please.PE_Please_DO_NOT_RETRY;
					m_device.onNativeDisconnect(/*wasExplicit=*/false, gattStatusOfOriginalDisconnect, /*doShortTermReconnect=*/false, /*saveLastDisconnect=*/true);
				}
			}
		}

		final boolean retryConnectOverall = BleDeviceConfig.bool(m_device.conf_device().connectFailRetryConnectingOverall, m_device.conf_mngr().connectFailRetryConnectingOverall);

		if( !m_triedReconnectingLongTerm && retryChoice__PE_Please != Please.PE_Please_NULL && Please.isRetry(retryChoice__PE_Please) && !m_device.is(CONNECTED))
		{
			if (m_device.isAny_internal(CONNECTED, CONNECTING, CONNECTING_OVERALL))
			{
				m_device.setStateToDisconnected(isAttemptingReconnect_longTerm, true, E_Intent.UNINTENTIONAL, gattStatus, /*forceMainStateTracker=*/false, P_BondManager.OVERRIDE_EMPTY_STATES);
			}
			m_device.attemptReconnect();
		}
		else
		{
			if (!m_device.is(CONNECTED))
			{
				m_failCount = 0;
			}
			else
			{
				m_pendingConnectionRetry = retryChoice__PE_Please;
			}
			if (m_device.isAny_internal(CONNECTED, CONNECTING, CONNECTING_OVERALL))
			{
				boolean isretryConnectOverall = Please.isRetry(retryChoice__PE_Please) && retryConnectOverall;
				m_device.setStateToDisconnected(isAttemptingReconnect_longTerm, isretryConnectOverall, E_Intent.UNINTENTIONAL, gattStatus, /*forceMainStateTracker=*/false, P_BondManager.OVERRIDE_EMPTY_STATES);
			}
		}

		return retryChoice__PE_Please;
	}

	ArrayList<ConnectionFailEvent> getHistory()
	{
		return new ArrayList<>(m_history);
	}

	private void addToHistory(ConnectionFailEvent event)
	{
		int maxSize = Math.max(BleDeviceConfig.integer(m_device.conf_device().maxConnectionFailHistorySize, m_device.conf_mngr().maxConnectionFailHistorySize), 1);
		if (m_history.size() >= maxSize)
		{
			m_history.remove(0);
		}
		m_history.add(event);
	}
	
	int/*__PE_Please*/ invokeCallback(final ConnectionFailEvent moreInfo)
	{
		int retryChoice__PE_Please = Please.PE_Please_NULL;
		
		if( m_connectionFailListener != null )
		{
			final Please please = m_connectionFailListener.onEvent(moreInfo);
			retryChoice__PE_Please = please != null ? please.please() : Please.PE_Please_NULL;
			
			m_device.getManager().getLogger().checkPlease(please, Please.class);
		}
		else if( m_device.getManager().m_defaultConnectionFailListener != null )
		{
			final Please please = m_device.getManager().m_defaultConnectionFailListener.onEvent(moreInfo);
			retryChoice__PE_Please = please != null ? please.please() : Please.PE_Please_NULL;
			
			m_device.getManager().getLogger().checkPlease(please, Please.class);
		}
		else
		{
			final Please please = BleDevice.DEFAULT_CONNECTION_FAIL_LISTENER.onEvent(moreInfo);
			retryChoice__PE_Please = please != null ? please.please() : Please.PE_Please_NULL;

			m_device.getManager().getLogger().checkPlease(please, Please.class);
		}

		retryChoice__PE_Please = retryChoice__PE_Please != Please.PE_Please_NULL ? retryChoice__PE_Please : Please.PE_Please_DO_NOT_RETRY;
		
		return retryChoice__PE_Please;
	}
	
	public void setListener(ConnectionFailListener listener)
	{
		m_connectionFailListener = listener;
	}
}
