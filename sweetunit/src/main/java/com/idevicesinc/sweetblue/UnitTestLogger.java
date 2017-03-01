package com.idevicesinc.sweetblue;


import android.util.Log;


public class UnitTestLogger implements SweetLogger
{
    @Override public void onLogEntry(int level, String tag, String msg)
    {
        StringBuilder b = new StringBuilder();
        switch (level)
        {
            case Log.ASSERT:
                b.append("A");
                break;
            case Log.DEBUG:
                b.append("D");
                break;
            case Log.ERROR:
                b.append("E");
                break;
            case Log.VERBOSE:
                b.append("V");
                break;
            case Log.WARN:
                b.append("W");
                break;
            default:
                b.append("I");
                break;
        }
        b.append("/").append(tag).append(" : ").append(msg);
        System.out.println(b.toString());
    }
}
