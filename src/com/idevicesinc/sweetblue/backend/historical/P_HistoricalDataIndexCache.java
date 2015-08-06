package com.idevicesinc.sweetblue.backend.historical;

import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;

class P_HistoricalDataIndexCache
{
	private final long m_from_date;
	private final long m_to_date;

	final int m_from_index;
	int m_to_index;

	P_HistoricalDataIndexCache(final EpochTime from_date, final EpochTime to_date, final int from_index, final int to_index)
	{
		this(from_date.toMilliseconds(), to_date.toMilliseconds(), from_index, to_index);
	}

	P_HistoricalDataIndexCache(final long from_date, final long to_date, final int from_index, final int to_index)
	{
		m_from_date = from_date;
		m_to_date = to_date;

		m_from_index = from_index;
		m_to_index = to_index;
	}

	boolean isValid()
	{
		return m_from_index >= 0 && m_to_index >= 0 && m_from_index <= m_to_index;
	}

	boolean equals(final EpochTimeRange range)
	{
		return m_from_date == range.from().toMilliseconds() && m_to_date == range.to().toMilliseconds();
	}

	boolean fromIsSame(final EpochTime from_date)
	{
		return m_from_date == from_date.toMilliseconds();
	}

	boolean toIsSame(final EpochTime to_date)
	{
		return m_to_date == to_date.toMilliseconds();
	}

	boolean isInBounds(final int offset)
	{
		return isValid() && m_from_index + offset <= m_to_index;
	}

	int getCount()
	{
		return isValid() ? (m_to_index - m_from_index) + 1 : 0;
	}
}
