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
	
	PA_StateTracker(P_Logger logger)
	{
		m_logger = logger;
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
	
	void update(Object ... statesAndValues)
	{
		synchronized ( m_lock )
		{
			int newStateBits = getMask(m_stateMask, statesAndValues);
		
			setStateMask(newStateBits);
		}
	}
	
	void append(BitwiseEnum newState)
	{
		synchronized ( m_lock )
		{
			if( newState./*already*/overlaps(m_stateMask) )
			{
	//			m_logger.w("Already in state: " + newState);
				
				return;
			}
			
			append_assert(newState);
			
			setStateMask(m_stateMask | newState.bit());
		}
	}
	
	void remove(BitwiseEnum state)
	{
		synchronized ( m_lock )
		{
			setStateMask(m_stateMask & ~state.bit() );
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
	
	void set(Object ... statesAndValues)
	{
		synchronized ( m_lock )
		{
			int newStateBits = getMask(0x0, statesAndValues);
			
			setStateMask(newStateBits);
		}
	}
	
	private void setStateMask(int newStateBits)
	{
		int oldStateBits = m_stateMask;
		m_stateMask = newStateBits;
		
		fireStateChange(oldStateBits, newStateBits);
	}
	
	protected abstract void onStateChange(int oldStateBits, int newStateBits);
	
	private void fireStateChange(int oldStateBits, int newStateBits)
	{
		if( oldStateBits == newStateBits )
		{
			//--- DRK > Should sorta have an assert here but there's a few
			//---		valid cases (like implicit connection fail) where it's valid
			//---		so we'd have to be a little more intelligent about asserting.
//			U_Bt.ASSERT(false);
			return;
		}
		
		onStateChange(oldStateBits, newStateBits);
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
