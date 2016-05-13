package com.idevicesinc.sweetblue;


public class BleServer extends BleNode
{

    public BleServer(BleManager mgr)
    {
        super(mgr);
    }

    @Override public String getMacAddress()
    {
        return null;
    }

    @Override public <T extends BleNodeConfig> T getConfig()
    {
        return null;
    }

    @Override P_ServiceManager newServiceManager()
    {
        return null;
    }

    @Override public boolean isNull()
    {
        return false;
    }
}
