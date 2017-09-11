package com.idevicesinc.sweetblue.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Arrays;
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


	protected final static String BLUETOOTH_CONNECTED_HASH = "52a50be3ae1cb7651143c516dcfb4bc29722897f";

	
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

	public static class GATTCharacteristicFormatTypeConversionException extends Exception
	{
		GATTCharacteristicFormatTypeConversionException()
		{

		}

		GATTCharacteristicFormatTypeConversionException(String message)
		{
			super(message);
		}

		GATTCharacteristicFormatTypeConversionException(String message, Throwable cause)
		{
			super(message, cause);
		}

		GATTCharacteristicFormatTypeConversionException(Throwable cause)
		{
			super(cause);
		}
	}


	public enum GATTFormatType
	{
		GCFT_rfu("rfu", "Reserved for future use", false, -1),
		GCFT_boolean("boolean", "unsigned 1-bit; 0=false, 1=true", false, 1),
		GCFT_2bit("2bit", "unsigned 2-bit integer", false, 2),
		GCFT_nibble("nibble", "unsigned 4-bit integer", false, 4),
		GCFT_uint8("uint8", "unsigned 8-bit integer", true, 8),
		GCFT_uint12("uint12", "unsigned 12-bit integer", true, 12),
		GCFT_uint16("uint16", "unsigned 16-bit integer", true, 16),
		GCFT_uint24("uint24", "unsigned 24-bit integer", true, 24),
		GCFT_uint32("uint32", "unsigned 32-bit integer", true, 32),
		GCFT_uint48("uint48", "unsigned 48-bit integer", true, 48),
		GCFT_uint64("uint64", "unsigned 64-bit integer", true, 64),
		GCFT_uint128("uint128", "unsigned 128-bit integer", true, 128),
		GCFT_sint8("sint8", "signed 8-bit integer", true, 8),
		GCFT_sint12("sint12", "signed 12-bit integer", true, 12),
		GCFT_sint16("sint16", "signed 16-bit integer", true, 16),
		GCFT_sint24("sint24", "signed 24-bit integer", true, 24),
		GCFT_sint32("sint32", "signed 32-bit integer", true, 32),
		GCFT_sint48("sint48", "signed 48-bit integer", true, 48),
		GCFT_sint64("sint64", "signed 64-bit integer", true, 64),
		GCFT_sint128("sint128", "signed 128-bit integer", true, 128),
		GCFT_float32("float32", "IEEE-754 32-bit floating point", false, 32),
		GCFT_float64("float64", "IEEE-754 64-bit floating point", false, 64),
		GCFT_SFLOAT("SFLOAT", "IEEE-11073 16-bit SFLOAT", false, 16),
		GCFT_FLOAT("FLOAT", "IEEE-11073 32-bit FLOAT", false, 32),
		GCFT_duint16("duint16", "IEEE-20601 format", false, 16),
		GCFT_utf8s("utf8s", "UTF-8 string", false, -1),
		GCFT_utf16s("utf16s", "UTF-16 string", false, -1),
		GCFT_struct("struct", "Opaque structure", false, -1)
		// Remaining values RFU
		;

		private String mShortName;
		private String mDescription;
		private boolean mExponentValue;
		private int mSizeBits;

		GATTFormatType(String shortName, String description, boolean exponentValue, int sizeBits)
		{
			mShortName = shortName;
			mDescription = description;
			mExponentValue = exponentValue;
			mSizeBits = sizeBits;
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

		public int getSizeBits()
		{
			return mSizeBits;
		}

		public int getSizeBytes()
		{
			return (mSizeBits + 7) / 8;
		}

		private void checkOrThrow(BigInteger bi, Exception biex, long min, long max) throws GATTCharacteristicFormatTypeConversionException
		{
			BigInteger biMin = BigInteger.valueOf(min);
			BigInteger biMax = BigInteger.valueOf(max);
			checkOrThrow(bi, biex, biMin, biMax);
		}

		private void checkOrThrow(BigInteger bi, Exception biex, BigInteger biMin, BigInteger biMax) throws GATTCharacteristicFormatTypeConversionException
		{
			if (bi == null)
				throw new GATTCharacteristicFormatTypeConversionException(biex);
			if (bi.compareTo(biMin) < 0 || bi.compareTo(biMax) > 0)
				throw new GATTCharacteristicFormatTypeConversionException("Value " + bi + " out of range.  Valid range is (" + biMin + ", " + biMax + ")");
		}

		private byte[] padBigInt(byte bigEndianBytes[], int toByteSize)
		{
			if (bigEndianBytes.length >= toByteSize)
				return bigEndianBytes;

			// Look at the most significant bit of the first byte
			int firstBit = bigEndianBytes.length > 0 ? bigEndianBytes[0] : 0;
			firstBit >>>= 7;
			firstBit &= 0x1;

			byte pad = (byte)(firstBit == 0 ? 0x0 : 0xFF);

			int shift = 16 - bigEndianBytes.length;
			byte paddedBytes[] = new byte[toByteSize];
			for (int i = paddedBytes.length - 1; i >= 0; --i)
			{
				int idx = i - shift;
				if (idx >= 0)
					paddedBytes[i] = bigEndianBytes[idx];
				else
					paddedBytes[i] = pad;
			}

			BigInteger t1 = new BigInteger(bigEndianBytes);
			BigInteger t2 = new BigInteger(paddedBytes);

			assert(t1.compareTo(t2) == 0);

			return paddedBytes;
		}

		private BigInteger parseHexString(String hexString)
		{
			try
			{
				hexString = hexString.trim();
				if (hexString.startsWith("0x"))
					hexString = hexString.replaceFirst("0x", "");
				byte hexBytes[] = Utils_Byte.hexStringToBytes(hexString);
				BigInteger bi = new BigInteger(hexBytes);
				return bi;
			}
			catch (Exception e)
			{
				return null;
			}
		}

		private BigInteger parseBinaryString(String binaryString)
		{
			try
			{
				binaryString = binaryString.trim();
				if (binaryString.startsWith("0b"))
					binaryString = binaryString.replaceFirst("0b", "");
				byte binaryBytes[] = Utils_Byte.binaryStringToBytes(binaryString);
				BigInteger bi = new BigInteger(binaryBytes);
				return bi;
			}
			catch (Exception e)
			{
				return null;
			}
		}

		public byte[] stringToByteArray(String s) throws GATTCharacteristicFormatTypeConversionException  //FIXME:  New exception
		{
			// First, try to interpret the string as a numeric type
			BigInteger bi;
			Exception biex = null;

			try
			{
				bi = new BigInteger(s);
			}
			catch (NumberFormatException e)
			{
				biex = e;

				// Also try interpreting as hex or binary
				bi = parseHexString(s);
				if (bi == null)
					bi = parseBinaryString(s);
			}

			// Also attempt to parse as a big decimal
			BigDecimal bd;
			Exception bdex = null;
			try
			{
				bd = new BigDecimal(s);
			}
			catch (NumberFormatException e)
			{
				bd = null;
				bdex = e;
			}

			byte result1[] = new byte[1];

			switch (this)
			{
				case GCFT_boolean:
				{
					try
					{
						Boolean b = new Boolean(s);
						result1[0] = Utils_Byte.boolToByte(b);
						return result1;
					}
					catch (Exception e)
					{
						throw new GATTCharacteristicFormatTypeConversionException(e);
					}
				}
				case GCFT_2bit:
				{
					checkOrThrow(bi, biex, 0L, 3L);
					byte b = bi.byteValue();
					result1[0] = b;
					return result1;
				}
				case GCFT_nibble:
				{
					checkOrThrow(bi, biex, 0L, 15L);
					byte b = bi.byteValue();
					result1[0] = b;
					return result1;
				}
				case GCFT_uint8:
				{
					checkOrThrow(bi, biex, 0L, 255L);
					byte b = bi.byteValue();
					result1[0] = b;
					return result1;
				}
				case GCFT_uint12:
				{
					checkOrThrow(bi, biex, 0L, 4095L);
					byte result[] = Utils_Byte.shortToBytes(bi.shortValue());
					return result;
				}
				case GCFT_uint16:
				{
					checkOrThrow(bi, biex, 0L, 65535L);
					byte result[] = Utils_Byte.shortToBytes(bi.shortValue());
					return result;
				}
				case GCFT_uint24:
				{
					checkOrThrow(bi, biex, 0L, 16777215L);
					byte result[] = Utils_Byte.intToBytes(bi.intValue());
					result = Arrays.copyOfRange(result, 1, 4);
					return result;
				}
				case GCFT_uint32:
				{
					checkOrThrow(bi, biex, 0L, 4294967295L);
					byte result[] = Utils_Byte.intToBytes(bi.intValue());
					return result;
				}
				case GCFT_uint48:
				{
					checkOrThrow(bi, biex, 0L, 281474976710655L);
					byte result[] = Utils_Byte.longToBytes(bi.intValue());
					result = Arrays.copyOfRange(result, 2, 8);
					return result;
				}
				case GCFT_uint64:
				{
					checkOrThrow(bi, biex, new BigInteger("0"), new BigInteger("18446744073709551615"));
					byte result[] = Utils_Byte.longToBytes(bi.longValue());
					return result;
				}
				case GCFT_uint128:
				{
					checkOrThrow(bi, biex, new BigInteger("0"), new BigInteger("340282366920938463463374607431768211455"));
					byte result[] = bi.toByteArray();
					return padBigInt(result, 16);
				}
				case GCFT_sint8:
				{
					checkOrThrow(bi, biex, -128L, 127L);
					byte b = bi.byteValue();
					result1[0] = b;
					return result1;
				}
				case GCFT_sint12:
				{
					checkOrThrow(bi, biex, -2048L, 2047L);
					byte result[] = Utils_Byte.shortToBytes(bi.shortValue());
					//FIXME:  Are we supposed to mask out the first 4 bits?  Or does it not matter?
					return result;
				}
				case GCFT_sint16:
				{
					checkOrThrow(bi, biex, -32768L, 32767L);
					byte result[] = Utils_Byte.shortToBytes(bi.shortValue());
					return result;
				}
				case GCFT_sint24:
				{
					checkOrThrow(bi, biex, -8388608L, 8388607L);
					byte result[] = Utils_Byte.intToBytes(bi.intValue());
					result = Arrays.copyOfRange(result, 1, 4);
					return result;
				}
				case GCFT_sint32:
				{
					checkOrThrow(bi, biex, -2147483648L, 2147483647L);
					byte result[] = Utils_Byte.intToBytes(bi.intValue());
					return result;
				}
				case GCFT_sint48:
				{
					checkOrThrow(bi, biex, -140737488355328L, 140737488355327L);
					byte result[] = Utils_Byte.longToBytes(bi.intValue());
					result = Arrays.copyOfRange(result, 2, 8);
					return result;
				}
				case GCFT_sint64:
				{
					checkOrThrow(bi, biex, new BigInteger("-9223372036854775808"), new BigInteger("9223372036854775807"));
					byte result[] = Utils_Byte.longToBytes(bi.longValue());
					return result;
				}
				case GCFT_sint128:
				{
					checkOrThrow(bi, biex, new BigInteger("-170141183460469231731687303715884105728"), new BigInteger("170141183460469231731687303715884105727"));
					byte result[] = bi.toByteArray();
					return padBigInt(result, 16);
				}
				case GCFT_float32:
				{
					float f = bd.floatValue();
					int raw = Float.floatToIntBits(f);
					byte result[] = Utils_Byte.intToBytes(raw);
					return result;
				}
				case GCFT_float64:
				{
					double d = bd.doubleValue();
					long raw = Double.doubleToLongBits(d);
					byte result[] = Utils_Byte.longToBytes(raw);
					return result;
				}
				case GCFT_SFLOAT:
				case GCFT_FLOAT:
				case GCFT_duint16:
				{
					throw new GATTCharacteristicFormatTypeConversionException("Not supported");
				}
				case GCFT_utf8s:
				{
					try
					{
						byte result[] = s.getBytes("UTF-8");
						//FIXME:  Null terminate?
						return result;
					}
					catch (UnsupportedEncodingException e)
					{
						throw new GATTCharacteristicFormatTypeConversionException(e);
					}
				}
				case GCFT_utf16s:
				{
					try
					{
						byte result[] = s.getBytes("UTF-16");
						//FIXME:  Null terminate?
						return result;
					}
					catch (UnsupportedEncodingException e)
					{
						throw new GATTCharacteristicFormatTypeConversionException(e);
					}
				}
				case GCFT_struct:
				{
					// Interpret input as hex string
					byte result[] = Utils_Byte.hexStringToBytes(s);
					return result;
				}
			}

			// Should never get here anyway...
			throw new GATTCharacteristicFormatTypeConversionException("Not supported");
		}

		public byte[] objectToByteArray(Object o) throws GATTCharacteristicFormatTypeConversionException
		{
			try
			{
				// See if the input is already in a byte format, and if so return it
				if (o instanceof byte[])
				{
					//FIXME:  Trim this down if appropriate
					return (byte[])o;
				}

				// To ease conversion later on, make Booleans into Integers (so they are Numbers)
				if (o instanceof Boolean)
				{
					Boolean b = (Boolean)o;
					o = new Integer(b ? 1 : 0);
				}

				Number n = (o instanceof Number) ? (Number)o : null;
				String s = (o instanceof String) ? (String)o : null;

				// If we don't have a numeric type but we have a string, attempt to convert
				if (n == null && s != null)
				{
					try
					{
						n = NumberFormat.getInstance().parse(s);
					}
					catch (Exception e)
					{
						// Ignore conversion problems here
					}
				}

				// Opposite case, same idea
				if (n != null && s == null)
				{
					s = String.valueOf(n);
				}

				byte byteArr[] = null;

				switch (this)
				{
					case GCFT_boolean:
					case GCFT_2bit:
					case GCFT_nibble:
					case GCFT_uint8:
					case GCFT_sint8:
					{
						byteArr = new byte[1];
						byteArr[0] = (n.byteValue());
						return byteArr;
					}
					case GCFT_uint12:
					case GCFT_uint16:
					case GCFT_sint12:
					case GCFT_sint16:
						return Utils_Byte.shortToBytes(n.shortValue());
					case GCFT_uint24:
					case GCFT_sint24:
					{
						byteArr = Utils_Byte.intToBytes(n.intValue());
						return Arrays.copyOfRange(byteArr, 1, 4);
					}
					case GCFT_uint32:
					case GCFT_sint32:
						return Utils_Byte.intToBytes(n.intValue());
					case GCFT_uint48:
					case GCFT_sint48:
					{
						byteArr = Utils_Byte.longToBytes(n.longValue());
						return Arrays.copyOfRange(byteArr, 3, 8);
					}
					case GCFT_uint64:
					case GCFT_sint64:
						return Utils_Byte.longToBytes(n.longValue());
					case GCFT_uint128:
					case GCFT_sint128:
						//FIXME:  Implement
						throw new GATTCharacteristicFormatTypeConversionException(this.toString() + " not currently supported");
					case GCFT_float32:
						return Utils_Byte.floatToBytes(n.floatValue());
					case GCFT_float64:
						return Utils_Byte.doubleToBytes(n.doubleValue());
					case GCFT_SFLOAT:
					case GCFT_FLOAT:
					case GCFT_duint16:
						//FIXME:  Implement
						throw new GATTCharacteristicFormatTypeConversionException(this.toString() + " not currently supported");
					case GCFT_utf8s:
						return s.getBytes("UTF-8");
					case GCFT_utf16s:
						return s.getBytes("UTF-16");

					// Not handled:
					// GCFT_rfu
					// GCFT_struct
				}

				throw new GATTCharacteristicFormatTypeConversionException(this.toString() + " not currently supported");
			}
			catch (Exception e)
			{
				throw new GATTCharacteristicFormatTypeConversionException(e);
			}
		}
	}

	public enum GATTDisplayType
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

		public Object stringToObject(String s)
		{
			switch (this)
			{
				case Boolean:
					return java.lang.Boolean.valueOf(s);
				case Bitfield:
					try
					{
						return Utils_Byte.binaryStringToBytes(s);
					}
					catch (Exception e)
					{
						return null;
					}
				case UnsignedInteger:
				case SignedInteger:
					return Long.valueOf(s);
				case Decimal:
					return Double.valueOf(s);
				case String:
					return s;
				case Hex:
					//TODO:  remove 0x if present
					return Utils_Byte.hexStringToBytes(s);
			}

			return null;
		}

		/*public byte[] stringToByteArray(String s)
		{
			byte result[] = null;

			switch (this)
			{
				case Boolean:
					Boolean b = java.lang.Boolean.valueOf(s);
					result = new byte[1];
					result[0] = (byte)(b ? 1 : 0);
					break;
				case Bitfield:
					result = Utils_Byte.binaryStringToBytes(s);
					break;
				case UnsignedInteger:
				case SignedInteger:
					Long l = Long.valueOf(s);
					result = Utils_Byte.longToBytes(l);
					break;
				case Decimal:
					Double d = Double.valueOf(s);

				case String:
				case Hex:
					break;
			}

			return result;
		}*/
	}

	public enum GATTCharacteristic
 	{
	 	AerobicHeartRateLowerLimit("Aerobic Heart Rate Lower Limit", "2A7E", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		AerobicHeartRateUpperLimit("Aerobic Heart Rate Upper Limit", "2A84", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		AerobicThreshold("Aerobic Threshold", "2A7F", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Age("Age", "2A80", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Aggregate("Aggregate", "2A5A", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		AlertCategoryID("Alert Category ID", "2A43", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		AlertCategoryIDBitMask("Alert Category ID Bit Mask", "2A42", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		AlertLevel("Alert Level", "2A06", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		AlertNotificationControlPoint("Alert Notification Control Point", "2A44", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		AlertStatus("Alert Status", "2A3F", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Altitude("Altitude", "2AB3", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		AnaerobicHeartRateLowerLimit("Anaerobic Heart Rate Lower Limit", "2A81", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		AnaerobicHeartRateUpperLimit("Anaerobic Heart Rate Upper Limit", "2A82", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		AnaerobicThreshold("Anaerobic Threshold", "2A83", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Analog("Analog", "2A58", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		ApparentWindDirection("Apparent Wind Direction", "2A73", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		ApparentWindSpeed("Apparent Wind Speed", "2A72", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		Appearance("Appearance", "2A01", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		BarometricPressureTrend("Barometric Pressure Trend", "2AA3", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		BatteryLevel("Battery Level", "2A19", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		BloodPressureFeature("Blood Pressure Feature", "2A49", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		BloodPressureMeasurement("Blood Pressure Measurement", "2A35", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		BodyCompositionFeature("Body Composition Feature", "2A9B", GATTFormatType.GCFT_uint32, GATTDisplayType.Bitfield),
		BodyCompositionMeasurement("Body Composition Measurement", "2A9C", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		BodySensorLocation("Body Sensor Location", "2A38", GATTFormatType.GCFT_uint8, GATTDisplayType.Bitfield),
		BondManagementControlPoint("Bond Management Control Point", "2AA4", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		BondManagementFeatures("Bond Management Features", "2AA5", GATTFormatType.GCFT_uint24, GATTDisplayType.Bitfield),
		BootKeyboardInputReport("Boot Keyboard Input Report", "2A22", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		BootKeyboardOutputReport("Boot Keyboard Output Report", "2A32", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		BootMouseInputReport("Boot Mouse Input Report", "2A33", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		CentralAddressResolution("Central Address Resolution", "2AA6", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		CGMFeature("CGM Feature", "2AA8", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CGMMeasurement("CGM Measurement", "2AA7", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CGMSessionRunTime("CGM Session Run Time", "2AAB", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CGMSessionStartTime("CGM Session Start Time", "2AAA", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CGMSpecificOpsControlPoint("CGM Specific Ops Control Point", "2AAC", GATTFormatType.GCFT_uint8, GATTDisplayType.Bitfield),
		CGMStatus("CGM Status", "2AA9", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CrossTrainerData("Cross Trainer Data", "2ACE", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CSCFeature("CSC Feature", "2A5C", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		CSCMeasurement("CSC Measurement", "2A5B", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CurrentTime("Current Time", "2A2B", GATTFormatType.GCFT_uint8, GATTDisplayType.Bitfield),
		CyclingPowerControlPoint("Cycling Power Control Point", "2A66", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		CyclingPowerFeature("Cycling Power Feature", "2A65", GATTFormatType.GCFT_uint32, GATTDisplayType.Bitfield),
		CyclingPowerMeasurement("Cycling Power Measurement", "2A63", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CyclingPowerVector("Cycling Power Vector", "2A64", GATTFormatType.GCFT_sint16, GATTDisplayType.SignedInteger),
		DatabaseChangeIncrement("Database Change Increment", "2A99", GATTFormatType.GCFT_uint32, GATTDisplayType.UnsignedInteger),
		DateofBirth("Date of Birth", "2A85", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		DateofThresholdAssessment("Date of Threshold Assessment", "2A86", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		DateTime("Date Time", "2A08", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		DayDateTime("Day Date Time", "2A0A", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		DayofWeek("Day of Week", "2A09", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		DescriptorValueChanged("Descriptor Value Changed", "2A7D", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		DeviceName("Device Name", "2A00", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		DewPoint("Dew Point", "2A7B", GATTFormatType.GCFT_sint8, GATTDisplayType.SignedInteger),
		Digital("Digital", "2A56", GATTFormatType.GCFT_2bit, GATTDisplayType.Bitfield),
		DSTOffset("DST Offset", "2A0D", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Elevation("Elevation", "2A6C", GATTFormatType.GCFT_sint24, GATTDisplayType.SignedInteger),
		EmailAddress("Email Address", "2A87", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		ExactTime256("Exact Time 256", "2A0C", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		FatBurnHeartRateLowerLimit("Fat Burn Heart Rate Lower Limit", "2A88", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		FatBurnHeartRateUpperLimit("Fat Burn Heart Rate Upper Limit", "2A89", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		FirmwareRevisionString("Firmware Revision String", "2A26", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		FirstName("First Name", "2A8A", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		FitnessMachineControlPoint("Fitness Machine Control Point", "2AD9", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		FitnessMachineFeature("Fitness Machine Feature", "2ACC", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		FitnessMachineStatus("Fitness Machine Status", "2ADA", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		FiveZoneHeartRateLimits("Five Zone Heart Rate Limits", "2A8B", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		FloorNumber("Floor Number", "2AB2", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Gender("Gender", "2A8C", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		GlucoseFeature("Glucose Feature", "2A51", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		GlucoseMeasurement("Glucose Measurement", "2A18", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		GlucoseMeasurementContext("Glucose Measurement Context", "2A34", GATTFormatType.GCFT_SFLOAT, GATTDisplayType.Decimal),
		GustFactor("Gust Factor", "2A74", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		HardwareRevisionString("Hardware Revision String", "2A27", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		HeartRateControlPoint("Heart Rate Control Point", "2A39", GATTFormatType.GCFT_uint8, GATTDisplayType.Bitfield),
		HeartRateMax("Heart Rate Max", "2A8D", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		HeartRateMeasurement("Heart Rate Measurement", "2A37", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		HeatIndex("Heat Index", "2A7A", GATTFormatType.GCFT_sint8, GATTDisplayType.SignedInteger),
		Height("Height", "2A8E", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		HIDControlPoint("HID Control Point", "2A4C", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		HIDInformation("HID Information", "2A4A", GATTFormatType.GCFT_uint8, GATTDisplayType.Bitfield),
		HipCircumference("Hip Circumference", "2A8F", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		HTTPControlPoint("HTTP Control Point", "2ABA", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		HTTPEntityBody("HTTP Entity Body", "2AB9", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		HTTPHeaders("HTTP Headers", "2AB7", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		HTTPStatusCode("HTTP Status Code", "2AB8", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		HTTPSSecurity("HTTPS Security", "2ABB", GATTFormatType.GCFT_boolean, GATTDisplayType.Boolean),
		Humidity("Humidity", "2A6F", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		IEEE1107320601RegulatoryCertificationDataList("IEEE 11073-20601 Regulatory Certification Data List", "2A2A", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		IndoorBikeData("Indoor Bike Data", "2AD2", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		IndoorPositioningConfiguration("Indoor Positioning Configuration", "2AAD", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		IntermediateCuffPressure("Intermediate Cuff Pressure", "2A36", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		IntermediateTemperature("Intermediate Temperature", "2A1E", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		Irradiance("Irradiance", "2A77", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		Language("Language", "2AA2", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		LastName("Last Name", "2A90", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		Latitude("Latitude", "2AAE", GATTFormatType.GCFT_sint32, GATTDisplayType.SignedInteger),
		LNControlPoint("LN Control Point", "2A6B", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		LNFeature("LN Feature", "2A6A", GATTFormatType.GCFT_uint32, GATTDisplayType.Bitfield),
		LocalEastCoordinate("Local East Coordinate", "2AB1", GATTFormatType.GCFT_sint16, GATTDisplayType.SignedInteger),
		LocalNorthCoordinate("Local North Coordinate", "2AB0", GATTFormatType.GCFT_sint16, GATTDisplayType.SignedInteger),
		LocalTimeInformation("Local Time Information", "2A0F", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		LocationandSpeedCharacteristic("Location and Speed Characteristic", "2A67", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		LocationName("Location Name", "2AB5", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		Longitude("Longitude", "2AAF", GATTFormatType.GCFT_sint32, GATTDisplayType.SignedInteger),
		MagneticDeclination("Magnetic Declination", "2A2C", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		MagneticFluxDensity2D("Magnetic Flux Density - 2D", "2AA0", GATTFormatType.GCFT_sint16, GATTDisplayType.SignedInteger),
		MagneticFluxDensity3D("Magnetic Flux Density - 3D", "2AA1", GATTFormatType.GCFT_sint16, GATTDisplayType.SignedInteger),
		ManufacturerNameString("Manufacturer Name String", "2A29", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		MaximumRecommendedHeartRate("Maximum Recommended Heart Rate", "2A91", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		MeasurementInterval("Measurement Interval", "2A21", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		ModelNumberString("Model Number String", "2A24", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		Navigation("Navigation", "2A68", GATTFormatType.GCFT_sint24, GATTDisplayType.SignedInteger),
		NewAlert("New Alert", "2A46", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		ObjectActionControlPoint("Object Action Control Point", "2AC5", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ObjectChanged("Object Changed", "2AC8", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ObjectFirstCreated("Object First-Created", "2AC1", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ObjectID("Object ID", "2AC3", GATTFormatType.GCFT_uint48, GATTDisplayType.UnsignedInteger),
		ObjectLastModified("Object Last-Modified", "2AC2", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ObjectListControlPoint("Object List Control Point", "2AC6", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ObjectListFilter("Object List Filter", "2AC7", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ObjectName("Object Name", "2ABE", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		ObjectProperties("Object Properties", "2AC4", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ObjectSize("Object Size", "2AC0", GATTFormatType.GCFT_uint32, GATTDisplayType.UnsignedInteger),
		ObjectType("Object Type", "2ABF", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		OTSFeature("OTS Feature", "2ABD", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		PeripheralPreferredConnectionParameters("Peripheral Preferred Connection Parameters", "2A04", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		PeripheralPrivacyFlag("Peripheral Privacy Flag", "2A02", GATTFormatType.GCFT_boolean, GATTDisplayType.Boolean),
		PLXContinuousMeasurementCharacteristic("PLX Continuous Measurement Characteristic", "2A5F", GATTFormatType.GCFT_SFLOAT, GATTDisplayType.Decimal),
		PLXFeatures("PLX Features", "2A60", GATTFormatType.GCFT_uint24, GATTDisplayType.Bitfield),
		PLXSpotCheckMeasurement("PLX Spot-Check Measurement", "2A5E", GATTFormatType.GCFT_SFLOAT, GATTDisplayType.Decimal),
		PnPID("PnP ID", "2A50", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		PollenConcentration("Pollen Concentration", "2A75", GATTFormatType.GCFT_uint24, GATTDisplayType.UnsignedInteger),
		PositionQuality("Position Quality", "2A69", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Pressure("Pressure", "2A6D", GATTFormatType.GCFT_uint32, GATTDisplayType.UnsignedInteger),
		ProtocolMode("Protocol Mode", "2A4E", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Rainfall("Rainfall", "2A78", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		ReconnectionAddress("Reconnection Address", "2A03", GATTFormatType.GCFT_uint48, GATTDisplayType.UnsignedInteger),
		RecordAccessControlPoint("Record Access Control Point", "2A52", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ReferenceTimeInformation("Reference Time Information", "2A14", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		Report("Report", "2A4D", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		ReportMap("Report Map", "2A4B", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		ResolvablePrivateAddressOnly("Resolvable Private Address Only", "2AC9", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		RestingHeartRate("Resting Heart Rate", "2A92", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		RingerControlpoint("Ringer Control point", "2A40", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		RingerSetting("Ringer Setting", "2A41", GATTFormatType.GCFT_uint8, GATTDisplayType.Bitfield),
		RowerData("Rower Data", "2AD1", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		RSCFeature("RSC Feature", "2A54", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		RSCMeasurement("RSC Measurement", "2A53", GATTFormatType.GCFT_uint32, GATTDisplayType.UnsignedInteger),
		SCControlPoint("SC Control Point", "2A55", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ScanIntervalWindow("Scan Interval Window", "2A4F", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		ScanRefresh("Scan Refresh", "2A31", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		SensorLocation("Sensor Location", "2A5D", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		SerialNumberString("Serial Number String", "2A25", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		ServiceChanged("Service Changed", "2A05", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		SoftwareRevisionString("Software Revision String", "2A28", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		SportTypeforAerobicandAnaerobicThresholds("Sport Type for Aerobic and Anaerobic Thresholds", "2A93", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		StairClimberData("Stair Climber Data", "2AD0", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		StepClimberData("Step Climber Data", "2ACF", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		SupportedHeartRateRange("Supported Heart Rate Range", "2AD7", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		SupportedInclinationRange("Supported Inclination Range", "2AD5", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		SupportedNewAlertCategory("Supported New Alert Category", "2A47", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		SupportedPowerRange("Supported Power Range", "2AD8", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		SupportedResistanceLevelRange("Supported Resistance Level Range", "2AD6", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		SupportedSpeedRange("Supported Speed Range", "2AD4", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		SupportedUnreadAlertCategory("Supported Unread Alert Category", "2A48", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		SystemID("System ID", "2A23", GATTFormatType.GCFT_uint24, GATTDisplayType.UnsignedInteger),
		TDSControlPoint("TDS Control Point", "2ABC", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		Temperature("Temperature", "2A6E", GATTFormatType.GCFT_sint16, GATTDisplayType.SignedInteger),
		TemperatureMeasurement("Temperature Measurement", "2A1C", GATTFormatType.GCFT_FLOAT, GATTDisplayType.Decimal),
		TemperatureType("Temperature Type", "2A1D", GATTFormatType.GCFT_uint8, GATTDisplayType.Bitfield),
		ThreeZoneHeartRateLimits("Three Zone Heart Rate Limits", "2A94", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		TimeAccuracy("Time Accuracy", "2A12", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		TimeSource("Time Source", "2A13", GATTFormatType.GCFT_uint8, GATTDisplayType.Bitfield),
		TimeUpdateControlPoint("Time Update Control Point", "2A16", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		TimeUpdateState("Time Update State", "2A17", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		TimewithDST("Time with DST", "2A11", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		TimeZone("Time Zone", "2A0E", GATTFormatType.GCFT_sint8, GATTDisplayType.SignedInteger),
		TrainingStatus("Training Status", "2AD3", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		TreadmillData("Treadmill Data", "2ACD", GATTFormatType.GCFT_sint16, GATTDisplayType.SignedInteger),
		TrueWindDirection("True Wind Direction", "2A71", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		TrueWindSpeed("True Wind Speed", "2A70", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		TwoZoneHeartRateLimit("Two Zone Heart Rate Limit", "2A95", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		TxPowerLevel("Tx Power Level", "2A07", GATTFormatType.GCFT_sint8, GATTDisplayType.SignedInteger),
		Uncertainty("Uncertainty", "2AB4", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		UnreadAlertStatus("Unread Alert Status", "2A45", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		URI("URI", "2AB6", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		UserControlPoint("User Control Point", "2A9F", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		UserIndex("User Index", "2A9A", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		UVIndex("UV Index", "2A76", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		VO2Max("VO2 Max", "2A96", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		WaistCircumference("Waist Circumference", "2A97", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		Weight("Weight", "2A98", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		WeightMeasurement("Weight Measurement", "2A9D", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		WeightScaleFeature("Weight Scale Feature", "2A9E", GATTFormatType.GCFT_uint32, GATTDisplayType.Bitfield),
		WindChill("Wind Chill", "2A79", GATTFormatType.GCFT_sint8, GATTDisplayType.SignedInteger);

	 	private String mName;
	 	private UUID mUUID;
	 	private GATTFormatType mFormat;
	 	private GATTDisplayType mDisplayType;
	 	private static Map<UUID, GATTCharacteristic> sUUIDMap = null;

	 	GATTCharacteristic(String name, String uuidHex, GATTFormatType format, GATTDisplayType displayType)
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

	 	public GATTFormatType getFormat()
	 	{
		 		return mFormat;
		}

	 	public GATTDisplayType getDisplayType()
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

	public enum GATTDescriptor
	{
		CharacteristicAggregateFormat("Characteristic Aggregate Format", "2905", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		CharacteristicExtendedProperties("Characteristic Extended Properties", "2900", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		CharacteristicPresentationFormat("Characteristic Presentation Format", "2904", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		CharacteristicUserDescription("Characteristic User Description", "2901", GATTFormatType.GCFT_utf8s, GATTDisplayType.String),
		ClientCharacteristicConfiguration("Client Characteristic Configuration", "2902", GATTFormatType.GCFT_uint16, GATTDisplayType.Bitfield),
		EnvironmentalSensingConfiguration("Environmental Sensing Configuration", "290B", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		EnvironmentalSensingMeasurement("Environmental Sensing Measurement", "290C", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		EnvironmentalSensingTriggerSetting("Environmental Sensing Trigger Setting", "290D", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ExternalReportReference("External Report Reference", "2907", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		NumberofDigitals("Number of Digitals", "2909", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		ReportReference("Report Reference", "2908", GATTFormatType.GCFT_uint8, GATTDisplayType.UnsignedInteger),
		ServerCharacteristicConfiguration("Server Characteristic Configuration", "2903", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		TimeTriggerSetting("Time Trigger Setting", "290E", GATTFormatType.GCFT_uint16, GATTDisplayType.UnsignedInteger),
		ValidRange("Valid Range", "2906", GATTFormatType.GCFT_struct, GATTDisplayType.Hex),
		ValueTriggerSetting("Value Trigger Setting", "290A", GATTFormatType.GCFT_uint32, GATTDisplayType.UnsignedInteger);

		private String mName;
		private UUID mUUID;
		private GATTFormatType mFormat;
		private GATTDisplayType mDisplayType;
		private static Map<UUID, GATTDescriptor> sUUIDMap = null;

		GATTDescriptor(String name, String uuidHex, GATTFormatType format, GATTDisplayType displayType)
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

		public GATTFormatType getFormat()
		{
			return mFormat;
		}

		public GATTDisplayType getDisplayType()
		{
			return mDisplayType;
		}

		public static GATTDescriptor getDescriptorForUUID(UUID uuid)
		{
			if (sUUIDMap == null)
			{
				sUUIDMap = new HashMap<>();
				for (GATTDescriptor gc : GATTDescriptor.values())
					sUUIDMap.put(gc.getUUID(), gc);
			}
			return sUUIDMap.get(uuid);
		}
	}
}
