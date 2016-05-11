package com.idevicesinc.sweetblue;


public class P_Task_LongRunner extends P_Task
{

    private final P_TaskPriority mPriority;

    public P_Task_LongRunner(P_TaskManager mgr, IStateListener listener, P_TaskPriority priority)
    {
        super(mgr, listener, true);
        mPriority = priority;
    }

    @Override public P_TaskPriority getPriority()
    {
        return mPriority;
    }

    @Override public boolean isInterruptible()
    {
        return true;
    }

    @Override public void execute()
    {
    }

    @Override public void update(long curTimeMs)
    {
        if (timeExecuting() >= 30000)
        {
            succeed();
        }
    }
}
