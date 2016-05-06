package com.idevicesinc.sweetblue.listeners;


import com.idevicesinc.sweetblue.BleManager;


public interface NativeStateListener
{

    class NativeStateEvent extends ManagerStateListener.StateEvent
    {

        protected NativeStateEvent(BleManager manager, int oldStateBits, int newStateBits, int intentMask)
        {
            super(manager, oldStateBits, newStateBits, intentMask);
        }
    }

    void onEvent(NativeStateEvent e);
}
