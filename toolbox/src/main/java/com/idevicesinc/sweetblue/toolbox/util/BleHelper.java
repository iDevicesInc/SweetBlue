package com.idevicesinc.sweetblue.toolbox.util;


import com.idevicesinc.sweetblue.BleDevice;


public class BleHelper
{

    private static final BleHelper ourInstance = new BleHelper();

    public static BleHelper get()
    {
        return ourInstance;
    }


    private BleDevice m_device;


    private BleHelper()
    {
    }

    public void setDevice(BleDevice device)
    {
        m_device = device;
    }

    public BleDevice getDevice()
    {
        return m_device;
    }
}
