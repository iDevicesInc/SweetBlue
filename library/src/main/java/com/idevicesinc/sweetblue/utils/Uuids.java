package com.idevicesinc.sweetblue.utils;

import java.util.HashMap;
import java.util.Map;
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


	protected final static String BLUETOOTH_CONNECTED_HASH = "fd56db762a92f10ffd37ae7609c6052936ea63b3";

	
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

	public enum GATTCharacteristicFormatType
	{
		GCFT_rfu("rfu", "Reserved for future use", false),
		GCFT_boolean("boolean", "unsigned 1-bit; 0=false, 1=true", false),
		GCFT_2bit("2bit", "unsigned 2-bit integer", false),
		GCFT_nibble("nibble", "unsigned 4-bit integer", false),
		GCFT_uint8("uint8", "unsigned 8-bit integer", true),
		GCFT_uint12("uint12", "unsigned 12-bit integer", true),
		GCFT_uint16("uint16", "unsigned 16-bit integer", true),
		GCFT_uint24("uint24", "unsigned 24-bit integer", true),
		GCFT_uint32("uint32", "unsigned 32-bit integer", true),
		GCFT_uint48("uint48", "unsigned 48-bit integer", true),
		GCFT_uint64("uint64", "unsigned 64-bit integer", true),
		GCFT_uint128("uint128", "unsigned 128-bit integer", true),
		GCFT_sint8("sint8", "signed 8-bit integer", true),
		GCFT_sint12("sint12", "signed 12-bit integer", true),
		GCFT_sint16("sint16", "signed 16-bit integer", true),
		GCFT_sint24("sint24", "signed 24-bit integer", true),
		GCFT_sint32("sint32", "signed 32-bit integer", true),
		GCFT_sint48("sint48", "signed 48-bit integer", true),
		GCFT_sint64("sint64", "signed 64-bit integer", true),
		GCFT_sint128("sint128", "signed 128-bit integer", true),
		GCFT_float32("float32", "IEEE-754 32-bit floating point", false),
		GCFT_float64("float64", "IEEE-754 64-bit floating point", false),
		GCFT_SFLOAT("SFLOAT", "IEEE-11073 16-bit SFLOAT", false),
		GCFT_FLOAT("FLOAT", "IEEE-11073 32-bit FLOAT", false),
		GCFT_duint16("duint16", "IEEE-20601 format", false),
		GCFT_utf8s("utf8s", "UTF-8 string", false),
		GCFT_utf16s("utf16s", "UTF-16 string", false),
		GCFT_struct("struct", "Opaque structure", false)
		// Remaining values RFU
		;

		private String mShortName;
		private String mDescription;
		private boolean mExponentValue;

		GATTCharacteristicFormatType(String shortName, String description, boolean exponentValue)
		{
			mShortName = shortName;
			mDescription = description;
			mExponentValue = exponentValue;
		}

		public String getShortName()
		{
			return mShortName;
		}

		public String getDescription()
		{
			return mDescription;
		}

		public boolean getExponentValue()
		{
			return mExponentValue;
		}
	}

	public enum GATTCharacteristicDisplayType
	{
		Boolean,
		Bitfield,
		UnsignedInteger,
		SignedInteger,
		Decimal,
		String,
		Hex;

		public String toString(byte data[])
		{
			switch (this)
			{
				case Boolean:
					Boolean b = new Boolean(Utils_Byte.byteToBool(data[0]));
					return b.toString();
				case Bitfield:
					return Utils_Byte.bytesToBinaryString(data, 1);  //TODO:  Size restriction?  For some bit fields we may only want 2 or 4 bits...
				case UnsignedInteger:
				case SignedInteger:
					Long l = Utils_Byte.bytesToLong(data);
					return l.toString();
				case Decimal:
					Double d = Utils_Byte.bytesToDouble(data);
					return d.toString();
				case String:
					try
					{
						String s = new String(data, "UTF-8");
						return s;
					}
					catch (Exception e)
					{
						return null;
					}
				case Hex:
					return "0x" + Utils_Byte.bytesToHexString(data);
			}
			return null;
		};
	};

	public enum GATTCharacteristic
	{
		AerobicHeartRateLowerLimit("Aerobic Heart Rate Lower Limit", "2A7E", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		AerobicHeartRateUpperLimit("Aerobic Heart Rate Upper Limit", "2A84", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		AerobicThreshold("Aerobic Threshold", "2A7F", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Age("Age", "2A80", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Aggregate("Aggregate", "2A5A", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		AlertCategoryID("Alert Category ID", "2A43", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		AlertCategoryIDBitMask("Alert Category ID Bit Mask", "2A42", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		AlertLevel("Alert Level", "2A06", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		AlertNotificationControlPoint("Alert Notification Control Point", "2A44", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		AlertStatus("Alert Status", "2A3F", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Altitude("Altitude", "2AB3", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		AnaerobicHeartRateLowerLimit("Anaerobic Heart Rate Lower Limit", "2A81", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		AnaerobicHeartRateUpperLimit("Anaerobic Heart Rate Upper Limit", "2A82", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		AnaerobicThreshold("Anaerobic Threshold", "2A83", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Analog("Analog", "2A58", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		ApparentWindDirection("Apparent Wind Direction", "2A73", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		ApparentWindSpeed("Apparent Wind Speed", "2A72", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		Appearance("Appearance", "2A01", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
		BarometricPressureTrend("Barometric Pressure Trend", "2AA3", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		BatteryLevel("Battery Level", "2A19", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		BloodPressureFeature("Blood Pressure Feature", "2A49", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
		BloodPressureMeasurement("Blood Pressure Measurement", "2A35", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
		BodyCompositionFeature("Body Composition Feature", "2A9B", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.Bitfield),
		BodyCompositionMeasurement("Body Composition Measurement", "2A9C", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		BodySensorLocation("Body Sensor Location", "2A38", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
		BondManagementControlPoint("Bond Management Control Point", "2AA4", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		BondManagementFeatures("Bond Management Features", "2AA5", GATTCharacteristicFormatType.GCFT_uint24, GATTCharacteristicDisplayType.Bitfield),
		BootKeyboardInputReport("Boot Keyboard Input Report", "2A22", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		BootKeyboardOutputReport("Boot Keyboard Output Report", "2A32", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		BootMouseInputReport("Boot Mouse Input Report", "2A33", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		CentralAddressResolution("Central Address Resolution", "2AA6", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		CGMFeature("CGM Feature", "2AA8", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		CGMMeasurement("CGM Measurement", "2AA7", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		CGMSessionRunTime("CGM Session Run Time", "2AAB", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		CGMSessionStartTime("CGM Session Start Time", "2AAA", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		CGMSpecificOpsControlPoint("CGM Specific Ops Control Point", "2AAC", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
		CGMStatus("CGM Status", "2AA9", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		CrossTrainerData("Cross Trainer Data", "2ACE", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		CSCFeature("CSC Feature", "2A5C", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
		CSCMeasurement("CSC Measurement", "2A5B", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		CurrentTime("Current Time", "2A2B", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
		CyclingPowerControlPoint("Cycling Power Control Point", "2A66", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		CyclingPowerFeature("Cycling Power Feature", "2A65", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.Bitfield),
		CyclingPowerMeasurement("Cycling Power Measurement", "2A63", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		CyclingPowerVector("Cycling Power Vector", "2A64", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
		DatabaseChangeIncrement("Database Change Increment", "2A99", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.UnsignedInteger),
		DateofBirth("Date of Birth", "2A85", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		DateofThresholdAssessment("Date of Threshold Assessment", "2A86", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		DateTime("Date Time", "2A08", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		DayDateTime("Day Date Time", "2A0A", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		DayofWeek("Day of Week", "2A09", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		DescriptorValueChanged("Descriptor Value Changed", "2A7D", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		DeviceName("Device Name", "2A00", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		DewPoint("Dew Point", "2A7B", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger),
		Digital("Digital", "2A56", GATTCharacteristicFormatType.GCFT_2bit, GATTCharacteristicDisplayType.Bitfield),
		DSTOffset("DST Offset", "2A0D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Elevation("Elevation", "2A6C", GATTCharacteristicFormatType.GCFT_sint24, GATTCharacteristicDisplayType.SignedInteger),
		EmailAddress("Email Address", "2A87", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		ExactTime256("Exact Time 256", "2A0C", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		FatBurnHeartRateLowerLimit("Fat Burn Heart Rate Lower Limit", "2A88", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		FatBurnHeartRateUpperLimit("Fat Burn Heart Rate Upper Limit", "2A89", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		FirmwareRevisionString("Firmware Revision String", "2A26", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		FirstName("First Name", "2A8A", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		FitnessMachineControlPoint("Fitness Machine Control Point", "2AD9", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		FitnessMachineFeature("Fitness Machine Feature", "2ACC", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		FitnessMachineStatus("Fitness Machine Status", "2ADA", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		FiveZoneHeartRateLimits("Five Zone Heart Rate Limits", "2A8B", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		FloorNumber("Floor Number", "2AB2", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Gender("Gender", "2A8C", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		GlucoseFeature("Glucose Feature", "2A51", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
		GlucoseMeasurement("Glucose Measurement", "2A18", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
		GlucoseMeasurementContext("Glucose Measurement Context", "2A34", GATTCharacteristicFormatType.GCFT_SFLOAT, GATTCharacteristicDisplayType.Decimal),
		GustFactor("Gust Factor", "2A74", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		HardwareRevisionString("Hardware Revision String", "2A27", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		HeartRateControlPoint("Heart Rate Control Point", "2A39", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
		HeartRateMax("Heart Rate Max", "2A8D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		HeartRateMeasurement("Heart Rate Measurement", "2A37", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		HeatIndex("Heat Index", "2A7A", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger),
		Height("Height", "2A8E", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		HIDControlPoint("HID Control Point", "2A4C", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		HIDInformation("HID Information", "2A4A", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
		HipCircumference("Hip Circumference", "2A8F", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		HTTPControlPoint("HTTP Control Point", "2ABA", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		HTTPEntityBody("HTTP Entity Body", "2AB9", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		HTTPHeaders("HTTP Headers", "2AB7", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		HTTPStatusCode("HTTP Status Code", "2AB8", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		HTTPSSecurity("HTTPS Security", "2ABB", GATTCharacteristicFormatType.GCFT_boolean, GATTCharacteristicDisplayType.Boolean),
		Humidity("Humidity", "2A6F", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		IEEE1107320601RegulatoryCertificationDataList("IEEE 11073-20601 Regulatory Certification Data List", "2A2A", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		IndoorBikeData("Indoor Bike Data", "2AD2", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		IndoorPositioningConfiguration("Indoor Positioning Configuration", "2AAD", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		IntermediateCuffPressure("Intermediate Cuff Pressure", "2A36", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		IntermediateTemperature("Intermediate Temperature", "2A1E", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		Irradiance("Irradiance", "2A77", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		Language("Language", "2AA2", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		LastName("Last Name", "2A90", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		Latitude("Latitude", "2AAE", GATTCharacteristicFormatType.GCFT_sint32, GATTCharacteristicDisplayType.SignedInteger),
		LNControlPoint("LN Control Point", "2A6B", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		LNFeature("LN Feature", "2A6A", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.Bitfield),
		LocalEastCoordinate("Local East Coordinate", "2AB1", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
		LocalNorthCoordinate("Local North Coordinate", "2AB0", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
		LocalTimeInformation("Local Time Information", "2A0F", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		LocationandSpeedCharacteristic("Location and Speed Characteristic", "2A67", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		LocationName("Location Name", "2AB5", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		Longitude("Longitude", "2AAF", GATTCharacteristicFormatType.GCFT_sint32, GATTCharacteristicDisplayType.SignedInteger),
		MagneticDeclination("Magnetic Declination", "2A2C", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		MagneticFluxDensity2D("Magnetic Flux Density - 2D", "2AA0", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
		MagneticFluxDensity3D("Magnetic Flux Density - 3D", "2AA1", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
		ManufacturerNameString("Manufacturer Name String", "2A29", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		MaximumRecommendedHeartRate("Maximum Recommended Heart Rate", "2A91", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		MeasurementInterval("Measurement Interval", "2A21", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		ModelNumberString("Model Number String", "2A24", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		Navigation("Navigation", "2A68", GATTCharacteristicFormatType.GCFT_sint24, GATTCharacteristicDisplayType.SignedInteger),
		NewAlert("New Alert", "2A46", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		ObjectActionControlPoint("Object Action Control Point", "2AC5", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ObjectChanged("Object Changed", "2AC8", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ObjectFirstCreated("Object First-Created", "2AC1", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ObjectID("Object ID", "2AC3", GATTCharacteristicFormatType.GCFT_uint48, GATTCharacteristicDisplayType.UnsignedInteger),
		ObjectLastModified("Object Last-Modified", "2AC2", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ObjectListControlPoint("Object List Control Point", "2AC6", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ObjectListFilter("Object List Filter", "2AC7", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ObjectName("Object Name", "2ABE", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		ObjectProperties("Object Properties", "2AC4", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ObjectSize("Object Size", "2AC0", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.UnsignedInteger),
		ObjectType("Object Type", "2ABF", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		OTSFeature("OTS Feature", "2ABD", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		PeripheralPreferredConnectionParameters("Peripheral Preferred Connection Parameters", "2A04", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		PeripheralPrivacyFlag("Peripheral Privacy Flag", "2A02", GATTCharacteristicFormatType.GCFT_boolean, GATTCharacteristicDisplayType.Boolean),
		PLXContinuousMeasurementCharacteristic("PLX Continuous Measurement Characteristic", "2A5F", GATTCharacteristicFormatType.GCFT_SFLOAT, GATTCharacteristicDisplayType.Decimal),
		PLXFeatures("PLX Features", "2A60", GATTCharacteristicFormatType.GCFT_uint24, GATTCharacteristicDisplayType.Bitfield),
		PLXSpotCheckMeasurement("PLX Spot-Check Measurement", "2A5E", GATTCharacteristicFormatType.GCFT_SFLOAT, GATTCharacteristicDisplayType.Decimal),
		PnPID("PnP ID", "2A50", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		PollenConcentration("Pollen Concentration", "2A75", GATTCharacteristicFormatType.GCFT_uint24, GATTCharacteristicDisplayType.UnsignedInteger),
		PositionQuality("Position Quality", "2A69", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Pressure("Pressure", "2A6D", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.UnsignedInteger),
		ProtocolMode("Protocol Mode", "2A4E", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Rainfall("Rainfall", "2A78", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		ReconnectionAddress("Reconnection Address", "2A03", GATTCharacteristicFormatType.GCFT_uint48, GATTCharacteristicDisplayType.UnsignedInteger),
		RecordAccessControlPoint("Record Access Control Point", "2A52", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ReferenceTimeInformation("Reference Time Information", "2A14", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		Report("Report", "2A4D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		ReportMap("Report Map", "2A4B", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		ResolvablePrivateAddressOnly("Resolvable Private Address Only", "2AC9", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		RestingHeartRate("Resting Heart Rate", "2A92", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		RingerControlpoint("Ringer Control point", "2A40", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		RingerSetting("Ringer Setting", "2A41", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
		RowerData("Rower Data", "2AD1", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		RSCFeature("RSC Feature", "2A54", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
		RSCMeasurement("RSC Measurement", "2A53", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.UnsignedInteger),
		SCControlPoint("SC Control Point", "2A55", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		ScanIntervalWindow("Scan Interval Window", "2A4F", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		ScanRefresh("Scan Refresh", "2A31", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		SensorLocation("Sensor Location", "2A5D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		SerialNumberString("Serial Number String", "2A25", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		ServiceChanged("Service Changed", "2A05", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		SoftwareRevisionString("Software Revision String", "2A28", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		SportTypeforAerobicandAnaerobicThresholds("Sport Type for Aerobic and Anaerobic Thresholds", "2A93", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		StairClimberData("Stair Climber Data", "2AD0", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		StepClimberData("Step Climber Data", "2ACF", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		SupportedHeartRateRange("Supported Heart Rate Range", "2AD7", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		SupportedInclinationRange("Supported Inclination Range", "2AD5", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		SupportedNewAlertCategory("Supported New Alert Category", "2A47", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		SupportedPowerRange("Supported Power Range", "2AD8", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		SupportedResistanceLevelRange("Supported Resistance Level Range", "2AD6", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		SupportedSpeedRange("Supported Speed Range", "2AD4", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		SupportedUnreadAlertCategory("Supported Unread Alert Category", "2A48", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		SystemID("System ID", "2A23", GATTCharacteristicFormatType.GCFT_uint24, GATTCharacteristicDisplayType.UnsignedInteger),
		TDSControlPoint("TDS Control Point", "2ABC", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		Temperature("Temperature", "2A6E", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
		TemperatureMeasurement("Temperature Measurement", "2A1C", GATTCharacteristicFormatType.GCFT_FLOAT, GATTCharacteristicDisplayType.Decimal),
		TemperatureType("Temperature Type", "2A1D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
		ThreeZoneHeartRateLimits("Three Zone Heart Rate Limits", "2A94", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		TimeAccuracy("Time Accuracy", "2A12", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		TimeSource("Time Source", "2A13", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
		TimeUpdateControlPoint("Time Update Control Point", "2A16", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		TimeUpdateState("Time Update State", "2A17", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		TimewithDST("Time with DST", "2A11", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		TimeZone("Time Zone", "2A0E", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger),
		TrainingStatus("Training Status", "2AD3", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		TreadmillData("Treadmill Data", "2ACD", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
		TrueWindDirection("True Wind Direction", "2A71", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		TrueWindSpeed("True Wind Speed", "2A70", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		TwoZoneHeartRateLimit("Two Zone Heart Rate Limit", "2A95", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		TxPowerLevel("Tx Power Level", "2A07", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger),
		Uncertainty("Uncertainty", "2AB4", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		UnreadAlertStatus("Unread Alert Status", "2A45", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		URI("URI", "2AB6", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
		UserControlPoint("User Control Point", "2A9F", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
		UserIndex("User Index", "2A9A", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		UVIndex("UV Index", "2A76", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		VO2Max("VO2 Max", "2A96", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
		WaistCircumference("Waist Circumference", "2A97", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		Weight("Weight", "2A98", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		WeightMeasurement("Weight Measurement", "2A9D", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
		WeightScaleFeature("Weight Scale Feature", "2A9E", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.Bitfield),
		WindChill("Wind Chill", "2A79", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger);

		private String mName;
		private UUID mUUID;
		private GATTCharacteristicFormatType mFormat;
		private GATTCharacteristicDisplayType mDisplayType;

		private static Map<UUID, GATTCharacteristic> sUUIDMap = null;

		GATTCharacteristic(String name, String uuidHex, GATTCharacteristicFormatType format, GATTCharacteristicDisplayType displayType)
		{
			mName = name;
			mUUID = Uuids.fromShort(uuidHex);
			mFormat = format;
			mDisplayType = displayType;
		}

		public String getName()
		{
			return mName;
		}

		public UUID getUUID()
		{
			return mUUID;
		}

		public GATTCharacteristicFormatType getFormat()
		{
			return mFormat;
		}

		public GATTCharacteristicDisplayType getDisplayType()
		{
			return mDisplayType;
		}

		public static GATTCharacteristic getCharacteristicForUUID(UUID uuid)
		{
			if (sUUIDMap == null)
			{
				sUUIDMap = new HashMap<>();

				for (GATTCharacteristic gc : GATTCharacteristic.values())
					sUUIDMap.put(gc.getUUID(), gc);
			}
			return sUUIDMap.get(uuid);
		}
	}
}
