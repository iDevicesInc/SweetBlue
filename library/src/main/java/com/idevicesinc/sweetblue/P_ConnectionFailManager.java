package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING_OVERALL;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;
import com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent;
import com.idevicesinc.sweetblue.ReconnectFilter.ConnectFailPlease;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;

import java.util.ArrayList;
import java.util.Stack;


final class P_ConnectionFailManager
{
	private final BleDevice m_device;
	

	private final Stack<DeviceReconnectFilter> m_connectionFailListenerStack;

	private int m_failCount = 0;
	private BleDeviceState m_highestStateReached_total = null;
	
	private Long m_timeOfFirstConnect = null;
	private Long m_timeOfLastConnectFail = null;
	private Integer m_pendingConnectionRetry = null;

	// This boolean is here to prevent trying to reconnect when we've fallen out of reconnecting long term
	private boolean m_triedReconnectingLongTerm = false;

	private final ArrayList<ConnectFailEvent> m_history = new ArrayList<>();
	
	P_ConnectionFailManager(BleDevice device)
	{
		m_device = device;

		m_connectionFailListenerStack = new Stack<>();
		
		resetFailCount();
	}

	final void onLongTermTimedOut()
	{
		m_triedReconnectingLongTerm = true;
	}

	final void onExplicitDisconnect()
	{
		resetFailCount();
	}

	final boolean hasPendingConnectionFailEvent()
	{
		return m_pendingConnectionRetry != null;
	}

	final int getPendingConnectionFailRetry()
	{
		return m_pendingConnectionRetry;
	}

	final void clearPendingRetry()
	{
		m_pendingConnectionRetry = null;
	}

	final void onFullyInitialized()
	{
		resetFailCount();
	}

	final void onExplicitConnectionStarted()
	{
		resetFailCount();

		m_timeOfFirstConnect = System.currentTimeMillis();
	}
	
	private void resetFailCount()
	{
		m_device.getManager().getPostManager().runOrPostToUpdateThread(() ->
        {
            m_failCount = 0;
            m_highestStateReached_total = null;
            m_timeOfFirstConnect = m_timeOfLastConnectFail = null;
            m_history.clear();
            m_triedReconnectingLongTerm = false;
        });
	}

	final int getRetryCount()
	{
		int retryCount = m_failCount;
		
		return retryCount;
	}

	final int/*__PE_Please*/ onConnectionFailed(DeviceReconnectFilter.Status reason_nullable, DeviceReconnectFilter.Timing timing, boolean isAttemptingReconnect_longTerm, int gattStatus, int bondFailReason, BleDeviceState highestStateReached, ReconnectFilter.AutoConnectUsage autoConnectUsage, ReadWriteListener.ReadWriteEvent txnFailReason)
	{
		if( reason_nullable == null )  return DeviceReconnectFilter.ConnectFailPlease.PE_Please_DO_NOT_RETRY;
		
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

		final ConnectFailEvent moreInfo = new ConnectFailEvent
		(
			m_device, reason_nullable, timing, m_failCount, attemptTime_latest, attemptTime_total, gattStatus,
			highestStateReached, m_highestStateReached_total, autoConnectUsage, bondFailReason, txnFailReason
		);

		addToHistory(moreInfo);
		
		//--- DRK > Not invoking callback if we're attempting short-term reconnect.
		int retryChoice__PE_Please = m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) ? ConnectFailPlease.PE_Please_DO_NOT_RETRY : invokeCallback(moreInfo);
		
		//--- DRK > Retry choice doesn't matter if we're attempting reconnect.
		retryChoice__PE_Please = !isAttemptingReconnect_longTerm ? retryChoice__PE_Please : ConnectFailPlease.PE_Please_DO_NOT_RETRY;
		
		//--- DRK > Disabling retry if app-land decided to call connect() themselves in fail callback...hopefully fringe but must check for now.
		//--- RB > Commenting this out for now. If the user calls connect in the fail callback, it gets posted to the update thread, so this shouldn't
		// be an issue anymore. Right now with the new changes to threading, this is causing issues (so a reconnect attempt doesn't happen when it should)
//		retryChoice__PE_Please = m_device.is_internal(BleDeviceState.CONNECTING_OVERALL) ? Please.PE_Please_DO_NOT_RETRY : retryChoice__PE_Please;
		
		if( reason_nullable != null && reason_nullable.wasCancelled() )
		{
			retryChoice__PE_Please = ConnectFailPlease.PE_Please_DO_NOT_RETRY;
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
					retryChoice__PE_Please = ConnectFailPlease.PE_Please_DO_NOT_RETRY;
					m_device.onNativeDisconnect(/*wasExplicit=*/false, gattStatusOfOriginalDisconnect, /*doShortTermReconnect=*/false, /*saveLastDisconnect=*/true);
				}
			}
		}

		final boolean retryConnectOverall = BleDeviceConfig.bool(m_device.conf_device().connectFailRetryConnectingOverall, m_device.conf_mngr().connectFailRetryConnectingOverall);

		if( !m_triedReconnectingLongTerm && retryChoice__PE_Please != ConnectFailPlease.PE_Please_NULL && ConnectFailPlease.isRetry(retryChoice__PE_Please) && !m_device.is(CONNECTED))
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
				boolean isretryConnectOverall = ConnectFailPlease.isRetry(retryChoice__PE_Please) && retryConnectOverall;
				m_device.setStateToDisconnected(isAttemptingReconnect_longTerm, isretryConnectOverall, E_Intent.UNINTENTIONAL, gattStatus, /*forceMainStateTracker=*/false, P_BondManager.OVERRIDE_EMPTY_STATES);
			}
		}

		return retryChoice__PE_Please;
	}

	final ArrayList<ConnectFailEvent> getHistory()
	{
		return new ArrayList<>(m_history);
	}

	private void addToHistory(ConnectFailEvent event)
	{
		int maxSize = Math.max(BleDeviceConfig.integer(m_device.conf_device().maxConnectionFailHistorySize, m_device.conf_mngr().maxConnectionFailHistorySize), 1);
		if (m_history.size() >= maxSize)
		{
			m_history.remove(0);
		}
		m_history.add(event);
	}

	final int/*__PE_Please*/ invokeCallback(final ConnectFailEvent moreInfo)
	{
		int retryChoice__PE_Please = ConnectFailPlease.PE_Please_NULL;

		final DeviceReconnectFilter listener = getListener();
		if( listener != null )
		{
			final ReconnectFilter.ConnectFailPlease please = listener.onConnectFailed(moreInfo);
			retryChoice__PE_Please = please != null ? please.please() : ConnectFailPlease.PE_Please_NULL;
			
			m_device.getManager().getLogger().checkPlease(please, ReconnectFilter.ConnectFailPlease.class);
		}
		else if( m_device.getManager().m_defaultDeviceReconnectFilter != null )
		{
			final ReconnectFilter.ConnectFailPlease please = m_device.getManager().m_defaultDeviceReconnectFilter.onConnectFailed(moreInfo);
			retryChoice__PE_Please = please != null ? please.please() : ConnectFailPlease.PE_Please_NULL;
			
			m_device.getManager().getLogger().checkPlease(please, ReconnectFilter.ConnectFailPlease.class);
		}
		else
		{
			final ReconnectFilter.ConnectFailPlease please = BleDevice.DEFAULT_CONNECTION_FAIL_LISTENER.onConnectFailed(moreInfo);
			retryChoice__PE_Please = please != null ? please.please() : ConnectFailPlease.PE_Please_NULL;

			m_device.getManager().getLogger().checkPlease(please, ReconnectFilter.ConnectFailPlease.class);
		}

		retryChoice__PE_Please = retryChoice__PE_Please != ConnectFailPlease.PE_Please_NULL ? retryChoice__PE_Please : ConnectFailPlease.PE_Please_DO_NOT_RETRY;

		final DeviceConnectListener.ConnectEvent event = new DeviceConnectListener.ConnectEvent(m_device, moreInfo);
		m_device.invokeConnectCallbacks(event);

		return retryChoice__PE_Please;
	}

	public final DeviceReconnectFilter getListener()
	{
		if (m_connectionFailListenerStack.empty())
			return null;
		return m_connectionFailListenerStack.peek();
	}
	
	public final void setListener(DeviceReconnectFilter listener)
	{
		m_connectionFailListenerStack.clear();
		m_connectionFailListenerStack.push(listener);
	}

	public final void clearListenerStack()
    {
        m_connectionFailListenerStack.clear();
    }

	public final void pushListener(DeviceReconnectFilter listener)
    {
        if (listener != null)
            m_connectionFailListenerStack.push(listener);
    }

    public final boolean popListener()
    {
        if (m_connectionFailListenerStack.empty())
            return false;
        m_connectionFailListenerStack.pop();
        return true;
    }
}
