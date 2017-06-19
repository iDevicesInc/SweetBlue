package com.idevicesinc.sweetblue.backend.historical;

import android.database.Cursor;

import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;

import java.util.List;

class P_HistoricalDataCursor_List implements HistoricalDataCursor
{
	private final List<HistoricalData> m_historicalData;
	private final P_HistoricalDataIndexCache m_indexCache;

	private int m_position = -1;

	private boolean m_isClosed = false;

	public P_HistoricalDataCursor_List(final List<HistoricalData> data, final P_HistoricalDataIndexCache indexCache)
	{
		m_historicalData = data;
		m_indexCache = indexCache;
	}

	@Override public int getCount()
	{
		return m_indexCache.getCount();
	}

	private boolean checkPosition(final int newPosition)
	{
		return newPosition >= m_indexCache.m_from_index && newPosition <= m_indexCache.m_to_index;
	}

	@Override public int getPosition()
	{
		return m_position;
	}

	@Override public boolean move(int offset)
	{
		return moveToPosition(getPosition() + offset);
	}

	@Override public boolean moveToPosition(int position)
	{
		if( checkPosition(position) )
		{
			m_position = position;

			return false;
		}
		else
		{
			return false;
		}
	}

	@Override public boolean moveToFirst()
	{
		return moveToPosition(m_indexCache.m_from_index);
	}

	@Override public boolean moveToLast()
	{
		return moveToPosition(getCount()-1);
	}

	@Override public boolean moveToNext()
	{
		return moveToPosition(getPosition()+1);
	}

	@Override public boolean moveToPrevious()
	{
		return moveToPosition(getPosition()-1);
	}

	@Override public boolean isFirst()
	{
		return getPosition() == 0;
	}

	@Override public boolean isLast()
	{
		return getPosition() == getCount()-1;
	}

	@Override public boolean isBeforeFirst()
	{
		return m_position == -1;
	}

	@Override public boolean isAfterLast()
	{
		return m_position >= getCount();
	}

	@Override public void close()
	{
		if( m_isClosed )  return;

		m_isClosed = true;
	}

	@Override public boolean isClosed()
	{
		return m_isClosed;
	}

	@Override public long getEpochTime()
	{
		return getHistoricalData().getEpochTime_millis();
	}

	@Override public byte[] getBlob()
	{
		return getHistoricalData().getBlob();
	}

	@Override public HistoricalData getHistoricalData()
	{
		if( checkPosition(getPosition()) )
		{
			return m_historicalData.get(getPosition());
		}
		else
		{
			return HistoricalData.NULL;
		}
	}

}
