package com.idevicesinc.sweetblue.utils;

import android.util.Log;
import com.idevicesinc.sweetblue.SweetLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Logger class which prints SweetBlue logs to Android's logcat. This also tracks log statements. The internal list holds a specific amount of
 * statements. When that limit is reached, the oldest statement gets dropped from the list.
 * See {@link #DebugLogger()} and {@link #DebugLogger(int)}.
 */
public final class DebugLogger implements SweetLogger
{

    public final static int DEFAULT_MAX_SIZE = 50;

    private final int maxLogSize;
    private final List<String> logList;


    /**
     * Default constructor which sets the max log size of {@link #DEFAULT_MAX_SIZE}.
     */
    public DebugLogger()
    {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Constructor which allows you to set a custom max log count size.
     */
    public DebugLogger(int maxLogSize)
    {
        this.maxLogSize = maxLogSize;
        logList = Collections.synchronizedList(new ArrayList<String>(maxLogSize));
    }


    @Override public final void onLogEntry(int level, String tag, String msg)
    {
        Log.println(level, tag, msg);
        if (logList.size() == maxLogSize)
        {
            logList.remove(0);
        }
        logList.add(Utils_String.makeString(new Date(), " ", level(level), "/", tag, ": ", msg));
    }

    /**
     * Return a {@link List} with the last @param count of log statements. If there haven't been any yet, and empty list is returned.
     */
    public final List<String> getLastLogs(int count)
    {
        if (count > logList.size())
        {
            count = logList.size();
        }
        if (logList.size() == 0)
        {
            return new ArrayList<>(0);
        }
        else
        {
            final ArrayList<String> list = new ArrayList<>(count);
            count--;
            for (int i = 0; i < count; i++)
            {
                list.add(logList.get(i));
            }
            return list;
        }
    }

    /**
     * Returns the last log statement
     */
    public final String getLastLog()
    {
        if (logList.size() > 0)
        {
            return logList.get(logList.size() - 1);
        }
        return "";
    }

    /**
     * Returns a {@link List} of log statements. The default max size is {@link #DEFAULT_MAX_SIZE}, but can also be set
     * by using the {@link #DebugLogger(int)} constructor.
     */
    public final List<String> getLogList()
    {
        return new ArrayList<>(logList);
    }

    /**
     * This is a convenience method which calls {@link #getLogList()}, then runs it through {@link Utils_String#prettyFormatLogList(List)}.
     */
    public final String getLogList_prettyString()
    {
        return Utils_String.prettyFormatLogList(getLogList());
    }

    private static String level(int level)
    {
        switch (level)
        {
            case 2:
                return "Verbose";
            case 3:
                return "Debug";
            case 4:
                return "Info";
            case 5:
                return "Warn";
            case 6:
                return "Error";
            case 7:
                return "Assert";
            default:
                return "Unknown";
        }
    }

}
