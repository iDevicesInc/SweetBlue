package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.text.TextUtils;
import android.util.Log;

import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.idevicesinc.sweetblue.BleManagerState.ON;


final class P_NativeDeviceWrapper
{
	private final BleDevice m_device;
	private P_NativeDeviceLayer m_device_native;
	private final String m_address;

	private String m_name_native;
	private String m_name_normalized;
	private String m_name_debug;
	private String m_name_override;

	private int m_bondState_cached = BluetoothDevice.BOND_NONE;
	
	//--- DRK > We have to track connection state ourselves because using
	//---		BluetoothManager.getConnectionState() is slightly out of date
	//---		in some cases. Tracking ourselves from callbacks seems accurate.
	private AtomicInteger m_nativeConnectionState = null;
	
	public P_NativeDeviceWrapper(BleDevice device, P_NativeDeviceLayer nativeLayer, String name_normalized, String name_native)
	{
		m_device = device;
		m_device_native = nativeLayer;
		m_address = m_device_native.getAddress() == null || m_device.isNull() ? BleDevice.NULL_MAC() : m_device_native.getAddress();

		m_nativeConnectionState = new AtomicInteger(-1);

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

	private P_NativeDeviceLayer createLayer(Class<? extends P_NativeDeviceLayer> layerClass)
	{
		P_NativeDeviceLayer layer = null;
		try
		{
			layer = layerClass.newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return layer;
	}

	private BleManager getManager()
	{
		return m_device.getManager();
	}

	private P_Logger getLogger()
	{
		return m_device.getManager().getLogger();
	}

	void updateNativeDeviceOnly(final P_NativeDeviceLayer device_native)
	{
		m_device_native.setNativeDevice(device_native.getNativeDevice());
	}

	void updateNativeDevice(final P_NativeDeviceLayer device_native, final byte[] scanRecord_nullable, boolean isSameScanRecord)
	{
		if (!isSameScanRecord)
		{
			String name_native;
			try
			{
				name_native = getManager().getDeviceName(device_native, scanRecord_nullable);
			} catch (Exception e)
			{
				getLogger().e("Failed to parse name, returning what BluetoothDevice returns.");
				name_native = device_native.getName();
			}

			if (!TextUtils.equals(name_native, m_name_native))
			{
				updateNativeName(name_native);
			}
		}

		m_device_native.setNativeDevice(device_native.getNativeDevice());

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

//		String[] address_split = m_address.split(":");
//		String lastFourOfMac = address_split[address_split.length - 2] + address_split[address_split.length - 1];
//		String debugName = m_name_normalized.length() == 0 ? "<no_name>" : m_name_normalized;
//		m_name_debug = m_device_native != null ? String.format("%s%s%s", debugName, "_", lastFourOfMac) : debugName;
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
		String[] address_split = m_address.split(":");
		String lastFourOfMac = address_split[address_split.length - 2] + address_split[address_split.length - 1];
		String debugName = m_name_normalized.length() == 0 ? "<no_name>" : m_name_normalized;
		String debug = m_device_native != null ? String.format("%s%s%s", debugName, "_", lastFourOfMac) : debugName;
		return debug;
	}

	public P_NativeDeviceLayer getDeviceLayer()
	{
		return m_device_native;
	}

	public BluetoothDevice getDevice()
	{
		if( m_device.isNull() )
		{
			return m_device.getManager().newNativeDevice(BleDevice.NULL_MAC()).getNativeDevice();
		}
		else
		{
			return m_device_native.getNativeDevice();
		}
	}
	
	public BluetoothGatt getGatt()
	{
		return m_device.layerManager().getGattLayer().getGatt();
	}
	
	private void updateGattFromCallback(BluetoothGatt gatt)
	{
		if (gatt == null && !getManager().m_config.unitTest)
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
			m_nativeConnectionState.set(getNativeConnectionState());
		}
		else
		{
			m_nativeConnectionState.set(state);
		}

		updateGattFromCallback(gatt);

		getLogger().i(getLogger().gattConn(m_nativeConnectionState.get()));
	}
	
	public int getNativeBondState()
	{
		int bondState_native;
		//--- > RB  If Bluetooth is not on, then we can't get the current bond state. This is here to catch the times when the system
		// 			decides to turn BT off/on. This prevents spamming the logs with a bunch of messages about not being able to get the
		// 			bond state
		if (m_device_native != null && getManager().is(ON))
		{
			bondState_native = m_device_native.getBondState();
			m_bondState_cached = bondState_native;
		}
		else
		{
			bondState_native = m_bondState_cached;
		}
		
		return bondState_native;
	}
	
	boolean isNativelyBonding()
	{
		return getNativeBondState() == BluetoothDevice.BOND_BONDING;
	}

	boolean isNativelyBonding(int bondState)
	{
		return bondState == BluetoothDevice.BOND_BONDING;
	}
	
	boolean isNativelyBonded()
	{
		return getNativeBondState() == BluetoothDevice.BOND_BONDED;
	}

	boolean isNativelyBonded(int bondState)
	{
		return bondState == BluetoothDevice.BOND_BONDED;
	}
	
	boolean isNativelyUnbonded()
	{
		return getNativeBondState() == BluetoothDevice.BOND_NONE;
	}

	boolean isNativelyUnbonded(int bondState)
	{
		return bondState == BluetoothDevice.BOND_NONE;
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

	boolean isNativelyConnectingOrConnected()
	{
		int state = getConnectionState();
		return state == BluetoothGatt.STATE_CONNECTED || state == BluetoothGatt.STATE_CONNECTING;
	}
	
	public int getNativeConnectionState()
	{
		//--- > RB If BT has been turned off quickly (for instance, there are many devices connected, then you go into BT settings and run a scan), then
		// 			we obviously won't be able to get a state. We return DISCONNECTED here for obvious reasons.
		if (getManager().m_config.nativeManagerLayer.isBluetoothEnabled())
		{
			return m_device.layerManager().getManagerLayer().getConnectionState(m_device_native, BluetoothGatt.GATT_SERVER);
		}
		else
		{
			return BluetoothGatt.STATE_DISCONNECTED;
		}
	}
	
	public int getConnectionState()
	{
		return performGetNativeState(null, null);
		// It was thought that getting the state from the UI thread would alleviate some issues. It wasn't found to be the case
		// I'm leaving it here just in case we need to switch back.
//		if (Utils.isOnMainThread())
//		{
//			return performGetNativeState(null, null);
//		}
//		else
//		{
//			// > RB - This may not be necessary anymore. It was thought that the check of the native state had to be made on the UI thread,
//			// so this is here to block the current thread until it gets the result back.
//			final CountDownLatch latch = new CountDownLatch(1);
//			final AtomicInteger state = new AtomicInteger(-1);
//			getManager().getPostManager().postToMain(new Runnable()
//			{
//				@Override public void run()
//				{
//					performGetNativeState(state, latch);
//				}
//			});
//			try
//			{
//				latch.await();
//			} catch (Exception e)
//			{
//				e.printStackTrace();
//			}
//			return state.get();
//		}
	}

	private int performGetNativeState(AtomicInteger state, CountDownLatch latch)
	{
		final int reportedNativeConnectionState = getNativeConnectionState();
		int connectedStateThatWeWillGoWith = reportedNativeConnectionState;

		if( m_nativeConnectionState != null && m_nativeConnectionState.get() != -1 )
		{
			if( m_nativeConnectionState.get() != reportedNativeConnectionState )
			{
				getLogger().e("Tracked native state " + getLogger().gattConn(m_nativeConnectionState.get()) + " doesn't match reported state " + getLogger().gattConn(reportedNativeConnectionState) + ".");
			}

			connectedStateThatWeWillGoWith = m_nativeConnectionState.get();
		}

		if( connectedStateThatWeWillGoWith != BluetoothGatt.STATE_DISCONNECTED )
		{
			if( gattLayer().isGattNull() )
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
		if (state != null)
		{
			state.set(connectedStateThatWeWillGoWith);
		}
		if (latch != null)
		{
			latch.countDown();
		}
		return connectedStateThatWeWillGoWith;
	}
	
	void closeGattIfNeeded(boolean disconnectAlso)
	{
		m_device.m_reliableWriteMngr.onDisconnect();

		if( gatt() == null )  return;

		closeGatt(disconnectAlso);
	}
	
	private void closeGatt(boolean disconnectAlso)
	{
		UhOh uhoh = m_device.layerManager().getGattLayer().closeGatt();
		if (uhoh != null)
		{
			m_device.getManager().uhOh(uhoh);
		}
		m_nativeConnectionState.set(BluetoothGatt.STATE_DISCONNECTED);
	}

	private P_GattLayer gattLayer()
	{
		return m_device.layerManager().getGattLayer();
	}

	private BluetoothGatt gatt()
	{
		return gattLayer().getGatt();
	}
	
	private void setGatt(BluetoothGatt gatt)
	{
		if( gatt() != null )
		{
			//--- DRK > This tripped with an S5 and iGrillv2 with low battery (not sure that matters).
			//---		AV was able to replicate twice but was not attached to debugger and now can't replicate.
			//---		As a result of a brief audit, moved gatt object setting from the ending state
			//---		handler of the connect task in P_BleDevice_Listeners to the execute method of the connect task itself.
			//---		Doesn't solve any particular issue found, but seems more logical.
			getManager().ASSERT(gatt() == gatt, "Different gatt object set.");

			if( gatt() != gatt )
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
			gattLayer().setGatt(null);
		}
		else
		{
			gattLayer().setGatt(gatt);
		}
	}
}
