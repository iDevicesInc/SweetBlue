package com.idevicesinc.sweetblue;


import android.os.Looper;

public abstract class UnitTestRunnable
{
    abstract BleManager setup(Looper looper);

    public boolean update()
    {
        return true;
    }
}
