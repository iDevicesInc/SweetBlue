package com.idevicesinc.sweetblue.utils;


import java.util.Random;


/**
 * Utility class to handle various things when unit testing, such as generating random mac addresses, random byte arrays. For generating a scan record, you
 * should use the {@link BleScanInfo} class.
 */
public final class Util
{

    private Util() {}


    /**
     * Returns a random mac address
     */
    public static String randomMacAddress()
    {
        byte[] add = new byte[6];
        new Random().nextBytes(add);
        return Utils_String.bytesToMacAddress(add);
    }

    /**
     * Returns a random byte array of the given size
     */
    public static byte[] randomBytes(int size)
    {
        byte[] bytes = new byte[size];
        new Random().nextBytes(bytes);
        return bytes;
    }


}
