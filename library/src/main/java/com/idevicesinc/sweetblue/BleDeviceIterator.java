package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Utils_State;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implementation of {@link Iterator} for {@link BleDevice} instances, returned from {@link BleManager#getDevices()} and its overloads.
 */
public final class BleDeviceIterator implements Iterator<BleDevice>
{
	private final List<Integer> m_all_states;
	private final List<BleDevice> m_all;
	private final Object[] m_query;
	private final int m_mask;
	
	private Integer m_next = null;
	private int m_base = 0;
	private BleDevice m_deviceReturned;
	
	public BleDeviceIterator(List<BleDevice> all)
	{
		m_all = all;
		m_query = null;
		m_mask = BleDeviceState.FULL_MASK;

		m_all_states = null;

		initStateList();
	}
	
	public BleDeviceIterator(List<BleDevice> all, final int mask)
	{
		m_all = all;
		m_query = null;
		m_mask = mask;

		m_all_states = new ArrayList<Integer>();

		initStateList();
	}
	
	public BleDeviceIterator(List<BleDevice> all, Object ... query)
	{
		m_all = all;
		m_query = query;
		m_mask = 0x0;

		m_all_states = new ArrayList<Integer>();

		initStateList();
	}

	private void initStateList()
	{
		if( m_all_states == null )  return;

		for( int i = 0; i < m_all.size(); i++ )
		{
			final BleDevice ith = m_all.get(i);

			m_all_states.add(ith.getStateMask());
		}
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
				final int mask = m_all_states != null ? m_all_states.get(i) : BleDeviceState.FULL_MASK;
				
				if( (mask & m_mask) != 0x0 )
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
				final int mask = m_all_states != null ? m_all_states.get(i) : BleDeviceState.FULL_MASK;
				
				if( Utils_State.query(mask, m_query) )
				{
					m_next = i;
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	private BleDevice next_private()
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

	@Override public BleDevice next()
	{
		m_deviceReturned = next_private();
		
		//--- DRK > Conforming to soft throw requirements dictated by Iterator interface.		
		if( m_deviceReturned == null )  throw new NoSuchElementException("No more BleDevice instances in this iterator.");
		
		return m_deviceReturned;
	}

	/**
	 * Calls {@link BleManager#undiscover(BleDevice)}, removing it from the {@link BleManager} singleton's internal list.
	 */
	@Override public void remove()
	{
		//--- DRK > Conforming to soft throw requirements dictated by Iterator interface.
		if( m_deviceReturned == null )  throw new IllegalStateException("remove() was already called.");
		
		final BleDevice toUndiscover = m_deviceReturned;
		m_deviceReturned = null;
		toUndiscover.undiscover();
	}
}
