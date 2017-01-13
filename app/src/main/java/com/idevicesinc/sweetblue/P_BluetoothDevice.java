package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

final class P_BluetoothDevice
{

    private BluetoothDevice m_device;
    private final P_GattLayer m_gattLayer;



    P_BluetoothDevice(BluetoothDevice device, P_GattLayer gattLayer)
    {
        m_device = device;
        m_gattLayer = gattLayer;
    }



    String getAddress()
    {
        return m_gattLayer.getAddress(m_device);
    }

    int getBondState()
    {
        return m_gattLayer.getBondState(m_device);
    }

    int getConnectionState(BluetoothManager manager)
    {
        return m_gattLayer.getNativeConnectionState(m_device, manager);
    }

    BluetoothDevice getDevice()
    {
        return m_device;
    }

    void updateDevice(BluetoothDevice device)
    {
        m_device = device;
    }

}
