package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;

import java.util.HashSet;
import java.util.List;

class P_ClientManager
{
	private static final int[] CONNECTING_OR_CONNECTED = {BluetoothGattServer.STATE_CONNECTING, BluetoothGattServer.STATE_CONNECTED};

	private final BleServer m_server;

	P_ClientManager(final BleServer server)
	{
		m_server = server;
	}

	private final HashSet<String> getAll()
	{
		final HashSet<String> allClients = new HashSet<String>();

		final P_TaskQueue queue = m_server.getManager().getTaskQueue();
		final List<PA_Task> queue_raw = queue.getRaw();
		final PA_Task current = queue.getCurrent();

		for( int i = queue_raw.size()-1; i >= 0; i-- )
		{
			final PA_Task ith = queue_raw.get(i);

			if( m_server.equals(ith.getServer()) )
			{
				if( ith instanceof PA_Task_ConnectOrDisconnectServer )
				{
					final PA_Task_ConnectOrDisconnectServer ith_cast = (PA_Task_ConnectOrDisconnectServer) ith;

					allClients.add(ith_cast.m_nativeDevice.getAddress());
				}
			}
		}

		final List<BluetoothDevice> devicesKnownToNativeStack = m_server.getManager().getNative().getDevicesMatchingConnectionStates(BluetoothGattServer.GATT, CONNECTING_OR_CONNECTED);

		if( devicesKnownToNativeStack != null )
		{
			for( int i = 0; i < devicesKnownToNativeStack.size(); i++ )
			{

			}
		}

		for( final String macAddress : allClients )
		{
			if( !m_server.isAny(macAddress, BleServerState.CONNECTED, BleServerState.CONNECTING) )
			{
				allClients.
			}
		}
	}
}
