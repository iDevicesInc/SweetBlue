package com.idevicesinc.sweetblue;


import java.util.Random;


public class P_Task_Looper extends P_Task
{

    private final P_TaskPriority mPriority;
    private Integer mLoopCount;


    public P_Task_Looper(P_TaskManager mgr, IStateListener listener, P_TaskPriority priority)
    {
        super(mgr, listener);
        mPriority = priority;
    }

    public P_Task_Looper(P_TaskManager mgr, IStateListener listener, P_TaskPriority priority, int loopCount)
    {
        this(mgr, listener, priority);
        mLoopCount = loopCount;
    }

    @Override public P_TaskPriority getPriority()
    {
        return mPriority;
    }

    @Override public void execute()
    {
        int loops = mLoopCount == null ? new Random().nextInt(250) + 1 : mLoopCount;
        for (int i = 0; i < loops; i++)
        {
            System.currentTimeMillis();
        }
    }
}
