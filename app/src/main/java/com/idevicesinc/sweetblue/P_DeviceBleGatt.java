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

import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;

import java.util.List;
import java.util.UUID;

final class P_DeviceBleGatt
{

    private final BleDevice m_device;
    private final P_GattLayer m_gattLayer;


    P_DeviceBleGatt(BleDevice device, P_GattLayer gattLayer)
    {
        m_device = device;
        m_gattLayer = gattLayer;
    }

    public String getAddress()
    {
        return m_gattLayer.getAddress(m_device.getNative());
    }

    public int getBondState()
    {
        return m_gattLayer.getBondState(m_device.getNative());
    }

    public int getNativeConnectionState()
    {
        return m_gattLayer.getNativeConnectionState(m_device.getNative(), m_device.getManager().getNative());
    }

    public List<BluetoothGattService> getNativeServiceList()
    {
        return m_gattLayer.getNativeServiceList(m_device.getNativeGatt(), m_device.logger());
    }

    public BluetoothGattService getService(UUID serviceUuid)
    {
        return m_gattLayer.getService(m_device.getNativeGatt(), serviceUuid, m_device.logger());
    }

    public boolean isGattNull()
    {
        return m_gattLayer.isGattNull(m_device.getNativeGatt());
    }

    public BluetoothGatt connect(boolean useAutoConnect)
    {
        return m_gattLayer.connect(m_device.getNative(), m_device.getManager().getApplicationContext(), useAutoConnect, m_device.getListeners());
    }

    public void disconnect()
    {
        m_gattLayer.disconnect(m_device.getNativeGatt());
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        return m_gattLayer.readCharacteristic(m_device.getNativeGatt(), characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        return m_gattLayer.writeCharacteristic(m_device.getNativeGatt(), characteristic);
    }

    public boolean readDescriptor(BluetoothGattDescriptor descriptor)
    {
        return m_gattLayer.readDescriptor(m_device.getNativeGatt(), descriptor);
    }

    public boolean writeDescriptor(BluetoothGattDescriptor descriptor)
    {
        return m_gattLayer.writeDescriptor(m_device.getNativeGatt(), descriptor);
    }

    public boolean createBond()
    {
        return m_gattLayer.createBond(m_device);
    }

    public boolean createBondSneaky(String methodName)
    {
        return m_gattLayer.createBondSneaky(m_device.getNative(), methodName, m_device.getManager().m_config.loggingEnabled);
    }

    public boolean startDiscovery()
    {
        return m_gattLayer.startDiscovery(m_device.getManager().getNativeAdapter());
    }

    public boolean cancelDiscovery()
    {
        return m_gattLayer.cancelDiscovery(m_device.getManager().getNativeAdapter());
    }

    public boolean refreshGatt()
    {
        return m_gattLayer.refreshGatt(m_device);
    }

    public boolean discoverServices()
    {
        return m_gattLayer.discoverServices(m_device.getNativeGatt());
    }

    public boolean executeReliableWrite()
    {
        return m_gattLayer.executeReliableWrite(m_device.getNativeGatt());
    }

    public boolean readRemoteRssi()
    {
        return m_gattLayer.readRemoteRssi(m_device.getNativeGatt());
    }

    public boolean requestConnectionPriority(BleConnectionPriority priority)
    {
        return m_gattLayer.requestConnectionPriority(m_device, priority);
    }

    public boolean requestMtu(int mtu)
    {
        return m_gattLayer.requestMtu(m_device, mtu);
    }

    public boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data)
    {
        return m_gattLayer.setCharValue(characteristic, data);
    }

    public boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data)
    {
        return m_gattLayer.setDescValue(descriptor, data);
    }



    public P_GattLayer getGattLayer()
    {
        return m_gattLayer;
    }



}
