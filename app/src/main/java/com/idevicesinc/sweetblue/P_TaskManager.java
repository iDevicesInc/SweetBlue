package com.idevicesinc.sweetblue;


import java.util.LinkedList;

public class P_TaskManager
{

    private final LinkedList<P_Task> mTaskQueue;
    private P_Task mCurrent;
    private final BleManager mBleManager;


    public P_TaskManager(BleManager mgr)
    {
        mTaskQueue = new LinkedList<>();
        mBleManager = mgr;
    }

    public void update(long curTimeMs)
    {
        // If the queue has items in it, and there's no current task, get the next task to execute
        if (mTaskQueue.size() > 0 && isCurrentNull())
        {
            mCurrent = getNextTask(true);
            mCurrent.executeTask();
        }
        else
        {
            // Check to see if the current task is interruptible, and if so, see if there's a higher
            // priority task to execute. If there is, interrupt the current task, and the higher priority
            // task will be executed on the next update cycle.
            if (!isCurrentNull() && mCurrent.isInterruptible())
            {
                P_Task task = getNextTask(false);
                if (task.hasHigherPriority(mCurrent))
                {
                    mCurrent.interrupt();
                }
            }
        }
    }

    private boolean isCurrentNull()
    {
        return mCurrent == null || mCurrent.isNull();
    }

    private P_Task getNextTask(boolean removeFromQueue)
    {
        int size = mTaskQueue.size();
        if (size == 1)
        {
            if (removeFromQueue)
            {
                return mTaskQueue.poll();
            }
            else
            {
                return mTaskQueue.peek();
            }
        }
        P_Task curTask = mTaskQueue.peekFirst();
        P_Task tempTask;
        for (int i = 0; i < size; i++)
        {
            tempTask = mTaskQueue.get(i);
            if (tempTask.hasHigherPriority(curTask) && tempTask.isExecutable())
            {
                curTask = tempTask;
            }
        }
        if (removeFromQueue)
        {
            mTaskQueue.remove(curTask);
        }
        return curTask;
    }

    public void succeedTask(P_Task task)
    {
        if (mCurrent == task)
        {
            mCurrent = null;
        }
    }

    public void failTask(P_Task task)
    {
        if (mCurrent == task)
        {
            mCurrent = null;
        }
    }

    public void add(P_Task task)
    {
        if (!task.isNull())
        {
            mTaskQueue.add(task);
        }
    }

    public void addInterruptedTask(P_Task task)
    {
        // Add the interrupted task to the beginning of the queue, so that it starts
        // next after the task which interrupted it is done executing.
        mTaskQueue.push(task);
    }

    public void cancel(P_Task task)
    {
        mTaskQueue.remove(task);
    }

    public P_Task getCurrent()
    {
        return mCurrent != null ? mCurrent : P_Task.NULL;
    }

}
