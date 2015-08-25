package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattService;

class P_ServerServiceManager
{
	private final BleServer m_server;

	P_ServerServiceManager(final BleServer server)
	{
		m_server = server;
	}

	public void addService_native(final BluetoothGattService service)
	{
		m_server.getNative().add
	}
}
