package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BleStatuses;

public class P_Task_Disconnect extends P_Task_RequiresConnection
{

    private final P_TaskPriority mPriority;


    public P_Task_Disconnect(BleDevice device, IStateListener listener)
    {
        this(device, listener, null);
    }

    public P_Task_Disconnect(BleDevice device, IStateListener listener, P_TaskPriority priority)
    {
        super(device, listener);
        mPriority = priority == null ? P_TaskPriority.MEDIUM : priority;
    }

    @Override public P_TaskPriority getPriority()
    {
        return mPriority;
    }

    @Override public void execute()
    {
        if (!getDevice().is(BleDeviceState.CONNECTED))
        {
            redundant();
            return;
        }

        if (getDevice().getNativeGatt() == null)
        {
            getManager().getLogger().e("Already disconnected and gatt==null");
            redundant();
            return;
        }
        if (getDevice().is(BleDeviceState.DISCONNECTING))
        {
            return;
        }
        getDevice().stateTracker().update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.DISCONNECTING, true);
        getDevice().mGattManager.disconnect();
    }
}
