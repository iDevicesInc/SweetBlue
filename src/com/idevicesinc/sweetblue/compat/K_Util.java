package com.idevicesinc.sweetblue.compat;


import com.idevicesinc.sweetblue.BleDevice;

public class K_Util
{

    private K_Util() {}


    public static void abortReliableWrite(BleDevice device) {
        device.getNativeGatt().abortReliableWrite();
    }

}
