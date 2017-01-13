package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import java.util.List;
import java.util.UUID;


interface P_GattLayer
{

    String getAddress(BluetoothDevice device);
    int getBondState(BluetoothDevice device);
    int getNativeConnectionState(BluetoothDevice device, BluetoothManager adapter);
    List<BluetoothGattService> getNativeServiceList(BluetoothGatt gatt, P_Logger logger);
    BluetoothGattService getService(BluetoothGatt gatt, UUID serviceUuid, P_Logger logger);
    boolean isGattNull(BluetoothGatt gatt);
    BluetoothGatt connect(BluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback);
    void disconnect(BluetoothGatt gatt);
    boolean readCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data);
    boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    boolean readDescriptor(BluetoothGatt gatt, BluetoothGattDescriptor descriptor);
    boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data);
    boolean writeDescriptor(BluetoothGatt gatt, BluetoothGattDescriptor descriptor);
    boolean createBond(BleDevice device);
    boolean createBondSneaky(BluetoothDevice device, String methodName, boolean loggingEnabled);
    boolean startDiscovery(BluetoothAdapter adapter);
    boolean cancelDiscovery(BluetoothAdapter adapter);
    boolean refreshGatt(BleDevice device);
    boolean discoverServices(BluetoothGatt gatt);
    boolean executeReliableWrite(BluetoothGatt gatt);
    boolean readRemoteRssi(BluetoothGatt gatt);
    boolean requestConnectionPriority(BleDevice device, BleConnectionPriority priority);
    boolean requestMtu(BleDevice device, int mtu);

}
