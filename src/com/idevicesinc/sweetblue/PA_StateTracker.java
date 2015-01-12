package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.BitwiseEnum;

/**
 * 
 * 
 *
 */
abstract class PA_StateTracker
{
	private int m_stateMask = 0x0;
	private final P_Logger m_logger;
	
	private final Object m_lock = new Object();
	private final long[] m_timesInState;
	
	static enum E_Intent
	{
		EXPLICIT, IMPLICIT;
		
		public int getMask()
		{
			return this == EXPLICIT ? 0xFFFFFFFF : 0x0;
		}
	}
	
	PA_StateTracker(P_Logger logger, BitwiseEnum[] enums)
	{
		m_logger = logger;
		m_timesInState = new long[enums.length];
	}
	
	public int getState()
	{
		return m_stateMask;
	}
	
	boolean checkBitMatch(BitwiseEnum flag, boolean value)
	{
		synchronized ( m_lock )
		{
			return ((flag.bit() & m_stateMask) != 0) == value;
		}
	}
	
	private int getMask(int currentStateMask, Object[] statesAndValues)
	{
		int newStateBits = currentStateMask;
		
		for( int i = 0; i < statesAndValues.length; i++ )
		{
			Object ithValue = statesAndValues[i];
			
			if( ithValue instanceof Object[] )
			{
				newStateBits = getMask(newStateBits, (Object[])ithValue);
				
				continue;
			}
			
			
			BitwiseEnum state = (BitwiseEnum) statesAndValues[i];
			boolean append = true;
			
			if( statesAndValues[i+1] instanceof Boolean )
			{
				append = (Boolean) statesAndValues[i+1];
				i++;
			}
			
			if( append )
			{
				append_assert(state);
				
				newStateBits |= state.bit();
			}
			else
			{
				newStateBits &= ~state.bit();
			}
		}
		
		return newStateBits;
	}
	
	
	
	
	
	void append(BitwiseEnum newState, E_Intent intent)
	{
		synchronized ( m_lock )
		{
			if( newState./*already*/overlaps(m_stateMask) )
			{
	//			m_logger.w("Already in state: " + newState);
				
				return;
			}
			
			append_assert(newState);
			
			setStateMask(m_stateMask | newState.bit(), intent == E_Intent.EXPLICIT ? newState.bit() : 0x0);
		}
	}
	
	void remove(BitwiseEnum state, E_Intent intent)
	{
		synchronized ( m_lock )
		{
			setStateMask(m_stateMask & ~state.bit(), intent == E_Intent.EXPLICIT ? state.bit() : 0x0);
		}
	}
	
	protected void append_assert(BitwiseEnum newState){}
	
//	void appendMultiple(I_BitwiseEnum ... states)
//	{
//		int newStateBits = m_stateMask;
//		for( int i = 0; i < states.length; i++ )
//		{
//			I_BitwiseEnum ithState = I_BitwiseEnum.values()[i];
//			
//			append_assert(ithState);
//			
//			newStateBits |= ithState.bit();
//		}
//		
//		setStateMask(newStateBits);
//	}
	
	void set(E_Intent intent, Object ... statesAndValues)
	{
		set(intent.getMask(), statesAndValues);
	}
	
	private void set(int explicitnessMask, Object ... statesAndValues)
	{
		synchronized ( m_lock )
		{
			int newStateBits = getMask(0x0, statesAndValues);
			
			setStateMask(newStateBits, explicitnessMask);
		}
	}
	
	void update(E_Intent intent, Object ... statesAndValues)
	{
		update(intent.getMask(), statesAndValues);
	}
	
	private void update(int explicitnessMask, Object ... statesAndValues)
	{
		synchronized ( m_lock )
		{
			int newStateBits = getMask(m_stateMask, statesAndValues);
		
			setStateMask(newStateBits, explicitnessMask);
		}
	}
	
	long getTimeInState(int stateOrdinal)
	{
		int bit = (0x1 << stateOrdinal);
		
		if( (bit & m_stateMask) != 0x0 )
		{
			return System.currentTimeMillis() - m_timesInState[stateOrdinal];
		}
		else
		{
			return m_timesInState[stateOrdinal];
		}
	}
	
	private void setStateMask(int newStateBits, int explicitnessMask)
	{
		int oldStateBits = m_stateMask;
		m_stateMask = newStateBits;
		
		//--- DRK > Minor skip optimization...shouldn't actually skip (too much) in practice
		//---		if other parts of the library are handling their state tracking sanely.
		if( oldStateBits != newStateBits )
		{
			for( int i = 0, bit = 0x1; i < m_timesInState.length; i++, bit <<= 0x1 )
			{
				//--- DRK > State exited...
				if( (oldStateBits & bit) != 0x0 && (newStateBits & bit) == 0x0 )
				{
					m_timesInState[i] = System.currentTimeMillis() - m_timesInState[i];
				}
				//--- DRK > State entered...
				else if( (oldStateBits & bit) == 0x0 && (newStateBits & bit) != 0x0 )
				{
					m_timesInState[i] = System.currentTimeMillis();
				}
				else
				{
					explicitnessMask &= ~bit;
				}
			}
		}
		else
		{
			explicitnessMask = 0x0;
		}
		
		fireStateChange(oldStateBits, newStateBits, explicitnessMask);
	}
	
	protected abstract void onStateChange(int oldStateBits, int newStateBits, int explicitnessMask);
	
	private void fireStateChange(int oldStateBits, int newStateBits, int explicitnessMask)
	{
		if( oldStateBits == newStateBits )
		{
			//--- DRK > Should sorta have an assert here but there's a few
			//---		valid cases (like implicit connection fail) where it's valid
			//---		so we'd have to be a little more intelligent about asserting.
//			U_Bt.ASSERT(false);
			return;
		}
		
		onStateChange(oldStateBits, newStateBits, explicitnessMask);
	}
	
	protected String toString(BitwiseEnum[] enums)
	{
		synchronized ( m_lock )
		{
			String toReturn = "";
			
			for( int i = 0; i < enums.length; i++ )
			{
				if( enums[i].overlaps(m_stateMask) )
				{
					toReturn += " " + enums[i].name().toUpperCase();
				}
				else
				{
//					toReturn += enums[i].name().toLowerCase();
				}
			}
			
			return toReturn;
		}
	}
}
