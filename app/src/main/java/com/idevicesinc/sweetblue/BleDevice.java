package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.annotations.Nullable;

public class BleDevice extends BleNode
{

    private final boolean mIsNull;
    private final BluetoothDevice mNativeDevice;


    BleDevice(BluetoothDevice nativeDevice, boolean isNull)
    {
        mNativeDevice = nativeDevice;
        mIsNull = isNull;
    }

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

    public boolean isNull()
    {
        return mIsNull;
    }

    public boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final BleDevice device_nullable)
    {
        if (device_nullable == null)												return false;
        if (device_nullable == this)												return true;
        if (device_nullable.getNative() == null || this.getNative() == null)		return false;
        if( this.isNull() && device_nullable.isNull() )								return true;

        return device_nullable.getNative().equals(this.getNative());
    }
}
