package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.*;
import com.idevicesinc.sweetblue.annotations.*;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;

/**
 * Used to set time-based options in {@link BleManagerConfig} and {@link BleDeviceConfig} and
 * for various methods and callbacks of {@link BleManager} and {@link BleDevice}. An {@link Interval} is a
 * self-documenting and "type-comfortable" way of representing time instead of using naked numeric primitives.
 */
@Immutable
public final class Interval extends Unit<Interval>
{
	private static final double DISABLED_VALUE = -1.0;

	/**
	 * Use this special value to disable options in {@link BleDeviceConfig} and {@link BleManagerConfig}.
	 */
	public static final Interval DISABLED		= Interval.secs(DISABLED_VALUE);
	
	/**
	 * Use this special value to signify positive infinite.
	 */
	public static final Interval INFINITE		= Interval.secs(Double.POSITIVE_INFINITY);
	
	/**
	 * Convenience value for zero time.
	 */
	public static final Interval ZERO			= Interval.secs(0.0);
	
	/**
	 * Convenience value representing one second.
	 */
	public static final Interval ONE_SEC		= Interval.secs(1.0);
	
	/**
	 * Convenience value representing five seconds.
	 */
	public static final Interval FIVE_SECS		= Interval.secs(5.0);
	
	/**
	 * Convenience value representing ten seconds.
	 */
	public static final Interval TEN_SECS		= Interval.secs(10.0);


	
	private final double m_secs;
	private final long m_millis;
	
	private Interval(double secs_in, long millis_in)
	{
		this.m_secs = secs_in;
		this.m_millis = millis_in;
	}
	
	/**
	 * Returns the value of this interval in seconds.
	 */
	public double secs()
	{
		return m_secs;
	}
	
	/**
	 * Returns the value of this interval in milliseconds.
	 */
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
	 * Returns a new {@link Interval} representing the given number of minutes.
	 */
	public static Interval mins(final int value)
	{
		return secs(value*60);
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
	public static double secs(@Nullable(Prevalence.NORMAL) Interval interval_nullable)
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
	public static boolean isEnabled(@Nullable(Prevalence.NORMAL) Interval interval_nullable)
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
	public static boolean isDisabled(@Nullable(Prevalence.NORMAL) Interval interval_nullable)
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
	public static boolean isDisabled(Double interval_nullable)
	{
		return interval_nullable == null || interval_nullable <= 0.0;
	}
	
	@Override public String toString()
	{
		return Utils_String.toFixed(secs())+"secs/"+millis()+"millis";
	}

	@Override protected double getRawValue()
	{
		return m_secs;
	}
	
	@Override protected Unit<Interval> newInstance(double rawValue)
	{
		return secs(rawValue);
	}
}
