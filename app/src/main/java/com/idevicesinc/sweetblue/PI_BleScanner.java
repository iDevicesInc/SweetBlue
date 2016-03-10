package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;

public interface PI_BleScanner
{

    boolean startClassicDiscovery();
    void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback);
    void startMScan(int scanMode, Interval delay, L_Util.ScanCallback callback);
    boolean startLeScan(BluetoothAdapter.LeScanCallback callback);
    void stopLeScan(BluetoothAdapter.LeScanCallback callback);
}
