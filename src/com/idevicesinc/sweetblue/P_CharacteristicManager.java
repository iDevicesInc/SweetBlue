package com.idevicesinc.sweetblue;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_CharacteristicManager
{
	private final P_Service m_service;
	private final P_Logger m_logger;
	
	private final HashMap<UUID, P_Characteristic> m_characteristics = new HashMap<UUID, P_Characteristic>();
	
	
	public P_CharacteristicManager(P_Service service)
	{
		m_service = service;
		m_logger = m_service.getDevice().getManager().getLogger();
	}
	
	public boolean has(UUID uuid)
	{
		return get(uuid) != null;
	}
	
	public P_Characteristic get(UUID uuid)
	{
		return m_characteristics.get(uuid);
	}
	
	private void put(P_Characteristic characteristic)
	{
		if( has(characteristic.getUuid()) )
		{			
			m_service.getDevice().getManager().ASSERT(false);
			
			return;
		}
		
		m_characteristics.put(characteristic.getUuid(), characteristic);
	}
	
	void clear()
	{
		m_characteristics.clear();
	}
	
	public BleDevice getDevice()
	{
		return m_service.getDevice();
	}
	
	void loadDiscoveredCharacteristics()
	{
		//--- DRK > Keep getting random concurrent modification exceptions here on this collection.
		//---		Hard to reproduce, so just trying this blanket fix for now.
		//---		UPDATE 2 WEEKS LATER: Seems to have worked (?)...no more exceptions!
		//---		UPDATE 2 WEEKS LATERER: Nevermind, got the exception again...going the toArray route.
		List<BluetoothGattCharacteristic> characteristics = m_service.getNative().getCharacteristics();
		Object[] raw = characteristics.toArray();
		
		for( int i = 0; i < raw.length; i++ )
		{
			BluetoothGattCharacteristic characteristic_native = (BluetoothGattCharacteristic) raw[i];
			
			if( characteristic_native == null )
			{
				m_service.getDevice().getManager().ASSERT(false);
				return;
			}
			
			P_Characteristic characteristic = new P_Characteristic(m_service, characteristic_native);
			
			if( !has(characteristic.getUuid()) )
			{
				put(characteristic);
			}
			else
			{
				m_logger.w("Already have " + m_logger.charName(characteristic.getUuid()));
			}
		}
	}
}
