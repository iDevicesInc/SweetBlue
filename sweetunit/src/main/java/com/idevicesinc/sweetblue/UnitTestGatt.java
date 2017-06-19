package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;

import java.util.List;
import java.util.Random;
import java.util.UUID;


public class UnitTestGatt implements P_GattLayer {


    private Interval m_delayTime;
    private boolean m_gattIsNull = true;
    private final BleDevice m_device;
    private boolean m_explicitDisconnect = false;
    private List<BluetoothGattService> m_services;


    public UnitTestGatt(BleDevice device)
    {
        m_device = device;
    }

    public UnitTestGatt(BleDevice device, GattDatabase gattDb)
    {
        this(device);
        m_services = gattDb.getServiceList();
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
        return m_services == null ? P_Const.EMPTY_SERVICE_LIST : m_services;
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
        }, 100);
        m_device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                if (!m_explicitDisconnect)
                {
                    setToConnected();
                }
            }
        }, 250);
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
        UnitTestUtils.setToConnected(m_device, BleStatuses.GATT_SUCCESS, Interval.millis(0));
    }

    @Override
    public void disconnect() {
        m_gattIsNull = true;
        m_explicitDisconnect = true;
        preDisconnect();
    }

    private void preDisconnect()
    {
        UnitTestUtils.setToDisconnected(m_device, BleStatuses.GATT_SUCCESS);
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
        sendReadResponse(characteristic, characteristic.getValue());
        return true;
    }

    public void sendReadResponse(BluetoothGattCharacteristic characteristic, byte[] data)
    {
        UnitTestUtils.readSuccess(getBleDevice(), characteristic, data, getDelayTime());
    }

    @Override
    public boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data) {
        characteristic.setValue(data);
        return true;
    }

    @Override
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        sendWriteResponse(characteristic);
        return true;
    }

    public void sendWriteResponse(BluetoothGattCharacteristic characteristic)
    {
        UnitTestUtils.writeSuccess(getBleDevice(), characteristic, getDelayTime());
    }

    @Override public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable)
    {
        return true;
    }

    @Override
    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        sendReadDescriptorResponse(descriptor, descriptor.getValue());
        return true;
    }

    public void sendReadDescriptorResponse(BluetoothGattDescriptor descriptor, byte[] data)
    {
        UnitTestUtils.readDescSuccess(getBleDevice(), descriptor, data, getDelayTime());
    }

    @Override
    public boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data) {
        descriptor.setValue(data);
        return true;
    }

    @Override
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        sendWriteDescResponse(descriptor);
        return true;
    }

    private void sendWriteDescResponse(BluetoothGattDescriptor descriptor)
    {
        UnitTestUtils.writeDescSuccess(getBleDevice(), descriptor, getDelayTime());
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
        }, getDelayTime().millis());
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


    public void setDelayTime(Interval delay)
    {
        m_delayTime = delay;
    }

    public Interval getDelayTime()
    {
        if (Interval.isDisabled(m_delayTime))
        {
            Random r = new Random();
            return Interval.millis(r.nextInt(2999) + 1);
        }
        else
        {
            return m_delayTime;
        }
    }
}
