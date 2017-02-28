package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.Please;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.ScanEvent;


final class P_ScanFilterManager
{
	private final ArrayList<BleManagerConfig.ScanFilter> m_filters = new ArrayList<BleManagerConfig.ScanFilter>();
	private ScanFilter m_default;
	private final BleManager m_mngr;
	
	P_ScanFilterManager(final BleManager mngr, final ScanFilter defaultFilter)
	{
		m_mngr = mngr;
		m_default = defaultFilter;
	}

	void updateFilter(ScanFilter filter)
	{
		m_default = filter;
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

	public boolean makeEvent()
	{
		return m_default != null || m_filters.size() > 0;
	}
	
	BleManagerConfig.ScanFilter.Please allow(P_Logger logger, final ScanEvent e)
	{
		if( m_filters.size() == 0 && m_default == null )  return Please.acknowledge();

		if( m_default != null )
		{
			final Please please = m_default.onEvent(e);
			
			logger.checkPlease(please, Please.class);

			stopScanningIfNeeded(m_default, please);
			
			if( please != null && please.ack() )
			{
				return please;
			}
		}
		
		for( int i = 0; i < m_filters.size(); i++ )
		{
			final ScanFilter ithFilter = m_filters.get(i);
			
			final Please please = ithFilter.onEvent(e);
			
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
