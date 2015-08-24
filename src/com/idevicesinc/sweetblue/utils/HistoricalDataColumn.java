package com.idevicesinc.sweetblue.utils;

import java.util.UUID;

/**
 * Enumeration/abstraction of database columns used to persist {@link HistoricalData}.
 */
public enum HistoricalDataColumn
{
	EPOCH_TIME("date"),
	DATA("data");

	private final String m_name;

	private HistoricalDataColumn(final String name)
	{
		m_name = name;
	}

	/**
	 * Gets the name of this database column - you can use this for example to do raw queries through {@link com.idevicesinc.sweetblue.BleDevice#queryHistoricalData(String)}.
	 */
	public String getColumnName()
	{
		return m_name;
	}

	/**
	 * Gets the name of this database column - you can use this for example to help navigate the {@link android.database.Cursor}
	 * returned by {@link com.idevicesinc.sweetblue.BleDevice#queryHistoricalData(String)}.
	 */
	public int getColumnIndex()
	{
		return ordinal();
	}
}
