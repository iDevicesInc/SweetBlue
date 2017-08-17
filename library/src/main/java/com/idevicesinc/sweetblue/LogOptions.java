package com.idevicesinc.sweetblue;

import android.util.Log;

/**
 * Class used to specify the logging options in SweetBlue. There are two types of logs SweetBlue will print. The first are SweetBlue
 * logs that have to do with SweetBlue logic, the other are "native" logs, which print when SweetBlue receives any callbacks from the
 * native stack. Each of these two types can then specify the log level to be printed (Use {@link LogLevel} to pick with ones you would like
 * to see).
 *
 * You can also use {@link #OFF} to shut all logging off, {@link #ON} to turn on most helpful logs, and {@link #ALL_ON} to turn all logging on for convenience (this one can
 * cause performance issues during scanning, as it will print a log every time a device is seen).
 */
public class LogOptions
{

    /**
     * Static instance for conveniently shutting off all logging.
     */
    public static final LogOptions OFF = new LogOptions();

    /**
     * Static instance for turning on most logging. This sets the log level at {@link LogLevel#DEBUG} for both SweetBlue and native logs.
     */
    public static final LogOptions ON = new LogOptions(LogLevel.DEBUG, LogLevel.DEBUG);

    /**
     * Static instance for turning ALL logging on. This will affect performance during scanning, as every time a device is seen, it will print
     * a log entry.
     */
    public static final LogOptions ALL_ON = new LogOptions(LogLevel.VERBOSE, LogLevel.VERBOSE);

    private int m_sweetBlueLevel = 0;
    private int m_nativeLevel = 0;



    public LogOptions()
    {
    }

    public LogOptions(LogLevel sweetBlueLevel, LogLevel nativeLevel)
    {
        enableSweetBlueLogs(sweetBlueLevel).enableNativeLogs(nativeLevel);
    }


    /**
     * Enable SweetBlue specific logs with the given {@link LogLevel}
     */
    public final LogOptions enableSweetBlueLogs(LogLevel level)
    {
        m_sweetBlueLevel = level.m_nativeBit;
        return this;
    }

    /**
     * Enable native callback logs with the given {@link LogLevel}
     */
    public final LogOptions enableNativeLogs(LogLevel level)
    {
        m_nativeLevel = level.m_nativeBit;
        return this;
    }



    final boolean enabled()
    {
        return sweetBlueEnabled() || nativeEnabled();
    }

    final boolean sweetBlueEnabled()
    {
        return m_sweetBlueLevel != 0;
    }

    final boolean nativeEnabled()
    {
        return m_nativeLevel != 0;
    }

    final boolean nativeEnabled(int logLevel)
    {
        return logLevel >= m_nativeLevel;
    }

    final boolean sweetBlueEnabled(int logLevel)
    {
        return logLevel >= m_sweetBlueLevel;
    }


    /**
     * Enumeration for setting the log level of SweetBlue's logger.
     */
    public enum LogLevel
    {

        /**
         * Print all Verbose logs, and all log levels above this one.
         */
        VERBOSE(Log.VERBOSE),

        /**
         * Print all Debug logs, and all log levels above this one.
         */
        DEBUG(Log.DEBUG),

        /**
         * Print all Info logs, and all log levels above this one.
         */
        INFO(Log.INFO),

        /**
         * Print all Warning logs, and all log levels above this one.
         */
        WARN(Log.WARN),

        /**
         * Print only Error logs.
         */
        ERROR(Log.ERROR);


        private final int m_nativeBit;
        private static LogLevel[] VALUES;


        LogLevel(int nativeBit)
        {
            m_nativeBit = nativeBit;
        }

        public int nativeBit()
        {
            return m_nativeBit;
        }

        public static LogLevel fromNative(int nativeBit)
        {
            for (LogLevel l : VALUES())
            {
                if (l.nativeBit() == nativeBit)
                {
                    return l;
                }
            }
            return VERBOSE;
        }

        public static LogLevel[] VALUES()
        {
            if (VALUES == null)
                VALUES = values();
            return VALUES;
        }

    }




}
