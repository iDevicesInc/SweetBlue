package com.idevicesinc.sweetblue.listeners;


import com.idevicesinc.sweetblue.BleManager;

public class P_EventFactory
{

    public static NativeStateListener.NativeStateEvent newNativeStateEvent(BleManager mgr, int oldStateBits, int newStateBits, int status)
    {
        return new NativeStateListener.NativeStateEvent(mgr, oldStateBits, newStateBits, status);
    }
}
