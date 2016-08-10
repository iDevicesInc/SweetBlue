package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;

public final class P_Task_TurnBleOff extends P_Task
{

    public P_Task_TurnBleOff(P_TaskManager mgr)
    {
        super(mgr, null, true);
    }

    @Override public final P_TaskPriority getPriority()
    {
        return P_TaskPriority.CRITICAL;
    }

    @Override public final void execute()
    {
        int curNativeState = getManager().getNativeAdapter().getState();
        if (curNativeState == BluetoothAdapter.STATE_OFF)
        {
            redundant();
        }
        else if (curNativeState == BluetoothAdapter.STATE_TURNING_OFF)
        {
            // Do nothing, as we're already turning off
        }
        else
        {
            if (!getManager().getNativeAdapter().disable())
            {
                fail();
            }
            else
            {
                // On our way to succeeding, the P_BleReceiverManager will succeed this task when it's received the broadcast
            }
        }
    }
}
