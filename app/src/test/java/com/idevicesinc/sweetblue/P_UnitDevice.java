package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

public final class P_UnitDevice implements P_NativeDeviceLayer {


    @Override
    public void setNativeDevice(BluetoothDevice device) {

    }

    @Override
    public int getBondState() {
        return 0;
    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public boolean createBond() {
        return false;
    }

    @Override
    public boolean createBondSneaky(String methodName, boolean loggingEnabled) {
        return false;
    }

    @Override
    public BluetoothDevice getNativeDevice() {
        return null;
    }

    @Override
    public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback) {
        return null;
    }
}
