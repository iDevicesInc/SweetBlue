package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;

/**
 * Used to set time-based options in {@link BleManagerConfig} and for various methods of
 * {@link BleManager} and {@link BleDevice}. An {@link Interval} is a self-documenting
 * and "type-comfortable" way of representing time instead of using naked numeric primitives.
 *  
 * 
 */
public class Interval
{
	private static final double DISABLED_VALUE = -1.0;
	
	/**
	 * Use this special value to disable options in {@link BleManagerConfig}.
	 */
	public static final Interval DISABLED = Interval.seconds(DISABLED_VALUE);
	
	/**
	 * Use this special value to signify positive infinite.
	 */
	public static final Interval INFINITE = Interval.seconds(Double.POSITIVE_INFINITY);
	
	
	public final double seconds;
	public final long milliseconds;
	
	private Interval(double seconds_in, long milliseconds_in)
	{
		this.seconds = seconds_in;
		this.milliseconds = milliseconds_in;
	}

	/**
	 * Returns a new {@link Interval} representing the given number of seconds.
	 */
	public static Interval seconds(double value)
	{
		return new Interval(value, (long) (value*1000));
	}
	
	/**
	 * Returns a new {@link Interval} representing the given number of milliseconds.
	 */
	public static Interval milliseconds(long milliseconds)
	{
		return new Interval(((double)milliseconds)/1000.0, milliseconds);
	}
	
	/**
	 * Returns the double values as seconds from a given nullable {@link Interval}.
	 */
	public static double asDouble(Interval interval_nullable)
	{
		if( interval_nullable == null )
		{
			return Interval.DISABLED.seconds;
		}
		
		return interval_nullable.seconds;
	}
	
	/**
	 * Returns true if the given {@link Interval} is not <code>null</code>
	 * and its value is greater than zero.
	 */
	public static boolean isEnabled(Interval interval_nullable)
	{
		return !isDisabled(interval_nullable);
	}
	
	/**
	 * Same as {@link #isEnabled(Interval)}.
	 */
	public static boolean isEnabled(double interval)
	{
		return !isDisabled(interval);
	}
	
	/**
	 * Returns true if the given {@link Interval} is either <code>null</code>
	 * or its value is less than or equal to zero.
	 */
	public static boolean isDisabled(Interval interval_nullable)
	{
		if( interval_nullable == null )
		{
			return true;
		}
		
		return isDisabled(interval_nullable.seconds);
	}
	
	/**
	 * Same as {@link #isDisabled(Interval)}.
	 */
	public static boolean isDisabled(double interval)
	{
		return interval <= 0.0;
	}
	
	@Override public boolean equals(Object object)
	{
		if( object != null && object instanceof Interval )
		{
			return ((Interval)object).seconds == this.seconds;
		}
		
		return super.equals(object);
	}
	
	@Override public String toString()
	{
		return seconds+"secs/"+milliseconds+"millis"; 
	}
}
