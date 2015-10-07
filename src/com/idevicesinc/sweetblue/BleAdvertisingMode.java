package com.idevicesinc.sweetblue;


import android.bluetooth.le.AdvertiseSettings;

public enum BleAdvertisingMode {

    /**
     * This option is recommended and will let SweetBlue automatically choose what advertising mode to use
     * based on whether the app is backgrounded, if we're doing a long-term, or short-term advertise, etc.
     */
    AUTO(-1),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_LOW_POWER}.
     */
    LOW_POWER   (AdvertiseSettings.ADVERTISE_MODE_LOW_POWER),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_BALANCED}.
     */
    MEDIUM_POWER    (AdvertiseSettings.ADVERTISE_MODE_BALANCED),

    /**
     * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_LOW_LATENCY}.
     */
    HIGH_POWER (AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);

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
