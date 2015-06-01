package com.idevicesinc.sweetblue.backend;

import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDataList;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDataList_Default;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase_Default;

/**
 * A collection of {@link java.lang.Class} instances used through {@link Class#newInstance()} to create instances of backend modules.
 */
public class Backend_Modules
{
	public static Class<? extends Backend_HistoricalDataList> HISTORICAL_DATA_LIST = Backend_HistoricalDataList_Default.class;
	public static Class<? extends Backend_HistoricalDatabase> HISTORICAL_DATABASE = Backend_HistoricalDatabase_Default.class;
}
