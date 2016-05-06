package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BitwiseEnum;
import com.idevicesinc.sweetblue.utils.State;

public enum BleDeviceState implements State
{
    CONNECTING,
    CONNECTED,
    DISCONNECTED
    ;

    @Override public boolean didEnter(int oldStateBits, int newStateBits)
    {
        return false;
    }

    @Override public boolean didExit(int oldStateBits, int newStateBits)
    {
        return false;
    }

    @Override public boolean isNull()
    {
        return false;
    }

    @Override public int or(BitwiseEnum state)
    {
        return 0;
    }

    @Override public int or(int bits)
    {
        return 0;
    }

    @Override public int bit()
    {
        return 0;
    }

    @Override public boolean overlaps(int mask)
    {
        return false;
    }
}
