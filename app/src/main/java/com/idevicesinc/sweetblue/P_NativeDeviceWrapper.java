package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;


class P_NativeDeviceWrapper
{
	private final BleDevice m_device;
	private BluetoothDevice m_device_native;
	private	BluetoothGatt m_gatt;
	private final String m_address;

	private String m_name_native;
	private String m_name_normalized;
	private String m_name_debug;
	private String m_name_override;
	
	//--- DRK > We have to track connection state ourselves because using
	//---		BluetoothManager.getConnectionState() is slightly out of date
	//---		in some cases. Tracking ourselves from callbacks seems accurate.
	private	Integer m_nativeConnectionState = null;
	
	public P_NativeDeviceWrapper(BleDevice device, BluetoothDevice device_native, String name_normalized, String name_native)
	{
		m_device = device;
		m_device_native = device_native;
		m_address = m_device_native == null || m_device_native.getAddress() == null ? BleDevice.NULL_MAC() : m_device_native.getAddress();

		updateName(name_native, name_normalized);

		//--- DRK > Manager can be null for BleDevice.NULL.
		final boolean hitDiskForOverrideName = true;
		final String name_disk = getManager() != null ? getManager().m_diskOptionsMngr.loadName(m_address, hitDiskForOverrideName) : null;

		if( name_disk != null )
		{
			setName_override(name_disk);
		}
		else
		{
			setName_override(m_name_native);
			final boolean saveToDisk = BleDeviceConfig.bool(m_device.conf_device().saveNameChangesToDisk, m_device.conf_mngr().saveNameChangesToDisk);
			getManager().m_diskOptionsMngr.saveName(m_address, m_name_native, saveToDisk);
		}
	}

	private BleManager getManager()
	{
		return m_device.getManager();
	}

	private P_Logger getLogger()
	{
		return m_device.getManager().getLogger();
	}

	void updateNativeDevice(final BluetoothDevice device_native)
	{
		final String name_native = device_native.getName();

		updateNativeName(name_native);

		m_device_native = device_native;
	}

	void setName_override(final String name)
	{
		m_name_override = name != null ? name : "";
	}

	void updateNativeName(final String name_native)
	{
		final String name_native_override;

		if( name_native != null )
		{
			name_native_override = name_native;
		}
		else
		{
			//--- DRK > After a ble reset using cached devices, calling updateNativeDevice with the old native BluetoothDevice
			//---		instance gives you a null name...not sure how long this has been the case, but I think only > 5.0
			name_native_override = m_name_native;
		}

		final String name_normalized = Utils_String.normalizeDeviceName(name_native_override);

		updateName(name_native_override, name_normalized);
	}

	void clearName_override()
	{
		setName_override(m_name_native);
	}

	private void updateName(String name_native, String name_normalized)
	{
		name_native = name_native != null ? name_native : "";
		m_name_native = name_native;

		m_name_normalized = name_normalized;

		String[] address_split = m_address.split(":");
		String lastFourOfMac = address_split[address_split.length - 2] + address_split[address_split.length - 1];
		String debugName = m_name_normalized.length() == 0 ? "<no_name>" : m_name_normalized;
		m_name_debug = m_device_native != null ? debugName + "_" + lastFourOfMac : debugName;
	}
	
	public String getAddress()
	{
		if( m_device_native != null )
		{
			m_device.getManager().ASSERT(m_address.equals(m_device_native.getAddress()));
		}
		
		return m_address;
	}
	
	public String getNormalizedName()
	{
		return m_name_normalized;
	}
	
	public String getNativeName()
	{
		return m_name_native;
	}

	public String getName_override()
	{
		return m_name_override;
	}
	
	public String getDebugName()
	{
		return m_name_debug;
	}
	
	public BluetoothDevice getDevice()
	{
		if( m_device.isNull() )
		{
			return m_device.getManager().newNativeDevice(BleDevice.NULL_MAC());
		}
		else
		{
			return m_device_native;
		}
	}
	
	public BluetoothGatt getGatt()
	{
		return m_gatt;
	}
	
	private void updateGattFromCallback(BluetoothGatt gatt)
	{
		if (gatt == null)
		{
			getLogger().w("Gatt object from callback is null.");
		}
		else
		{
			setGatt(gatt);
		}
	}
	
	void updateGattInstance(BluetoothGatt gatt)
	{
		updateGattFromCallback(gatt);
	}
	
	void updateNativeConnectionState(BluetoothGatt gatt)
	{
		updateNativeConnectionState(gatt, null);
	}
	
	void updateNativeConnectionState(BluetoothGatt gatt, Integer state)
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

		getLogger().i(getLogger().gattConn(m_nativeConnectionState));
	}
	
	public int getNativeBondState()
	{
		final int bondState_native = m_device_native != null ? m_device_native.getBondState() : BluetoothDevice.BOND_NONE;
		
		return bondState_native;
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
	
	public int getNativeConnectionState()
	{
		return m_device.getManager().getNative().getConnectionState(m_device_native, BluetoothGatt.GATT_SERVER );
	}
	
	public int getConnectionState()
	{
		synchronized (this)
		{
			final int reportedNativeConnectionState = getNativeConnectionState();
			int connectedStateThatWeWillGoWith = reportedNativeConnectionState;
			
			if( m_nativeConnectionState != null )
			{
				if( m_nativeConnectionState != reportedNativeConnectionState )
				{
					getLogger().e("Tracked native state " + getLogger().gattConn(m_nativeConnectionState) + " doesn't match reported state " + getLogger().gattConn(reportedNativeConnectionState) + ".");
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
						getLogger().e("Gatt is null with " + getLogger().gattConn(connectedStateThatWeWillGoWith));
						
						connectedStateThatWeWillGoWith = BluetoothGatt.STATE_DISCONNECTED;

						getManager().uhOh(UhOh.CONNECTED_WITHOUT_EVER_CONNECTING);
					}
					else
					{
						getManager().ASSERT(false, "Gatt is null with tracked native state: " + getLogger().gattConn(connectedStateThatWeWillGoWith));
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
		m_device.m_reliableWriteMngr.onDisconnect();

		if( m_gatt == null )  return;

		closeGatt(disconnectAlso);
	}
	
	private void closeGatt(boolean disconnectAlso)
	{
		if( m_gatt == null )  return;

		//--- DRK > Tried this to see if it would kill autoConnect, but alas it does not, at least on S5.
		//---		Don't want to keep it here because I'm afraid it has a better chance to do bad than good.
//			if( disconnectAlso )
//			{
//				m_gatt.disconnect();
//			}

		//--- DRK > This can randomly throw an NPE down stream...NOT from m_gatt being null, but a few methods downstream.
		//---		See below for more info.
		try
		{
			m_gatt.close();
		}
		catch(NullPointerException e)
		{
			//--- DRK > From Flurry crash reports...happened several times on S4 running 4.4.4 but was not able to reproduce.
//				This error occurred: java.lang.NullPointerException
//				android.os.Parcel.readException(Parcel.java:1546)
//				android.os.Parcel.readException(Parcel.java:1493)
//				android.bluetooth.IBluetoothGatt$Stub$Proxy.unregisterClient(IBluetoothGatt.java:905)
//				android.bluetooth.BluetoothGatt.unregisterApp(BluetoothGatt.java:710)
//				android.bluetooth.BluetoothGatt.close(BluetoothGatt.java:649)
//				com.idevicesinc.sweetblue.P_NativeDeviceWrapper.closeGatt(P_NativeDeviceWrapper.java:238)
//				com.idevicesinc.sweetblue.P_NativeDeviceWrapper.closeGattIfNeeded(P_NativeDeviceWrapper.java:221)
//				com.idevicesinc.sweetblue.BleDevice.onNativeConnectFail(BleDevice.java:2193)
//				com.idevicesinc.sweetblue.P_BleDevice_Listeners$1.onStateChange_synchronized(P_BleDevice_Listeners.java:78)
//				com.idevicesinc.sweetblue.P_BleDevice_Listeners$1.onStateChange(P_BleDevice_Listeners.java:49)
//				com.idevicesinc.sweetblue.PA_Task.setState(PA_Task.java:118)
//				com.idevicesinc.sweetblue.PA_Task.setEndingState(PA_Task.java:242)
//				com.idevicesinc.sweetblue.P_TaskQueue.endCurrentTask(P_TaskQueue.java:220)
//				com.idevicesinc.sweetblue.P_TaskQueue.tryEndingTask(P_TaskQueue.java:267)
//				com.idevicesinc.sweetblue.P_TaskQueue.fail(P_TaskQueue.java:260)
//				com.idevicesinc.sweetblue.P_BleDevice_Listeners.onConnectionStateChange_synchronized(P_BleDevice_Listeners.java:168)
			m_device.getManager().uhOh(UhOh.RANDOM_EXCEPTION);
		}

		m_nativeConnectionState = BluetoothGatt.STATE_DISCONNECTED;
		m_gatt = null;
	}
	
	private void setGatt(BluetoothGatt gatt)
	{
		if( m_gatt != null )
		{
			//--- DRK > This tripped with an S5 and iGrillv2 with low battery (not sure that matters).
			//---		AV was able to replicate twice but was not attached to debugger and now can't replicate.
			//---		As a result of a brief audit, moved gatt object setting from the ending state
			//---		handler of the connect task in P_BleDevice_Listeners to the execute method of the connect task itself.
			//---		Doesn't solve any particular issue found, but seems more logical.
			getManager().ASSERT(m_gatt == gatt, "Different gatt object set.");

			if( m_gatt != gatt )
			{
				closeGatt(/*disconnectAlso=*/false);
			}
			else
			{
				return;
			}
		}

		if( gatt == null )
		{
			m_gatt = null;
		}
		else
		{
			m_gatt = gatt;
		}
	}
}
