package com.idevicesinc.sweetblue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.idevicesinc.sweetblue.utils.Interval;

final class P_TaskManager
{
    private final P_TaskQueue m_queue;
    private final Object m_lock = new Object();
    private final AtomicReference<PA_Task> m_current;
    private long m_updateCount;
    private final P_Logger m_logger;
    private final BleManager m_mngr;
    private double m_time = 0.0;
    private double m_timeSinceEnding = 0.0;
    private boolean m_suspended = false;

    // This counter tracks how many levels deep we are into a recursive loop, which lets us avoid stack overflows
    private int m_recursionCounter = 0;

    private final static int kRecursionLimit = 10;

    private int m_currentOrdinal;

    P_TaskManager(BleManager mngr)
    {
        m_mngr = mngr;
        m_logger = mngr.getLogger();

        m_current = new AtomicReference<>(null);

        m_queue = new P_TaskQueue();
    }

    //TODO:  Re-examine this and see if it's needed or not
    final int assignOrdinal()
    {
        synchronized (m_lock)
        {
            // If we didn't synchronize this, its possible two simultaneous calls from different threads could be returned the same value
            int retVal = m_currentOrdinal;
            m_currentOrdinal++;
            return retVal;
        }
    }

    final void setSuspended(boolean suspended)
    {
        if (m_suspended == suspended)
            return;

        m_suspended = suspended;
        m_logger.i("Setting TaskManager suspended flag to " + suspended);
    }

    final int getCurrentOrdinal()
    {
        return m_currentOrdinal;
    }

    public final PA_Task peek()
    {
        synchronized (m_lock)
        {
            return m_queue.peek();
        }
    }

    private boolean tryCancellingCurrentTask(PA_Task newTask)
    {
        synchronized (m_lock)
        {
            // See if we can abort the current task
            if (getCurrent() != null && getCurrent().isCancellableBy(newTask))
            {
                // If so, cancel the current...
                endCurrentTask(PE_TaskState.CANCELLED);

                // And insert the new task at the front of the queue so it will be dequeued next
                addToFront(newTask);

                return true;
            }
        }

        return false;
    }

    private boolean tryInterruptingCurrentTask(PA_Task newTask)
    {
        synchronized (m_lock)
        {
            // See if we can interrupt the current task
            if (getCurrent() != null && getCurrent().isInterruptableBy(newTask))
            {
                // Cache the current task, since we will need to reschedule it
                PA_Task current_saved = getCurrent();

                // Interrupt the current task
                endCurrentTask(PE_TaskState.INTERRUPTED);

                // Shove both the current and new task into the queue so the new task will run, followed by the current
                addToFront(current_saved);
                addToFront(newTask);

                return true;
            }
        }

        return false;
    }

    public final void softlyCancelTasks(PA_Task task)
    {
        synchronized (m_lock)
        {
            // Softly cancel anything in the queue that is softly cancelable by the given task
            m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task d)
                {
                    if (d.isSoftlyCancellableBy(task))
                        d.attemptToSoftlyCancel(task);

                    return ProcessResult.Continue;
                }
            });

            PA_Task current = getCurrent();
            if (current != null && current.isSoftlyCancellableBy(task))
                current.attemptToSoftlyCancel(task);
        }
    }

    private void addToFront(PA_Task task)
    {
        synchronized (m_lock)
        {
            m_queue.pushFront(task);
            onTaskAddedToQueue(task);
        }
    }

    private void addToBack(PA_Task task)
    {
        synchronized (m_lock)
        {
            m_queue.pushBack(task);
            onTaskAddedToQueue(task);
        }
    }

    private void onTaskAddedToQueue(PA_Task task)
    {
        synchronized (m_lock)
        {
            task.assignDefaultOrdinal(this);

            softlyCancelTasks(task);

            task.onAddedToQueue(this);

            print();
        }
    }

    private void onTaskRemovedFromQueue(PA_Task task)
    {
        if (task.wasSoftlyCancelled())
            task.setEndingState(PE_TaskState.SOFTLY_CANCELLED);
        else
            task.setEndingState(PE_TaskState.CLEARED_FROM_QUEUE);

        print();
    }

    final void add(final PA_Task newTask)
    {
        addTask(newTask);
    }

    final void addTask(final PA_Task newTask)
    {
        m_logger.i("Adding task to queue: " + newTask);
        synchronized (m_lock)
        {
            newTask.init();

            // Check the idle status to ensure the new task gets executed as soon as possible (rather than
            // waiting until the idle interval's next tick)
            m_mngr.checkIdleStatus();
            if (tryCancellingCurrentTask(newTask))
            {
                if (getCurrent() == null)
                    dequeue();
            }
            else if (tryInterruptingCurrentTask(newTask))
            {
                // Why don't we dequeue here, if we do after cancel?
            }
            else
            {
                // Toss the task into the queue at the 'best' location (earliest spot it can go)
                m_queue.insertAtSoonestPosition(newTask);
                onTaskAddedToQueue(newTask);
            }
        }
        print();
    }

    final double getTime()
    {
        return m_time;
    }

    public final boolean update(double timeStep, long currentTime)
    {
        if (m_suspended)
            return false;

        boolean executingTask = false;

        m_time += timeStep;

        if (getCurrent() == null)
            m_timeSinceEnding += timeStep;

        if (m_current.get() == null)
            executingTask = dequeue();

        if (getCurrent() != null)
        {
            getCurrent().update_internal(timeStep, currentTime);
            executingTask = true;
        }

        m_updateCount++;

        return executingTask;
    }

    private boolean hasDelayTimePassed()
    {
        Interval delayTime = m_mngr.m_config.delayBetweenTasks;
        if (Interval.isDisabled(delayTime))
            return true;

        return m_timeSinceEnding >= delayTime.secs();
    }

    private boolean dequeue()
    {
        synchronized (m_lock)
        {
            // This is only legal if there is no current task
            if (!m_mngr.ASSERT(m_current.get() == null))
                return false;

            // Make sure we obey the delay timer
            if (!hasDelayTimePassed())
                return false;

            // Locate the next armable task, if any, in the queue
            PA_Task nextTask = m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task d)
                {
                    // If the task is armable, dequeue it and return
                    if (d.isArmable())
                        return ProcessResult.ReturnAndDequeue;

                    // Otherwise, keep walking the queue
                    return ProcessResult.Continue;
                }
            }).getTask();

            // If we found a next task, run it.  It will already have been removed from the queue
            if (nextTask != null)
            {
                m_current.set(nextTask);
                nextTask.arm();
                if (!nextTask.tryExecuting())
                    print();
                return true;
            }
        }

        return false;
    }

    public final long getUpdateCount()
    {
        return m_updateCount;
    }

    public final PA_Task getCurrent()
    {
        return m_current.get();
    }

    private boolean endCurrentTask(PE_TaskState endingState)
    {
        synchronized (m_lock)
        {
            //
            if (!m_mngr.ASSERT(endingState.isEndingState()))
                return false;

            PA_Task current_saved = m_current.get();

            if (current_saved == null)
                return false;

            m_current.set(null);
            m_timeSinceEnding = 0.0;
            current_saved.setEndingState(endingState);

            boolean printed = false;

            if (m_queue.size() > 0 && m_recursionCounter++ < kRecursionLimit)
                printed = dequeue();

            --m_recursionCounter;

            if (!printed)
                print();

            return true;
        }
    }

    public final void interrupt(Class<? extends PA_Task> taskClass, BleManager manager)
    {
        synchronized (m_lock)
        {
            PA_Task current = getCurrent(taskClass, manager);

            if (doesTaskMatch(getCurrent(), taskClass, manager, null, null))
            {
                tryEndingTask(current, PE_TaskState.INTERRUPTED);

                // Add the task back to the queue at the most appropriate spot (this obeys the normal priority logic)
                addTask(current);
            }
        }
    }


    final boolean succeed(Class<? extends PA_Task> taskClass, BleManager manager)
    {
        return tryEndingTask(taskClass, manager, null, null, PE_TaskState.SUCCEEDED);
    }

    final boolean succeed(Class<? extends PA_Task> taskClass, BleDevice device)
    {
        return tryEndingTask(taskClass, null, device, null, PE_TaskState.SUCCEEDED);
    }

    final boolean succeed(Class<? extends PA_Task> taskClass, BleServer server)
    {
        return tryEndingTask(taskClass, null, null, server, PE_TaskState.SUCCEEDED);
    }

    final boolean fail(Class<? extends PA_Task> taskClass, BleManager manager)
    {
        return tryEndingTask(taskClass, manager, null, null, PE_TaskState.FAILED);
    }

    final boolean fail(Class<? extends PA_Task> taskClass, BleDevice device)
    {
        return tryEndingTask(taskClass, null, device, null, PE_TaskState.FAILED);
    }

    final boolean fail(Class<? extends PA_Task> taskClass, BleServer server)
    {
        return tryEndingTask(taskClass, null, null, server, PE_TaskState.FAILED);
    }

    private boolean tryEndingTask(final Class<? extends PA_Task> taskClass, final BleManager mngr_nullable, final BleDevice device_nullable, final BleServer server_nullable, final PE_TaskState endingState)
    {
        synchronized (m_lock)
        {
            if (doesTaskMatch(getCurrent(), taskClass, mngr_nullable, device_nullable, server_nullable))
                return endCurrentTask(endingState);
        }

        return false;
    }

    final void tryEndingTask(final PA_Task task, final PE_TaskState endingState)
    {
        synchronized (m_lock)
        {
            if (task != null && task == getCurrent())
            {
                if (!endCurrentTask(endingState))
                {
                    m_mngr.ASSERT(false);
                }
            }
        }
    }

    final boolean isCurrent(Class<? extends PA_Task> taskClass, BleManager mngr)
    {
        synchronized (m_lock)
        {
            return doesTaskMatch(getCurrent(), taskClass, mngr, null, null);
        }
    }

    final boolean isCurrent(Class<? extends PA_Task> taskClass, BleDevice device)
    {
        synchronized (m_lock)
        {
            return doesTaskMatch(getCurrent(), taskClass, null, device, null);
        }
    }

    final boolean isCurrent(Class<? extends PA_Task> taskClass, BleServer server)
    {
        synchronized (m_lock)
        {
            return doesTaskMatch(getCurrent(), taskClass, null, null, server);
        }
    }

    private boolean isInQueue(Class<? extends PA_Task> taskClass, BleManager mngr_nullable, BleDevice device_nullable, BleServer server_nullable)
    {
        synchronized (m_lock)
        {
            return m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task task)
                {
                    if (doesTaskMatch(task, taskClass, mngr_nullable, device_nullable, server_nullable))
                        return ProcessResult.Return;
                    return ProcessResult.Continue;
                }
            }).getTask() != null;
        }
    }

    private int positionInQueue(Class<? extends PA_Task> taskClass, BleManager mngr_nullable, BleDevice device_nullable, BleServer server_nullable)
    {
        synchronized (m_lock)
        {
            return m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task task)
                {
                    if (doesTaskMatch(task, taskClass, mngr_nullable, device_nullable, server_nullable))
                        return ProcessResult.Return;
                    return ProcessResult.Continue;
                }
            }).getTaskPosition();
        }
    }

    final int getSize()
    {
        synchronized (m_lock)
        {
            return m_queue.size();
        }
    }

    //FIXME:  Replace this with a way to get a read only forEach iterator over the queue
    public final List<PA_Task> getRaw()
    {
        return m_queue.getRaw();
    }

    public final int positionInQueue(Class<? extends PA_Task> taskClass, BleManager mngr)
    {
        return positionInQueue(taskClass, mngr, null, null);
    }

    public final int positionInQueue(Class<? extends PA_Task> taskClass, BleDevice device)
    {
        return positionInQueue(taskClass, null, device, null);
    }

    public final int positionInQueue(Class<? extends PA_Task> taskClass, BleServer server)
    {
        return positionInQueue(taskClass, null, null, server);
    }

    public final boolean isInQueue(Class<? extends PA_Task> taskClass, BleManager mngr)
    {
        return isInQueue(taskClass, mngr, null, null);
    }

    public final boolean isInQueue(Class<? extends PA_Task> taskClass, BleDevice device)
    {
        return isInQueue(taskClass, null, device, null);
    }

    public final boolean isInQueue(Class<? extends PA_Task> taskClass, BleServer server)
    {
        return isInQueue(taskClass, null, null, server);
    }

    public final boolean isCurrentOrInQueue(Class<? extends PA_Task> taskClass, BleManager mngr)
    {
        return isCurrent(taskClass, mngr) || isInQueue(taskClass, mngr);
    }

    @SuppressWarnings("unchecked")
    public final <T extends PA_Task> T get(Class<T> taskClass, BleManager mngr)
    {
        synchronized (m_lock)
        {
            // See if the current task matches
            if (doesTaskMatch(getCurrent(), taskClass, mngr, null, null))
                return (T)getCurrent();

            // See if any task in queue matches
            return (T)m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task task)
                {
                    if (doesTaskMatch(task, taskClass, mngr, null, null))
                        return ProcessResult.Return;
                    return ProcessResult.Continue;
                }
            }).getTask();
        }
    }

    @SuppressWarnings("unchecked")
    public final <T extends PA_Task> T getCurrent(Class<T> taskClass, BleDevice device)
    {
        synchronized (m_lock)
        {
            if (doesTaskMatch(getCurrent(), taskClass, null, device, null))
                return (T)getCurrent();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public final <T extends PA_Task> T getCurrent(Class<T> taskClass, BleManager mngr)
    {
        synchronized (m_lock)
        {
            if (doesTaskMatch(getCurrent(), taskClass, mngr, null, null))
                return (T)getCurrent();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public final <T extends PA_Task> T getCurrent(Class<T> taskClass, BleServer server)
    {
        synchronized (m_lock)
        {
            if (doesTaskMatch(getCurrent(), taskClass, null, null, server))
                return (T)getCurrent();
        }

        return null;
    }

    final void print()
    {
        if (m_logger.isEnabled())
            m_logger.i(this.toString());
    }

    public final void clearQueueOf(Class<? extends PA_Task> taskClass, BleManager mngr)
    {
        synchronized (m_lock)
        {
            m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task task)
                {
                    if (doesTaskMatch(task, taskClass, mngr, null, null))
                    {
                        onTaskRemovedFromQueue(task);
                        return ProcessResult.ContinueAndDequeue;
                    }
                    return ProcessResult.Continue;
                }
            });
        }
    }

    public final void clearQueueOf(Class<? extends PA_Task> taskClass, BleDevice device, final int ordinal)
    {
        synchronized (m_lock)
        {
            m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task task)
                {
                    if (ordinal <= -1 || ordinal >= 0 && task.getOrdinal() <= ordinal)
                    {
                        if (doesTaskMatch(task, taskClass, null, device, null))
                        {
                            onTaskRemovedFromQueue(task);
                            return ProcessResult.ContinueAndDequeue;
                        }
                    }
                    return ProcessResult.Continue;
                }
            });
        }
    }

    public final void clearQueueOf(Class<? extends PA_Task> taskClass, BleServer server)
    {
        synchronized (m_lock)
        {
            m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task task)
                {
                    if (doesTaskMatch(task, taskClass, null, null, server))
                    {
                        onTaskRemovedFromQueue(task);
                        return ProcessResult.ContinueAndDequeue;
                    }
                    return ProcessResult.Continue;
                }
            });
        }
    }

    public final void clearQueueOfAll()
    {
        synchronized (m_lock)
        {
            m_queue.forEachTask(new P_TaskQueue.ForEachTaskHandler()
            {
                @Override
                public ProcessResult process(PA_Task task)
                {
                    onTaskRemovedFromQueue(task);
                    return ProcessResult.ContinueAndDequeue;
                }
            });
        }
    }

    @Override
    public final String toString()
    {
        return (toString(10));
    }

    public final String toString(int taskLimit)
    {
        synchronized (m_lock)
        {
            StringBuilder sb = new StringBuilder();

            sb.append(m_current.get() != null ? m_current.get().toString() : "no current task");

            sb.append(" ");

            int queueSize = m_queue.size();
            int loopLimit = taskLimit >= 0 ? Math.min(taskLimit, queueSize) : queueSize;

            sb.append("[");
            for (int i = 0; i < loopLimit; ++i)
            {
                sb.append(m_queue.get(i).toString());
                if (i < loopLimit - 1)
                    sb.append(", ");
            }

            // Make a note of how many we skipped over
            if (loopLimit < queueSize)
                sb.append(" ... and " + (queueSize - loopLimit) + " more");

            sb.append("]");

            return sb.toString();
        }
    }

    // Utility method to determine if a given task matches a set of requirements
    private static boolean doesTaskMatch(PA_Task task, Class<? extends PA_Task> taskClass, BleManager mngr_nullable, BleDevice device_nullable, BleServer server_nullable)
    {
        if (task == null)
            return false;

        if (taskClass.isAssignableFrom(task.getClass()))
        {
            if (mngr_nullable == null)
            {
                if (device_nullable == null && server_nullable == null)
                    return true;
                else if (device_nullable != null && device_nullable.equals(task.getDevice()))
                    return true;
                else if (server_nullable != null && server_nullable.equals(task.getServer()))
                    return true;
            }
            else
            {
                if (mngr_nullable == task.getManager())
                    return true;
            }
        }

        return false;
    }
}