package com.idevicesinc.sweetblue;


public class BleDeviceConfig extends BleNodeConfig implements Cloneable
{

    public boolean cacheDeviceOnUndiscovery                     = true;

    @Override protected BleDeviceConfig clone()
    {
        return (BleDeviceConfig) super.clone();
    }
}
