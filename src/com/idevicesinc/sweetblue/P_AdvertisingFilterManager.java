package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;

import com.idevicesinc.sweetblue.BleManagerConfig.AdvertisingFilter;

/**
 * 
 * 
 *
 */
class P_AdvertisingFilterManager
{
	private final ArrayList<BleManagerConfig.AdvertisingFilter> m_filters = new ArrayList<BleManagerConfig.AdvertisingFilter>();
	private final AdvertisingFilter m_default;
	
	P_AdvertisingFilterManager(AdvertisingFilter defaultFilter)
	{
		m_default = defaultFilter;
	}
	
	void clear()
	{
		m_filters.clear();
	}
	
	void remove(AdvertisingFilter filter)
	{
		while( m_filters.remove(filter) ){};
	}
	
	void add(AdvertisingFilter filter)
	{
		if( filter == null )  return;
		
		if( m_filters.contains(filter) )
		{
			return;
		}
		
		m_filters.add(filter);
	}
	
	boolean allow(BluetoothDevice nativeInstance, List<UUID> uuids, String deviceName, String normalizedDeviceName, byte[] scanRecord, int rssi)
	{
		if( m_filters.size() == 0 && m_default == null )  return true;
		
		if( m_default != null )
		{
			if( m_default.acknowledgeDiscovery(nativeInstance, uuids, deviceName, normalizedDeviceName, scanRecord, rssi) )
			{
				return true;
			}
		}
		
		for( int i = 0; i < m_filters.size(); i++ )
		{
			AdvertisingFilter ithFilter = m_filters.get(i);
			
			if( ithFilter.acknowledgeDiscovery(nativeInstance, uuids, deviceName, normalizedDeviceName, scanRecord, rssi) )
			{
				return true;
			}
		}
		
		return false;
	}
}
