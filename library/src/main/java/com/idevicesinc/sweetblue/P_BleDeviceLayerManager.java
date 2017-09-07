package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;


final class P_BleDeviceLayerManager
{

    private final BleDevice m_device;
    private final P_GattLayer m_gattLayer;
    private final P_NativeDeviceLayer m_deviceLayer;
    private final P_NativeManagerLayer m_managerLayer;


    P_BleDeviceLayerManager(BleDevice device, P_GattLayer gattLayer, P_NativeDeviceLayer deviceLayer, P_NativeManagerLayer managerLayer)
    {
        m_device = device;
        m_gattLayer = gattLayer;
        m_deviceLayer = deviceLayer;
        m_managerLayer = managerLayer;
    }


    public final String getAddress()
    {
        return m_deviceLayer.getAddress();
    }

    public final int getBondState()
    {
        return m_deviceLayer.getBondState();
    }

    public final int getNativeConnectionState()
    {
        return m_managerLayer.getConnectionState(m_deviceLayer, BluetoothGatt.GATT_SERVER);
    }

    public final List<BluetoothGattService> getNativeServiceList()
    {
        return m_gattLayer.getNativeServiceList(m_device.logger());
    }

    public final BleServiceWrapper getService(UUID serviceUuid)
    {
        return m_gattLayer.getBleService(serviceUuid, m_device.logger());
    }

    public final boolean isGattNull()
    {
        return m_gattLayer.isGattNull();
    }

    public final BluetoothGatt connect(boolean useAutoConnect)
    {
        return m_gattLayer.connect(m_deviceLayer, m_device.getManager().getApplicationContext(), useAutoConnect, m_device.getListeners());
    }

    public final void disconnect()
    {
        m_gattLayer.disconnect();
    }

    public final boolean readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        return m_gattLayer.readCharacteristic(characteristic);
    }

    public final boolean writeCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        return m_gattLayer.writeCharacteristic(characteristic);
    }

    public final boolean readDescriptor(BluetoothGattDescriptor descriptor)
    {
        return m_gattLayer.readDescriptor(descriptor);
    }

    public final boolean writeDescriptor(BluetoothGattDescriptor descriptor)
    {
        return m_gattLayer.writeDescriptor(descriptor);
    }

    public final boolean createBond()
    {
        return m_deviceLayer.createBond();
    }

    public final boolean createBondSneaky(String methodName)
    {
        return m_deviceLayer.createBondSneaky(methodName, m_device.getManager().m_config.loggingEnabled);
    }

    public final boolean startDiscovery()
    {
        return m_managerLayer.startDiscovery();
    }

    public final boolean cancelDiscovery()
    {
        return m_managerLayer.cancelDiscovery();
    }

    public final boolean refreshGatt()
    {
        return m_gattLayer.refreshGatt();
    }

    public final boolean discoverServices()
    {
        return m_gattLayer.discoverServices();
    }

    public final boolean executeReliableWrite()
    {
        return m_gattLayer.executeReliableWrite();
    }

    public final boolean readRemoteRssi()
    {
        return m_gattLayer.readRemoteRssi();
    }

    public final boolean requestConnectionPriority(BleConnectionPriority priority)
    {
        return m_gattLayer.requestConnectionPriority(priority);
    }

    public final boolean requestMtu(int mtu)
    {
        return m_gattLayer.requestMtu(mtu);
    }

    public final boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data)
    {
        return m_gattLayer.setCharValue(characteristic, data);
    }

    public final boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data)
    {
        return m_gattLayer.setDescValue(descriptor, data);
    }

    public final boolean gattEquals(BluetoothGatt gatt)
    {
        return m_gattLayer.equals(gatt);
    }

    public final P_GattLayer getGattLayer()
    {
        return m_gattLayer;
    }

    public final P_NativeDeviceLayer getDeviceLayer()
    {
        return m_deviceLayer;
    }

    public final P_NativeManagerLayer getManagerLayer()
    {
        return m_managerLayer;
    }

}
