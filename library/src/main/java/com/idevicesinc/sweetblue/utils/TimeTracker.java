package com.idevicesinc.sweetblue.utils;


import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class TimeTracker
{

    private TimeTracker() {}

    private static TimeTracker s_instance = new TimeTracker();

    public static TimeTracker get()
    {
        return s_instance;
    }

    private long m_start;
    private final Map<String, Long> m_timeMap = new TreeMap<>();


    public final void start()
    {
        m_start = System.currentTimeMillis();
    }

    public final long stop(String methodName)
    {
        final long diff = System.currentTimeMillis() - m_start;
        m_timeMap.put(methodName, diff);
        return diff;
    }

    public final void printLog()
    {
        StringBuilder b = new StringBuilder();
        b.append("Times:\n\n");
        for (String method : m_timeMap.keySet())
        {
            b.append(method).append(": ");
            b.append(m_timeMap.get(method)).append("ms\n");
        }
        Log.e("TimeTracker", b.toString());
        m_timeMap.clear();
    }

}
