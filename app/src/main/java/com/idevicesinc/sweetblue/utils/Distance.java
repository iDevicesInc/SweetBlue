package com.idevicesinc.sweetblue.utils;


import com.idevicesinc.sweetblue.annotations.*;

/**
 * Wrapper for a positive-only physical distance supporting various units of measurement.
 */
@Immutable
public final class Distance extends Unit<Distance>
{
	public static final double FEET_PER_METER = 3.28084;
	
	/**
	 * Convenience value for zero meters.
	 */
	public static final Distance ZERO		= meters(0.0);
	
	/**
	 * Convenience value representing an invalid/impossible distance, arbitrarily chosen to be negative one meter.
	 */
	public static final Distance INVALID	= meters(-1.0);
	
	private final double m_meters;
	
	private Distance(double meters)
	{
		m_meters = meters;
	}
	
	/**
	 * Returns the value of this distance in meters.
	 */
	public double meters()
	{
		return m_meters;
	}
	
	/**
	 * Returns the value of this distance in feet.
	 */
	public double feet()
	{
		return m_meters * FEET_PER_METER;
	}
	
	/**
	 * Creates a new value in meters.
	 */
	public static Distance meters(double meters)
	{
		return new Distance(meters);
	}
	
	/**
	 * Creates a new value in feet.
	 */
	public static Distance feet(double feet)
	{
		return meters(feet/FEET_PER_METER);
	}
	
	/**
	 * Returns <code>true</code> if {@link #meters()} is >= 0.
	 */
	public boolean isValid()
	{
		return meters() >= 0.0;
	}
	
	@Override public String toString()
	{
		if( !isValid() )
		{
			return "INVALID";
		}
		else
		{
			return Utils_String.toFixed(meters())+"meters/"+Utils_String.toFixed(feet())+"feet";
		}
	}

	@Override protected double getRawValue()
	{
		return m_meters;
	}

	@Override protected Unit<Distance> newInstance(double rawValue)
	{
		return meters(rawValue);
	}
}
