package com.idevicesinc.sweetblue;

/**
 * Enumeration signifying how a {@link BleDevice} instance was created.
 */
public enum BleDeviceOrigin
{
	/**
	 * Created from {@link BleManager#newDevice(String, String)} or overloads.
	 * This type of device can only be {@link BleDeviceState#UNDISCOVERED} by using
	 * {@link BleManager#undiscover(BleDevice)}.
	 */
	EXPLICIT,
	
	/**
	 * Created from an advertising discovery right before {@link BleManager.DiscoveryListener#onEvent(com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent)} is called.
	 */
	FROM_DISCOVERY;
}
