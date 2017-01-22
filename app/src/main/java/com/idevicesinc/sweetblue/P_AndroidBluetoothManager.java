package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;

public final class P_AndroidBluetoothManager implements P_NativeManagerLayer
{

    private BluetoothManager m_manager;
    private BluetoothAdapter m_adaptor;


    @Override
    public int getConnectionState(P_NativeDeviceLayer device)
    {
        if (m_manager != null)
        {
            return m_manager.getConnectionState(device.getNativeDevice(), BluetoothGatt.GATT_SERVER);
        }
        return 0;
    }

    @Override
    public boolean startDiscovery()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.startDiscovery();
        }
        return false;
    }

    @Override
    public boolean cancelDiscovery()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.cancelDiscovery();
        }
        return false;
    }

    @Override
    public void setNativeManager(BluetoothManager manager) {
        m_manager = manager;
    }

    @Override
    public void setNativeAdaptor(BluetoothAdapter adaptor) {
        m_adaptor = adaptor;
    }

    @Override
    public BluetoothAdapter getNativeAdaptor() {
        return m_adaptor;
    }

    @Override
    public BluetoothManager getNativeManager() {
        return m_manager;
    }
}
