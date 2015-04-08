package com.idevicesinc.sweetblue.utils;

import java.util.UUID;

/**
 * A collection of standard {@link UUID}s for services, characteristics, and descriptors.
 * Some convenience methods for creating {@link UUID}s also.
 */
public class Uuids
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
	
	public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID	= fromShort("2902");
	
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
	 * @param assignedNumber	A {@link String} of length 4 as the hex representation of a 2-byte (short) value, for example "2a19".
	 * @param uuidTemplate		See {@link #STANDARD_UUID_TEMPLATE} for an example.
	 * @return {@link #INVALID} if there's any issue, otherwise a valid {@link UUID}.
	 */
	public static UUID fromShort(String assignedNumber, String uuidTemplate)
	{
		if( assignedNumber == null || assignedNumber.length() != 4 )  return INVALID;
		
		String uuid = uuidTemplate.substring(0, 4) + assignedNumber + uuidTemplate.substring(8, uuidTemplate.length());
		
		return UUID.fromString(uuid);
	}
}
