package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;

public class P_NativeServerWrapper {

	private final BleServer m_server;
	private BluetoothGattServer m_native;
	private	BluetoothDevice m_device;
//	private final String m_address;
//	private final String m_nativeName;
//	private final String m_normalizedName;
//	private final String m_debugName;
	private final P_Logger m_logger;
	private final BleManager m_mngr;
	
	//--- DRK > We have to track connection state ourselves because using
	//---		BluetoothManager.getConnectionState() is slightly out of date
	//---		in some cases. Tracking ourselves from callbacks seems accurate.
	private	Integer m_nativeConnectionState = null;
	
	public P_NativeServerWrapper(BleServer server ) {
		// TODO Auto-generated constructor stub
		m_server = server;
		//m_address = m_native..getAddress() == null ? "" : m_native.getAddress();
		
		//String nativeName = m_native.getName();
//		nativeName = nativeName != null ? nativeName : "";
//		m_nativeName = nativeName;
//		
//		m_normalizedName = normalizedName;
		
//		String[] address_split = m_address.split(":");
//		String lastFourOfMac = address_split[address_split.length - 2] + address_split[address_split.length - 1];
//		String debugName = m_normalizedName.length() == 0 ? "<no_name>" : m_normalizedName;
//		m_debugName = debugName + "_" + lastFourOfMac;
		
		m_logger = m_server.getManager().getLogger();
		m_mngr = m_server.getManager();
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

	private void setDevice(BluetoothDevice device)
	{
		synchronized (this)
		{
			if( device == null )
			{
				device = null;
			}
			else
			{
				if( device != null )
				{
					//--- DRK > No idea why this assert tripped once but it did
					//---		so it might be a "valid" case where android sends
					//---		back a new gatt reference for different connections.
//					m_mngr.ASSERT(m_gatt == gatt);
				}
				
				m_device = device;
			}
		}
	}
	void setNative( BluetoothGattServer server ) {
		m_native = server;
	}
	public BluetoothDevice getNativeDevice() {
		return m_device;
	}
}
