package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_String;


abstract class P_Task
{

    private final P_TaskManager mTaskMgr;
    private P_TaskState mState;
    private BleDevice mDevice;
    private BleServer mServer;
    private IStateListener mStateListener;
    private long mLastUpdate;
    private long mTimeExecuting;
    private long mTimeCreated;


    public P_Task(BleServer server, IStateListener listener)
    {
        this(server.getManager().mTaskManager, listener, true);
        mServer = server;
    }

    public P_Task(BleDevice device, IStateListener listener)
    {
        this(device.getManager().mTaskManager, listener, true);
        mDevice = device;
    }

    public P_Task(P_TaskManager mgr, IStateListener listener, boolean getCreateTime)
    {
        mTaskMgr = mgr;
        if (listener == null && this instanceof IStateListener)
        {
            mStateListener = (IStateListener) this;
        }
        else
        {
            mStateListener = listener;
        }
        setState(P_TaskState.CREATED);
        if (getCreateTime)
        {
            mTimeCreated = System.currentTimeMillis();
        }
    }

    P_Task(P_TaskManager mgr, long timeCreated, IStateListener listener)
    {
        this(mgr, listener, false);
        mTimeCreated = timeCreated;
    }

    private P_Task()
    {
        mTaskMgr = null;
    }


    public abstract P_TaskPriority getPriority();

    public abstract void execute();

    public void update(long curTimeMs)
    {}

    public boolean isInterruptible()
    {
        return false;
    }

    public boolean isExecutable()
    {
        return true;
    }

    void executeTask()
    {
        setState(P_TaskState.EXECUTING);
        execute();
    }

    void addedToQueue()
    {
        // If this task was interrupted, then we put it in the requeued state, to show that the task was
        // running at some point, and is now waiting to execute again. This is just for informational
        // purposes.
        if (mState == P_TaskState.INTERRUPTED)
        {
            setState(P_TaskState.REQUEUED);
        }
        else
        {
            setState(P_TaskState.QUEUED);
        }
    }

    void onCanceled()
    {
        setState(P_TaskState.CANCELLED);
    }

    BleManager getManager()
    {
        return mTaskMgr.getManager();
    }

    long timeCreated()
    {
        return mTimeCreated;
    }

    long timeExecuting()
    {
        return mTimeExecuting;
    }

    boolean requeued()
    {
        return mState == P_TaskState.REQUEUED;
    }

    private void setState(P_TaskState state)
    {
        if (mState != state && mTaskMgr != null)
        {
            mState = state;
            if (getLogger().isEnabled())
            {
                if (mState.isEndingState())
                {
                    String logText = toString();
                    if (mTaskMgr != null)
                    {
                        logText += " - " + mTaskMgr.getUpdateCount();
                    }
                    getLogger().i(logText);
                }
                else if (mState == P_TaskState.EXECUTING)
                {
                    mTaskMgr.print();
                }
            }
            if (mStateListener != null)
            {
                mStateListener.onStateChanged(this, mState);
            }
        }
    }

    private P_Logger getLogger()
    {
        return mTaskMgr.getManager().getLogger();
    }

    boolean hasHigherPriorityThan(P_Task task)
    {
        return getPriority().ordinal() > task.getPriority().ordinal();
    }

    boolean hasHigherOrTheSamePriorityThan(P_Task task)
    {
        return getPriority().ordinal() >= task.getPriority().ordinal();
    }

    public boolean interrupt()
    {
        if (isInterruptible() && mState == P_TaskState.EXECUTING)
        {
            setState(P_TaskState.INTERRUPTED);
            mTaskMgr.addInterruptedTask(this);
            return true;
        }
        return false;
    }

    public void succeed()
    {
        mTaskMgr.succeedTask(this);
        setState(P_TaskState.SUCCEEDED);
    }

    public void fail()
    {
        mTaskMgr.failTask(this);
        setState(P_TaskState.FAILED);
    }

    public boolean isNull()
    {
        return this == NULL;
    }

    void updateTask(long curTimeMs)
    {
        if (mLastUpdate != 0)
        {
            mTimeExecuting += (curTimeMs - mLastUpdate);
        }
        mLastUpdate = curTimeMs;
        update(curTimeMs);
    }

    void onTaskTimedOut()
    {
        setState(P_TaskState.TIMED_OUT);
    }

    void checkTimeOut()
    {
        if (Interval.isDisabled(getManager().mConfig.taskTimeout))
        {
            return;
        }
        else
        {
            if (mTimeExecuting >= getManager().mConfig.taskTimeout.millis())
            {
                mTaskMgr.timeOut(this);
            }
        }
    }

    /**
     * Override this to add additional information to the tasks {@link #toString()} method.
     * {@link #toString()} prints out in this order:
     * <pre>
     *     Task name (from classname, and replaces P_Task_ with "")
     *     state name
     *     device name - if device is not null
     *     addition - if not null (the results of this method, default is null)
     * </pre>
     */
    String getToStringAddition()
    {
        return null;
    }

    @Override public String toString()
    {
        String name = getClass().getSimpleName();
        name = name.replace("P_Task_", "");

        String deviceEntry = mDevice != null ? " " + mDevice.getName_debug() : "";
        String addition = getToStringAddition() != null ? " " + getToStringAddition() : "";
        return Utils_String.concatStrings(name, "(", mState.name(), deviceEntry, addition, ")");
    }

    interface IStateListener
    {
        void onStateChanged(P_Task task, P_TaskState state);
    }

    final static P_Task NULL = new P_Task()
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
}
