package com.idevicesinc.sweetblue;

public abstract class BluetoothEnablerController
{
    public BluetoothEnablerController()
    {

    }

    public abstract BluetoothEnabler.Please onEvent(BluetoothEnabler.BluetoothEnablerStateEvent event);

}
