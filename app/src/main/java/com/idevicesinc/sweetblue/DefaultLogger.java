package com.idevicesinc.sweetblue;


import android.util.Log;

public class DefaultLogger implements SweetLogger
{
    @Override public void onLogEntry(int level, String tag, String msg)
    {
        Log.println(level, tag, msg);
    }
}
