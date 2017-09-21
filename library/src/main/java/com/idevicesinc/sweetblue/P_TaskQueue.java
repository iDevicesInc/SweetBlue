package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class P_TaskQueue
{
    private List<PA_Task> m_taskList = new ArrayList<>();

    static class HandlerResult
    {
        PA_Task mTask;
        int mTaskPosition;

        private HandlerResult(PA_Task task, int taskPosition)
        {
            mTask = task;
            mTaskPosition = taskPosition;
        }

        PA_Task getTask()
        {
            return mTask;
        }

        int getTaskPosition()
        {
            return mTaskPosition;
        }
    }

    abstract static class ForEachTaskHandler
    {
        enum ProcessResult
        {
            Continue,
            ContinueAndDequeue,
            Return,
            ReturnAndDequeue
        };
        public abstract ProcessResult process(PA_Task task);
    }

    /*HandlerResult forEachTask(ForEachTaskHandler handler)
    {
        Iterator<PA_Task> it = m_taskList.iterator();

        while (it.hasNext())
        {
            PA_Task task = it.next();
            ForEachTaskHandler.ProcessResult pr = handler.process(task);

            switch (pr)
            {
                case Return:
                    return new HandlerResult(task, 0);

                case ReturnAndDequeue:
                    it.remove();
                    return new HandlerResult(task, 0);

                case ContinueAndDequeue:
                    it.remove();
                    break;
            }
        }

        return new HandlerResult(null, -1);
    }*/

    // This method allows for forward iteration over the queue
    // During the iteration, any number of elements can be removed
    // The handler can also choose to have one element returned, which aborts the iteration at that point
    HandlerResult forEachTask(ForEachTaskHandler handler)
    {
        ArrayList<PA_Task> rebuiltList = null;

        for (int i = 0; i < m_taskList.size(); ++i)
        {
            PA_Task task = m_taskList.get(i);
            ForEachTaskHandler.ProcessResult pr = handler.process(task);

            switch (pr)
            {
                case Return:
                {
                    // See if we have a rebuilt list in progress.  If so, add the remaining elements (including this one), and switch over to that list
                    if (rebuiltList != null)
                    {
                        // Add all tasks remaining in the task list that we haven't copied yet, including this one
                        rebuiltList.addAll(m_taskList.subList(i, m_taskList.size()));

                        // Update our list to point to the rebuilt one
                        m_taskList = rebuiltList;
                    }

                    // Return the current task, which is also still in the task list
                    return new HandlerResult(task, i);
                }

                case ReturnAndDequeue:
                {
                    // See if we have a rebuilt list in progress
                    if (rebuiltList != null)
                    {
                        // If so, we need to add any remaining tasks that we haven't copied yet, not including this one
                        if (i < m_taskList.size() - 1)
                            rebuiltList.addAll(m_taskList.subList(i + 1, m_taskList.size()));

                        // Update our list to point to the rebuilt one
                        m_taskList = rebuiltList;
                    }
                    else
                    {
                        // If we don't have a rebuilt list in progress, that means we didn't make any changes to the list yet
                        // In this case, it's safe to directly remove the current element since we won't be continuing iteration over it
                        m_taskList.remove(i);
                    }

                    // Return the current task, which has been removed from the task list
                    return new HandlerResult(task, i);
                }

                case ContinueAndDequeue:
                {
                    // If we haven't started a rebuilt list yet, we need to, since we will be making changes to the list but continuing iteration
                    if (rebuiltList == null)
                    {
                        // Create a new rebuilt list which includes any elements which we've already iterated over but not removed
                        if (i > 0)
                            rebuiltList = new ArrayList<>(m_taskList.subList(0, i));
                        else
                            rebuiltList = new ArrayList<>();
                    }

                    // If there was already a rebuilt list, we don't need to do anything.  By continuing, we won't copy over the current element
                    // This has the effect of removing it, since we will be making the rebuilt list into the task list when we finish iterating
                    break;
                }

                case Continue:
                {
                    // If we already modified the list, continue to maintain it by adding the current task to it
                    if (rebuiltList != null)
                        rebuiltList.add(task);

                    // Otherwise, we take no action
                }
            }
        }

        // If we get here, that means we never returned out of the loop
        // If we have an in progress rebuilt list, we have to update our task list to point to it
        if (rebuiltList != null)
            m_taskList = rebuiltList;

        // Since the handler never said to return a task, we return null
        return new HandlerResult(null, -1);
    }

    PA_Task peek()
    {
        return m_taskList.size() > 0 ? m_taskList.get(0) : null;
    }

    PA_Task get(int index)
    {
        return m_taskList.get(index);
    }

    void pushFront(PA_Task task)
    {
        m_taskList.add(0, task);
    }

    void pushBack(PA_Task task)
    {
        m_taskList.add(m_taskList.size(), task);
    }

    void insertAtSoonestPosition(PA_Task task)
    {
        // Before walking the entire list, see if the task is more important than the last.  If not, just throw it in the back
        if (m_taskList.size() > 0)
        {
            PA_Task last = m_taskList.get(m_taskList.size() - 1);
            if (!task.isMoreImportantThan(last))
            {
                m_taskList.add(task);
                return;
            }
        }

        //FIXME:  This can be done with a modified binary search that finds the first element that task claims it is more important than

        // Locate the best spot to insert, and put the task there
        for (int i = 0; i < m_taskList.size(); ++i)
        {
            if (task.isMoreImportantThan(m_taskList.get(i)))
            {
                m_taskList.add(i, task);
                return;
            }
        }

        m_taskList.add(task);
    }

    int size()
    {
        return m_taskList.size();
    }

    List<PA_Task> getRaw()
    {
        return new ArrayList<>(m_taskList);
    }

    @Override public final String toString()
    {
        return m_taskList.toString();
    }
}
