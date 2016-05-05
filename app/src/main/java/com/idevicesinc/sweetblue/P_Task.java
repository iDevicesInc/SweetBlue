package com.idevicesinc.sweetblue;


public abstract class P_Task
{

    public final static P_Task NULL = new P_Task(null)
    {
        @Override public P_TaskPriority getPriority()
        {
            return P_TaskPriority.TRIVIAL;
        }

        @Override public void execute()
        {
        }

        @Override public boolean isInterruptible()
        {
            return true;
        }

        @Override public boolean isExecutable()
        {
            return false;
        }
    };

    private final P_TaskManager mTaskMgr;
    private boolean mExecuting = false;


    public P_Task(P_TaskManager mgr)
    {
        mTaskMgr = mgr;
    }

    public abstract P_TaskPriority getPriority();
    public abstract void execute();
    public abstract boolean isInterruptible();


    public boolean isExecutable()
    {
        return true;
    }

    void executeTask()
    {
        mExecuting = true;
        execute();
    }

    public boolean hasHigherPriority(P_Task task)
    {
        return getPriority().ordinal() > task.getPriority().ordinal();
    }

    public boolean interrupt()
    {
        if (isInterruptible() && mExecuting)
        {
            mExecuting = false;
            mTaskMgr.addInterruptedTask(this);
            return true;
        }
        return false;
    }

    public void succeed()
    {
        mTaskMgr.succeedTask(this);
    }

    public void fail()
    {
        mTaskMgr.failTask(this);
    }

    public boolean isNull()
    {
        return this == NULL;
    }
}
