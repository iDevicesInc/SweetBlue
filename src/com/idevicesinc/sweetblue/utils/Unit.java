package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.Immutable;

/**
 * Common abstract base class for all units of measurement, providing default convenience methods
 * for things like comparison.
 *
 * @param <T> Used to enforce just-in-case subclass type checking for methods like {@link #lt(Unit)} so you can't compare one subclass with another subclass.
 */
@Immutable
public abstract class Unit<T>
{
	/**
	 * "less than" comparison.
	 */
	public boolean lt(final Unit<T> otherUnit)
	{
		return this.getRawValue() < otherUnit.getRawValue();
	}
	
	/**
	 * "less than or equal" comparison.
	 */
	public boolean lte(final Unit<T> otherUnit)
	{
		return this.getRawValue() <= otherUnit.getRawValue();
	}
	
	/**
	 * "greater than" comparison.
	 */
	public boolean gt(final Unit<T> otherUnit)
	{
		return this.getRawValue() > otherUnit.getRawValue();
	}
	
	/**
	 * "greater than or equal" comparison.
	 */
	public boolean gte(final Unit<T> otherUnit)
	{
		return this.getRawValue() >= otherUnit.getRawValue();
	}
	
	/**
	 * "less than" comparison.
	 */
	public boolean lt(final double otherUnit)
	{
		return this.getRawValue() < otherUnit;
	}
	
	/**
	 * "less than or equal" comparison.
	 */
	public boolean lte(final double otherUnit)
	{
		return this.getRawValue() <= otherUnit;
	}
	
	/**
	 * "greater than" comparison.
	 */
	public boolean gt(final double otherUnit)
	{
		return this.getRawValue() > otherUnit;
	}
	
	/**
	 * "greater than or equal" comparison.
	 */
	public boolean gte(final double otherUnit)
	{
		return this.getRawValue() >= otherUnit;
	}
	
	@Override public int hashCode()
	{
		return Double.valueOf(getRawValue()).hashCode();
	}
	
	@Override public boolean equals(Object object)
	{
		if( object != null && object instanceof Unit )
		{
			return ((Unit)object).getRawValue() == this.getRawValue();
		}
		
		return super.equals(object);
	}
	
	protected abstract double getRawValue();
}
