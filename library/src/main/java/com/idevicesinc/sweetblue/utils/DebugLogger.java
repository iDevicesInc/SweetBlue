package com.idevicesinc.sweetblue.utils;

import android.util.Log;
import com.idevicesinc.sweetblue.SweetLogger;
import com.idevicesinc.sweetblue.BleManagerConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Logger class which prints SweetBlue logs to Android's logcat. This also tracks log statements. The internal list holds a specific amount of
 * statements. When that limit is reached, the oldest statement gets dropped from the list.
 * See {@link #DebugLogger()}, {@link #DebugLogger(int)}, and {@link #DebugLogger(int, boolean)}.
 */
public final class DebugLogger implements SweetLogger
{

    public interface LogEvent
    {
        void onLogEntry(String entry);
    }


    public final static int DEFAULT_MAX_SIZE = 50;

    private final int m_maxLogSize;
    private final List<String> m_logList;
    private final boolean m_unitTest;
    private final boolean m_printToLogCat;

    private LogEvent m_log_listener = null;


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
        this(maxLogSize, true);
    }

    /**
     * Constructor which allows you to set a custom max log count size, and whether or not you want the logger to print to log cat.
     * A good case where you <i>wouldn't</i> want to print to log cat, is for production builds. This way you can still get access to
     * SweetBlue logs (so if there's an issue, you can send those logs to us for debugging). Please be aware that in this case, you still
     * need to have {@link BleManagerConfig#loggingEnabled} set to <code>true</code>.
     */
    public DebugLogger(int maxLogSize, boolean printToLogCat)
    {
        this(false, maxLogSize, printToLogCat);
    }

    DebugLogger(boolean unitTest, int maxLogSize)
    {
        this(unitTest, maxLogSize, true);
    }

    DebugLogger(boolean unitTest, int maxLogSize, boolean printToLogCat)
    {
        this.m_maxLogSize = maxLogSize;
        m_logList = Collections.synchronizedList(new ArrayList<String>(maxLogSize));
        this.m_unitTest = unitTest;
        m_printToLogCat = printToLogCat;
    }

    @Override public final void onLogEntry(int level, String tag, String msg)
    {
        String entry = Utils_String.makeString(new Date(), " ", level(level), "/", tag, ": ", msg);

        if (m_printToLogCat)
        {
            if (m_unitTest)
            {
                System.out.print(entry + "\n");
            }
            else
            {
                Log.println(level, tag, msg);
            }
        }
        if (m_logList.size() == m_maxLogSize)
        {
            m_logList.remove(0);
        }
        m_logList.add(entry);

        if (m_log_listener != null) m_log_listener.onLogEntry(entry);
    }

    /**
     * Return a {@link List} with the last count of log statements. If there haven't been any yet, an empty list is returned.
     */
    public final List<String> getLastLogs(int count)
    {
        if (count > m_logList.size())
        {
            count = m_logList.size();
        }
        if (m_logList.size() == 0)
        {
            return new ArrayList<>(0);
        }
        else
        {
            final ArrayDeque<String> list = new ArrayDeque<>(count);
            count--;
            int start = m_logList.size() - 1;
            int end = start - count;
            for (int i = start; i >= end; i--)
            {

                list.push(m_logList.get(i));
            }
            return new ArrayList<>(list);
        }
    }

    /**
     * Returns the last log statement
     */
    public final String getLastLog()
    {
        if (m_logList.size() > 0)
        {
            return m_logList.get(m_logList.size() - 1);
        }
        return "";
    }

    /**
     * Returns a {@link List} of log statements. The default max size is {@link #DEFAULT_MAX_SIZE}, but can also be set
     * by using the {@link #DebugLogger(int)} constructor.
     */
    public final List<String> getLogList()
    {
        return new ArrayList<>(m_logList);
    }

    /**
     * This is a convenience method which calls {@link #getLogList()}, then runs it through {@link Utils_String#prettyFormatLogList(List)}.
     */
    public final String getLogList_prettyString()
    {
        return Utils_String.prettyFormatLogList(getLogList());
    }

    public static String level(int level)
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

    public void setLogListener(LogEvent logListener){
        m_log_listener = logListener;
    }
}
