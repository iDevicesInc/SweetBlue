package com.idevicesinc.sweetblue;


import android.annotation.TargetApi;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

/**
 * Type-safe parallel of various static final int members of {@link ScanSettings} for setting the scanning power
 * when using the Lollipop scanning API. This is ONLY valid on devices running Lollipop, or higher.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public enum BleScanPower
{

    /**
     * SweetBlue will automatically set the scan power depending on if the app is in the foreground or not. When foregrounded,
     * and running a periodic, or limited scan, {@link #HIGH_POWER} will be used. If you are using an infinite scan length,
     * then {@link #MEDIUM_POWER} will be used instead. Otherwise, when in the background, {@link #LOW_POWER} is used.
     */
    AUTO(-1),

    /**
     * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_OPPORTUNISTIC}.
     * For phones lower than Lollipop, this will be ignored.
     */
    VERY_LOW_POWER(ScanSettings.SCAN_MODE_OPPORTUNISTIC),

    /**
     * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_LOW_POWER}.
     * For phones lower than Lollipop, this will be ignored.
     */
    LOW_POWER(ScanSettings.SCAN_MODE_LOW_POWER),

    /**
     * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_BALANCED}.
     * For phones lower than Lollipop, this will be ignored.
     */
    MEDIUM_POWER(ScanSettings.SCAN_MODE_BALANCED),

    /**
     * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_LOW_LATENCY}.
     * For phones lower than Lollipop, this will be ignored.
     */
    HIGH_POWER(ScanSettings.SCAN_MODE_LOW_LATENCY);


    private final int nativeMode;

    BleScanPower(int nativeMode)
    {
        this.nativeMode = nativeMode;
    }

    public int getNativeMode()
    {
        return nativeMode;
    }

    public static BleScanPower fromBleScanMode(BleScanMode mode)
    {
        switch (mode)
        {
            case HIGH_POWER:
                return HIGH_POWER;
            case MEDIUM_POWER:
                return MEDIUM_POWER;
            case LOW_POWER:
                return LOW_POWER;
            default:
                return AUTO;
        }
    }

}
