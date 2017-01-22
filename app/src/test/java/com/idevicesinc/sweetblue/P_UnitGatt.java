package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import java.util.List;
import java.util.UUID;


public final class P_UnitGatt implements P_GattLayer {


    private boolean m_gattIsNull = true;


    @Override
    public void setGatt(BluetoothGatt gatt) {

    }

    @Override
    public BleManager.UhOhListener.UhOh closeGatt() {
        return null;
    }

    @Override
    public BluetoothGatt getGatt() {
        return null;
    }

    @Override
    public List<BluetoothGattService> getNativeServiceList(P_Logger logger) {
        return null;
    }

    @Override
    public BluetoothGattService getService(UUID serviceUuid, P_Logger logger) {
        return null;
    }

    @Override
    public boolean isGattNull() {
        return m_gattIsNull;
    }

    @Override
    public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback) {
        m_gattIsNull = false;
        return null;
    }

    @Override
    public void disconnect() {
        m_gattIsNull = true;
    }

    @Override
    public boolean requestMtu(int mtu) {
        return false;
    }

    @Override
    public boolean refreshGatt() {
        return false;
    }

    @Override
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    public boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data) {
        return false;
    }

    @Override
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        return false;
    }

    @Override
    public boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data) {
        return false;
    }

    @Override
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        return false;
    }

    @Override
    public boolean requestConnectionPriority(BleConnectionPriority priority) {
        return false;
    }

    @Override
    public boolean discoverServices() {
        return false;
    }

    @Override
    public boolean executeReliableWrite() {
        return false;
    }

    @Override
    public boolean readRemoteRssi() {
        return false;
    }
}
