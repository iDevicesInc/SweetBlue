package com.idevicesinc.sweetblue.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;

import com.idevicesinc.sweetblue.BleManagerConfig;

/**
 * Convenience implementation of {@link UuidNameMap} that takes a {@link Class} object
 * and through reflection attempts to parse out all the static UUID members, for example
 * on {@link Uuids}.
 * 
 * Provide an instance to {@link BleManagerConfig#uuidNameMaps} if desired.
 * 
 * @see BleManagerConfig#uuidNameMaps
 * @see BasicUuidNameMap
 * 
 * @author dougkoellmer
 */
public class ReflectionUuidNameMap implements UuidNameMap
{
	private final HashMap<String, String> m_dict = new HashMap<String, String>();
	
	public ReflectionUuidNameMap(Class<?> classWithStaticUuids)
	{
		for( Field field : classWithStaticUuids.getFields() )
		{
			String uuid = uuidFieldValue(field);
			
			m_dict.put(uuid, field.getName());
		}
	}
	
	private static String uuidFieldValue(Field field)
	{
		Object uuid = staticFieldValue(field);
		
		String uuidString = "";
		
		if( uuid instanceof String )
		{
			uuidString = (String) uuid;
		}
		else if( uuid instanceof UUID )
		{
			uuidString = uuid.toString();
		}
		
		uuidString = uuidString.toLowerCase();
		
		return uuidString;
	}
	
	private static <T extends Object> T staticFieldValue(Field field)
	{
		Object value = null;
		
		try {
			value = field.get(null);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		
		return (T) value;
	}

	@Override
	public String getUuidName(String uuid)
	{
		uuid = uuid.toLowerCase();
		return m_dict.get(uuid);
	}
}
