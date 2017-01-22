package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import java.util.List;
import java.util.UUID;


interface P_GattLayer
{

    void setGatt(BluetoothGatt gatt);
    BleManager.UhOhListener.UhOh closeGatt();
    BluetoothGatt getGatt();
    List<BluetoothGattService> getNativeServiceList(P_Logger logger);
    BluetoothGattService getService(UUID serviceUuid, P_Logger logger);
    boolean isGattNull();
    BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback);
    void disconnect();
    boolean requestMtu(int mtu);
    boolean refreshGatt();
    boolean readCharacteristic(BluetoothGattCharacteristic characteristic);
    boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data);
    boolean writeCharacteristic(BluetoothGattCharacteristic characteristic);
    boolean readDescriptor(BluetoothGattDescriptor descriptor);
    boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data);
    boolean writeDescriptor(BluetoothGattDescriptor descriptor);
    boolean requestConnectionPriority(BleConnectionPriority priority);
    boolean discoverServices();
    boolean executeReliableWrite();
    boolean readRemoteRssi();

}
