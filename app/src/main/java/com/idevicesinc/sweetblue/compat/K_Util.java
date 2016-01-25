package com.idevicesinc.sweetblue.compat;


import android.annotation.TargetApi;
import android.os.Build;
import com.idevicesinc.sweetblue.BleDevice;


@TargetApi(Build.VERSION_CODES.KITKAT)
public class K_Util
{

    private K_Util() {}


    public static void abortReliableWrite(BleDevice device) {
        device.getNativeGatt().abortReliableWrite();
    }

    public static boolean createBond(BleDevice device) {
        return device.getNative().createBond();
    }

}
