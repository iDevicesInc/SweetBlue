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
}
