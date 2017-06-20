package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * Static-only class with some common BLE services for {@link BleServer#addService(BleService)}.
 */
public final class BleServices
{
	private BleServices(){}

	/**
	 * Returns a new service conforming to the "Current Time Service" specification.
	 */
	public static BleService currentTime()
	{
		final BleDescriptor descriptor						= new BleDescriptor
		(
			Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID,
			BleDescriptorPermission.READ, BleDescriptorPermission.WRITE
		);

		final BleCharacteristic characteristic_currentTime	= new BleCharacteristic
		(
			Uuids.CURRENT_TIME_SERVICE__CURRENT_TIME,
			descriptor,
			BleCharacteristicPermission.READ,
			BleCharacteristicProperty.READ, BleCharacteristicProperty.NOTIFY
		);

		final BleCharacteristic characteristic_localTime	= new BleCharacteristic
		(
			Uuids.CURRENT_TIME_SERVICE__LOCAL_TIME_INFO,
			BleCharacteristicPermission.READ,
			BleCharacteristicProperty.READ
		);

		final BleService currentTimeService					= new BleService
		(
			Uuids.CURRENT_TIME_SERVICE,
			characteristic_currentTime, characteristic_localTime
		);

		return currentTimeService;
	}
}
