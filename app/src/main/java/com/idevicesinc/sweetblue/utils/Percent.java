package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.*;

/**
 * Wrapper for a percentage value, generally from 0-100%;
 */
@Immutable
public final class Percent extends Unit<Percent>
{
	/**
	 * Convenience value representing 0%.
	 */
	public static final Percent ZERO		= Percent.fromInt(0);
	
	/**
	 * Convenience value representing 100%.
	 */
	public static final Percent HUNDRED		= Percent.fromInt(100);
	
	
	private final double m_doubleValue;
	
	private Percent(final double doubleValue)
	{
		m_doubleValue = doubleValue;
	}
	
	/**
	 * Returns a new instance clamped between 0% and 100%.
	 */
	public Percent clamp()
	{
		return Percent.fromDouble_clamped(this.toDouble());
	}
	
	/**
	 * Returns .5 for 50%, for example.
	 */
	public double toFraction()
	{
		return toDouble() / 100.0;
	}
	
	/**
	 * Returns the <code>double</code> value of this instance.
	 */
	public double toDouble()
	{
		return m_doubleValue;
	}
	
	/**
	 * <code>int</code> from {@link Math#round(double)}.
	 */
	public int toInt_round()
	{
		return (int) Math.round(m_doubleValue);
	}
	
	/**
	 * <code>int</code> from {@link Math#ceil(double)}.
	 */
	public int toInt_ceil()
	{
		return (int) Math.ceil(m_doubleValue);
	}
	
	/**
	 * <code>int</code> from {@link Math#floor(double)}.
	 */
	public int toInt_floor()
	{
		return (int) Math.floor(m_doubleValue);
	}
	
	/**
	 * Returns a new instance using the given <code>double</code> value.
	 */
	public static Percent fromDouble(final double value)
	{
		return new Percent(value);
	}
	
	/**
	 * Returns a new instance clamped between 0% and 100%, regardless of input value.
	 */
	public static Percent fromDouble_clamped(final double value)
	{
		return new Percent(clamp(value));
	}
	
	private static double clamp(final double rawValue)
	{
		return rawValue < 0.0 ? 0.0 : (rawValue > 100.0 ? 100.0 : rawValue);
	}
	
	/**
	 * Returns a new instance using the given <code>int</code> value.
	 */
	public static Percent fromInt(final int value)
	{
		return new Percent(value);
	}

	/**
	 * Returns a new instance clamped between 0% and 100%, regardless of input value.
	 */
	public static Percent fromInt_clamped(final int value)
	{
		return new Percent(clamp(value));
	}
	
	@Override public String toString()
	{
		return toInt_round()+"%";
	}

	@Override protected double getRawValue()
	{
		return m_doubleValue;
	}
	
	@Override protected Unit<Percent> newInstance(double rawValue)
	{
		return fromDouble(rawValue);
	}
}
