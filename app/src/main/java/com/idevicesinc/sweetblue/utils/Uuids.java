package com.idevicesinc.sweetblue.utils;

import java.util.UUID;

/**
 * A collection of standard {@link UUID}s for services, characteristics, and descriptors.
 * Some convenience methods for creating {@link UUID}s also.
 */
public final class Uuids
{
	/**
	 * A {@link UUID} instance composed of all zeros and used instead of <code>null</code> in various places.
	 */
	public static final UUID INVALID												= new UUID(0x0, 0x0);
	
	/**
	 * The template for standard services, characteristics, and descriptors - see <a href="https://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx">https://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx</a>
	 * This is used to generate some of the other static {@link UUID} instances in this class using {@link #fromShort(String, String)}.
	 */
	public static final String STANDARD_UUID_TEMPLATE								= "00000000-0000-1000-8000-00805f9b34fb";
	
	public static final UUID GENERIC_ATTRIBUTES_SERVICE_UUID 						= fromShort("1801");
	public static final UUID GENERIC_ACCESS_SERVICE_UUID							= fromShort("1800");
	public static final UUID DEVICE_INFORMATION_SERVICE_UUID						= fromShort("180a");
	public static final UUID BATTERY_SERVICE_UUID 									= fromShort("180f");
	
	public static final UUID DEVICE_NAME											= fromShort("2a00");

	public static final UUID BATTERY_LEVEL											= fromShort("2a19");



	public static final UUID MANUFACTURER_NAME										= fromShort("2a29");
	public static final UUID MODEL_NUMBER											= fromShort("2a24");
	public static final UUID SERIAL_NUMBER											= fromShort("2a25");
	public static final UUID HARDWARE_REVISION										= fromShort("2a27");
	public static final UUID FIRMWARE_REVISION										= fromShort("2a26");
	public static final UUID SOFTWARE_REVISION										= fromShort("2a28");
	public static final UUID SYSTEM_ID												= fromShort("2a23");

	public static final UUID[] DEVICE_INFORMATION_UUIDS =
	{
		MANUFACTURER_NAME,
		MODEL_NUMBER,
		SERIAL_NUMBER,
		HARDWARE_REVISION,
		FIRMWARE_REVISION,
		SOFTWARE_REVISION,
		SYSTEM_ID
	};

	public static final UUID CURRENT_TIME_SERVICE = Uuids.fromShort(0x1805);
	public static final UUID CURRENT_TIME_SERVICE__CURRENT_TIME = Uuids.fromShort(0x2a2b);
	public static final UUID CURRENT_TIME_SERVICE__LOCAL_TIME_INFO = Uuids.fromShort(0x2a0f);

	public static final UUID CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID		= fromShort("2901");
	public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID	= fromShort("2902");
	public static final UUID CHARACTERISTIC_PRESENTATION_FORMAT_DESCRIPTOR_UUID		= fromShort("2904");
	

	protected final static String BLUETOOTH_CONNECTED_HASH = "6216bcd07f9c58c52eda2579f31d10cbb69bb07b";

	
	/**
	 * Convenience overload of {@link #fromShort(String, String)} that uses {@link #STANDARD_UUID_TEMPLATE}.
	 * For example to create the battery level characteristic you would call
	 * <code>{@link Uuids}.fromShort("180f")</code>.
	 */
	public static UUID fromShort(String assignedNumber)
	{
		return fromShort(assignedNumber, STANDARD_UUID_TEMPLATE);
	}
	
	/**
	 * Convenience overload of {@link #fromShort(short, String)} that uses {@link #STANDARD_UUID_TEMPLATE}.
	 * For example to create the battery level characteristic you would call
	 * <code>{@link Uuids}.fromShort((short)0x180f)</code>.
	 */
	public static UUID fromShort(short assignedNumber)
	{
		return fromShort(assignedNumber, STANDARD_UUID_TEMPLATE);
	}
	
	/**
	 * Overload of {@link #fromShort(short)} so you don't have to downcast hardcoded integers yourself.
	 */
	public static UUID fromShort(int assignedNumber)
	{
		return fromShort((short)assignedNumber);
	}
	
	/**
	 * Convenience overload of {@link #fromShort(String, String)} that converts the given
	 * short to a {@link String} hex representation.
	 * For example to create the battery level characteristic you would call
	 * <code>{@link Uuids}.fromShort((short)0x180f, {@link #STANDARD_UUID_TEMPLATE})</code>.
	 */
	public static UUID fromShort(short assignedNumber, String uuidTemplate)
	{
		String hex = Integer.toHexString(assignedNumber & 0xffff);
		
		return fromShort(hex, uuidTemplate);
	}
	
	/**
	 * Convenience overload of {@link #fromShort(short, String)} so you don't
	 * have to downcast hardcoded integers yourself.
	 */
	public static UUID fromShort(int assignedNumber, String uuidTemplate)
	{
		return fromShort((short) assignedNumber, uuidTemplate);
	}
	
	/**
	 * Replaces the characters at indices 4, 5, 6, and 7 of <code>uuidTemplate</code> with the
	 * <code>assignedNumber</code> parameter and returns the resulting {@link UUID} using {@link UUID#fromString(String)}.
	 *
	 * @param assignedNumber	A {@link String} of length <= 4 as the hex representation of a 2-byte (short) value, for example "2a19".
	 * @param uuidTemplate		See {@link #STANDARD_UUID_TEMPLATE} for an example.
	 * @return {@link #INVALID} if there's any issue, otherwise a valid {@link UUID}.
	 */
	public static UUID fromShort(String assignedNumber, String uuidTemplate)
	{
		assignedNumber = chopOffHexPrefix(assignedNumber);

		if( assignedNumber_earlyOut(assignedNumber, 4) )  return INVALID;

		String uuid = uuidTemplate.substring(0, 4) + padAssignedNumber(assignedNumber, 4) + uuidTemplate.substring(8, uuidTemplate.length());

		return fromString(uuid);
	}

	public static UUID fromInt(final int assignedNumber)
	{
		return fromInt(assignedNumber, STANDARD_UUID_TEMPLATE);
	}

	/**
	 * Convenience overload of {@link #fromInt(String, String)}.
	 */
	public static UUID fromInt(int assignedNumber, String uuidTemplate)
	{
		final String hex = Integer.toHexString(assignedNumber);

		return fromInt(hex, uuidTemplate);
	}

	/**
	 * Convenience overload of {@link #fromInt(String, String)} that uses {@link #STANDARD_UUID_TEMPLATE}.
	 */
	public static UUID fromInt(String assignedNumber)
	{
		return fromInt(assignedNumber, STANDARD_UUID_TEMPLATE);
	}

	/**
	 * Replaces the characters at indices 0-7 (inclusive) of <code>uuidTemplate</code> with the
	 * <code>assignedNumber</code> parameter and returns the resulting {@link UUID} using {@link UUID#fromString(String)}.
	 *
	 * @param assignedNumber	A {@link String} of length <= 8 as the hex representation of a 4-byte (int) value, for example "12630102".
	 * @param uuidTemplate		See {@link #STANDARD_UUID_TEMPLATE} for an example.
	 * @return {@link #INVALID} if there's any issue, otherwise a valid {@link UUID}.
	 */
	public static UUID fromInt(String assignedNumber, String uuidTemplate)
	{
		assignedNumber = chopOffHexPrefix(assignedNumber);

		if( assignedNumber_earlyOut(assignedNumber, 8) )  return INVALID;

		String uuid = padAssignedNumber(assignedNumber, 8) + uuidTemplate.substring(8, uuidTemplate.length());

		return fromString(uuid);
	}

	/**
	 * Convenience forwarding of {@link UUID#fromString(String)}, {@link #fromShort(String)}, or {@link #fromInt(String)} depending on the length of string given.
	 */
	public static UUID fromString(final String value)
	{
		if( value.length() == 4 || (value.length() == 6 && hasHexPrefix(value)) )
		{
			return fromShort(value);
		}
		else if( value.length() == 8 || (value.length() == 10 && hasHexPrefix(value)) )
		{
			return fromInt(value);
		}
		else
		{
			return UUID.fromString(value);
		}
	}

	/**
	 * Parses the first 8 characters of the string representations of the given {@link UUID} as an integer hex string.
	 */
	public static int getInt(final UUID uuid)
	{
		final String firstChunk = uuid.toString().substring(0, 8);
		final int toReturn = Integer.parseInt(firstChunk, 16);

		return toReturn;
	}

	public static UUID random()
	{
		return UUID.randomUUID();
	}

	private static boolean hasHexPrefix(final String string)
	{
		if( string == null )
		{
			return false;
		}
		else
		{
			if( string.length() > 2 )
			{
				if( string.charAt(0) == '0' && string.charAt(1) == 'x' )
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			else
			{
				return false;
			}
		}
	}

	private static String chopOffHexPrefix(final String string)
	{
		if( hasHexPrefix(string) )
		{
			return string.substring(2);
		}
		else
		{
			return string;
		}
	}

	private static boolean assignedNumber_earlyOut(final String assignedNumber, final int length)
	{
		if( assignedNumber == null || assignedNumber.isEmpty() || assignedNumber.length() > length )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	private static String padAssignedNumber(String assignedNumber, final int length)
	{
		while(assignedNumber.length() < length )
		{
			assignedNumber = "0" + assignedNumber;
		}

		return assignedNumber;
	}
}
