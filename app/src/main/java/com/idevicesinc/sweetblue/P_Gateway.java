package com.idevicesinc.sweetblue;


import java.util.UUID;

public class P_Gateway
{

    public static String gattUnbondReason(BleManager manager, int code)
    {
        return manager.getLogger().gattUnbondReason(code);
    }

    public static String gattStatus(BleManager manager, int code)
    {
        return manager.getLogger().gattStatus(code);
    }

    public static String uuidName(BleManager manager, UUID uuid)
    {
        return manager.getLogger().uuidName(uuid);
    }

}
