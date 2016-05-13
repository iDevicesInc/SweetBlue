package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BleStatuses;


public enum BleScanPower
{

    /**
     * Auto will use {@link #HIGH} when the app is foregrounded, otherwise it will use {@link #MEDIUM}.
     */
    AUTO(-1),

    /**
     * A new setting in Android M which uses the least amount of power/battery. According to the android documentation,
     * applications using this scan mode will passively listen for other scan results without starting BLE scans themselves.
     * Basically, this mode will only find devices if another app on the phone is performing a scan.
     */
    VERY_LOW(BleStatuses.SCAN_MODE_OPPORTUNISTIC),

    /**
     * Scans for Bluetooth devices using the least amount of power, increasing battery efficiency.
     */
    LOW(BleStatuses.SCAN_MODE_LOW_POWER),

    /**
     * Scan using a balanced amount of power
     */
    MEDIUM(BleStatuses.SCAN_MODE_BALANCED),

    /**
     * The most aggressive power setting. This will be the least efficient when it comes to battery life.
     */
    HIGH(BleStatuses.SCAN_MODE_LOW_LATENCY);

    private final int mNativeMode;

    BleScanPower(int nativeMode)
    {
        mNativeMode = nativeMode;
    }

    public int getNativeMode()
    {
        return mNativeMode;
    }

}
