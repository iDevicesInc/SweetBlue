package com.idevicesinc.sweetblue;

import android.content.Context;
import android.content.SharedPreferences;

import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

final class P_HistoricalDataManager_PreviousUuids
{
	private static final int ACCESS_MODE = Context.MODE_PRIVATE;
	private static final String NAMESPACE = "sweetblue__previous_historical_data_uuids";

	private final String m_macAddress;
	private final Context m_context;

	private final Set<UUID> m_uuids = new HashSet<UUID>();

	private boolean m_loaded = false;

	public P_HistoricalDataManager_PreviousUuids(final Context context, final String macAddress)
	{
		m_context = context;
		m_macAddress = macAddress;
	}

	public void addUuid(final UUID uuid)
	{
		synchronized(this)
		{
			load();

			if( m_uuids.contains(uuid) ) return;

			m_uuids.add(uuid);

			save();
		}
	}

	public void clearUuid(final UUID uuid)
	{
		synchronized(this)
		{
			load();

			m_uuids.remove(uuid);

			save();
		}
	}

	public void clearAll()
	{
		synchronized(this)
		{
			m_uuids.clear();
			prefs().edit().clear().commit();
		}
	}

	public int getCount()
	{
		synchronized(this)
		{
			load();

			return m_uuids.size();
		}
	}

	public Iterator<UUID> getUuids()
	{
		synchronized(this)
		{
			load();

			return m_uuids.iterator();
		}
	}

	private SharedPreferences prefs()
	{
		final SharedPreferences prefs = m_context.getSharedPreferences(NAMESPACE, ACCESS_MODE);

		return prefs;
	}

	private void load()
	{
		if( m_loaded )  return;

		final SharedPreferences prefs = prefs();

		if( prefs.contains(m_macAddress) )
		{
			final Set<String> uuids = prefs.getStringSet(m_macAddress, null);

			if( uuids != null && !uuids.isEmpty() )
			{
				final Iterator<String> iterator = uuids.iterator();

				while( iterator.hasNext() )
				{
					final String uuid_string = iterator.next();

					if( uuid_string == null || uuid_string.isEmpty() )  continue;

					final UUID uuid = Uuids.fromString(uuid_string);

					m_uuids.add(uuid);
				}
			}
		}

		m_loaded = true;
	}

	private void save()
	{
		if( m_uuids.isEmpty() )  return;

		final Iterator<UUID> iterator = m_uuids.iterator();
		final Set<String> toSave = new HashSet<String>();

		while( iterator.hasNext() )
		{
			final UUID uuid = iterator.next();

			if( uuid == null )  continue;

			toSave.add(uuid.toString());
		}

		prefs().edit().putStringSet(m_macAddress, toSave).commit();
	}
}
