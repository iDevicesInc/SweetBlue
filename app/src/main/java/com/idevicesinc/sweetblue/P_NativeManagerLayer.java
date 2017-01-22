package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

interface P_NativeManagerLayer {

    int getConnectionState(P_NativeDeviceLayer device);
    boolean startDiscovery();
    boolean cancelDiscovery();
    void setNativeManager(BluetoothManager manager);
    void setNativeAdaptor(BluetoothAdapter adaptor);
    BluetoothAdapter getNativeAdaptor();
    BluetoothManager getNativeManager();

}
