package com.idevicesinc.sweetblue.backend.historical;

import android.util.Log;

import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;
import com.idevicesinc.sweetblue.utils.SingleElementIterator;
import com.idevicesinc.sweetblue.utils.UpdateLoop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class Backend_HistoricalDataList_Default implements Backend_HistoricalDataList
{
	private static final Iterator<HistoricalData> EMPTY_ITERATOR = new EmptyIterator<HistoricalData>();
	private static final HistoricalDataCursor EMPTY_CURSOR = new P_HistoricalDataCursor_Empty();

	private HistoricalData m_data = null;

	private String m_macAddress;
	private Backend_HistoricalDatabase m_database;

	//--- RB > Shut off the historical data warnings, as we aren't really offering the support for it at this time.
	private boolean m_hasShownWarning_read = true;
	private boolean m_hasShownWarning_write = true;

	public Backend_HistoricalDataList_Default()
	{
	}

	@Override public void init(final Backend_HistoricalDatabase database, final UpdateLoop updateLoop, final String macAddress, final UUID uuid, final String uuidName, final boolean hasExistingTable)
	{
		m_database = database;
		m_macAddress = macAddress;
	}

	private boolean isDataInRange(final EpochTimeRange range)
	{
		return m_data != null && m_data.getEpochTime().isBetween_inclusive(range);
	}

	private void printWarning_read()
	{
		if( m_hasShownWarning_read )  return;

		printWarning();

		m_hasShownWarning_read = true;
	}

	private void printWarning_write()
	{
		if( m_hasShownWarning_write )  return;

		printWarning();

		m_hasShownWarning_write = true;
	}

	static void printWarning()
	{
		Log.w
		(
			"SweetBlue",
			"NOTICE: The historical data API backend in this version of SweetBlue can only track one piece of data at a time to RAM only. " +
					"Please contact sweetblue@idevicesinc.com to discuss upgrading to the unlimited backend with database persistence."
		);
	}

	@Override public void add_single(HistoricalData historicalData, final int persistenceLevel, long limit)
	{
		if( persistenceLevel == BleDeviceConfig.HistoricalDataLogFilter.PersistenceLevel_NONE )  return;

		if( limit <= 0 )
		{
			m_data = null;
		}

		final boolean alreadyHadData = m_data != null;

		m_data = historicalData;

		if( alreadyHadData || persistenceLevel == BleDeviceConfig.HistoricalDataLogFilter.PersistenceLevel_DISK )
		{
			printWarning_write();
		}
	}

	@Override public void add_multiple(Iterator<HistoricalData> historicalData, final int persistenceLevel, final long limit)
	{
		while(historicalData.hasNext() )
		{
			add_single(historicalData.next(), persistenceLevel, Long.MAX_VALUE);
		}
	}

	@Override public void add_multiple(ForEach_Returning<HistoricalData> historicalData, final int persistenceLevel, final long limit)
	{
		int i = 0;

		while( true )
		{
			final HistoricalData next = historicalData.next(i);

			if( next == null )  break;

			add_single(next, persistenceLevel, limit);

			i++;
		}
	}

	@Override public int getCount(EpochTimeRange range)
	{
		if( isDataInRange(range) )
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}

	@Override public HistoricalData get(EpochTimeRange range, int offset)
	{
		if( isDataInRange(range) )
		{
			if( offset > 0 )
			{
				printWarning_read();

				return HistoricalData.NULL;
			}
			else
			{
				return m_data;
			}
		}
		else
		{
			return HistoricalData.NULL;
		}
	}

	@Override public Iterator<HistoricalData> getIterator(EpochTimeRange range)
	{
		if( isDataInRange(range) )
		{
			return new SingleElementIterator<HistoricalData>(m_data)
			{
				@Override protected void onRemove()
				{
					m_data = null;
				}
			};
		}
		else
		{
			return EMPTY_ITERATOR;
		}
	}

	@Override public boolean doForEach(EpochTimeRange range, Object forEach)
	{
		if( isDataInRange(range) )
		{
			if( forEach instanceof ForEach_Void )
			{
				((ForEach_Void)forEach).next(m_data);

				return true;
			}
			else if( forEach instanceof ForEach_Breakable )
			{
				((ForEach_Breakable)forEach).next(m_data);

				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}

	@Override public void delete_fromMemoryOnly(EpochTimeRange range, long count)
	{
		if( count > 0 && isDataInRange(range) )
		{
			m_data = null;
		}

		if( count > 1 )
		{
			printWarning_write();
		}
	}

	@Override public void delete_fromMemoryOnlyForNowButDatabaseSoon(EpochTimeRange range, long count)
	{
		delete_fromMemoryOnly(range, count);

		printWarning_write();
	}

	@Override public void delete_fromMemoryAndDatabase(EpochTimeRange range, long count)
	{
		delete_fromMemoryOnly(range, count);

		printWarning_write();
	}

	@Override public String getMacAddress()
	{
		return m_macAddress;
	}

	@Override public void load(AsyncLoadCallback callback_nullable)
	{
		printWarning_write();
	}

	@Override public int getLoadState()
	{
		return LOAD_STATE__NOT_LOADED;
	}

	@Override public HistoricalDataCursor getCursor(EpochTimeRange range)
	{
		if( m_data != null )
		{
			final ArrayList<HistoricalData> list = new ArrayList<HistoricalData>();
			list.add(m_data);

			P_HistoricalDataIndexCache indexCache = new P_HistoricalDataIndexCache(m_data.getEpochTime(), m_data.getEpochTime(), 0, 0);

			return new P_HistoricalDataCursor_List(list, indexCache);
		}
		else
		{
			return EMPTY_CURSOR;
		}
	}

	@Override public EpochTimeRange getRange()
	{
		if( m_data != null )
		{
			return EpochTimeRange.instant(m_data.getEpochTime());
		}
		else
		{
			return EpochTimeRange.NULL;
		}
	}
}
