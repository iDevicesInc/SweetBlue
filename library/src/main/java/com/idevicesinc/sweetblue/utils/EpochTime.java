package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.Immutable;

import java.util.Date;

/**
 * A class fulfilling a similar role to Java's built-in {@link java.util.Date}, i.e.
 * milliseconds since 1970. We don't use {@link java.util.Date} because it is mutable,
 * which makes certain APIs in SweetBlue harder to design.
 */
@Immutable
public final class EpochTime implements Comparable<EpochTime>, UsesCustomNull
{
	/**
	 * Fulfills the soft contract of {@link com.idevicesinc.sweetblue.utils.UsesCustomNull} - used for {@link #isNull()}.
	 */
	public static final EpochTime NULL = new EpochTime(Long.MIN_VALUE);

	/**
	 * Convenience instance using {@link Long#MIN_VALUE}.
	 */
	public static final EpochTime MIN = new EpochTime(Long.MIN_VALUE);

	/**
	 * Convenience instance using {@link Long#MAX_VALUE}.
	 */
	public static final EpochTime MAX = new EpochTime(Long.MAX_VALUE);

	/**
	 * Convenience instance representing the start date of 1970.
	 */
	public static final EpochTime ZERO = new EpochTime(0);

	/**
	 * Simply a more readable version of the default constructor {@link EpochTime#EpochTime()}.
	 */
	public static EpochTime now()
	{
		return new EpochTime();
	}

	private final long m_millisecondsSince1970;

	/**
	 * Converts from Java's built-in date.
	 */
	public EpochTime(final Date date)
	{
		this(date.getTime());
	}

	/**
	 * Constructs a new instance using milliseconds since 1970.
	 */
	public EpochTime(final long millisecondsSince1970)
	{
		m_millisecondsSince1970 = millisecondsSince1970;
	}

	/**
	 * Constructs a new instance using {@link System#currentTimeMillis()}.
	 */
	public EpochTime()
	{
		this(System.currentTimeMillis());
	}

	/**
	 * Converts to milliseconds since 1970, which is currently also how the value is stored internally.
	 */
	public long toMilliseconds()
	{
		return m_millisecondsSince1970;
	}

	/**
	 * Converts to seconds since 1970.
	 */
	public double toSeconds()
	{
		return ((double)toMilliseconds())/1000.0;
	}

	/**
	 * Converts to Java's built-in date class.
	 */
	public Date toDate()
	{
		return new Date(toMilliseconds());
	}

	/**
	 * Does an inclusive check for <code>this</code> being in the given range.
	 */
	public boolean isBetween_inclusive(final EpochTime from, final EpochTime to)
	{
		return this.toMilliseconds() >= from.toMilliseconds() && this.toMilliseconds() <= to.toMilliseconds();
	}

	/**
	 * Overload of {@link #isBetween_inclusive(EpochTime, EpochTime)}.
	 */
	public boolean isBetween_inclusive(final EpochTimeRange range)
	{
		return isBetween_inclusive(range.from(), range.to());
	}

	@Override public int compareTo(final EpochTime another)
	{
		if( another == null )
		{
			return 1;
		}

		if (toMilliseconds() < another.toMilliseconds())
		{
			return -1;
		}

		if (toMilliseconds() == another.toMilliseconds())
		{
			return 0;
		}

		return 1;
	}

	/**
	 * Returns the delta resulting from <code>this-epochTime</code>.
	 */
	public EpochTimeRange minus(final EpochTime epochTime)
	{
		return EpochTimeRange.fromGiven_toGiven(epochTime, this);
	}

	/**
	 * Returns <code>true</code> if <code>this</code> is referentially equal to {@link #NULL}.
	 */
	@Override public boolean isNull()
	{
		return this == NULL;
	}

	@Override public String toString()
	{
		return toMilliseconds()+"";
	}
}
