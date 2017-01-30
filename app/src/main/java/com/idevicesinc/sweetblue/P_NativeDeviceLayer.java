package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;


interface P_NativeDeviceLayer {

    void setNativeDevice(BluetoothDevice device);
    int getBondState();
    String getAddress();
    String getName();
    boolean createBond();
    boolean isDeviceNull();
    boolean removeBond();
    boolean cancelBond();
    boolean equals(P_NativeDeviceLayer device);
    boolean createBondSneaky(String methodName, boolean loggingEnabled);
    BluetoothDevice getNativeDevice();
    BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback);
    void updateBleDevice(BleDevice device);
    BleDevice getBleDevice();

    P_NativeDeviceLayer NULL = new P_NativeDeviceLayer()
    {
        @Override public void setNativeDevice(BluetoothDevice device)
        {
        }

        @Override public int getBondState()
        {
            return 0;
        }

        @Override public String getAddress()
        {
            return "";
        }

        @Override public String getName()
        {
            return "";
        }

        @Override public boolean createBond()
        {
            return false;
        }

        @Override public boolean isDeviceNull()
        {
            return false;
        }

        @Override public boolean removeBond()
        {
            return false;
        }

        @Override public boolean cancelBond()
        {
            return false;
        }

        @Override public boolean equals(P_NativeDeviceLayer device)
        {
            return device == this;
        }

        @Override public boolean createBondSneaky(String methodName, boolean loggingEnabled)
        {
            return false;
        }

        @Override public BluetoothDevice getNativeDevice()
        {
            return null;
        }

        @Override public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            return null;
        }

        @Override public void updateBleDevice(BleDevice device)
        {
        }

        @Override public BleDevice getBleDevice()
        {
            return BleDevice.NULL;
        }
    };

}
