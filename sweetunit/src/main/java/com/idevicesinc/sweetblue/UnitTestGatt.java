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


public class UnitTestGatt implements P_GattLayer {


    private boolean m_gattIsNull = true;
    private final BleDevice m_device;
    private boolean m_explicitDisconnect = false;
    private List<BluetoothGattService> m_services;


    public UnitTestGatt(BleDevice device)
    {
        m_device = device;
    }


    public void setDabatase(GattDatabase db)
    {
        m_services = db.getServiceList();
    }

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
        return m_services == null ? PA_ServiceManager.EMPTY_SERVICE_LIST : m_services;
    }

    @Override
    public BluetoothGattService getService(UUID serviceUuid, P_Logger logger) {
        if (m_services != null)
        {
            for (BluetoothGattService service : m_services)
            {
                if (service.getUuid().equals(serviceUuid))
                {
                    return service;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isGattNull() {
        return m_gattIsNull;
    }

    @Override
    public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback) {
        m_gattIsNull = false;
        m_explicitDisconnect = false;
        ((UnitTestManagerLayer) m_device.layerManager().getManagerLayer()).updateDeviceState(m_device, BluetoothGatt.STATE_CONNECTING);
        m_device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                if (!m_explicitDisconnect)
                {
                    setToConnecting();
                }
            }
        }, 50);
        m_device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                if (!m_explicitDisconnect)
                {
                    setToConnected();
                }
            }
        }, 150);
        return device.connect(context, useAutoConnect, callback);
    }

    public void setGattNull(boolean isNull)
    {
        m_gattIsNull = isNull;
    }

    public void setToConnecting()
    {
        m_device.m_listeners.onConnectionStateChange(null, BleStatuses.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTING);
    }

    public void setToConnected()
    {
        ((UnitTestManagerLayer) m_device.layerManager().getManagerLayer()).updateDeviceState(m_device, BluetoothGatt.STATE_CONNECTED);
        m_device.m_listeners.onConnectionStateChange(null, BleStatuses.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTED);
    }

    @Override
    public void disconnect() {
        m_gattIsNull = true;
        m_explicitDisconnect = true;
        preDisconnect();
    }

    private void preDisconnect()
    {
        m_device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                ((UnitTestManagerLayer) m_device.layerManager().getManagerLayer()).updateDeviceState(m_device, BluetoothGatt.STATE_DISCONNECTED);
                m_device.m_listeners.onConnectionStateChange(null, BleStatuses.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTED);
            }
        }, 50);
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
        int size;
        if (characteristic.getValue() != null)
        {
            size = characteristic.getValue().length;
        }
        else
        {
            size = 10;
        }
        sendReadResponse(characteristic, UnitTestUtils.randomBytes(size));
        return true;
    }

    public void sendReadResponse(BluetoothGattCharacteristic characteristic, byte[] data)
    {
        UnitTestUtils.readSuccess(getBleDevice(), characteristic, data, 150);
    }

    @Override
    public boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data) {
        return true;
    }

    @Override
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        sendWriteResponse(characteristic);
        return true;
    }

    public void sendWriteResponse(BluetoothGattCharacteristic characteristic)
    {
        UnitTestUtils.writeSuccess(getBleDevice(), characteristic, 150);
    }

    @Override public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable)
    {
        sendToggleNotifyResponse(characteristic, enable);
        return true;
    }

    public void sendToggleNotifyResponse(BluetoothGattCharacteristic characteristic, boolean enable)
    {
        // TODO - Implement this
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
        m_device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                if (!m_explicitDisconnect)
                {
                    setServicesDiscovered();
                }
            }
        }, 250);
        return true;
    }

    public void setServicesDiscovered()
    {
        m_device.m_listeners.onServicesDiscovered(null, BleStatuses.GATT_SUCCESS);
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

    @Override public BleDevice getBleDevice()
    {
        return m_device;
    }
}
