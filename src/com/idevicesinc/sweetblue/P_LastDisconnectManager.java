package com.idevicesinc.sweetblue;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;

import com.idevicesinc.sweetblue.BleManagerConfig.AdvertisingFilter.LastDisconnect;

class P_LastDisconnectManager
{
	//--- DRK > Just adding some salt to this to mitigate any possible conflict.
	private static final String SHARED_PREFS_KEY = "sweetblue_16l@{&a}";
	private static final int ACCESS_MODE = Context.MODE_PRIVATE;
	
	private final Context m_context;
	
	private final HashMap<String, Integer> m_inMemoryDb = new HashMap<String, Integer>();
	
	public P_LastDisconnectManager(Context context)
	{
		m_context = context;
	}
	
	public void save(String mac, LastDisconnect lastDisconnect, boolean hitDisk)
	{
		final int diskValue = lastDisconnect == null ? LastDisconnect.UNKNOWN.toDiskValue() : lastDisconnect.toDiskValue();
		m_inMemoryDb.put(mac, diskValue);
		
		if( !hitDisk )  return;

		final SharedPreferences prefs = m_context.getSharedPreferences(SHARED_PREFS_KEY, ACCESS_MODE);
		prefs.edit().putInt(mac, diskValue).commit();
	}
	
	public LastDisconnect load(final String mac, final boolean hitDisk)
	{
		final Integer value_memory = m_inMemoryDb.get(mac);
		
		if( value_memory != null )
		{
			final LastDisconnect lastDisconnect_memory = LastDisconnect.fromDiskValue(value_memory);
			
			return lastDisconnect_memory;
		}
		
		if( !hitDisk )  return LastDisconnect.UNKNOWN;
		
		final SharedPreferences prefs = m_context.getSharedPreferences(SHARED_PREFS_KEY, ACCESS_MODE);
		
		final int diskValue = prefs.getInt(mac, LastDisconnect.UNKNOWN.toDiskValue());
		
		final LastDisconnect lastDisconnect = LastDisconnect.fromDiskValue(diskValue);
		
		return lastDisconnect;
	}
}
