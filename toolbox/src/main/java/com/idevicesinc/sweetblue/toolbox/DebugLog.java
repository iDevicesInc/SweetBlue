package com.idevicesinc.sweetblue.toolbox;

import com.idevicesinc.sweetblue.utils.DebugLogger;

public class DebugLog
{
    private static DebugLogger debugger = new DebugLogger();

    public static DebugLogger getDebugger() {
        return debugger;
    }
}
