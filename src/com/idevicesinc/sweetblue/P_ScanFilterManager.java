package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;

import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.Please;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.ScanEvent;
import com.idevicesinc.sweetblue.utils.State;

/**
 * 
 * 
 *
 */
class P_ScanFilterManager
{
	private final ArrayList<BleManagerConfig.ScanFilter> m_filters = new ArrayList<BleManagerConfig.ScanFilter>();
	private final ScanFilter m_default;
	
	P_ScanFilterManager(ScanFilter defaultFilter)
	{
		m_default = defaultFilter;
	}
	
	void clear()
	{
		m_filters.clear();
	}
	
	void remove(ScanFilter filter)
	{
		while( m_filters.remove(filter) ){};
	}
	
	void add(ScanFilter filter)
	{
		if( filter == null )  return;
		
		if( m_filters.contains(filter) )
		{
			return;
		}
		
		m_filters.add(filter);
	}
	
	BleManagerConfig.ScanFilter.Please allow(BluetoothDevice nativeInstance, List<UUID> uuids, String deviceName, String normalizedDeviceName, byte[] scanRecord, int rssi, State.ChangeIntent lastDisconnectIntent)
	{
		if( m_filters.size() == 0 && m_default == null )  return Please.acknowledge();
		
		ScanEvent result = null;
		
		if( m_default != null )
		{
			result = new ScanEvent(nativeInstance, uuids, deviceName, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent);
			
			Please ack = m_default.onEvent(result);
			
			if( ack != null && ack.ack() )
			{
				return ack;
			}
		}
		
		for( int i = 0; i < m_filters.size(); i++ )
		{
			result = result != null ? result : new ScanEvent(nativeInstance, uuids, deviceName, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent);
			
			ScanFilter ithFilter = m_filters.get(i);
			
			Please ack = ithFilter.onEvent(result);
			
			if( ack != null && ack.ack() )
			{
				return ack;
			}
		}
		
		return BleManagerConfig.ScanFilter.Please.ignore();
	}
}
