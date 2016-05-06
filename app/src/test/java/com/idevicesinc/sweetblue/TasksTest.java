package com.idevicesinc.sweetblue;


import org.junit.Test;

public class TasksTest
{

    @Test
    public void taskTestOne()
    {
        // Spawn a thread to run the test in, then spawn another one to call update on the task manager.
        P_TaskManager mgr = new P_TaskManager(null);
        P_Task_Looper loop = new P_Task_Looper(mgr, new P_Task.IStateListener()
        {
            @Override public void onStateChanged(P_Task task, P_TaskState state)
            {

            }
        }, P_TaskPriority.MEDIUM);
        mgr.add(loop);
    }

}
