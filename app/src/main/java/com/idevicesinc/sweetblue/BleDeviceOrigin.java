package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.listeners.DiscoveryListener;

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
     * Created from an advertising discovery right before {@link DiscoveryListener#onEvent(DiscoveryListener.DiscoveryEvent)} is called.
     */
    FROM_DISCOVERY;
}
