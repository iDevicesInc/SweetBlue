package com.idevicesinc.sweetblue.impl;


import android.util.Log;

import com.idevicesinc.sweetblue.SweetLogger;

public final class DefaultLogger implements SweetLogger
{
    @Override public void onLogEntry(int level, String tag, String msg)
    {
        Log.println(level, tag, msg);
    }
}
