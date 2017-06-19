package com.idevicesinc.sweetblue;


import android.util.Log;

public final class DefaultLogger implements SweetLogger
{
    @Override public void onLogEntry(int level, String tag, String msg)
    {
        Log.println(level, tag, msg);
    }
}
