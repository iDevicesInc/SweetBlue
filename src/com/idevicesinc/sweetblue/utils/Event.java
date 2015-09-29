package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.utils.*;
import com.idevicesinc.sweetblue.*;
import com.idevicesinc.sweetblue.BleDevice.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Abstract base class for all events in SweetBlue, e.g. {@link State.ChangeEvent}.
 */
public abstract class Event
{
	/**
	 * Convenience query method to check if this event "is for"/relevant-to any of the supplied values.
	 * For example for {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent} you could write
	 * <code>e.isForAny("DE:CA::FF:CO::FF::EE", "DE:AD:BE:EF:BA:BE")</code> to quickly check if the event
	 * is associated with either of the two devices. This method would then just do a comparison
	 * with the input strings against {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent#macAddress()}.
	 * 
	 * @param values
	 */
	public boolean isForAny(final Object ... values)
	{
		final ArrayList<Object> objects = getAllObjects();

		for( int i = 0; i < objects.size(); i++ )
		{
			final Object next = objects.get(i);

			if( next != null )
			{
				for( int j = 0; j < values.length; j++ )
				{
					final Object jth = values[j];

					if( jth.equals(next) )
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Convenience query method to check if this event "is for"/relevant-to all of the supplied values.
	 * For example for {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent} you could write
	 * <code>e.isForAll("DE:CA::FF:CO::FF::EE", MY_UUID)</code> to quickly check if the event
	 * is associated with both the given mac address and the given {@link java.util.UUID} (e.g. for a characteristic).
	 * This method would then just do a comparison with the input values against {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent#macAddress()},
	 * {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent#charUuid()} {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent#descUuid()}, and
	 * {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent#serviceUuid()}.
	 * 
	 * @param values
	 */
	public boolean isForAll(final Object ... values)
	{
		final ArrayList<Object> objects = getAllObjects();

		for( int i = 0; i < objects.size(); i++ )
		{
			final Object next = objects.get(i);

			if( next != null )
			{
				for( int j = 0; j < values.length; j++ )
				{
					final Object jth = values[j];

					if( false == jth.equals(next) )
					{
						return false;
					}
				}
			}
		}

		return true;
	}


	private static final HashMap<Class<?>, ArrayList<Method>> s_methods = new HashMap<Class<?>, ArrayList<Method>>();

	private static ArrayList<Method> extractMethods(final Class<?> type)
	{
		final Method[] methods = type.getMethods();

		if( methods != null && methods.length > 0 )
		{
			final ArrayList<Method> toReturn = new ArrayList<Method>();

			for( int i = 0; i < methods.length; i++ )
			{
				final Method ith = methods[i];

				final Class<?> returnType = ith.getReturnType();

				if( returnType == null || returnType.isPrimitive() || returnType.isArray() )
				{
					// Skip these cause they're too generic to test equality for.
					// E.g. ReadWriteEvent in BleDevice has mtu(), rssi(), etc.
				}
				else if( returnType == String.class && false == ith.getName().equals("macAddress") )
				{
					// Special case of only allowing String return value for mac address.
					// Other methods that return String are too generic and variable so we skip them.
				}
				else if( Unit.class.isAssignableFrom(returnType) )
				{
					// Skipping Units for same reason as skipping primitives.
				}
				else
				{
					if( ith.getParameterTypes().length == 0 )
					{
						toReturn.add(ith);
					}
				}
			}

			return toReturn;
		}
		else
		{
			return new ArrayList<Method>();
		}
	}

	private ArrayList<Method> getMethods()
	{
		synchronized(s_methods)
		{
			final ArrayList<Method> methods_nullable = s_methods.get(this.getClass());

			if( methods_nullable == null )
			{
				final ArrayList<Method> methods = extractMethods(this.getClass());

				s_methods.put(this.getClass(), methods);

				return methods;
			}
			else
			{
				return methods_nullable;
			}
		}
	}

	private ArrayList<Object> getAllObjects()
	{
		final ArrayList<Method> methods = getMethods();
		final ArrayList<Object> toReturn = new ArrayList<Object>();

		for( int i = 0; i < methods.size(); i++ )
		{
			final Method ith = methods.get(i);

			try
			{
				final Object value = ith.invoke(this);

				if( value != null )
				{
					toReturn.add(value);
				}
			}
			catch(IllegalAccessException e)
			{
			}
			catch(InvocationTargetException e)
			{
			}
		}

		return toReturn;
	}
}
