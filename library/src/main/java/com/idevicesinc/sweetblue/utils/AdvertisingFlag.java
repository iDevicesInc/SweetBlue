package com.idevicesinc.sweetblue.utils;


public enum AdvertisingFlag
{

    Limited_Discoverable_Mode(0x0),
    General_Discoverable_Mode(1 << 1),
    BR_EDR_Not_Supported(1 << 2),
    LE_And_EDR_Supported_Controller(1 << 3),
    LE_And_EDR_Supported_Host(1 << 4),
    Unknown(1 << 5);


    private final int bit;

    AdvertisingFlag(int bit)
    {
        this.bit = bit;
    }

    public byte getBit()
    {
        return (byte) bit;
    }

    public boolean overlaps(int mask)
    {
        return (mask & bit) != 0;
    }

    public static AdvertisingFlag fromBit(int bit)
    {
        for (AdvertisingFlag f : values())
        {
            if (f.bit == bit)
            {
                return f;
            }
        }
        return Unknown;
    }

}
