package com.idevicesinc.sweetblue.utils;

/**
 * Wrapper for a positive-only physical distance supporting various units of measurement.
 */
public class Distance
{
	public static final double FEET_PER_METER = 3.28084;
	
	public static final Distance ZERO		= meters(0.0);
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
	
	@Override public boolean equals(Object object)
	{
		if( object instanceof Distance )
		{
			return ((Distance)object).meters() == this.meters();
		}
		
		return super.equals(object);
	}
	
	@Override public String toString()
	{
		return meters()+"meters/"+feet()+"feet"; 
	}
}
