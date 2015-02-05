package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.*;

/**
 * Used to set time-based options in {@link BleManagerConfig} and {@link BleDeviceConfig} and
 * for various methods and callbacks of {@link BleManager} and {@link BleDevice}. An {@link Interval} is a
 * self-documenting and "type-comfortable" way of representing time instead of using naked numeric primitives.
 */
public class Interval
{
	private static final double DISABLED_VALUE = -1.0;
	
	/**
	 * Use this special value to disable options in {@link BleDeviceConfig} and {@link BleManagerConfig}.
	 */
	public static final Interval DISABLED = Interval.secs(DISABLED_VALUE);
	
	/**
	 * Use this special value to signify positive infinite.
	 */
	public static final Interval INFINITE = Interval.secs(Double.POSITIVE_INFINITY);
	
	/**
	 * Convenience value for zero time.
	 */
	public static final Interval ZERO = Interval.secs(0.0);
	
	private final double m_secs;
	private final long m_millis;
	
	private Interval(double secs_in, long millis_in)
	{
		this.m_secs = secs_in;
		this.m_millis = millis_in;
	}
	
	public double secs()
	{
		return m_secs;
	}
	
	public long millis()
	{
		return m_millis;
	}

	/**
	 * Returns a new {@link Interval} representing the given number of seconds.
	 */
	public static Interval secs(double value)
	{
		return new Interval(value, (long) (value*1000));
	}
	
	/**
	 * Returns a new {@link Interval} representing the given number of milliseconds.
	 */
	public static Interval millis(long milliseconds)
	{
		return new Interval(((double)milliseconds)/1000.0, milliseconds);
	}
	
	/**
	 * Returns a new {@link Interval} representing the time since the given past epoch time,
	 * using {@link System#currentTimeMillis()}.
	 */
	public static Interval since(long epochTime_milliseconds)
	{
		return Interval.delta(epochTime_milliseconds, System.currentTimeMillis());
	}
	
	/**
	 * Returns a new {@link Interval} representing the delta between the two epoch times.
	 */
	public static Interval delta(long earlierTime_millis, long laterTime_millis)
	{
		return Interval.millis(laterTime_millis - earlierTime_millis);
	}
	
	/**
	 * Returns the double values as seconds from a given nullable {@link Interval}.
	 */
	public static double asDouble(Interval interval_nullable)
	{
		if( interval_nullable == null )
		{
			return Interval.DISABLED.m_secs;
		}
		
		return interval_nullable.m_secs;
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
		
		return isDisabled(interval_nullable.m_secs);
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
			return ((Interval)object).m_secs == this.m_secs;
		}
		
		return super.equals(object);
	}
	
	@Override public String toString()
	{
		return m_secs+"secs/"+m_millis+"millis"; 
	}
}
