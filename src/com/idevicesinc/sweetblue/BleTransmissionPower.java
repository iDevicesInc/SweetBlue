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
public enum BleTransmissionPower {

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_TX_POWER_ULTRA_LOW}.
     */
    ULTRA_LOW   (AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_TX_POWER_LOW}.
     */
    LOW         (AdvertiseSettings.ADVERTISE_TX_POWER_LOW),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_TX_POWER_MEDIUM}.
     */
    MEDIUM      (AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_TX_POWER_HIGH}.
     */
    HIGH        (AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

    private final int m_nativeMode;

    BleTransmissionPower(int nativeMode)
    {
        m_nativeMode = nativeMode;
    }

    /**
     * Returns one of the static final int members of {@link AdvertiseSettings}
     */
    public int getNativeMode()
    {
        return m_nativeMode;
    }

}
