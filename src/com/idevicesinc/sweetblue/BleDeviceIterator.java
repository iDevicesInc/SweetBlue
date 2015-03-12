package com.idevicesinc.sweetblue;

import java.util.Iterator;
import java.util.List;

/**
 * Implementation of {@link Iterator} for {@link BleDevice} instances, returned from {@link BleManager#getDevices()} and its overloads.
 * {@link #remove()} has no effect, for now at least.
 */
public class BleDeviceIterator implements Iterator<BleDevice>
{
	private final List<BleDevice> m_all;
	private final Object[] m_query;
	private final int m_mask;
	
	private Integer m_next = null;
	private int m_base = 0;
	
	public BleDeviceIterator(List<BleDevice> all)
	{
		m_all = all;
		m_query = null;
		m_mask = BleDeviceState.FULL_MASK;
	}
	
	public BleDeviceIterator(List<BleDevice> all, final int mask)
	{
		m_all = all;
		m_query = null;
		m_mask = mask;
	}
	
	public BleDeviceIterator(List<BleDevice> all, Object ... query)
	{
		m_all = all;
		m_query = query;
		m_mask = 0x0;
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
			for( int i = m_base; i < m_all.size(); i++ )
			{
				BleDevice device = m_all.get(i);
				
				if( device.isAny(m_mask) )
				{
					m_next = i;
					
					return true;
				}
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
	 * For now doesn't do anything.
	 */
	@Override public void remove()
	{		
	}
}
