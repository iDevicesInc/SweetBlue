package com.idevicesinc.sweetblue;


public class P_Task_Connect extends P_Task_RequiresBleOn
{

    private final P_TaskPriority mPriority;
    private final boolean mExplicit;


    public P_Task_Connect(BleDevice device, IStateListener listener)
    {
        this(device, listener, true, null);
    }

    public P_Task_Connect(BleDevice device, IStateListener listener, boolean explicit, P_TaskPriority priority)
    {
        super(device, listener);

        mPriority = priority == null ? P_TaskPriority.MEDIUM : priority;
        mExplicit = explicit;
    }

    @Override public P_TaskPriority getPriority()
    {
        return mPriority;
    }

    @Override public void execute()
    {
        getDevice().doNativeConnect();
    }
}
