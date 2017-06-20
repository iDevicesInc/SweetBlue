package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;

/**
 * Class representing a range of time between two instances of {@link com.idevicesinc.sweetblue.utils.EpochTime}.
 * This is similar to {@link Interval} but stores the actual beginning and end times and is generally meant for longer
 * periods of time.
 */
@Immutable
public final class EpochTimeRange implements UsesCustomNull
{
	/**
	 * Fulfills the soft contract of {@link com.idevicesinc.sweetblue.utils.UsesCustomNull} - used for {@link #isNull()}.
	 */
	public static final EpochTimeRange NULL = new EpochTimeRange(EpochTime.NULL, EpochTime.NULL);

	public static final EpochTimeRange FROM_MIN_TO_1970 = new EpochTimeRange(EpochTime.MIN, EpochTime.ZERO);

	public static final EpochTimeRange FROM_MIN_TO_MAX = new EpochTimeRange(EpochTime.MIN, EpochTime.MAX);

	public static final EpochTimeRange FROM_1970_TO_MAX = new EpochTimeRange(EpochTime.ZERO, EpochTime.MAX);

	public static final EpochTimeRange ZERO = new EpochTimeRange(EpochTime.ZERO, EpochTime.ZERO);

	public static final EpochTimeRange FIVE_SECONDS = new EpochTimeRange(EpochTime.ZERO, new EpochTime(1000 * 5));

	/**
	 * Returns a new instance representing the time range from the given value to {@link Long#MAX_VALUE}.
	 */
	public static EpochTimeRange fromGiven_toMax(final EpochTime from)
	{
		return new EpochTimeRange(from, EpochTime.MAX);
	}

	/**
	 * Returns a new instance representing the time range from the given value to now.
	 */
	public static EpochTimeRange fromGiven_toNow(final EpochTime from)
	{
		return new EpochTimeRange(from, EpochTime.now());
	}

	/**
	 * Basically just a more readable overload for the normal constructor {@link #EpochTimeRange(EpochTime, EpochTime)}.
	 */
	public static EpochTimeRange fromGiven_toGiven(final EpochTime from, final EpochTime to)
	{
		return new EpochTimeRange(from, EpochTime.now());
	}

	/**
	 * Returns a new instance representing the time range from 1970 to the given value.
	 */
	public static EpochTimeRange from1970_toGiven(final EpochTime to)
	{
		return new EpochTimeRange(EpochTime.ZERO, to);
	}

	/**
	 * Returns a new instance representing the time range from {@link Long#MIN_VALUE} to the given value.
	 */
	public static EpochTimeRange fromMin_toGiven(final EpochTime to)
	{
		return new EpochTimeRange(EpochTime.MIN, to);
	}

	/**
	 * Returns a new instance representing the time range from 1970 to now.
	 */
	public static EpochTimeRange from1970_toNow()
	{
		return new EpochTimeRange(EpochTime.ZERO, EpochTime.now());
	}

	/**
	 * Returns a new instance representing an instant zero length time range.
	 */
	public static EpochTimeRange instant(final EpochTime time)
	{
		return new EpochTimeRange(time, time);
	}

	public static @Nullable(Nullable.Prevalence.NEVER) EpochTimeRange denull(@Nullable(Nullable.Prevalence.NORMAL) final EpochTimeRange range_nullable)
	{
		return range_nullable != null ? range_nullable : NULL;
	}

	private final EpochTime m_from;
	private final EpochTime m_to;

	/**
	 * Returns a new instance representing the time range between the given epoch times.
	 */
	public EpochTimeRange(final EpochTime from, final EpochTime to)
	{
		m_from = from != null ? from : EpochTime.NULL;
		m_to = to != null ? to : EpochTime.NULL;
	}

	/**
	 * Returns a new instance representing the time range between the given epoch times as primitive longs.
	 */
	public EpochTimeRange(final long from, final long to)
	{
		this(new EpochTime(from), new EpochTime(to));
	}

	/**
	 * Returns the "from" date passed into the constructor,
	 * or {@link com.idevicesinc.sweetblue.utils.EpochTime#NULL} if <code>null</code>
	 * was originally passed in.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) EpochTime from()
	{
		return m_from;
	}

	/**
	 * Returns the "to" date passed into the constructor,
	 * or {@link com.idevicesinc.sweetblue.utils.EpochTime#NULL} if <code>null</code>
	 * was originally passed in.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) EpochTime to()
	{
		return m_to;
	}

	/**
	 * Returns <code>true</code> if {@link #from()} is less than or equal to {@link #to()}.
	 */
	public boolean isValid()
	{
		return m_from.toMilliseconds() < m_to.toMilliseconds();
	}

	/**
	 * Returns <code>true</code> if {@link #from()} is greater than {@link #to()}.
	 */
	public boolean isInvalid()
	{
		return !isValid();
	}

	/**
	 * Returns <code>true</code> if {@link #from()} and {@link #to()} are equal.
	 */
	public boolean isZero()
	{
		return m_from.toMilliseconds() == m_to.toMilliseconds();
	}

	/**
	 * Returns the raw milliseconds between {@link #from()} and {@link #to()}.
	 */
	public long getDelta()
	{
		return to().toMilliseconds() - from().toMilliseconds();
	}

	/**
	 * See {@link #isInvalid()} and {@link #isZero()}.
	 */
	public boolean isInvalidOrZero()
	{
		return isInvalidOrZero();
	}

	/**
	 * Returns <code>true</code> if <code>this</code> is referentially equal to {@link #NULL}.
	 */
	@Override public boolean isNull()
	{
		return this == NULL;
	}

	/**
	 * "less than" comparison.
	 */
	public boolean lt(final EpochTimeRange otherRange)
	{
		return this.getDelta() < otherRange.getDelta();
	}

	/**
	 * "less than or equal" comparison.
	 */
	public boolean lte(final EpochTimeRange otherRange)
	{
		return this.getDelta() <= otherRange.getDelta();
	}

	/**
	 * "greater than" comparison.
	 */
	public boolean gt(final EpochTimeRange otherRange)
	{
		return this.getDelta() > otherRange.getDelta();
	}

	/**
	 * "greater than or equal" comparison.
	 */
	public boolean gte(final EpochTimeRange otherRange)
	{
		return this.getDelta() >= otherRange.getDelta();
	}
}
