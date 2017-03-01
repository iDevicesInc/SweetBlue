package com.idevicesinc.sweetblue.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.util.Log;

/**
 * Some utilities for dealing with raw byte array scan records.
 */
public final class Utils_Reflection extends Utils
{
	private Utils_Reflection(){super();}

	private static final String TAG = Utils_Reflection.class.getName();
	
	public static String fieldStringValue(Field field)
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
	
	public static <T extends Object> T staticFieldValue(Field field)
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
	
	public static <T> void nullOut(T target, Class<? extends T> type)
	{
		Class<?> clazz = type;
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields)
		{
			if( Modifier.isStatic(field.getModifiers()) )  continue;
			
//			Log.e("", field.getName()+"");
			
			try
			{
				field.set(target, null);
			}
			catch (IllegalAccessException e)
			{
//				e.printStackTrace();
			}
			catch (IllegalArgumentException e)
			{
//				e.printStackTrace();
			}
		}
	}

	public static boolean callBooleanReturnMethod(final Object instance, final String methodName, boolean showError)
	{
		return callBooleanReturnMethod(instance, methodName, null, showError);
	}

	public static boolean callBooleanReturnMethod(final Object instance, final String methodName, final Class[] paramTypes, boolean showError, final Object ... params)
	{
		try
		{
			final Method method = instance.getClass().getMethod(methodName, paramTypes);
			final Boolean result = (Boolean) method.invoke(instance, params);

			if( result == null || !result )
			{
				return false;
			}
		}
		catch (Exception e)
		{
			if (showError)
			{
				Log.e("SweetBlue", "Problem calling method: " + methodName + " - " + e);
			}

			return false;
		}

		return true;
	}
}
