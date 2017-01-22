package com.idevicesinc.sweetblue.compat;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.P_SweetHandler;


@TargetApi(Build.VERSION_CODES.KITKAT)
public class K_Util
{

    private K_Util() {}


    public static void abortReliableWrite(BleDevice device)
    {
        device.getNativeGatt().abortReliableWrite();
    }

    // TODO - Remove for version 3.0
    @Deprecated
    public static boolean createBond(BleDevice device)
    {
        return createBond(device.getNative());
    }

    public static boolean createBond(BluetoothDevice device)
    {
        return device.createBond();
    }

}
