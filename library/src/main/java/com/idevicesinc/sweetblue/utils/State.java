package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleDevice.StateListener.StateEvent;
import com.idevicesinc.sweetblue.BleManager.StateListener.*;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;

/**
 * Bitwise enum contract for representing the state of devices and managers.
 * Implementations are {@link BleDeviceState} and {@link BleManagerState}.
 * Not intended for subclassing outside this library but go wild if you want.
 */
public interface State extends UsesCustomNull, BitwiseEnum
{
	/**
	 * Abstract base class for {@link com.idevicesinc.sweetblue.BleDevice.StateListener.StateEvent} and {@link com.idevicesinc.sweetblue.BleManager.StateListener.StateEvent}.
	 */
	public static abstract class ChangeEvent<T_State extends State> extends Event
	{
		/**
		 * The bitwise representation of the {@link BleDevice} or {@link BleManager}
		 * before the event took place.
		 */
		public int oldStateBits(){  return m_oldStateBits;  }
		private final int m_oldStateBits;
		
		/**
		 * The new and now current bitwise representation of a {@link BleDevice}
		 * or {@link BleManager}. Will be the same as {@link BleDevice#getStateMask()}
		 * or {@link BleManager#getStateMask()}.
		 */
		public int newStateBits(){  return m_newStateBits;  }
		private final int m_newStateBits;
		
		/**
		 * For each old->new bit difference, this mask will tell you if the transition was intentional. "Intentional" generally means a call was made to
		 * a public method of the library from app-code to trigger the state change, and so usually the stacktrace started from a user input event upstream.
		 * Otherwise the given bit will be 0x0 and so the state change was "unintentional". An example of intentional is if you call
		 * {@link BleDevice#disconnect()} in response to a button click, whereas unintentional would be if the device disconnected because it
		 * went out of range. As much as possible these flags are meant to represent the actual app <i>user's</i> intent through the app, not
		 * the intent of you the programmer, nor the intent of the user outside the bounds of the app, like disconnecting by turning the peripheral off.
		 * For example after a disconnect you might be using {@link BleManagerConfig#reconnectFilter} to try periodically
		 * reconnecting. From you the programmer's perspective a connect, if/when it happens, is arguably an intentional action. From the user's
		 * perspective however the connect was unintentional. Therefore this mask is currently meant to serve an analytics or debugging role,
		 * not to necessarily gate application logic.
		 */
		public int intentMask(){  return m_intentMask;  }
		private final int m_intentMask;
		
		protected ChangeEvent(int oldStateBits, int newStateBits, int intentMask)
		{
			this.m_oldStateBits = oldStateBits;
			this.m_newStateBits = newStateBits;
			this.m_intentMask = intentMask;
		}
		
		/**
		 * Returns all the states that were entered as a bit mask.
		 */
		public int enterMask()
		{
			return newStateBits() & ~oldStateBits();
		}
		
		/**
		 * Returns all the states that were exited as a bit mask.
		 */
		public int exitMask()
		{
			return oldStateBits() & ~newStateBits();
		}
		
		/**
		 * Convenience forwarding of {@link State#didEnter(int, int)}.
		 */
		public boolean didEnter(T_State state)
		{
			return state.didEnter(oldStateBits(), newStateBits());
		}
		
		/**
		 * Convenience forwarding of {@link State#didExit(int, int)}.
		 */
		public boolean didExit(T_State state)
		{
			return state.didExit(oldStateBits(), newStateBits());
		}

		/**
		 * Returns <code>true</code> if {@link #didEnter(State)} or {@link #didExit(State)} return true.
		 */
		public boolean isFor(T_State state)
		{
			return didEnter(state) || didExit(state);
		}

		/**
		 * Returns <code>true</code> if {@link #didEnterAny(State...)} or {@link #didExitAny(State...)} return true.
		 */
		public boolean isForAny(T_State ... states)
		{
			return didEnterAny(states) || didExitAny(states);
		}

		/**
		 * Returns <code>true</code> if {@link #didEnterAll(State...)} or {@link #didExitAll(State...)} return true.
		 */
		public boolean isForAll(T_State ... states)
		{
			return didEnterAll(states) || didExitAll(states);
		}
		
		/**
		 * Convenience to return <code>true</code> if {@link #didEnter(State)} returns <code>true</code> on any of the {@link State} instances given.
		 */
		public boolean didEnterAny(T_State ... states)
		{
			for( int i = 0; i < states.length; i++ )
			{
				if( didEnter(states[i]) )  return true;
			}
			
			return false;
		}
		
		/**
		 * Convenience to return <code>true</code> if {@link #didExit(State)} returns <code>true</code> on any of the {@link State} instances given.
		 */
		public boolean didExitAny(T_State ... states)
		{
			for( int i = 0; i < states.length; i++ )
			{
				if( didExit(states[i]) )  return true;
			}
			
			return false;
		}
		
		/**
		 * Convenience to return <code>true</code> if {@link #didEnter(State)} returns <code>true</code> for all the {@link State} instances given.
		 */
		public boolean didEnterAll(T_State ... states)
		{
			for( int i = 0; i < states.length; i++ )
			{
				if( !didEnter(states[i]) )  return false;
			}
			
			return true;
		}
		
		/**
		 * Convenience to return <code>true</code> if {@link #didExit(State)} returns <code>true</code> for all the {@link State} instances given.
		 */
		public boolean didExitAll(T_State ... states)
		{
			for( int i = 0; i < states.length; i++ )
			{
				if( !didExit(states[i]) )  return false;
			}
			
			return true;
		}
		
		/**
		 * Returns the intention behind the state change, or {@link ChangeIntent#NULL} if no state
		 * change for the given state occurred.
		 */
		public ChangeIntent getIntent(T_State state)
		{
			if( (state.bit() & oldStateBits()) == (state.bit() & newStateBits()) )
			{
				return ChangeIntent.NULL;
			}
			else
			{
				return state.overlaps(intentMask()) ? ChangeIntent.INTENTIONAL : ChangeIntent.UNINTENTIONAL;
			}
		}
	}
	
	/**
	 * Enumerates the intention behind a state change - as comprehensively as possible, whether the
	 * application user intended for the state change to happen or not. See {@link ChangeEvent#intentMask()} for more
	 * discussion on user intent.
	 */
	public static enum ChangeIntent
	{
		/**
		 * Used instead of Java's built-in <code>null</code> wherever appropriate.
		 */
		NULL,
		
		/**
		 * The state change was not intentional.
		 */
		UNINTENTIONAL,
		
		/**
		 * The state change was intentional.
		 */
		INTENTIONAL;
		
		private static final int DISK_VALUE__NULL				= -1;
		private static final int DISK_VALUE__UNINTENTIONAL		=  0;
		private static final int DISK_VALUE__INTENTIONAL		=  1;
		
		/**
		 * The integer value to write to disk. Not using ordinal to avoid
		 * unintentional consequences of changing enum order by accident or something.
		 */
		public int toDiskValue()
		{
			switch(this)
			{
				case INTENTIONAL:		return DISK_VALUE__INTENTIONAL;
				case UNINTENTIONAL:		return DISK_VALUE__UNINTENTIONAL;
				case NULL:
				default:				return DISK_VALUE__NULL;
			}
		}
		
		public static int toDiskValue(final ChangeIntent intent_nullable)
		{
			if( intent_nullable == null )	return NULL.toDiskValue();
			else							return intent_nullable.toDiskValue();
		}
		
		/**
		 * Transforms {@link #toDiskValue()} back to the enum.
		 * Returns {@link #NULL} if diskValue can't be resolved.
		 */
		public static ChangeIntent fromDiskValue(int diskValue)
		{
			ChangeIntent[] values = values();
			for( int i = 0; i < values.length; i++ )
			{
				if( values[i].toDiskValue() == diskValue )
				{
					return values[i];
				}
			}
			
			return NULL;
		}

		public int bits()
		{
			return this == INTENTIONAL ? 0xFFFFFFFF : 0x00000000;
		}
	}
	
	/**
	 * Given an old and new state mask, for example from {@link com.idevicesinc.sweetblue.BleDevice.StateListener#onEvent(com.idevicesinc.sweetblue.BleDevice.StateListener.StateEvent)}
	 *  or {@link com.idevicesinc.sweetblue.BleManager.StateListener#onEvent(com.idevicesinc.sweetblue.BleManager.StateListener.StateEvent)}, this method tells you whether the
	 * the 'this' state was appended.
	 * 
	 * @see #didExit(int, int)
	 */
	boolean didEnter(int oldStateBits, int newStateBits);
	
	/**
	 * Reverse of {@link #didEnter(int, int)}.
	 * 
	 * @see #didEnter(int, int)
	 */
	boolean didExit(int oldStateBits, int newStateBits);
	
	/**
	 * Returns <code>true</code> if this state is meant to stand in for Java's built-in <code>null</code>.
	 */
	boolean isNull();
}
