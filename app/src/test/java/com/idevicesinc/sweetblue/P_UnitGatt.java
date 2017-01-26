package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import java.util.List;
import java.util.UUID;


public class P_UnitGatt implements P_GattLayer {


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

    @Override public Boolean getAuthRetryValue()
    {
        return true;
    }

    @Override public boolean equals(BluetoothGatt gatt)
    {
        return false;
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
        return device.connect(context, useAutoConnect, callback);
    }

    @Override
    public void disconnect() {
        m_gattIsNull = true;
    }

    @Override
    public boolean requestMtu(int mtu) {
        return true;
    }

    @Override
    public boolean refreshGatt() {
        return true;
    }

    @Override
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        return true;
    }

    @Override
    public boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data) {
        return true;
    }

    @Override
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        return true;
    }

    @Override public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable)
    {
        return true;
    }

    @Override
    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        return true;
    }

    @Override
    public boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data) {
        return true;
    }

    @Override
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        return true;
    }

    @Override
    public boolean requestConnectionPriority(BleConnectionPriority priority) {
        return true;
    }

    @Override
    public boolean discoverServices() {
        return true;
    }

    @Override
    public boolean executeReliableWrite() {
        return true;
    }

    @Override public boolean beginReliableWrite()
    {
        return true;
    }

    @Override public void abortReliableWrite(BluetoothDevice device)
    {

    }

    @Override
    public boolean readRemoteRssi() {
        return true;
    }
}
