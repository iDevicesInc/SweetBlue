package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;

import com.idevicesinc.sweetblue.utils.Interval;

import java.util.ArrayList;

class P_ServerConnectionFailManager
{
	private static BleServer.ConnectionFailListener DEFAULT_CONNECTION_FAIL_LISTENER = new BleServer.DefaultConnectionFailListener();

	final BleServer m_server;

	private int m_failCount = 0;

	private Long m_timeOfFirstConnect = null;
	private Long m_timeOfLastConnectFail = null;

	private final ArrayList<BleServer.ConnectionFailListener.ConnectionFailEvent> m_history = new ArrayList<BleServer.ConnectionFailListener.ConnectionFailEvent>();

	private BleServer.ConnectionFailListener m_connectionFailListener = DEFAULT_CONNECTION_FAIL_LISTENER;

	P_ServerConnectionFailManager(final BleServer server)
	{
		m_server = server;

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

	public void setListener(BleServer.ConnectionFailListener listener)
	{
		m_connectionFailListener = listener;
	}

	void onNativeConnectFail(final BluetoothDevice nativeDevice, final BleServer.ConnectionFailListener.Status status, final int gattStatus)
	{
		final long currentTime = System.currentTimeMillis();

		//--- DRK > Can be null if this is a spontaneous connect (can happen with autoConnect sometimes for example).
		m_timeOfFirstConnect = m_timeOfFirstConnect != null ? m_timeOfFirstConnect : currentTime;
		final Long timeOfLastConnectFail = m_timeOfLastConnectFail != null ? m_timeOfLastConnectFail : m_timeOfFirstConnect;
		final Interval attemptTime_latest = Interval.delta(timeOfLastConnectFail, currentTime);
		final Interval attemptTime_total = Interval.delta(m_timeOfFirstConnect, currentTime);

		m_failCount++;

		final BleServer.ConnectionFailListener.ConnectionFailEvent e = new BleServer.ConnectionFailListener.ConnectionFailEvent
		(
			m_server, nativeDevice, status, m_failCount, attemptTime_latest, attemptTime_total,
			gattStatus, BleServer.ConnectionFailListener.AutoConnectUsage.NOT_APPLICABLE, m_history
		);

		m_history.add(e);

		final BleServer.ConnectionFailListener.Please.PE_Please ePlease = invokeCallback(e);

		if( ePlease.isRetry() )
		{
			m_server.connect_internal(nativeDevice);
		}
		else
		{
			resetFailCount();
		}
	}

	BleServer.ConnectionFailListener.Please.PE_Please invokeCallback(final BleServer.ConnectionFailListener.ConnectionFailEvent e)
	{
		final BleServer.ConnectionFailListener.Please.PE_Please ePlease;

		if( m_connectionFailListener != null )
		{
			final BleServer.ConnectionFailListener.Please please = m_connectionFailListener.onEvent(e);

			ePlease = please != null ? please.please() : BleServer.ConnectionFailListener.Please.PE_Please.DO_NOT_RETRY;
		}
		else
		{
			final BleServer.ConnectionFailListener.Please please = m_server.getManager().m_defaultConnectionFailListener_server.onEvent(e);

			ePlease = please != null ? please.please() : BleServer.ConnectionFailListener.Please.PE_Please.DO_NOT_RETRY;
		}

		return ePlease;
	}
}
