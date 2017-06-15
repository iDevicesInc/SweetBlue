package com.idevicesinc.sweetblue.utils;

import android.database.Cursor;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;

import java.text.DateFormat;
import java.util.Date;

/**
 * Simple struct wrapping arbitrary blob data as a byte array along with an epoch timestamp
 * marking when the blob was originally created/collected.
 */
@Immutable
public final class HistoricalData implements UsesCustomNull
{
	/**
	 * Special value that replaces Java's built-int <code>null</code> and
	 * is used everywhere Java's would otherwise be used.
	 */
	public static HistoricalData NULL = new HistoricalData(P_Const.EMPTY_BYTE_ARRAY, 0);


	public static @Nullable(Nullable.Prevalence.NEVER) HistoricalData denull(@Nullable(Nullable.Prevalence.NORMAL) final HistoricalData historicalData_nullable)
	{
		return historicalData_nullable != null ? historicalData_nullable : HistoricalData.NULL;
	}

	private final byte[] m_blob;
	private final EpochTime m_epochTime;

	public HistoricalData(final long millisecondsSince1970, final byte[] blob)
	{
		this(new EpochTime(millisecondsSince1970), blob);
	}

	public HistoricalData(final EpochTime epochTime, final byte[] blob)
	{
		this(blob, epochTime);
	}

	public HistoricalData(final byte[] blob, final long millisecondsSince1970)
	{
		this(blob, new EpochTime(millisecondsSince1970));
	}

	public HistoricalData(final byte[] blob, final EpochTime epochTime)
	{
		m_blob = blob != null ? blob : P_Const.EMPTY_BYTE_ARRAY;
		m_epochTime = epochTime != null ? epochTime : new EpochTime();
	}

	public HistoricalData(final byte[] blob)
	{
		this(blob, null);
	}

	public static @Nullable(Nullable.Prevalence.NEVER) HistoricalData fromCursor(final Cursor cursor)
	{
		if( cursor == null )
		{
			return HistoricalData.NULL;
		}

		final HistoricalData data = new HistoricalData(cursor.getLong(HistoricalDataColumn.EPOCH_TIME.getColumnIndex()), cursor.getBlob(HistoricalDataColumn.DATA.getColumnIndex()));

		return data;
	}

	/**
	 * Returns the timestamp passed into the constructor.
	 */
	public EpochTime getEpochTime()
	{
		return m_epochTime;
	}

	/**
	 * Convenience to return the timestamp as a {@link java.util.Date} instance.
	 */
	public Date getEpochTime_date()
	{
		return m_epochTime.toDate();
	}

	/**
	 * Convenience to return the epoch time as milliseconds since 1970.
	 */
	public long getEpochTime_millis()
	{
		return m_epochTime.toMilliseconds();
	}

	/**
	 * Convenience to return the timestamp as a formatted string, for example pass <code>new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")</code>.
	 */
	public String getEpochTime_dateString(final DateFormat dateFormat)
	{
		return dateFormat.format(getEpochTime_date());
	}

	/**
	 * Attempts to parse {@link #getBlob()} as a UTF-8 string.
	 */
	public String getBlob_string()
	{
		return Utils_String.getStringValue(getBlob(), "UTF-8");
	}

	/**
	 * Returns the data instance (WARNING: not cloned) passed into the constructor.
	 */
	public byte[] getBlob()
	{
		return m_blob;
	}

	/**
	 * Checks if this is referentially equal to {@link #NULL}.
	 */
	@Override public boolean isNull()
	{
		return this == NULL;
	}

	@Override public String toString()
	{
		return isNull() ? "NULL" : m_blob.toString();
	}
}
