package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;

import com.idevicesinc.sweetblue.utils.Interval;

import java.util.ArrayList;

final class P_ServerConnectionFailEntry
{
	private int m_failCount = 0;

	private Long m_timeOfFirstConnect = null;
	private Long m_timeOfLastConnectFail = null;

	private final ArrayList<ServerConnectionFailListener.ConnectionFailEvent> m_history = new ArrayList<ServerConnectionFailListener.ConnectionFailEvent>();

	private final P_ServerConnectionFailManager m_mngr;

	P_ServerConnectionFailEntry(final P_ServerConnectionFailManager mngr)
	{
		m_mngr = mngr;

		resetFailCount();
	}

	void onExplicitDisconnect()
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
		m_timeOfFirstConnect = m_timeOfLastConnectFail = null;
		m_history.clear();
	}

	void onNativeConnectFail(final BluetoothDevice nativeDevice, final ServerConnectionFailListener.Status status, final int gattStatus)
	{
		final long currentTime = System.currentTimeMillis();

		//--- DRK > Can be null if this is a spontaneous connect (can happen with autoConnect sometimes for example).
		m_timeOfFirstConnect = m_timeOfFirstConnect != null ? m_timeOfFirstConnect : currentTime;
		final Long timeOfLastConnectFail = m_timeOfLastConnectFail != null ? m_timeOfLastConnectFail : m_timeOfFirstConnect;
		final Interval attemptTime_latest = Interval.delta(timeOfLastConnectFail, currentTime);
		final Interval attemptTime_total = Interval.delta(m_timeOfFirstConnect, currentTime);

		m_failCount++;

		final ServerConnectionFailListener.ConnectionFailEvent e = new ServerConnectionFailListener.ConnectionFailEvent
		(
			m_mngr.m_server, nativeDevice, status, m_failCount, attemptTime_latest, attemptTime_total,
			gattStatus, ServerConnectionFailListener.AutoConnectUsage.NOT_APPLICABLE, m_history
		);

		m_history.add(e);

		final int ePlease__PE_Please = m_mngr.invokeCallback(e);

		if( NodeConnectionFailListener.Please.isRetry(ePlease__PE_Please) )
		{
			m_mngr.m_server.connect_internal(nativeDevice);
		}
		else
		{
			resetFailCount();
		}
	}
}
