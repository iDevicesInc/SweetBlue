package com.idevicesinc.sweetblue.backend.historical;

import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;
import com.idevicesinc.sweetblue.utils.UpdateLoop;
import com.idevicesinc.sweetblue.utils.UuidNameMap;

import java.util.Iterator;
import java.util.UUID;

/**
 * Defines a specification for an interface over an in-memory list of historical data that optionally syncs to/from
 * disk using an implementation of {@link com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase}.
 */
public interface Backend_HistoricalDataList
{
	public static int LOAD_STATE__NOT_LOADED	= 0;
	public static int LOAD_STATE__LOADING		= 1;
	public static int LOAD_STATE__LOADED		= 2;

	public interface AsyncLoadCallback
	{
		void onDone();
	}

	void init(final Backend_HistoricalDatabase database, final UpdateLoop updateLoop, final String macAddress, final UUID uuid, final String uuidName, final boolean hasExistingTable);

	void add_single(final HistoricalData historicalData, final int persistenceLevel, final long limit);

	void add_multiple(final Iterator<HistoricalData> historicalData, final int persistenceLevel, final long limit);

	void add_multiple(final ForEach_Returning<HistoricalData> historicalData, final int persistenceLevel, final long limit);

	int getCount(final EpochTimeRange range);

	HistoricalData get(final EpochTimeRange range, final int offset);

	Iterator<HistoricalData> getIterator(final EpochTimeRange range);

	boolean doForEach(final EpochTimeRange range, final Object forEach);

	void delete_fromMemoryOnly(final EpochTimeRange range, final long count);

	void delete_fromMemoryOnlyForNowButDatabaseSoon(final EpochTimeRange range, final long count);

	void delete_fromMemoryAndDatabase(final EpochTimeRange range, final long count);

	String getMacAddress();

	void load(final AsyncLoadCallback callback_nullable);

	int getLoadState();

	HistoricalDataCursor getCursor(final EpochTimeRange range);

	EpochTimeRange getRange();
}
