package com.idevicesinc.sweetblue;


abstract class P_Task_RequiresBleOn extends P_Task
{

    public P_Task_RequiresBleOn(BleServer server, IStateListener listener)
    {
        super(server, listener);
    }

    public P_Task_RequiresBleOn(P_TaskManager manager, IStateListener listener)
    {
        super(manager, listener, true);
    }

    public P_Task_RequiresBleOn(BleDevice device, IStateListener listener)
    {
        super(device, listener);
    }

    @Override public boolean isExecutable()
    {
        return BleManagerState.ON.overlaps(getManager().getNativeStateMask());
    }
}
