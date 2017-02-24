package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.Random;


public final class P_UnitUtils
{

    private P_UnitUtils() {}


    public static String randomMacAddress()
    {
        byte[] add = new byte[6];
        new Random().nextBytes(add);
        return Utils_String.bytesToMacAddress(add);
    }

    public static byte[] newScanRecord(String name)
    {
        byte[] nameBytes = name.getBytes();

        byte[] record = new byte[nameBytes.length + 2];

        record[0] = 0x09; // FULL Name type

        for (int i = 0; i < record.length; i++)
        {
            if (i == 0)
            {
                record[i] = (byte) (nameBytes.length + 1);
            }
            else if (i == 1)
            {
                record[i] = 0x09;
            }
            else
            {
                record[i] = nameBytes[i - 2];
            }
        }

        return record;
    }

}
