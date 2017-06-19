package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanSettings;
import java.util.List;

/**
 * Type-safe parallel of various static final int members of {@link android.bluetooth.le.ScanSettings} and a way to
 * force pre-Lollipop scanning mode. Provide an option to {@link BleManagerConfig#scanApi}.
 */
public enum BleScanApi
{

    /**
     * This option is recommended and will let SweetBlue automatically choose what scanning api to use
     * based on whether the app is backgrounded, if we're doing a long-term scan, polling scan, etc.
     */
    AUTO,

    /**
     * Will force SweetBlue to use {@link BluetoothAdapter#startDiscovery()}, which is so-called "Bluetooth Classic" discovery.
     * This is the scanning mode used on the Android Bluetooth Settings screen. It only returns the mac address and name of your
     * device through a {@link com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.ScanEvent}, as opposed to full LE scanning packets
     * which usually have a service {@link java.util.UUID} (at the least) as well.
     */
    CLASSIC,

    /**
     * This forces the use of the pre-Lollipop scanning API {@link BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)},
     * which was deprecated in Lollipop. This is the default API that SweetBlue uses, as it was found to be more effective at
     * discovering devices than the API introduced in Lollipop.
     */
    PRE_LOLLIPOP,

    /**
     * This will tell SweetBlue to use the newer scanning API introduced in Lollipop
     * ({@link android.bluetooth.le.BluetoothLeScanner#startScan(List, ScanSettings, ScanCallback)}) . We've found that this API is
     * not yet as good as it's predecessor. It may be better for battery life, as you have more control over the scanning power (using
     * {@link BleScanPower}), however, even at {@link BleScanPower#HIGH_POWER}, we've found that it doesn't discover devices
     * as reliably as the pre-lollipop scan API.
     */
    POST_LOLLIPOP;


    public static BleScanApi fromBleScanMode(BleScanMode mode)
    {
        switch (mode)
        {
            case AUTO:
                return AUTO;
            case CLASSIC:
                return CLASSIC;
            case POST_LOLLIPOP:
            case HIGH_POWER:
            case LOW_POWER:
            case MEDIUM_POWER:
                return POST_LOLLIPOP;
            default:
                return PRE_LOLLIPOP;
        }
    }

}
