package com.idevicesinc.sweetblue;


import android.app.Activity;
import android.os.Looper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 21)
@RunWith(RobolectricTestRunner.class)
public class TasksTest
{

    private Activity mActivity;

    @Before
    public void setup()
    {
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    P_TaskManager mgr;

    @Test
    public void testTaskLoadAndStrictOrder() throws Exception
    {
        // Spawn a thread to run the test in, then spawn another one to call update on the task manager.
        final Semaphore s = new Semaphore(0);


        new TestThread(new LoopRunnable()
        {
            @Override public void run(Looper looper)
            {
                BleManagerConfig config = new BleManagerConfig();
                config.updateLooper = Looper.myLooper();
                BleManager bleMgr = BleManager.get(mActivity, config);
                mgr = new P_TaskManager(bleMgr);
                int taskCount = 1000;
                final TaskStrictOrderStateListener listener = new TaskStrictOrderStateListener(taskCount, s);
                int pri = 0;
                for (int i = 0; i < taskCount; i++)
                {
                    if (pri > 4)
                    {
                        pri = 0;
                    }
                    P_Task_Looper loop = new P_Task_Looper(mgr, listener, P_TaskPriority.values()[pri]);
                    mgr.add(loop);
                    pri++;
                }
            }
        }, new Runnable()
        {
            @Override public void run()
            {
                while (true)
                {
                    mgr.update(System.currentTimeMillis());
                    try
                    {
                        Thread.sleep(25);
                    } catch (Exception e)
                    {
                    }
                }
            }
        }).start();


        s.acquire();
    }

    private class TestThread extends Thread
    {

        private final LoopRunnable setupRunnable;
        private final UpdateRunnable updateRunnable;

        public TestThread(LoopRunnable setupRunner, UpdateRunnable updateRunnable)
        {
            super("TestThread");
            setupRunnable = setupRunner;
            this.updateRunnable = updateRunnable;
        }

        @Override public void run()
        {
            Looper.prepare();
            if (setupRunnable != null)
            {
                setupRunnable.run(Looper.myLooper());
            }
            if (updateRunnable != null)
            {
                updateRunnable.update();
            }
            Looper.loop();
        }
    }

    private interface LoopRunnable
    {
        void run(Looper looper);
    }

    private interface UpdateRunnable<T>
    {
        void update(T obj);
    }

    private class TaskStrictOrderStateListener implements P_Task.IStateListener
    {

        private final Semaphore semaphore;
        private final int taskCount;
        private int curFinishedTasks;
        private int priChange;
        private int priInterval;
        private P_TaskPriority curPriority = P_TaskPriority.CRITICAL;


        public TaskStrictOrderStateListener(int taskCount, Semaphore sem)
        {
            this.taskCount = taskCount;
            if (taskCount % 5 != 0)
            {
                throw new RuntimeException("Task count MUST be divisible by 5.");
            }
            priChange = taskCount / 5;
            priInterval = priChange;
            priChange++;
            semaphore = sem;
        }

        @Override public void onStateChanged(P_Task task, P_TaskState state)
        {
            if (state == P_TaskState.SUCCEEDED || state == P_TaskState.FAILED)
            {
                curFinishedTasks++;
                if (curFinishedTasks >= priChange)
                {
                    assertTrue(task.getPriority().compareTo(curPriority) < 0);
                    if (curPriority != P_TaskPriority.TRIVIAL)
                    {
                        curPriority = P_TaskPriority.values()[curPriority.ordinal() - 1];
                    }
                    priChange += priInterval;
                }
                if (curFinishedTasks >= taskCount)
                {
                    semaphore.release();
                }
            }
        }
    }

}
