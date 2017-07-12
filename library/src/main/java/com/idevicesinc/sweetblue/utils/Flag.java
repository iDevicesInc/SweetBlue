package com.idevicesinc.sweetblue.utils;


public interface Flag
{

    /**
     * Returns the bit (0x1, 0x2, 0x4, etc.) this enum represents based on the {@link #ordinal()}.
     */
    int bit();

    /**
     * Same as {@link Enum#ordinal()}.
     */
    int ordinal();

}
