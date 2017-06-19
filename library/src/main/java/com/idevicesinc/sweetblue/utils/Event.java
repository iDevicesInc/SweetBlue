package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.Extendable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Abstract base class for all events in SweetBlue, e.g. {@link State.ChangeEvent}.
 */
@Extendable
public abstract class Event
{
	/**
	 * More reader-friendly of {@link #isForAll(Object...)} or {@link #isForAny(Object...)} in the event you only have one parameter to match.
	 */
	public boolean isFor(final Object value)
	{
		return isForAll(value);
	}

	/**
	 * Convenience query method to check if this event "is for"/relevant-to any of the supplied values.
	 * For example for {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent} you could write
	 * <code>e.isForAny("DE:CA::FF:CO::FF::EE", "DE:AD:BE:EF:BA:BE")</code> to quickly check if the event
	 * is associated with either of the two devices. This method would then just do a comparison
	 * with the input strings against {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent#macAddress()}.
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
	 */
	public boolean isForAll(final Object ... values)
	{
		final ArrayList<Object> objects = getAllObjects();

		for( int j = 0; j < values.length; j++ )
		{
			final Object jth = values[j];

			boolean foundMatch = false;

			for( int i = 0; i < objects.size(); i++ )
			{
				final Object next = objects.get(i);

				if( next != null )
				{
					if( true == jth.equals(next) )
					{
						foundMatch = true;
						break;
					}
				}
			}

			if( false == foundMatch )
			{
				return false;
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

				if( isComparableType(ith, returnType) )
				{
					toReturn.add(ith);
				}
			}

			return toReturn;
		}
		else
		{
			return new ArrayList<Method>();
		}
	}

	private static boolean isComparableType(final Method method, final Class<?> returnType)
	{
		if( returnType == null || returnType.isPrimitive() || returnType.isArray() )
		{
			// Skip these cause they're too generic to test equality for.
			// E.g. ReadWriteEvent in BleDevice has mtu(), rssi(), etc.

			return false;
		}
		else if( returnType == String.class )
		{
			if( method.getName().equals("toString") )
			{
				return false;
			}
			else
			{
				return method.getParameterTypes().length == 0;
			}
		}
		else if( Unit.class.isAssignableFrom(returnType) )
		{
			// Skipping Units for same reason as skipping primitives.

			return false;
		}
		else if( method.getParameterTypes().length == 0 )
		{
			return true;
		}
		else
		{
			return false;
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
					if( value instanceof List )
					{
						final List value_cast = (List) value;

						for( int j = 0; j < value_cast.size(); j++ )
						{
							toReturn.add(value_cast.get(j));
						}
					}
					else
					{
						toReturn.add(value);
					}
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
