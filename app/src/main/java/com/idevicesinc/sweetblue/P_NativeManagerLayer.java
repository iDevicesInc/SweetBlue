package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;

interface P_NativeManagerLayer {

    int getConnectionState(P_NativeDeviceLayer device);
    boolean startDiscovery();
    boolean cancelDiscovery();
    void setNativeManager(BluetoothManager manager);
    void setNativeAdaptor(BluetoothAdapter adaptor);
    BluetoothAdapter getNativeAdaptor();
    BluetoothManager getNativeManager();
    boolean isLocationEnabledForScanning_byOsServices();
    boolean isLocationEnabledForScanning_byRuntimePermissions();
    boolean isLocationEnabledForScanning();
    boolean isBluetoothEnabled();
    boolean startClassicDiscovery();
    void stopClassicDiscovery();
    void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback);
    void startMScan(int scanMode, Interval delay, L_Util.ScanCallback callback);
    boolean startLeScan(BluetoothAdapter.LeScanCallback callback);
    void stopLeScan(BluetoothAdapter.LeScanCallback callback);

}
