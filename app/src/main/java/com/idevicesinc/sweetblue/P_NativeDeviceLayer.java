package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;


interface P_NativeDeviceLayer {

    void setNativeDevice(BluetoothDevice device);
    int getBondState();
    String getAddress();
    boolean createBond();
    boolean createBondSneaky(String methodName, boolean loggingEnabled);
    BluetoothDevice getNativeDevice();
    BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback);

}
