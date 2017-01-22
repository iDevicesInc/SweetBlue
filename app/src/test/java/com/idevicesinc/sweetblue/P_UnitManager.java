package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;


public final class P_UnitManager implements P_NativeManagerLayer {


    @Override
    public int getConnectionState(P_NativeDeviceLayer device) {
        return 0;
    }

    @Override
    public boolean startDiscovery() {
        return false;
    }

    @Override
    public boolean cancelDiscovery() {
        return false;
    }

    @Override
    public void setNativeManager(BluetoothManager manager) {

    }

    @Override
    public void setNativeAdaptor(BluetoothAdapter adaptor) {

    }

    @Override
    public BluetoothAdapter getNativeAdaptor() {
        return null;
    }

    @Override
    public BluetoothManager getNativeManager() {
        return null;
    }
}
