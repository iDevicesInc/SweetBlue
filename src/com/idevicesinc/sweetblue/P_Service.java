package com.idevicesinc.sweetblue;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

/**
 * 
 * 
 *
 */
class P_Service
{
	private final BluetoothGattService m_native;
	private final BleDevice m_device;
	private final P_CharacteristicManager m_mngr;
	
	P_Service(BleDevice device, BluetoothGattService service_native)
	{
		m_device = device;
		m_native = service_native;
		m_mngr = new P_CharacteristicManager(this);
	}
	
	public UUID getUuid()
	{
		return m_native.getUuid();
	}
	
	public P_Characteristic get(UUID uuid)
	{
		return m_mngr.get(uuid);
	}
	
	public BleDevice getDevice()
	{
		return m_device;
	}
	
	public BluetoothGattService getNative()
	{
		return m_native;
	}

	void loadCharacteristics()
	{
		m_mngr.loadDiscoveredCharacteristics();
	}
	
	void addToList(List<BluetoothGattCharacteristic> chars_out)
	{
		for( int i = 0; i < m_mngr.getCount(); i++ )
		{
			final P_Characteristic char_ith = m_mngr.get(i);
			
			chars_out.add(char_ith.getGuaranteedNative());
		}
	}
	
	@Override public String toString()
	{
		return m_mngr.toString();
	}
}
