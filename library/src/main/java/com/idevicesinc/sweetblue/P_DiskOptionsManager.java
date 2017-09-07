package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import android.content.Context;
import android.content.SharedPreferences;
import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.State;


final class P_DiskOptionsManager
{
	private static final int ACCESS_MODE = Context.MODE_PRIVATE;

	//--- DRK > Just adding some salt to these to mitigate any possible conflict.
	private static enum E_Namespace
	{
		LAST_DISCONNECT("sweetblue_16l@{&a}"),
		NEEDS_BONDING("sweetblue_p59=F%k"),
		DEVICE_NAME("sweetblue_qurhzpoc");


		private final String m_key;

		private E_Namespace(final String key)
		{
			m_key = key;
		}

		public String key()
		{
			return m_key;
		}
	}
	
	private final Context m_context;
	
	private final HashMap<String, Integer> m_inMemoryDb_lastDisconnect = new HashMap<String, Integer>();
	private final HashMap<String, Boolean> m_inMemoryDb_needsBonding = new HashMap<String, Boolean>();
	private final HashMap<String, String> m_inMemoryDb_name = new HashMap<String, String>();

	private final HashMap[] m_inMemoryDbs = new HashMap[E_Namespace.values().length];

	public P_DiskOptionsManager(Context context)
	{
		m_context = context;

		m_inMemoryDbs[E_Namespace.LAST_DISCONNECT.ordinal()] = m_inMemoryDb_lastDisconnect;
		m_inMemoryDbs[E_Namespace.NEEDS_BONDING.ordinal()] = m_inMemoryDb_needsBonding;
		m_inMemoryDbs[E_Namespace.DEVICE_NAME.ordinal()] = m_inMemoryDb_name;

		final E_Namespace[] values = E_Namespace.values();

		for( int i = 0; i < values.length; i++ )
		{
			Object ith = m_inMemoryDbs[i];

			if( ith == null )
			{
				throw new Error("Expected in-memory DB to be not null");
			}
		}
	}
	
	private SharedPreferences prefs(E_Namespace namespace)
	{
		final SharedPreferences prefs = m_context.getSharedPreferences(namespace.key(), ACCESS_MODE);
		
		return prefs;
	}

	
	public void saveLastDisconnect(final String mac, final State.ChangeIntent changeIntent, final boolean hitDisk)
	{
		final int diskValue = State.ChangeIntent.toDiskValue(changeIntent);
		m_inMemoryDb_lastDisconnect.put(mac, diskValue);
		
		if( !hitDisk )  return;

		prefs(E_Namespace.LAST_DISCONNECT).edit().putInt(mac, diskValue).commit();
	}
	
	public State.ChangeIntent loadLastDisconnect(final String mac, final boolean hitDisk)
	{
		final Integer value_memory = m_inMemoryDb_lastDisconnect.get(mac);
		
		if( value_memory != null )
		{
			final State.ChangeIntent lastDisconnect_memory = State.ChangeIntent.fromDiskValue(value_memory);
			
			return lastDisconnect_memory;
		}
		
		if( !hitDisk )  return State.ChangeIntent.NULL;
		
		final SharedPreferences prefs = prefs(E_Namespace.LAST_DISCONNECT);
		
		final int value_disk = prefs.getInt(mac, State.ChangeIntent.NULL.toDiskValue());
		
		final State.ChangeIntent lastDisconnect = State.ChangeIntent.fromDiskValue(value_disk);
		
		return lastDisconnect;
	}
	
	public void saveNeedsBonding(final String mac, final boolean hitDisk)
	{
		m_inMemoryDb_needsBonding.put(mac, true);
		
		if( !hitDisk )  return;
		
		prefs(E_Namespace.NEEDS_BONDING).edit().putBoolean(mac, true).commit();
	}
	
	public boolean loadNeedsBonding(final String mac, final boolean hitDisk)
	{
		final Boolean value_memory = m_inMemoryDb_needsBonding.get(mac);
		
		if( value_memory != null )
		{
			return value_memory;
		}
		
		if( !hitDisk )  return false;
		
		final SharedPreferences prefs = prefs(E_Namespace.NEEDS_BONDING);
		
		final boolean value_disk = prefs.getBoolean(mac, false);
		
		return value_disk;
	}

	public void saveName(final String mac, final String name, final boolean hitDisk)
	{
		final String name_override = name != null ? name : "";

		m_inMemoryDb_name.put(mac, name_override);

		if( !hitDisk )  return;

		prefs(E_Namespace.DEVICE_NAME).edit().putString(mac, name_override).commit();
	}

	public String loadName(final String mac, final boolean hitDisk)
	{
		final String value_memory = m_inMemoryDb_name.get(mac);

		if( value_memory != null )
		{
			return value_memory;
		}

		if( !hitDisk )  return null;

		final SharedPreferences prefs = prefs(E_Namespace.DEVICE_NAME);

		final String value_disk = prefs.getString(mac, null);

		return value_disk;
	}

	void clear()
	{
		final E_Namespace[] values = E_Namespace.values();

		for( int i = 0; i < values.length; i++ )
		{
			final SharedPreferences prefs = prefs(values[i]);
			prefs.edit().clear().commit();

			final HashMap ith = m_inMemoryDbs[i];

			if( ith != null )
			{
				ith.clear();
			}
		}
	}

	void clearName(final String macAddress)
	{
		final E_Namespace namespace = E_Namespace.DEVICE_NAME;

		clearNamespace(macAddress, namespace);
	}

	private void clearNamespace(final String macAddress, final E_Namespace namespace)
	{
		final int ordinal = namespace.ordinal();
		final SharedPreferences prefs = prefs(namespace);
		prefs.edit().remove(macAddress).commit();

		final HashMap ith = m_inMemoryDbs[ordinal];

		if( ith != null )
		{
			ith.remove(macAddress);
		}
	}

	void clear(final String macAddress)
	{
		final E_Namespace[] values = E_Namespace.values();

		for( int i = 0; i < values.length; i++ )
		{
			clearNamespace(macAddress, values[i]);
		}
	}

	Iterator<String> getPreviouslyConnectedDevices()
	{
		final SharedPreferences prefs = prefs(E_Namespace.LAST_DISCONNECT);

		Map<String, ?> map = prefs.getAll();

		if( map != null )
		{
			List<String> keys = new ArrayList<String>(map.keySet());
			Collections.sort(keys);
			return keys.iterator();
		}
		else
		{
			return new EmptyIterator<String>();
		}
	}
}
