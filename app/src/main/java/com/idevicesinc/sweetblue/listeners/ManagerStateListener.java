package com.idevicesinc.sweetblue.listeners;


import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_String;


public interface ManagerStateListener extends P_BaseStateListener<ManagerStateListener.StateEvent>
{
    public static class StateEvent extends State.ChangeEvent<BleManagerState>
    {

        public BleManager manager()
        {
            return mManager;
        }
        private final BleManager mManager;

        protected StateEvent(BleManager manager, int oldStateBits, int newStateBits, int intentMask)
        {
            super(oldStateBits, newStateBits, intentMask);
            mManager = manager;
        }

        @Override public String toString()
        {
            return Utils_String.toString(
                    getClass(),
                    "entered",      Utils_String.toString(enterMask(), BleManagerState.VALUES()),
                    "exited",       Utils_String.toString(exitMask(), BleManagerState.VALUES()),
                    "current",      Utils_String.toString(newStateBits(), BleManagerState.VALUES()));
        }
    }
}
