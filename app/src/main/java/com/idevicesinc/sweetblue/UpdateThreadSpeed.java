package com.idevicesinc.sweetblue;

public enum UpdateThreadSpeed
{
    SIXTEEN_MS(16),
    TWENTY_FIVE_MS(25),
    FIFTY_MS(50),
    SEVENTY_FIVE_MS(75),
    ONE_HUNDRED_MS(100),
    ONE_HUNDRED_TWENTY_FIVE_MS(125),
    ONE_HUNDRED_FIFTY_MS(150),
    ONE_HUNDRED_SEVENTY_FIVE_MS(175),
    TWO_HUNDRED_MS(200),
    TWO_HUNDRED_TWENTY_FIVE_MS(225),
    TWO_HUNDRED_FIFTY_MS(250);

    private int milliseconds;

    UpdateThreadSpeed(int milli)
    {
        milliseconds = milli;
    }

    int getMilliseconds()
    {
        return milliseconds;
    }

}
