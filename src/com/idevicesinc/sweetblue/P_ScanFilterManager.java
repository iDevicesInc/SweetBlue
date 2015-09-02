package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;

import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.Please;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.ScanEvent;
import com.idevicesinc.sweetblue.utils.State;

class P_ScanFilterManager
{
	private final ArrayList<BleManagerConfig.ScanFilter> m_filters = new ArrayList<BleManagerConfig.ScanFilter>();
	private final ScanFilter m_default;
	private final BleManager m_mngr;
	
	P_ScanFilterManager(final BleManager mngr, final ScanFilter defaultFilter)
	{
		m_mngr = mngr;
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
	
	BleManagerConfig.ScanFilter.Please allow(P_Logger logger, BluetoothDevice nativeInstance, List<UUID> uuids, String deviceName, String normalizedDeviceName, byte[] scanRecord, int rssi, State.ChangeIntent lastDisconnectIntent)
	{
		if( m_filters.size() == 0 && m_default == null )  return Please.acknowledge();
		
		ScanEvent result = null;
		
		if( m_default != null )
		{
			result = new ScanEvent(nativeInstance, uuids, deviceName, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent);
			
			final Please please = m_default.onEvent(result);
			
			logger.checkPlease(please, Please.class);

			stopScanningIfNeeded(m_default, please);
			
			if( please != null && please.ack() )
			{
				return please;
			}
		}
		
		for( int i = 0; i < m_filters.size(); i++ )
		{
			result = result != null ? result : new ScanEvent(nativeInstance, uuids, deviceName, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent);
			
			final ScanFilter ithFilter = m_filters.get(i);
			
			final Please please = ithFilter.onEvent(result);
			
			logger.checkPlease(please, Please.class);

			stopScanningIfNeeded(ithFilter, please);
			
			if( please != null && please.ack() )
			{
				return please;
			}
		}
		
		return BleManagerConfig.ScanFilter.Please.ignore();
	}

	private void stopScanningIfNeeded(final ScanFilter filter, final BleManagerConfig.ScanFilter.Please please_nullable)
	{
		if( please_nullable != null )
		{
			if( please_nullable.ack() )
			{
				if( (please_nullable.m_stopScanOptions & Please.STOP_PERIODIC_SCAN) != 0x0 )
				{
					m_mngr.stopPeriodicScan(filter);
				}

				if( (please_nullable.m_stopScanOptions & Please.STOP_SCAN) != 0x0 )
				{
					m_mngr.stopScan(filter);
				}
			}
		}
	}
}
