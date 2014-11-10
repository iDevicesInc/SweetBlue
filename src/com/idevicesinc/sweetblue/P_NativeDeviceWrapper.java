package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;


class P_NativeDeviceWrapper
{
	private final BleDevice m_device;
	private final BluetoothDevice m_native;
	private	BluetoothGatt m_gatt;
	private final String m_address;
	private final String m_nativeName;
	private final String m_normalizedName;
	private final String m_debugName;
	private final P_Logger m_logger;
	private final BleManager m_mngr;
	
	//--- DRK > We have to track connection state ourselves because using
	//---		BluetoothManager.getConnectionState() is slightly out of date
	//---		in some cases. Tracking ourselves from callbacks seems accurate.
	private	Integer m_nativeConnectionState = null;
	
	public P_NativeDeviceWrapper(BleDevice device, BluetoothDevice device_native, String normalizedName)
	{
		m_device = device;
		m_native = device_native;
		m_address = m_native.getAddress() == null ? "" : m_native.getAddress();
		
		String nativeName = m_native.getName();
		nativeName = nativeName != null ? nativeName : "";
		m_nativeName = nativeName;
		
		m_normalizedName = normalizedName;
		
		String[] address_split = m_address.split(":");
		String lastFourOfMac = address_split[address_split.length - 2] + address_split[address_split.length - 1];
		String debugName = m_normalizedName.length() == 0 ? "<no_name>" : m_normalizedName;
		m_debugName = debugName + "_" + lastFourOfMac;
		
		m_logger = m_device.getManager().getLogger();
		m_mngr = m_device.getManager();
	}
	
	public String getAddress()
	{
		if( m_native != null )
		{
			m_device.getManager().ASSERT(m_address.equals(m_native.getAddress()));
		}
		
		return m_address;
	}
	
	public String getNormalizedName()
	{
		return m_normalizedName;
	}
	
	public String getNativeName()
	{
		return m_nativeName;
	}
	
	public String getDebugName()
	{
		return m_debugName;
	}
	
	public BluetoothDevice getDevice()
	{
		return m_native;
	}
	
	public BluetoothGatt getGatt()
	{
		return m_gatt;
	}
	
	private void updateGattFromCallback(BluetoothGatt gatt)
	{
		if (gatt == null)
		{
			m_logger.w("Gatt object from callback is null.");
		}
		else
		{
			setGatt(gatt);
		}
	}
	
	void updateNativeConnectionState(BluetoothGatt gatt)
	{
		updateNativeConnectionState(gatt, null);
	}
	
	void updateNativeConnectionState(BluetoothGatt gatt, Integer state)
	{
		synchronized (this)
		{
			if( state == null )
			{
				m_nativeConnectionState = getNativeConnectionState();
			}
			else
			{
				m_nativeConnectionState = state;
			}
			
			updateGattFromCallback(gatt);
			
			m_logger.i(m_logger.gattConn(m_nativeConnectionState));
		}
	}
	
	public int getNativeBondState()
	{
		return m_native.getBondState();
	}
	
	boolean isNativelyBonding()
	{
		return getNativeBondState() == BluetoothDevice.BOND_BONDING;
	}
	
	boolean isNativelyBonded()
	{
		return getNativeBondState() == BluetoothDevice.BOND_BONDED;
	}
	
	boolean isNativelyUnbonded()
	{
		return getNativeBondState() == BluetoothDevice.BOND_NONE;
	}
	
	boolean isNativelyConnected()
	{
		synchronized (this)
		{
			return getConnectionState() == BluetoothGatt.STATE_CONNECTED;
		}
	}
	
	boolean isNativelyConnecting()
	{
		return getConnectionState() == BluetoothGatt.STATE_CONNECTING;
	}
	
	boolean isNativelyDisconnecting()
	{
		return getConnectionState() == BluetoothGatt.STATE_DISCONNECTING;
	}
	
	private int getNativeConnectionState()
	{
		return m_device.getManager().getNative().getConnectionState( m_native, BluetoothGatt.GATT_SERVER );
	}
	
	public int getConnectionState()
	{
		synchronized (this)
		{
			int reportedConnectedState = getNativeConnectionState();
			int connectedStateThatWeWillGoWith = reportedConnectedState;
			
			if( m_nativeConnectionState != null )
			{
				if( m_nativeConnectionState != reportedConnectedState )
				{
					m_logger.e("Tracked native state "+m_logger.gattConn(m_nativeConnectionState)+" doesn't match reported state "+m_logger.gattConn(reportedConnectedState)+".");
				}
				
				connectedStateThatWeWillGoWith = m_nativeConnectionState;
			}
			
			if( connectedStateThatWeWillGoWith != BluetoothGatt.STATE_DISCONNECTED )
			{
				if( m_gatt == null )
				{
					//--- DRK > Can't assert here because gatt can legitmately be null even though we have a connecting/ed native state.
					//---		This was observed on the moto G right after app start up...getNativeConnectionState() reported connecting/ed
					//---		but we haven't called connect yet. Really rare...only seen once after 4 months.
					if( m_nativeConnectionState == null )
					{
						m_logger.e("Gatt is null with " + m_logger.gattConn(connectedStateThatWeWillGoWith));
						
						connectedStateThatWeWillGoWith = BluetoothGatt.STATE_DISCONNECTED;
						
						m_mngr.uhOh(UhOh.CONNECTED_WITHOUT_EVER_CONNECTING);
					}
					else
					{
						m_mngr.ASSERT(false, "Gatt is null with tracked native state: " + m_logger.gattConn(connectedStateThatWeWillGoWith));
					}
				}
			}
			else
			{
				//--- DRK > Had this assert here but must have been a brain fart because we can be disconnected
				//---		but still have gatt be not null cause we're trying to reconnect.
	//			if( !m_mngr.ASSERT(m_gatt == null) )
	//			{
	//				m_logger.e(m_logger.gattConn(connectedStateThatWeWillGoWith));
	//			}
			}
			
			return connectedStateThatWeWillGoWith;
		}
	}
	
	void closeGattIfNeeded(boolean disconnectAlso)
	{
		synchronized (this)
		{
			if( m_gatt == null )  return;
			
			closeGatt(disconnectAlso);
		}
	}
	
	private void closeGatt(boolean disconnectAlso)
	{
		synchronized (this)
		{
			if( !m_mngr.ASSERT(m_gatt != null) )  return;
			
			//--- DRK > Tried this to see if it would kill autoConnect, but alas it does not, at least on S5.
			//---		Don't want to keep it here because I'm afraid it has a higher chance to do bad than good.
//			if( disconnectAlso )
//			{
//				m_gatt.disconnect();
//			}
			
			m_gatt.close();
			setGatt(null);
		}
	}
	
	private void setGatt(BluetoothGatt gatt)
	{
		synchronized (this)
		{
			if( gatt == null )
			{
				m_gatt = null;
			}
			else
			{
				if( m_gatt != null )
				{
					//--- DRK > No idea why this assert tripped once but it did
					//---		so it might be a "valid" case where android sends
					//---		back a new gatt reference for different connections.
//					m_mngr.ASSERT(m_gatt == gatt);
				}
				
				m_gatt = gatt;
			}
		}
	}
	
}
