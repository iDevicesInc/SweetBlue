package com.idevicesinc.sweetblue.backend.historical;


import android.content.Context;
import android.database.Cursor;

import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;

import java.util.UUID;

/**
 * Defines a specification for an interface over a disk-persisted database (probably SQL-based but not necessarily)
 * storing arbitrary historical data for each MAC-address/UUID combination provided.
 */
public interface Backend_HistoricalDatabase
{
	void init(final BleManager manager);

	void add_single(final String macAddress, final UUID uuid, final HistoricalData data, final long maxCountToDelete);

	void add_multiple_start();

	void add_multiple_next(final String macAddress, final UUID uuid, final HistoricalData data);

	void add_multiple_end();

	void delete_singleUuid_all(final String macAddress, final UUID uuid);

	void delete_singleUuid_inRange(final String macAddress, final UUID uuid, final EpochTimeRange range, final long maxCountToDelete);

	void delete_singleUuid_singleDate(final String macAddress, final UUID uuid, final long date);

	void delete_multipleUuids(final String[] macAddresses, final UUID[] uuids, final EpochTimeRange range, final long count);

	boolean doesDataExist(final String macAddress, final UUID uuid);

	void load(final String macAddress, final UUID uuid, final EpochTimeRange range, final ForEach_Void<HistoricalData> forEach);

	int getCount(final String macAddress, final UUID uuid, final EpochTimeRange range);

	HistoricalDataCursor getCursor(final String macAddress, final UUID uuid, final EpochTimeRange range);

	Cursor query(final String query);

	String getTableName(final String macAddress, final UUID uuid);
}
