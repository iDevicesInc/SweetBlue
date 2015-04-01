package com.idevicesinc.sweetblue;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import com.idevicesinc.sweetblue.utils.State;

class P_DiskOptionsManager
{
	//--- DRK > Just adding some salt to this to mitigate any possible conflict.
	private static final int ACCESS_MODE = Context.MODE_PRIVATE;
	
	private final Context m_context;
	
	private final HashMap<String, Integer> m_inMemoryDb_lastDisconnect = new HashMap<String, Integer>();
	private final HashMap<String, Boolean> m_inMemoryDb_needsBonding = new HashMap<String, Boolean>();
	
	private static enum E_Namespace
	{
		LAST_DISCONNECT("sweetblue_16l@{&a}"),
		NEEDS_BONDING("sweetblue_p59=F%k");
		
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
	
	public P_DiskOptionsManager(Context context)
	{
		m_context = context;
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
}
