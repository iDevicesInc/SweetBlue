package com.idevicesinc.sweetblue;


import android.bluetooth.le.AdvertiseSettings;

public enum BleTransmissionPower {

    ULTRA_LOW   (AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW),
    LOW         (AdvertiseSettings.ADVERTISE_TX_POWER_LOW),
    MEDIUM      (AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM),
    HIGH        (AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

    private final int m_nativeMode;

    BleTransmissionPower(int nativeMode)
    {
        m_nativeMode = nativeMode;
    }

    public int getNativeMode()
    {
        return m_nativeMode;
    }

}
