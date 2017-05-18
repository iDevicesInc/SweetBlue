package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.UsesCustomNull;


final class NativeBleGattService implements UsesCustomNull
{

    final BluetoothGattService m_service;
    final BleManager.UhOhListener.UhOh m_uhOh;

    NativeBleGattService()
    {
        this(null, null);
    }

    NativeBleGattService(BleManager.UhOhListener.UhOh uhoh)
    {
        this(null, uhoh);
    }

    NativeBleGattService(BluetoothGattService service)
    {
        this(service, null);
    }

    NativeBleGattService(BluetoothGattService service, BleManager.UhOhListener.UhOh uhoh)
    {
        m_service = service;
        m_uhOh = uhoh;
    }

    public BluetoothGattService getService()
    {
        return m_service;
    }

    public BleManager.UhOhListener.UhOh getUhOh()
    {
        return m_uhOh;
    }

    @Override public boolean isNull()
    {
        return m_service == null;
    }
}
