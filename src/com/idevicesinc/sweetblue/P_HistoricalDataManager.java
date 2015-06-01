package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter.*;

import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDataList;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

class P_HistoricalDataManager
{
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	private static final EmptyIterator<HistoricalData> EMPTY_ITERATOR = new EmptyIterator<HistoricalData>();

	private final Object LIST_CREATE_MUTEX = new Object();

	private final HashMap<UUID, Backend_HistoricalDataList> m_lists = new HashMap<UUID, Backend_HistoricalDataList>();
	private final BleDevice m_device;

	private BleDevice.HistoricalDataLoadListener m_defaultListener = null;

	private final P_HistoricalDataManager_PreviousUuids m_previousUuidsWithDataAdded;

	P_HistoricalDataManager(final BleDevice device)
	{
		m_device = device;

		m_previousUuidsWithDataAdded = new P_HistoricalDataManager_PreviousUuids(m_device.getManager().getApplicationContext(), m_device.getMacAddress());
	}

	void setListener(final BleDevice.HistoricalDataLoadListener listener)
	{
		m_defaultListener = listener;
	}

	Backend_HistoricalDatabase getDatabase()
	{
		return m_device.getManager().m_historicalDatabase;
	}

	//GOOD
	private Backend_HistoricalDataList getList_doNotCreate(final UUID uuid)
	{
		synchronized(LIST_CREATE_MUTEX)
		{
			final Backend_HistoricalDataList existingList = m_lists.get(uuid);

			return existingList;
		}
	}

	//GOOD
	private Backend_HistoricalDataList getList_onlyCreateIfDataIsOnDisk(final UUID uuid)
	{
		synchronized(LIST_CREATE_MUTEX)
		{
			final Backend_HistoricalDataList existingList = m_lists.get(uuid);

			if( existingList == null )
			{
				final boolean tableExists = getDatabase().doesDataExist(m_device.getMacAddress(), uuid);

				if( tableExists )
				{
					final Backend_HistoricalDataList newList = PU_HistoricalData.newList(getDatabase(), m_device.getMacAddress(), uuid, tableExists);
					m_lists.put(uuid, newList);

					return newList;
				}
			}

			return existingList;
		}
	}

	//GOOD
	private Backend_HistoricalDataList getList_createIfNotExists(final UUID uuid)
	{
		synchronized(LIST_CREATE_MUTEX)
		{
			final Backend_HistoricalDataList existingList = m_lists.get(uuid);

			if( existingList == null )
			{
				final boolean tableExists = getDatabase().doesDataExist(m_device.getMacAddress(), uuid);

				final Backend_HistoricalDataList newList = PU_HistoricalData.newList(getDatabase(), m_device.getMacAddress(), uuid, tableExists);
				m_lists.put(uuid, newList);

				return newList;
			}
			else
			{
				return existingList;
			}
		}
	}

	//GOOD
	public void add_single(final UUID uuid, final byte[] data, final EpochTime epochTime, final Source source)
	{
		final Backend_HistoricalDataList list = getList_createIfNotExists(uuid);

		final Please please = PU_HistoricalData.getPlease(m_device, uuid, data, epochTime, source);

		if( PU_HistoricalData.add_earlyOut(list, please) )  return;

		final HistoricalData historicalData = m_device.newHistoricalData(PU_HistoricalData.getAmendedData(data, please), PU_HistoricalData.getAmendedTimestamp(epochTime, please));

		m_previousUuidsWithDataAdded.addUuid(uuid);

		list.add_single(historicalData, please.m_logChoice, please.getLimit());
	}

	//GOOD
	public void add_single(final UUID uuid, final HistoricalData historicalData, final Source source)
	{
		final Backend_HistoricalDataList list = getList_createIfNotExists(uuid);

		final Please please = PU_HistoricalData.getPlease(m_device, uuid, historicalData.getBlob(), historicalData.getEpochTime(), source);

		final HistoricalData historicalData_override;

		if( please.getAmendedData() != null || !please.getAmendedEpochTime().isNull() )
		{
			historicalData_override = m_device.newHistoricalData(PU_HistoricalData.getAmendedData(historicalData.getBlob(), please), PU_HistoricalData.getAmendedTimestamp(historicalData.getEpochTime(), please));
		}
		else
		{
			historicalData_override = historicalData;
		}

		if( PU_HistoricalData.add_earlyOut(list, please) )  return;

		m_previousUuidsWithDataAdded.addUuid(uuid);

		list.add_single(historicalData_override, please.m_logChoice, please.getLimit());
	}

	//GOOD
	public void add_multiple(final UUID uuid, final Iterator<HistoricalData> historicalData)
	{
		final Backend_HistoricalDataList list = getList_createIfNotExists(uuid);

		final Please please = PU_HistoricalData.getPlease(m_device, uuid, EMPTY_BYTE_ARRAY, EpochTime.NULL, Source.MULTIPLE_MANUAL_ADDITIONS);

		if( PU_HistoricalData.add_earlyOut(list, please) )  return;

		m_previousUuidsWithDataAdded.addUuid(uuid);

		list.add_multiple(historicalData, please.m_logChoice, please.getLimit());
	}

	//GOOD
	public void add_multiple(final UUID uuid, final ForEach_Returning<HistoricalData> historicalData)
	{
		final Backend_HistoricalDataList list = getList_createIfNotExists(uuid);

		final Please please = PU_HistoricalData.getPlease(m_device, uuid, EMPTY_BYTE_ARRAY, EpochTime.NULL, Source.MULTIPLE_MANUAL_ADDITIONS);

		if( PU_HistoricalData.add_earlyOut(list, please) )  return;

		m_previousUuidsWithDataAdded.addUuid(uuid);

		list.add_multiple(historicalData, please.m_logChoice, please.getLimit());
	}

	//GOOD
	public HistoricalData getWithOffset(final UUID uuid, final EpochTimeRange range, final int offset)
	{
		final Backend_HistoricalDataList list = getList_onlyCreateIfDataIsOnDisk(uuid);

		if( list == null )
		{
			return HistoricalData.NULL;
		}
		else
		{
			return list.get(range, offset);
		}
	}

	//GOOD
	public void delete(final UUID uuid, final EpochTimeRange range, final long limit, final boolean memoryOnly)
	{
		final Backend_HistoricalDataList list = getList_doNotCreate(uuid);

		if( memoryOnly )
		{
			if( list != null )
			{
				list.delete_fromMemoryOnly(range, limit);
			}
		}
		else
		{
			if( list != null )
			{
				list.delete_fromMemoryAndDatabase(range, limit);
			}
			else
			{
				getDatabase().delete_singleUuid_inRange(m_device.getMacAddress(), uuid, range, limit);
			}
		}
	}

	//GOOD
	public void delete_all(final EpochTimeRange range, final long limit, final boolean memoryOnly)
	{
		final Iterator<UUID> knownUuids = m_previousUuidsWithDataAdded.getUuids();

		final UUID[] uuids = !memoryOnly ? new UUID[m_previousUuidsWithDataAdded.getCount()] : null;
		final String[] macs = uuids != null ? new String[uuids.length] : null;

		int i = 0;

		while( knownUuids.hasNext() )
		{
			final UUID ith = knownUuids.next();

			if( uuids != null )
			{
				uuids[i] = ith;
				macs[i] = m_device.getMacAddress();
			}

			i++;

			Backend_HistoricalDataList list = getList_doNotCreate(ith);

			if( list == null )  continue;

			if( memoryOnly )
			{
				list.delete_fromMemoryOnly(range, limit);
			}
			else
			{
				list.delete_fromMemoryOnlyForNowButDatabaseSoon(range, limit);
			}
		}

		if( !memoryOnly )
		{
			getDatabase().delete_multipleUuids(macs, uuids, range, Long.MAX_VALUE);
		}
	}

	//GOOD
	public Iterator<HistoricalData> getIterator(final UUID uuid, final EpochTimeRange range)
	{
		final Backend_HistoricalDataList list = getList_onlyCreateIfDataIsOnDisk(uuid);

		if( list != null )
		{
			return list.getIterator(range);
		}
		else
		{
			return EMPTY_ITERATOR;
		}
	}

	//GOOD
	public boolean doForEach(final UUID uuid, final EpochTimeRange range, Object forEach)
	{
		final Backend_HistoricalDataList list = getList_onlyCreateIfDataIsOnDisk(uuid);

		if( list != null )
		{
			return list.doForEach(range, forEach);
		}
		else
		{
			return false;
		}
	}

	//GOOD
	public int getCount(UUID uuid, final EpochTimeRange range)
	{
		final Backend_HistoricalDataList list = getList_onlyCreateIfDataIsOnDisk(uuid);

		return list != null ? list.getCount(range) : 0;
	}

	//GOOD
	public HistoricalDataCursor getCursor(final UUID uuid, final EpochTimeRange range)
	{
		final Backend_HistoricalDataList list = getList_doNotCreate(uuid);

		if( list != null )
		{
			return list.getCursor(range);
		}
		else
		{
			return getDatabase().getCursor(m_device.getMacAddress(), uuid, range);
		}
	}

	//GOOD
	public boolean hasHistoricalData(final UUID uuid, final EpochTimeRange range)
	{
		return getCount(uuid, range) > 0;
	}

	//GOOD
	public boolean hasHistoricalData(final EpochTimeRange range)
	{
		final boolean memoryOnly = false;
		final Iterator<UUID> previousUuids = m_previousUuidsWithDataAdded.getUuids();

		while( previousUuids.hasNext() )
		{
			final UUID ithUuid = previousUuids.next();

			final Backend_HistoricalDataList ithList = getList_doNotCreate(ithUuid);

			if( memoryOnly )
			{
				if( ithList != null && ithList.getCount(range) > 0 )
				{
					return true;
				}
			}
			else
			{
				if( ithList != null && ithList.getCount(range) > 0 )
				{
					return true;
				}
				else
				{
					if( getDatabase().getCount(m_device.getMacAddress(), ithUuid, range) > 0 )
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	//TODO: Need to implement the uui==null case
	public void load(final UUID uuid_nullable, final BleDevice.HistoricalDataLoadListener listener_nullable)
	{
		if( isLoaded(uuid_nullable) )
		{
			if( uuid_nullable != null )
			{
				final Backend_HistoricalDataList list = getList_doNotCreate(uuid_nullable);

				if( list != null )
				{
					invokeListeners(uuid_nullable, list.getRange(), BleDevice.HistoricalDataLoadListener.Status.ALREADY_LOADED, listener_nullable);
				}
				else
				{
					m_device.getManager().ASSERT(false, "list should not have been null.");
				}
			}
			else
			{

			}
		}
		else
		{
			if( uuid_nullable != null )
			{
				if( hasHistoricalData(uuid_nullable, EpochTimeRange.FROM_MIN_TO_MAX) )
				{
					final Backend_HistoricalDataList list = getList_createIfNotExists(uuid_nullable);

					if( list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADING )
					{
						invokeListeners(uuid_nullable, EpochTimeRange.NULL, BleDevice.HistoricalDataLoadListener.Status.ALREADY_LOADING, listener_nullable);
					}
					else
					{
						invokeListeners(uuid_nullable, EpochTimeRange.NULL, BleDevice.HistoricalDataLoadListener.Status.STARTED_LOADING, listener_nullable);
					}

					list.load(new Backend_HistoricalDataList.AsyncLoadCallback()
					{
						@Override public void onDone()
						{
							if( list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADED )
							{
								invokeListeners(uuid_nullable, list.getRange(), BleDevice.HistoricalDataLoadListener.Status.LOADED, listener_nullable);
							}
							else if( list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__NOT_LOADED )
							{
								//--- DRK > Should be fringe but technically possible if user is doing things on multiple threads (they shouldn't but if they do...).
								invokeListeners(uuid_nullable, EpochTimeRange.NULL, BleDevice.HistoricalDataLoadListener.Status.NOTHING_TO_LOAD, listener_nullable);
							}
							else
							{
								m_device.getManager().ASSERT(false, "Didn't expect to still be loading historical data.");
							}
						}
					});
				}
				else
				{
					invokeListeners(uuid_nullable, EpochTimeRange.NULL, BleDevice.HistoricalDataLoadListener.Status.NOTHING_TO_LOAD, listener_nullable);
				}
			}
			else
			{

			}
		}
	}

	//GOOD
	public boolean isLoaded(final UUID uuid_nullable)
	{
		if( uuid_nullable != null )
		{
			final Backend_HistoricalDataList list = getList_doNotCreate(uuid_nullable);

			if( list != null )
			{
				return list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADED;
			}
			else
			{
				return false;
			}
		}
		else
		{
			final Iterator<UUID> previousUuids = m_previousUuidsWithDataAdded.getUuids();

			while( previousUuids.hasNext() )
			{
				final UUID ithUuid = previousUuids.next();

				final Backend_HistoricalDataList ithList = getList_doNotCreate(ithUuid);

				if( ithList == null || ithList.getLoadState() != Backend_HistoricalDataList.LOAD_STATE__LOADED )
				{
					return false;
				}
			}

			return true;
		}
	}

	//GOOD
	public boolean isLoading(final UUID uuid_nullable)
	{
		if( uuid_nullable != null )
		{
			final Backend_HistoricalDataList list = getList_doNotCreate(uuid_nullable);

			if( list != null )
			{
				return list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADING;
			}
			else
			{
				return false;
			}
		}
		else
		{
			final Iterator<UUID> previousUuids = m_previousUuidsWithDataAdded.getUuids();

			while( previousUuids.hasNext() )
			{
				final UUID ithUuid = previousUuids.next();

				final Backend_HistoricalDataList ithList = getList_doNotCreate(ithUuid);

				if( ithList == null )  continue;

				if( ithList.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADING )
				{
					return true;
				}
			}

			return false;
		}
	}

	//GOOD
	void invokeListeners(final UUID uuid, final EpochTimeRange range, final BleDevice.HistoricalDataLoadListener.Status status, final BleDevice.HistoricalDataLoadListener listener_nullable)
	{
		BleDevice.HistoricalDataLoadListener.HistoricalDataLoadEvent event = null;

		event = invokeListener(uuid, range, listener_nullable, status, event);
		event = invokeListener(uuid, range, m_defaultListener, status, event);
		event = invokeListener(uuid, range, m_device.getManager().m_historicalDataLoadListener, status, event);
	}

	//GOOD
	private BleDevice.HistoricalDataLoadListener.HistoricalDataLoadEvent invokeListener(final UUID uuid, final EpochTimeRange range, final BleDevice.HistoricalDataLoadListener listener_nullable, BleDevice.HistoricalDataLoadListener.Status status, BleDevice.HistoricalDataLoadListener.HistoricalDataLoadEvent event_nullable)
	{
		if( listener_nullable != null )
		{
			event_nullable = event_nullable != null ? event_nullable : new BleDevice.HistoricalDataLoadListener.HistoricalDataLoadEvent(m_device, uuid, range, status);

			listener_nullable.onEvent(event_nullable);
		}

		return event_nullable;
	}
}
