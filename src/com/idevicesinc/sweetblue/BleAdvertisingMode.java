package com.idevicesinc.sweetblue;


import android.bluetooth.le.AdvertiseSettings;

import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Type-safe parallel of static final int members of {@link android.bluetooth.le.AdvertiseSettings}.
 * Provide an instance to {@link BleServer#startAdvertising(BleAdvertisingPacket, BleAdvertisingMode, BleTransmissionPower)},
 * {@link BleServer#startAdvertising(BleAdvertisingPacket, BleAdvertisingMode, BleTransmissionPower, Interval)}, or
 * {@link BleServer#startAdvertising(BleAdvertisingPacket, BleAdvertisingMode, BleTransmissionPower, Interval, BleServer.AdvertisingListener)}
 * This is only applicable for Android >= 5.0 OS levels.
 */
public enum BleAdvertisingMode {

    /**
     * This option is recommended and will let SweetBlue automatically choose what advertising mode to use
     * based on whether the app is backgrounded, if we're doing a continuous, or short-term advertisement
     */
    AUTO(-1),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_LOW_POWER}.
     */
    LOW_FREQUENCY(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_BALANCED}.
     */
    MEDIUM_FREQUENCY(AdvertiseSettings.ADVERTISE_MODE_BALANCED),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_LOW_LATENCY}.
     */
    HIGH_FREQUENCY(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);

    private final int m_nativeMode;

    BleAdvertisingMode(int nativeMode)
    {
        m_nativeMode = nativeMode;
    }

    /**
     * Returns one of the static final int members of {@link AdvertiseSettings}, or -1 for {@link #AUTO}.
     */
    public int getNativeMode()
    {
        return m_nativeMode;
    }
}
