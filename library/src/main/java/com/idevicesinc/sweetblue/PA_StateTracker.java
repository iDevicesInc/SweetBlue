package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;

abstract class PA_StateTracker
{
	static enum E_Intent
	{
		INTENTIONAL, UNINTENTIONAL;
		
		public int getMask()
		{
			return this == INTENTIONAL ? 0xFFFFFFFF : 0x0;
		}
		
		public State.ChangeIntent convert()
		{
			switch(this)
			{
				case INTENTIONAL:	  return State.ChangeIntent.INTENTIONAL;
				case UNINTENTIONAL:	  return State.ChangeIntent.UNINTENTIONAL;
			}
			
			return State.ChangeIntent.NULL;
		}
	}
	
	private int m_stateMask = 0x0;

	private final long[] m_timesInState;
	private final int m_stateCount;
	
	PA_StateTracker(final State[] enums, final boolean trackTimes)
	{
		m_stateCount = enums.length;
		m_timesInState = trackTimes ? new long[m_stateCount] : null;
	}
	
	PA_StateTracker(final State[] enums)
	{
		this(enums, /*trackTimes=*/true);
	}
	
	public boolean is(State state)
	{
		return checkBitMatch(state, true);
	}
	
	public int getState()
	{
		return m_stateMask;
	}
	
	boolean checkBitMatch(State flag, boolean value)
	{
		return ((flag.bit() & m_stateMask) != 0) == value;
	}
	
	private int getMask(final int currentStateMask, final Object[] statesAndValues)
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
			
			State state = (State) statesAndValues[i];
			boolean append = true;
			
			if( statesAndValues[i+1] instanceof Boolean )
			{
				append = (Boolean) statesAndValues[i+1];
				i++;
			}

			// TODO - Investigate this further to attempt to find the root cause
			// Sometimes we get a weird BLE state back from the native stack. For now, we'll just ignore it.
			if (state != null)
			{
				if (append)
				{
					append_assert(state);

					newStateBits |= state.bit();
				}
				else
				{
					newStateBits &= ~state.bit();
				}
			}
		}
		
		return newStateBits;
	}
	
	void append(State newState, E_Intent intent, int status)
	{
		if( newState./*already*/overlaps(m_stateMask) )
		{
//			m_logger.w("Already in state: " + newState);

			return;
		}

		append_assert(newState);

		setStateMask(m_stateMask | newState.bit(), intent == E_Intent.INTENTIONAL ? newState.bit() : 0x0, status);
	}
	
	void remove(State state, E_Intent intent, int status)
	{
		setStateMask(m_stateMask & ~state.bit(), intent == E_Intent.INTENTIONAL ? state.bit() : 0x0, status);
	}
	
	protected void append_assert(State newState){}
	
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
	
	void set(final E_Intent intent, final int status, final Object ... statesAndValues)
	{
		set(intent.getMask(), status, statesAndValues);
	}
	
	private void set(final int intentMask, final int status, final Object ... statesAndValues)
	{
		final int newStateBits = getMask(0x0, statesAndValues);

		setStateMask(newStateBits, intentMask, status);
	}
	
	void update(E_Intent intent, int status, Object ... statesAndValues)
	{
		update(intent.getMask(), status, statesAndValues);
	}
	
	private void update(int intentMask, int status, Object ... statesAndValues)
	{
		int newStateBits = getMask(m_stateMask, statesAndValues);

		setStateMask(newStateBits, intentMask, status);
	}
	
	long getTimeInState(int stateOrdinal)
	{
		if( m_timesInState == null )  return 0;
		
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
	
	protected void copy(PA_StateTracker stateTracker)
	{
		this.setStateMask(stateTracker.getState(), 0x0, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	private long currentTime()
	{
		if (BleManager.s_instance == null)
		{
			return System.currentTimeMillis();
		}
		return BleManager.s_instance.currentTime();
	}

	private void setStateMask(final int newStateBits, int intentMask, final int status)
	{
		int oldStateBits = m_stateMask;
		m_stateMask = newStateBits;
		
		//--- DRK > Minor skip optimization...shouldn't actually skip (too much) in practice
		//---		if other parts of the library are handling their state tracking sanely.
		if( oldStateBits != newStateBits )
		{
			for( int i = 0, bit = 0x1; i < m_stateCount; i++, bit <<= 0x1 )
			{
				//--- DRK > State exited...
				if( (oldStateBits & bit) != 0x0 && (newStateBits & bit) == 0x0 )
				{
					if( m_timesInState != null )
					{
						m_timesInState[i] = currentTime() - m_timesInState[i];
					}
				}
				//--- DRK > State entered...
				else if( (oldStateBits & bit) == 0x0 && (newStateBits & bit) != 0x0 )
				{
					if( m_timesInState != null )
					{
						m_timesInState[i] = currentTime();
					}
				}
				else
				{
					intentMask &= ~bit;
				}
			}
		}
		else
		{
			intentMask = 0x0;
		}
		
		fireStateChange(oldStateBits, newStateBits, intentMask, status);
	}
	
	protected abstract void onStateChange(int oldStateBits, int newStateBits, int intentMask, int status);
	
	private void fireStateChange(int oldStateBits, int newStateBits, int intentMask, int status)
	{
		if( oldStateBits == newStateBits )
		{
			//--- DRK > Should sorta have an assert here but there's a few
			//---		valid cases (like implicit connection fail) where it's valid
			//---		so we'd have to be a little more intelligent about asserting.
//			U_Bt.ASSERT(false);
			return;
		}
		
		onStateChange(oldStateBits, newStateBits, intentMask, status);
	}
	
	protected String toString(State[] enums)
	{
		return Utils_String.toString(m_stateMask, enums);
	}
}
