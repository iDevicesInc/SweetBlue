package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import java.util.List;
import java.util.UUID;


public final class UnitTestServerLayer implements P_NativeServerLayer
{


    @Override
    public boolean isServerNull()
    {
        return false;
    }

    @Override
    public boolean addService(BluetoothGattService service)
    {
        return true;
    }

    @Override
    public void cancelConnection(BluetoothDevice device)
    {

    }

    @Override
    public void clearServices()
    {

    }

    @Override
    public void close()
    {

    }

    @Override
    public boolean connect(BluetoothDevice device, boolean autoConnect)
    {
        return true;
    }

    @Override
    public BluetoothGattService getService(UUID uuid)
    {
        return null;
    }

    @Override
    public List<BluetoothGattService> getServices()
    {
        return null;
    }

    @Override
    public boolean notifyCharacteristicChanged(BluetoothDevice device, BluetoothGattCharacteristic characteristic, boolean confirm)
    {
        return true;
    }

    @Override
    public boolean removeService(BluetoothGattService service)
    {
        return true;
    }

    @Override
    public boolean sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value)
    {
        return true;
    }

    @Override
    public BluetoothGattServer getNativeServer()
    {
        return null;
    }
}
