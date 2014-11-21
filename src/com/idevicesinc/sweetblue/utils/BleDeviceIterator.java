package com.idevicesinc.sweetblue.utils;

import java.util.Iterator;
import java.util.List;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;

/**
 * Implementation of {@link Iterator} for {@link BleDevice} instances, returned from {@link BleManager#getDevices()} and its overloads.
 * 
 * @author dougkoellmer
 */
public class BleDeviceIterator implements Iterator<BleDevice>
{
	private final List<BleDevice> m_all;
	private final Object[] m_query;
	
	private Integer m_next = null;
	private int m_base = 0;
	
	public BleDeviceIterator(List<BleDevice> all)
	{
		m_all = all;
		m_query = null;
	}
	
	public BleDeviceIterator(List<BleDevice> all, Object ... query)
	{
		m_all = all;
		m_query = query;
	}
	
	@Override public boolean hasNext()
	{
		if( m_next == null )
		{
			if( !findNext() )  return false;
		}
		
		return true;
	}
	
	private boolean findNext()
	{
		if( m_next != null )  return true;
		
		if( m_query == null )
		{
			if( m_base < m_all.size() )
			{
				m_next = m_base;
				
				return true;
			}
		}
		else
		{
			for( int i = m_base; i < m_all.size(); i++ )
			{
				BleDevice device = m_all.get(i);
				
				if( device.is(m_query) )
				{
					m_next = i;
					
					return true;
				}
			}
		}
		
		return false;
	}

	@Override public BleDevice next()
	{
		if( m_next == null )
		{
			if( !findNext() )  return null;
		}
		
		int next = m_next;
		m_next = null;
		m_base = next+1;
		
		return m_all.get(next);
	}

	/**
	 * Doesn't do anything.
	 */
	@Override public void remove()
	{		
	}
}
