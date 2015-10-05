package com.idevicesinc.sweetblue;



public enum BleAdvertiseMode {


    LOW_POWER   (/*AdvertiseSettings.ADVERTISE_MODE_LOW_POWER*/     0),
    BALANCED    (/*AdvertiseSettings.ADVERTISE_MODE_BALANCED*/      1),
    LOW_LATENCY (/*AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY*/   2);

    private final int m_native;

    BleAdvertiseMode(int nativeBit)
    {
        m_native = nativeBit;
    }

    public int getNativeBit()
    {
        return m_native;
    }
}
