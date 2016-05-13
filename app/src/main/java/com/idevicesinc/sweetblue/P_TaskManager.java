package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BleScanInfo;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;


public class P_TaskManager
{

    private final LinkedList<P_Task> mTaskQueue;
    private volatile P_Task mCurrent;
    private final BleManager mBleManager;
    private volatile long mUpdateCount;
    private TaskSorter mTaskSorter;


    public P_TaskManager(BleManager mgr)
    {
        mTaskQueue = new LinkedList<>();
        mBleManager = mgr;
        mTaskSorter = new TaskSorter();
    }

    // This returns true if there are tasks in the queue, or there is a task executing currently
    public boolean update(long curTimeMs)
    {

        boolean hasTasksInQueue = mTaskQueue.size() > 0;
        // If the queue has items in it, and there's no current task, get the next task to execute
        if (hasTasksInQueue && isCurrentNull())
        {
            mCurrent = mTaskQueue.poll();
            mCurrent.executeTask();
        }
        else
        {
            // Check to see if the current task is interruptible, and if so, check the next item in the queue,
            // which will be the highest priority item in the queue. If it's not higher than the current task,
            // we leave the current task alone. Otherwise, null out the current task, then interrupt it. Then,
            // the higher priority task will get polled on the next update cycle.
            if (!isCurrentNull() && mCurrent.isInterruptible() && hasTasksInQueue)
            {
                P_Task task = mTaskQueue.peek();
                if (task.hasHigherPriorityThan(mCurrent))
                {
//                    P_Task tempTask = mCurrent;
//                    mCurrent = null;
                    interruptTask(mCurrent);
                    //tempTask.interrupt();
                }
            }
        }
        if (!isCurrentNull())
        {
            mCurrent.updateTask(curTimeMs);
            // Checks if this task should be timed out. If it should, the task then calls timeOut(P_Task).
            mCurrent.checkTimeOut(curTimeMs);
        }
        mUpdateCount++;
        return hasTasksInQueue || !isCurrentNull();
    }

    BleManager getManager()
    {
        return mBleManager;
    }

    long getUpdateCount()
    {
        return mUpdateCount;
    }

    private boolean isCurrentNull()
    {
        return mCurrent == null || mCurrent.isNull();
    }


    public void succeedTask(final P_Task task)
    {
        if (mBleManager.isOnSweetBlueThread())
        {
            succeedTask_private(task);
        }
        else
        {
            mBleManager.mPostManager.postToUpdateThread(new Runnable()
            {
                @Override public void run()
                {
                    succeedTask_private(task);
                }
            });
        }
    }

    private void succeedTask_private(P_Task task)
    {
        if (mCurrent == task)
        {
            mCurrent = null;
        }
        else
        {
            mTaskQueue.remove(task);
        }
        task.onSucceed();
    }

    public void succeedTask(Class<? extends P_Task> taskClass, BleManager mgr)
    {
        P_Task task = findTask(taskClass, mgr, null, null);
        if (task != null)
        {
            succeedTask(task);
        }
    }

    public void succeedTask(Class<? extends P_Task> taskClass, BleDevice device)
    {
        P_Task task = findTask(taskClass, null, device, null);
        if (task != null)
        {
            succeedTask(task);
        }
    }

    public void succeedTask(Class<? extends P_Task> taskClass, BleServer server)
    {
        P_Task task = findTask(taskClass, null, null, server);
        if (task != null)
        {
            succeedTask(task);
        }
    }

    public void failTask(final P_Task task)
    {
        if (mBleManager.isOnSweetBlueThread())
        {
            failTask_private(task);
        }
        else
        {
            mBleManager.mPostManager.postToUpdateThread(new Runnable()
            {
                @Override public void run()
                {
                    failTask_private(task);
                }
            });
        }
    }

    private void failTask_private(P_Task task)
    {
        if (mCurrent == task)
        {
            mCurrent = null;
        }
        else
        {
            mTaskQueue.remove(task);
        }
        task.onFail();
    }

    public void clearTask(P_Task task)
    {
        if (mCurrent == task)
        {
            mCurrent = null;
        }
    }

    public void failTask(Class<? extends P_Task> taskClass, BleManager mgr)
    {
        P_Task task = findTask(taskClass, mgr, null, null);
        if (task != null)
        {
            failTask(task);
        }
    }

    public void failTask(Class<? extends P_Task> taskClass, BleDevice device)
    {
        P_Task task = findTask(taskClass, null, device, null);
        if (task != null)
        {
            failTask(task);
        }
    }

    public void failTask(Class<? extends P_Task> taskClass, BleServer server)
    {
        P_Task task = findTask(taskClass, null, null, server);
        if (task != null)
        {
            failTask(task);
        }
    }

    public boolean isCurrent(Class<? extends P_Task> taskClass, BleManager mgr)
    {
        if (mCurrent == null)
        {
            return false;
        }
        if (taskClass.isAssignableFrom(mCurrent.getClass()))
        {
            if (mgr == mCurrent.getManager())
            {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrent(Class<? extends P_Task> taskClass, BleDevice device)
    {
        if (mCurrent == null)
        {
            return false;
        }
        if (taskClass.isAssignableFrom(mCurrent.getClass()))
        {
            if (device == null)
            {
                if (mCurrent.getDevice() == null)
                {
                    return true;
                }
            }
            else if (device.equals(mCurrent.getDevice()))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrent(Class<? extends P_Task> taskClass, BleServer server)
    {
        if (mCurrent == null)
        {
            return false;
        }
        if (taskClass.isAssignableFrom(mCurrent.getClass()))
        {
            if (server == null)
            {
                if (mCurrent.getServer() == null)
                {
                    return true;
                }
            }
            else if (server.equals(mCurrent.getServer()))
            {
                return true;
            }
        }
        return false;
    }

    public void add(final P_Task task)
    {
        if (!task.isNull())
        {
            if (getManager().isOnSweetBlueThread())
            {
                add_private(task);
            }
            else
            {
                getManager().mPostManager.postToUpdateThread(new Runnable()
                {
                    @Override public void run()
                    {
                        add_private(task);
                    }
                });
            }
        }
    }

    private void add_private(P_Task task)
    {
        if (mTaskQueue.size() == 0)
        {
            mTaskQueue.add(task);
        }
        else
        {
            mTaskQueue.add(task);
            // Sort the list, first by priority, then by the time it was created, or if it's requeued.
            Collections.sort(mTaskQueue, mTaskSorter);
        }
        task.addedToQueue();
    }

    void interruptTask(Class<? extends P_Task> taskClass, BleManager mgr)
    {
        if (mCurrent != null)
        {
            if (matches(mCurrent, taskClass, mgr, null, null))
            {
                interruptTask(mCurrent);
            }
        }
    }

    void interruptTask(Class<? extends P_Task> taskClass, BleDevice device)
    {
        if (matches(mCurrent, taskClass, null, device, null))
        {
            interruptTask(mCurrent);
        }
    }

    void interruptTask(Class<? extends P_Task> taskClass, BleServer server)
    {
        if (matches(mCurrent, taskClass, null, null, server))
        {
            interruptTask(mCurrent);
        }
    }

    private void interruptTask(P_Task task)
    {
        if (task.isInterruptible() && task.isExecuting())
        {
            mCurrent = null;
            task.onInterrupted();
            addInterrupted_private(task);
        }
    }

    public void addInterruptedTask(final P_Task task)
    {
        // Add the interrupted task to the beginning of the queue, so that it starts
        // next after the task which interrupted it is done executing.
        if (getManager().isOnSweetBlueThread())
        {
            addInterrupted_private(task);
        }
        else
        {
            getManager().mPostManager.postToUpdateThread(new Runnable()
            {
                @Override public void run()
                {
                    addInterrupted_private(task);
                }
            });
        }
    }

    private void addInterrupted_private(P_Task task)
    {
        if (mTaskQueue.size() == 0)
        {
            mTaskQueue.push(task);
        }
        else
        {
            int size = mTaskQueue.size();
            for (int i = 0; i < size; i++)
            {
                if (task.hasHigherOrTheSamePriorityThan(mTaskQueue.get(i)))
                {
                    mTaskQueue.add(i, task);
                    task.addedToQueue();
                    return;
                }
            }
            // If we got here, it means all other tasks are higher priority, so we'll just add this
            // to the end of the queue.
            mTaskQueue.add(task);
        }
        task.addedToQueue();
    }

    public void timeOut(final P_Task task)
    {
        if (getManager().isOnSweetBlueThread())
        {
            timeOut_private(task);
        }
        else
        {
            getManager().mPostManager.postToUpdateThread(new Runnable()
            {
                @Override public void run()
                {
                    timeOut_private(task);
                }
            });
        }
    }

    private void timeOut_private(P_Task task)
    {
        mCurrent = null;
        task.onTaskTimedOut();
    }

    public void cancel(final P_Task task)
    {
        if (getManager().isOnSweetBlueThread())
        {
            cancel_private(task);
        }
        else
        {
            getManager().mPostManager.postToUpdateThread(new Runnable()
            {
                @Override public void run()
                {
                    cancel_private(task);
                }
            });
        }
    }

    private void cancel_private(P_Task task)
    {
        mTaskQueue.remove(task);
        task.onCanceled();
    }

    public P_Task getCurrent()
    {
        return mCurrent != null ? mCurrent : P_Task.NULL;
    }

    void print()
    {
        if (getManager().getLogger().isEnabled())
        {
            getManager().getLogger().i(toString());
        }
    }

    private P_Task findTask(Class<? extends P_Task> clazz, BleManager mgr, BleDevice device, BleServer server)
    {
        int size = mTaskQueue.size();
        P_Task task;
        for (int i = 0; i < size; i++)
        {
            task = mTaskQueue.get(i);
            if (matches(task, clazz, mgr, device, server))
            {
                return task;
            }
        }
        if (mCurrent != null)
        {
            if (matches(mCurrent, clazz, mgr, device, server))
            {
                return mCurrent;
            }
        }
        return null;
    }

    private boolean matches(P_Task task, Class<? extends P_Task> taskClass, BleManager mgr, BleDevice device, BleServer server)
    {
        if (taskClass.isAssignableFrom(task.getClass()))
        {
            if (mgr == null)
            {
                if (device == null && server == null)
                {
                    return true;
                }
                else if (device != null && device.equals(task.getDevice()))
                {
                    return true;
                }
                else if (server != null && server.equals(task.getServer()))
                {
                    return true;
                }
            }
            else
            {
                if (mgr == task.getManager())
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override public String toString()
    {
        final String current = mCurrent != null ? mCurrent.toString() : "no current task";

        final String queue = mTaskQueue.size() > 0 ? mTaskQueue.toString() : "[queue empty]";

        return Utils_String.concatStrings(current, " ", queue);
    }

    private class TaskSorter implements Comparator<P_Task>
    {

        @Override public int compare(P_Task lhs, P_Task rhs)
        {
            int comp = rhs.getPriority().compareTo(lhs.getPriority());
            if (comp != 0)
            {
                return comp;
            }
            else
            {
                if (lhs.requeued() && !rhs.requeued())
                {
                    return -1;
                }
                else if (rhs.requeued())
                {
                    return 1;
                }
                return lhs.timeCreated() < rhs.timeCreated() ? -1 : (lhs.timeCreated() == rhs.timeCreated() ? 0 : 1);
            }
        }
    }
}
