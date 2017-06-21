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


	// Bluetooth spec Service UUIDs

	public static final UUID GENERIC_ACCESS_SERVICE_UUID							= fromShort("1800");
	public static final UUID GENERIC_ATTRIBUTES_SERVICE_UUID 						= fromShort("1801");
	public static final UUID IMMEDIATE_ALERT_SERVICE_UUID							= fromShort("1802");
	public static final UUID LINK_LOSS_SERVICE_UUID									= fromShort("1803");
	public static final UUID TX_POWER_SERVICE_UUID									= fromShort("1804");
	public static final UUID CURRENT_TIME_SERVICE_UUID								= fromShort("1805");
	public static final UUID REFERENCE_TIME_UPDATE_SERVICE_UUID						= fromShort("1806");
	public static final UUID NEXT_DST_CHANGE_SERVICE_UUID							= fromShort("1807");
	public static final UUID GLUCOSE_SERVICE_UUID									= fromShort("1808");
	public static final UUID HEALTH_THERMOMETER_SERVICE_UUID						= fromShort("1809");
	public static final UUID DEVICE_INFORMATION_SERVICE_UUID						= fromShort("180a");
	public static final UUID HEART_RATE_SERVICE_UUID								= fromShort("180d");
	public static final UUID PHONE_ALERT_STATUS_SERVICE_UUID						= fromShort("180e");
	public static final UUID BATTERY_SERVICE_UUID 									= fromShort("180f");
	public static final UUID BLOOD_PRESSURE_SERVICE_UUID							= fromShort("1810");
	public static final UUID ALERT_NOTIFICATION_SERVICE_UUID						= fromShort("1811");
	public static final UUID HUMAN_INTERFACE_DEVICE_SERVICE_UUID					= fromShort("1812");
	public static final UUID SCAN_PARAMETERS_SERVICE_UUID							= fromShort("1813");
	public static final UUID RUNNING_SPEED_AND_CADENCE_SERVICE_UUID					= fromShort("1814");
	public static final UUID AUTOMATION_IO_SERVICE_UUID								= fromShort("1815");
	public static final UUID CYCLING_SPEED_AND_CADENCE_SERVICE_UUID					= fromShort("1816");
	public static final UUID CYCLING_POWER_SERVICE_UUID								= fromShort("1818");
	public static final UUID LOCATION_AND_NAVIGATION_SERVICE_UUID					= fromShort("1819");
	public static final UUID ENVIRONMENTAL_SENSING_SERVICE_UUID						= fromShort("181a");
	public static final UUID BODY_COMPOSITION_SERVICE_UUID							= fromShort("181b");
	public static final UUID USER_DATA_SERVICE_UUID									= fromShort("181c");
	public static final UUID WEIGHT_SCALE_SERVICE_UUID								= fromShort("181d");
	public static final UUID BOND_MANAGEMENT_SERVICE_UUID							= fromShort("181e");
	public static final UUID CONTINUOUS_GLUCOSE_MONITORING_SERVICE_UUID				= fromShort("181f");
	public static final UUID INTERNET_PROTOCOL_SUPPORT_SERVICE_UUID					= fromShort("1820");
	public static final UUID INDOOR_POSITIONING_SERVICE_UUID						= fromShort("1821");
	public static final UUID PULSE_OXIMETER_SERVICE_UUID							= fromShort("1822");
	public static final UUID HTTP_PROXY_SERVICE_UUID								= fromShort("1823");
	public static final UUID TRANSPORT_DISCOVERY_SERVICE_UUID						= fromShort("1824");
	public static final UUID OBJECT_TRANSFER_SERVICE_UUID							= fromShort("1825");
	public static final UUID FITNESS_MACHINE_SERVICE_UUID							= fromShort("1826");



	// Bluetooth spec characteristic UUIDs
	
	public static final UUID DEVICE_NAME											= fromShort("2a00");
	public static final UUID APPEARANCE												= fromShort("2a01");
	public static final UUID PERIPHERAL_PRIVACY_FLAG								= fromShort("2a02");
	public static final UUID RECONNECTION_ADDRESS									= fromShort("2a03");
	public static final UUID PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS				= fromShort("2a04");
	public static final UUID SERVICE_CHANGED										= fromShort("2a05");
	public static final UUID ALERT_LEVEL											= fromShort("2a06");
	public static final UUID TX_POWER_LEVEL											= fromShort("2a07");
	public static final UUID DATE_TIME												= fromShort("2a08");
	public static final UUID DAY_OF_WEEK											= fromShort("2a09");
	public static final UUID DAY_DATE_TIME											= fromShort("2a0a");
	public static final UUID EXACT_TIME_256											= fromShort("2a0c");
	public static final UUID DST_OFFSET												= fromShort("2a0d");
	public static final UUID TIME_ZONE												= fromShort("2a0e");
	public static final UUID LOCAL_TIME_INFORMATION									= fromShort("2a0f");
	public static final UUID TIME_WITH_DST											= fromShort("2a11");
	public static final UUID TIME_ACCURACY											= fromShort("2a12");
	public static final UUID TIME_SOURCE											= fromShort("2a13");
	public static final UUID REFERENCE_TIME_INFORMATION								= fromShort("2a14");
	public static final UUID TIME_UPDATE_CONTROL_POINT								= fromShort("2a16");
	public static final UUID TIME_UPDATE_STATE										= fromShort("2a17");
	public static final UUID GLUCOSE_MEASUREMENT									= fromShort("2a18");
	public static final UUID BATTERY_LEVEL											= fromShort("2a19");
	public static final UUID TEMPERATURE_MEASUREMENT								= fromShort("2a1c");
	public static final UUID TEMPERATURE_TYPE										= fromShort("2a1d");
	public static final UUID IMMEDIATE_TEMPERATURE									= fromShort("2a1e");
	public static final UUID MEASUREMENT_INTERVAL									= fromShort("2a21");
	public static final UUID BOOT_KEYBOARD_INPUT_REPORT								= fromShort("2a22");
	public static final UUID SYSTEM_ID												= fromShort("2a23");
	public static final UUID MODEL_NUMBER											= fromShort("2a24");
	public static final UUID SERIAL_NUMBER											= fromShort("2a25");
	public static final UUID FIRMWARE_REVISION										= fromShort("2a26");
	public static final UUID HARDWARE_REVISION										= fromShort("2a27");
	public static final UUID SOFTWARE_REVISION										= fromShort("2a28");
	public static final UUID MANUFACTURER_NAME										= fromShort("2a29");
	public static final UUID IEE_REGULATORY_CERT_DATA_LIST							= fromShort("2a2a");
	public static final UUID CURRENT_TIME											= fromShort("2a2b");
	public static final UUID MAGNETIC_DECLINATION									= fromShort("2a2c");
	public static final UUID SCAN_REFRESH											= fromShort("2a31");
	public static final UUID BOOT_KEYBOARD_OUTPUT_REPORT							= fromShort("2a32");
	public static final UUID BOOT_MOUSE_INPUT_REPORT								= fromShort("2a33");
	public static final UUID GLUCOSE_MEASUREMENT_CONTEXT							= fromShort("2a34");
	public static final UUID BLOOD_PRESSURE_MEASUREMENT								= fromShort("2a35");
	public static final UUID INTERMEDIATE_CUFF_PRESSURE								= fromShort("2a36");
	public static final UUID HEART_RATE_MEASUREMENT									= fromShort("2a37");
	public static final UUID BODY_SENSOR_LOCATION									= fromShort("2a38");
	public static final UUID HEART_RATE_CONTROL_POINT								= fromShort("2a39");
	public static final UUID ALERT_STATUS											= fromShort("2a3f");
	public static final UUID RINGER_CONTROL_POINT									= fromShort("2a40");
	public static final UUID RINGER_SETTING											= fromShort("2a41");
	public static final UUID ALERT_CATEGORY_ID_BIT_MASK								= fromShort("2a42");
	public static final UUID ALERT_CATEGORY_ID										= fromShort("2a43");
	public static final UUID ALERT_NOTIFICATION_CONTROL_POINT						= fromShort("2a44");
	public static final UUID UNREAD_ALERT_STATUS									= fromShort("2a45");
	public static final UUID NEW_ALERT												= fromShort("2a46");
	public static final UUID SUPPORTED_NEW_ALERT_CATEGORY							= fromShort("2a47");
	public static final UUID SUPPORTED_UNREAD_ALERT_CATEGORY						= fromShort("2a48");
	public static final UUID BLOOD_PRESSURE_FEATURE									= fromShort("2a49");
	public static final UUID HID_INFORMATION										= fromShort("2a4a");
	public static final UUID REPORT_MAP												= fromShort("2a4b");
	public static final UUID HID_CONTROL_POINT										= fromShort("2a4c");
	public static final UUID REPORT													= fromShort("2a4d");
	public static final UUID PROTOCOL_MODE											= fromShort("2a4e");
	public static final UUID SCAN_INTERVAL_WINDOW									= fromShort("2a4f");
	public static final UUID PNP_ID													= fromShort("2a50");
	public static final UUID GLUCOSE_FEATURE										= fromShort("2a51");
	public static final UUID RECORD_ACCESS_CONTROL_POINT							= fromShort("2a52");
	public static final UUID RSC_MEASUREMENT										= fromShort("2a53");
	public static final UUID RSC_FEATURE											= fromShort("2a54");
	public static final UUID SC_CONTROL_POINT										= fromShort("2a55");
	public static final UUID DIGITAL												= fromShort("2a56");
	public static final UUID ANALOG													= fromShort("2a58");
	public static final UUID AGGREGATE												= fromShort("2a5a");
	public static final UUID CSC_MEASUREMENT										= fromShort("2a5b");
	public static final UUID CSC_FEATURE											= fromShort("2a5c");
	public static final UUID SENSOR_LOCATION										= fromShort("2a5d");
	public static final UUID PLX_SPOT_CHECK_MEASUREMENT								= fromShort("2a5e");
	public static final UUID PLX_CONTINUOUS_MEASUREMENT								= fromShort("2a5f");
	public static final UUID PLX_FEATURES											= fromShort("2a60");
	public static final UUID CYCLING_POWER_MEASUREMENT								= fromShort("2a63");
	public static final UUID CYCLING_POWER_VECTOR									= fromShort("2a64");
	public static final UUID CYCLING_POWER_FEATURE									= fromShort("2a65");
	public static final UUID CYCLING_POWER_CONTROL_POINT							= fromShort("2a66");
	public static final UUID LOCATION_AND_SPEED										= fromShort("2a67");
	public static final UUID NAVIGATION												= fromShort("2a68");
	public static final UUID POSITION_QUALITY										= fromShort("2a69");
	public static final UUID LN_FEATURE												= fromShort("2a6a");
	public static final UUID LN_CONTROL_POINT										= fromShort("2a6b");
	public static final UUID ELEVATION												= fromShort("2a6c");
	public static final UUID PRESSURE												= fromShort("2a6d");
	public static final UUID TEMPERATURE											= fromShort("2a6e");
	public static final UUID HUMIDITY												= fromShort("2a6f");
	public static final UUID TRUE_WIND_SPEED										= fromShort("2a70");
	public static final UUID TRUE_WIND_DIRECTION									= fromShort("2a71");
	public static final UUID APPARENT_WIND_SPEED									= fromShort("2a72");
	public static final UUID APPARENT_WIND_DIRECTION								= fromShort("2a73");
	public static final UUID GUST_FACTOR											= fromShort("2a74");
	public static final UUID POLLEN_CONCENTRATION									= fromShort("2a75");
	public static final UUID UV_INDEX												= fromShort("2a76");
	public static final UUID IRRADIANCE												= fromShort("2a77");
	public static final UUID RAINFALL												= fromShort("2a78");
	public static final UUID WIND_CHILL												= fromShort("2a79");
	public static final UUID HEAT_INDEX												= fromShort("2a7a");
	public static final UUID DEW_POINT												= fromShort("2a7b");
	public static final UUID DESCRIPTOR_VALUE_CHANGED								= fromShort("2a7d");
	public static final UUID AEROBIC_HEART_RATE_LOWER_LIMIT							= fromShort("2a7e");
	public static final UUID AGE													= fromShort("2a80");
	public static final UUID ANAEROBIC_HEART_RATE_LOWER_LIMIT						= fromShort("2a81");
	public static final UUID ANAEROBIC_HEART_RATE_UPPER_LIMIT						= fromShort("2a82");
	public static final UUID ANAEROBIC_THRESHOLD									= fromShort("2a83");
	public static final UUID AEROBIC_HEART_RATE_UPPER_LIMIT							= fromShort("2a84");
	public static final UUID DATE_OF_BIRTH											= fromShort("2a85");
	public static final UUID DATE_OF_THRESHOLD_ASSESSMENT							= fromShort("2a86");
	public static final UUID EMAIL_ADDRESS											= fromShort("2a87");
	public static final UUID FAT_BURN_HEART_RATE_LOWER_LIMIT						= fromShort("2a88");
	public static final UUID FAT_BURN_HEART_RATE_UPPER_LIMIT						= fromShort("2a89");
	public static final UUID FIRST_NAME												= fromShort("2a8a");
	public static final UUID FIVE_ZONE_HEART_RATE_LIMITS							= fromShort("2a8b");
	public static final UUID GENDER													= fromShort("2a8c");
	public static final UUID HEART_RATE_MAX											= fromShort("2a8d");
	public static final UUID HEIGHT													= fromShort("2a8e");
	public static final UUID HIP_CIRCUMFERENCE										= fromShort("2a8f");
	public static final UUID LAST_NAME												= fromShort("2a90");
	public static final UUID MAXIMUM_RECOMMENDED_HEART_RATE							= fromShort("2a91");
	public static final UUID RESTING_HEART_RATE										= fromShort("2a92");
	public static final UUID SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS		= fromShort("2a93");
	public static final UUID THREE_ZONE_HEART_RATE_LIMITS							= fromShort("2a94");
	public static final UUID TWO_ZONE_HEART_RATE_LIMIT								= fromShort("2a95");
	public static final UUID VO2_MAX												= fromShort("2a96");
	public static final UUID WAIST_CIRCUMFERENCE									= fromShort("2a97");
	public static final UUID WEIGHT													= fromShort("2a98");
	public static final UUID DATABASE_CHANGE_INCREMENT								= fromShort("2a99");
	public static final UUID USER_INDEX												= fromShort("2a9a");
	public static final UUID BODY_COMPOSITION_FEATURE								= fromShort("2a9b");
	public static final UUID BODY_COMPOSITION_MEASUREMENT							= fromShort("2a9c");
	public static final UUID WEIGHT_MEASUREMENT										= fromShort("2a9d");
	public static final UUID WEIGHT_SCALE_FEATURE									= fromShort("2a9e");
	public static final UUID USER_CONTROL_POINT										= fromShort("2a9f");
	public static final UUID MAGNETIC_FLUX_DENSITY_2D								= fromShort("2aa0");
	public static final UUID MAGNETIC_FLUX_DENSITY_3D								= fromShort("2aa1");
	public static final UUID LANGUAGE												= fromShort("2aa2");
	public static final UUID BAROMETRIC_PRESSURE_TREND								= fromShort("2aa3");
	public static final UUID BOND_MANAGEMENT_CONTROL_POINT							= fromShort("2aa4");
	public static final UUID BOND_MANAGEMENT_FEATURE								= fromShort("2aa5");
	public static final UUID CENTRAL_ADDRESS_RESOLUTION								= fromShort("2aa6");
	public static final UUID CGM_MEASUREMENT										= fromShort("2aa7");
	public static final UUID CGM_FEATURE											= fromShort("2aa8");
	public static final UUID CGM_STATUS												= fromShort("2aa9");
	public static final UUID CGM_SESSION_START_TIME									= fromShort("2aaa");
	public static final UUID CGM_SESSION_RUN_TIME									= fromShort("2aab");
	public static final UUID CGM_SPECIFIC_OPS_CONTROL_POINT							= fromShort("2aac");
	public static final UUID INDOOR_POSITIONING_CONFIGURATION						= fromShort("2aad");
	public static final UUID LATITUDE												= fromShort("2aae");
	public static final UUID LONGITUDE												= fromShort("2aaf");
	public static final UUID LOCAL_NORTH_COORDINATE									= fromShort("2ab0");
	public static final UUID LOCAL_EAST_COORDINATE									= fromShort("2ab1");
	public static final UUID FLOOR_NUMBER											= fromShort("2ab2");
	public static final UUID ALTITUDE												= fromShort("2ab3");
	public static final UUID UNCERTAINTY											= fromShort("2ab4");
	public static final UUID LOCATION_NAME											= fromShort("2ab5");
	public static final UUID URI													= fromShort("2ab6");
	public static final UUID HTTP_HEADERS											= fromShort("2ab7");
	public static final UUID HTTP_STATUS_CODE										= fromShort("2ab8");
	public static final UUID HTTP_ENTITY_BODY										= fromShort("2ab9");
	public static final UUID HTTP_CONTROL_POINT										= fromShort("2aba");
	public static final UUID HTTPS_SECURITY											= fromShort("2abb");
	public static final UUID TDS_CONTROL_POINT										= fromShort("2abc");
	public static final UUID OTS_FEATURE											= fromShort("2abd");
	public static final UUID OBJECT_NAME											= fromShort("2abe");
	public static final UUID OBJECT_TYPE											= fromShort("2abf");
	public static final UUID OBJECT_SIZE											= fromShort("2ac0");
	public static final UUID OBJECT_FIRST_CREATED									= fromShort("2ac1");
	public static final UUID OBJECT_LAST_MODIFIED									= fromShort("2ac2");
	public static final UUID OBJECT_ID												= fromShort("2ac3");
	public static final UUID OBJECT_PROPERTIES										= fromShort("2ac4");
	public static final UUID OBJECT_ACTION_CONTROL_POINT							= fromShort("2ac5");
	public static final UUID OBJECT_LIST_CONTROL_POINT								= fromShort("2ac6");
	public static final UUID OBJECT_LIST_FILTER										= fromShort("2ac7");
	public static final UUID OBJECT_CHANGED											= fromShort("2ac8");
	public static final UUID RESOLVABLE_PRIVATE_ADDRESS_ONLY						= fromShort("2ac9");
	public static final UUID FITNESS_MACHINE_FEATURE								= fromShort("2acc");
	public static final UUID TREADMILL_DATA											= fromShort("2acd");
	public static final UUID CROSS_TRAINER_DATA										= fromShort("2ace");
	public static final UUID STEP_CLIMBER_DATA										= fromShort("2acf");
	public static final UUID STAIR_CLIMBER_DATA										= fromShort("2ad0");
	public static final UUID ROWER_DATA												= fromShort("2ad1");
	public static final UUID INDOOR_BIKE_DATA										= fromShort("2ad2");
	public static final UUID TRAINING_STATUS										= fromShort("2ad3");
	public static final UUID SUPPORTED_SPEED_RANGE									= fromShort("2ad4");
	public static final UUID SUPPORTED_INCLINATION_RANGE							= fromShort("2ad5");
	public static final UUID SUPPORTED_RESISTANCE_LEVEL_RANGE						= fromShort("2ad6");
	public static final UUID SUPPORTED_HEART_RATE_RANGE								= fromShort("2ad7");
	public static final UUID SUPPORTED_POWER_RANGE									= fromShort("2ad8");
	public static final UUID FITNESS_MACHINE_CONTROL_POINT							= fromShort("2ad9");
	public static final UUID FITNESS_MACHINE_STATUS									= fromShort("2ada");



	// Bluetooth spec descriptor UUIDs

	public static final UUID CHARACTERISTIC_EXTENDED_PROPERTIES						= fromShort("2900");
	public static final UUID CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID		= fromShort("2901");
	public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID	= fromShort("2902");
	public static final UUID SERVER_CHARACTERISTIC_CONFIGURATION					= fromShort("2903");
	public static final UUID CHARACTERISTIC_PRESENTATION_FORMAT_DESCRIPTOR_UUID		= fromShort("2904");
	public static final UUID CHARACTERISTIC_AGGREGATE_FORMAT						= fromShort("2905");
	public static final UUID VALID_RANGE											= fromShort("2906");
	public static final UUID EXTERNAL_REPORT_REFERENCE								= fromShort("2907");
	public static final UUID REPORT_REFERENCE										= fromShort("2908");
	public static final UUID NUMBER_OF_DIGITALS										= fromShort("2909");
	public static final UUID VALUE_TRIGGER_SETTING									= fromShort("290a");
	public static final UUID ENVIRONMENTAL_SENSING_CONFIGURATION					= fromShort("290b");
	public static final UUID ENVIRONMENTAL_SENSING_MEASUREMENT						= fromShort("290c");
	public static final UUID ENVIRONMENTAL_SENSING_TRIGGER_SETTING					= fromShort("290d");
	public static final UUID TIME_TRIGGER_SETTING									= fromShort("290e");


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


	protected final static String BLUETOOTH_CONNECTED_HASH = "1654fee13cf1381a3153d2280d33a160d8a8911e";

	
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
