package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;

import com.idevicesinc.sweetblue.impl.DefaultServerReconnectFilter;

import java.util.HashMap;

final class P_ServerConnectionFailManager
{
	private static ServerReconnectFilter DEFAULT_CONNECTION_FAIL_LISTENER = new DefaultServerReconnectFilter();

	final BleServer m_server;

	private ServerReconnectFilter m_connectionFailListener = DEFAULT_CONNECTION_FAIL_LISTENER;

	private final HashMap<String, P_ServerConnectionFailEntry> m_entries = new HashMap<String, P_ServerConnectionFailEntry>();

	P_ServerConnectionFailManager(final BleServer server)
	{
		m_server = server;
	}

	private P_ServerConnectionFailEntry getOrCreateEntry(final String macAddress)
	{
		final P_ServerConnectionFailEntry entry_nullable = m_entries.get(macAddress);

		if( entry_nullable != null )
		{
			return entry_nullable;
		}
		else
		{
			final P_ServerConnectionFailEntry entry = new P_ServerConnectionFailEntry(this);

			m_entries.put(macAddress, entry);

			return entry;
		}
	}

	void onExplicitDisconnect(final String macAddress)
	{
		getOrCreateEntry(macAddress).onExplicitDisconnect();
	}

	void onExplicitConnectionStarted(final String macAddress)
	{
		getOrCreateEntry(macAddress).onExplicitConnectionStarted();
	}

	public void setListener(ServerReconnectFilter listener)
	{
		m_connectionFailListener = listener;
	}

	public ServerReconnectFilter getListener()
	{
		return m_connectionFailListener;
	}

	void onNativeConnectFail(final BluetoothDevice nativeDevice, final ServerReconnectFilter.Status status, final int gattStatus)
	{
		getOrCreateEntry(nativeDevice.getAddress()).onNativeConnectFail(nativeDevice, status, gattStatus);
	}

	int/*__PE_Please*/ invokeCallback(final ServerReconnectFilter.ConnectFailEvent e)
	{
		final int ePlease__PE_Please;

		if( m_connectionFailListener != null )
		{
			final ServerReconnectFilter.ConnectFailPlease please = m_connectionFailListener.onConnectFailed(e);

			ePlease__PE_Please = please != null ? please.please() : ReconnectFilter.ConnectFailPlease.PE_Please_DO_NOT_RETRY;
		}
		else
		{
			final ServerReconnectFilter.ConnectFailPlease please = m_server.getManager().m_defaultConnectionFailListener_server.onConnectFailed(e);

			ePlease__PE_Please = please != null ? please.please() : ReconnectFilter.ConnectFailPlease.PE_Please_DO_NOT_RETRY;
		}

		return ePlease__PE_Please;
	}
}
