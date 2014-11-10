package com.idevicesinc.sweetblue.utils;

import java.util.UUID;

/**
 * A collection of common {@link UUID}s for services, characteristics, and descriptors.
 * 
 * @author dougkoellmer
 */
public class StandardUuids
{
	public static final UUID GENERIC_ATTRIBUTES_SERVICE_UUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
	public static final UUID GENERIC_ACCESS_SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
	public static final UUID DEVICE_INFORMATION_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	public static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	
	public static final UUID DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
	public static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	
	public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}
