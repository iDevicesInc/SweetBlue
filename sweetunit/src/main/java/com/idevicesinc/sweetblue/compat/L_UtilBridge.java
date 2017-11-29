package com.idevicesinc.sweetblue.compat;


public class L_UtilBridge
{

    public static void setAdvListener(L_Util.AdvertisingCallback listener)
    {
        L_Util.setAdvCallback(listener);
    }

}
