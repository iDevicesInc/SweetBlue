package com.idevicesinc.sweetblue.utils;

import java.util.HashMap;
import java.util.UUID;

import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.annotations.Extendable;

/**
 * Manual convenience implementation of {@link UuidNameMap} that's basically just a {@link HashMap}.
 * 
 * Provide an instance to {@link BleManagerConfig#uuidNameMaps} if desired.
 * 
 * @see ReflectionUuidNameMap
 * @see BleManagerConfig#uuidNameMaps
 */
@Extendable
public class BasicUuidNameMap implements UuidNameMap
{
	private final HashMap<String, String> m_dict = new HashMap<String, String>();
	
	/**
	 * Add a {@link UUID}-to-debug name entry.
	 */
	public void add(String uuid, String name)
	{
		m_dict.put(uuid, name);
	}
	
	@Override public String getUuidName(String uuid)
	{
		return m_dict.get(uuid);
	}
}
