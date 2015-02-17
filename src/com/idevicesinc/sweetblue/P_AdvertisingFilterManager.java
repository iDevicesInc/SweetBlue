package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;

import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.Please;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.Result;
import com.idevicesinc.sweetblue.utils.State;

/**
 * 
 * 
 *
 */
class P_AdvertisingFilterManager
{
	private final ArrayList<BleManagerConfig.ScanFilter> m_filters = new ArrayList<BleManagerConfig.ScanFilter>();
	private final ScanFilter m_default;
	
	P_AdvertisingFilterManager(ScanFilter defaultFilter)
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
		
		Result packet = null;
		
		if( m_default != null )
		{
			packet = new Result(nativeInstance, uuids, deviceName, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent);
			
			Please ack = m_default.onScanResult(packet);
			
			if( ack != null && ack.ack() )
			{
				return ack;
			}
		}
		
		for( int i = 0; i < m_filters.size(); i++ )
		{
			packet = packet != null ? packet : new Result(nativeInstance, uuids, deviceName, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent);
			
			ScanFilter ithFilter = m_filters.get(i);
			
			Please ack = ithFilter.onScanResult(packet);
			
			if( ack != null && ack.ack() )
			{
				return ack;
			}
		}
		
		return BleManagerConfig.ScanFilter.Please.ignore();
	}
}
