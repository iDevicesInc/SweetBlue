package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;

public class P_Task_TurnBleOn extends P_Task
{

    P_Task_TurnBleOn(P_TaskManager mgr)
    {
        super(mgr, null, true);
    }

    @Override public P_TaskPriority getPriority()
    {
        return P_TaskPriority.CRITICAL;
    }

    @Override public void execute()
    {
        int curNativeState = getManager().getNativeAdapter().getState();
        if (curNativeState == BluetoothAdapter.STATE_ON)
        {
            redundant();
        }
        else if (curNativeState == BluetoothAdapter.STATE_TURNING_ON)
        {
            // Do nothing as we're in the process of turning on
        }
        else
        {
            if (!getManager().getNativeAdapter().enable())
            {
                fail();
            }
            else
            {
                // On our way to succeeding, P_ManagerListenersManager will mark this task as succeeded when it's detected that BLE is on
            }
        }
    }
}
