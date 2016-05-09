package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

public class BleDevice extends BleNode
{


    public String getName_debug()
    {
        // TODO
        // TODO
        // TODO
        // TODO Actually implement this
        return "";
    }

    BleManager getManager()
    {
        // TODO
        // TODO
        // TODO
        // TODO Implement differently -- return stored instance of the manager
        return BleManager.get();
    }

    public BluetoothDevice getNative()
    {
        // TODO
        // TODO
        // TODO
        // TODO Actually implement this
        return null;
    }

    public BluetoothGatt getNativeGatt()
    {
        // TODO
        // TODO
        // TODO
        // TODO Actually implement this
        return null;
    }

    @Override public String getMacAddress()
    {
        // TODO
        // TODO
        // TODO
        // TODO Implement this so it actually returns the mac address
        return "";
    }
}
