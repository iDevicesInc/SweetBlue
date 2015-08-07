package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;

public class P_NativeServerWrapper
{
	private final BleServer m_server;
	private final P_Logger m_logger;
	private final BleManager m_mngr;

	private BluetoothGattServer m_native;
	
	//--- DRK > We have to track connection state ourselves because using
	//---		BluetoothManager.getConnectionState() is slightly out of date
	//---		in some cases. Tracking ourselves from callbacks seems accurate.
	private	Integer m_nativeConnectionState = null;
	
	public P_NativeServerWrapper(BleServer server )
	{
		m_server = server;
		m_logger = m_server.getManager().getLogger();
		m_mngr = m_server.getManager();
	}

	BluetoothGattServer getNative()
	{
		return m_native;
	}

	void updateNativeConnectionState(BluetoothDevice device)
	{
		updateNativeConnectionState(device, null);
	}
	
	void updateNativeConnectionState(BluetoothDevice device, Integer state)
	{
		synchronized (this)
		{
			updateDeviceFromCallback(device);

			if( state == null )
			{
				m_nativeConnectionState = getNativeConnectionState();
			}
			else
			{
				m_nativeConnectionState = state;
			}

			m_logger.i(m_logger.gattConn(m_nativeConnectionState));
		}
	}

	private int getNativeConnectionState()
	{
		return m_server.getManager().getNative().getConnectionState( m_device, BluetoothGatt.GATT_SERVER );
	}

	private void updateDeviceFromCallback(BluetoothDevice device)
	{
		if (device == null)
		{
			m_logger.w("BluetoothDevice object from callback is null.");
		}
		else
		{
			setDevice(device);
		}
	}

	void setNative( BluetoothGattServer server )
	{
		m_native = server;
	}
}
