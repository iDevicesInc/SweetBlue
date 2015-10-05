package com.idevicesinc.sweetblue;


public enum BleAdvertisePower {

    ULTRA_LOW   (/*AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW*/     0),
    LOW         (/*AdvertiseSettings.ADVERTISE_TX_POWER_LOW*/           1),
    MEDIUM      (/*AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM*/        2),
    HIGH        (/*AdvertiseSettings.ADVERTISE_TX_POWER_HIGH*/          3);

    private final int m_native;

    BleAdvertisePower(int nativeBit)
    {
        m_native = nativeBit;
    }

    public int getNativeBit()
    {
        return m_native;
    }

}
