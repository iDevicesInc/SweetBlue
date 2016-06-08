package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.listeners.EnablerDoneListener;

public abstract class BluetoothEnablerController
{
    EnablerDoneListener listener;

    public BluetoothEnablerController()
    {

    }

    public abstract BluetoothEnabler.Please onEvent(BluetoothEnabler.BluetoothEnablerStateEvent event);

}
