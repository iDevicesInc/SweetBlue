package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;

import java.util.HashMap;

class P_NativeServerWrapper
{
	private final BleServer m_server;
	private final P_Logger m_logger;
	private final BleManager m_mngr;

	private BluetoothGattServer m_native;
	private String m_name = "";

	private final HashMap<String, Integer> m_nativeConnectionStates = new HashMap<String, Integer>();
	
	public P_NativeServerWrapper(BleServer server )
	{
		m_server = server;
		m_logger = m_server.getManager().getLogger();
		m_mngr = m_server.getManager();

		if( server.isNull() )
		{
			m_name = BleDevice.NULL_STRING();
			m_native = null;
		}
		else
		{
			m_name = ""; //TODO
		}
	}

	public void closeServer()
	{
		if( m_native == null )
		{
			m_mngr.ASSERT(false, "Native server is already closed and nulled out.");

			return;
		}

		m_native.clearServices();
		m_native.close();

		m_native = null;
	}

	public boolean openServer()
	{
		if( m_native != null )
		{
			m_mngr.ASSERT(false, "Native server is already not null!");

			return true;
		}
		else
		{
			assertThatAllClientsAreDisconnected();

			clearAllConnectionStates();

			m_native = m_mngr.getNative().openGattServer(m_mngr.getApplicationContext(), m_server.m_listeners);

			if( m_native == null )
			{
				return false;
			}
			else
			{
				return true;
			}
		}
	}

	private void assertThatAllClientsAreDisconnected()
	{
		if( m_nativeConnectionStates.size() == 0 )  return;

		for( String macAddress : m_nativeConnectionStates.keySet() )
		{
			final Integer state = m_nativeConnectionStates.get(macAddress);

			if( state != null && state != BluetoothGattServer.STATE_DISCONNECTED )
			{
				m_mngr.ASSERT(false, "Found a server connection state that is not disconnected when it should be.");

				return;
			}
		}
	}

	public boolean isDisconnecting(final String macAddress)
	{
		return getNativeState(macAddress) == BluetoothGattServer.STATE_DISCONNECTING;
	}

	public boolean isDisconnected(final String macAddress)
	{
		return getNativeState(macAddress) == BluetoothGattServer.STATE_DISCONNECTED;
	}

	public boolean isConnected(final String macAddress)
	{
		return getNativeState(macAddress) == BluetoothGattServer.STATE_CONNECTED;
	}

	public boolean isConnecting(final String macAddress)
	{
		return getNativeState(macAddress) == BluetoothGattServer.STATE_CONNECTING;
	}

	public boolean isConnectingOrConnected(final String macAddress)
	{
		final int  nativeState = getNativeState(macAddress);

		return nativeState == BluetoothGattServer.STATE_CONNECTING || nativeState == BluetoothGattServer.STATE_CONNECTED;
	}

	public boolean isDisconnectingOrDisconnected(final String macAddress)
	{
		final int  nativeState = getNativeState(macAddress);

		return nativeState == BluetoothGattServer.STATE_DISCONNECTING || nativeState == BluetoothGattServer.STATE_DISCONNECTED;
	}

	private void clearAllConnectionStates()
	{
		m_nativeConnectionStates.clear();
	}

	public int getNativeState(final String macAddress)
	{
		if( m_nativeConnectionStates.containsKey(macAddress) )
		{
			return m_nativeConnectionStates.get(macAddress);
		}
		else
		{
			return BluetoothGattServer.STATE_DISCONNECTED;
		}
	}

	BluetoothGattServer getNative()
	{
		return m_native;
	}

	void updateNativeConnectionState(final String macAddress, final int state)
	{
		m_nativeConnectionStates.put(macAddress, state);
	}

	void updateNativeConnectionState(final BluetoothDevice device)
	{
		if( m_native == null )
		{
			m_mngr.ASSERT(false, "Did not expect native server to be null when implicitly refreshing state.");
		}
		else
		{
//			final int nativeState = m_native.getConnectionState(device);

			final int nativeState = m_server.getManager().getNative().getConnectionState( device, BluetoothGatt.GATT );

			updateNativeConnectionState(device.getAddress(), nativeState);
		}
	}
}
