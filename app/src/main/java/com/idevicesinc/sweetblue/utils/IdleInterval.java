package com.idevicesinc.sweetblue.utils;

public enum IdleInterval
{
    TWO_HUNDRED_FIFTY_MILLI_SEC(250),
    FIVE_HUNDRED_MILLI_SEC(500),
    SEVEN_HUNDRED_FIFTY_MILLI_SEC(750),
    ONE_SEC(1000);

    private int idleWaitTime;

    IdleInterval(int milli)
    {
        idleWaitTime = milli;
    }

    int getTime()
    {
        return idleWaitTime;
    }
}
