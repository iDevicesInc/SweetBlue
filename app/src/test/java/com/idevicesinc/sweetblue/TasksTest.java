package com.idevicesinc.sweetblue;


import android.app.Activity;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Random;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertThat;
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


    @Test
    public void testTaskLoadAndStrictOrder() throws Exception
    {

        final Semaphore s = new Semaphore(0);

        new TestThread(new TestRunnable()
        {

            P_TaskManager mgr;

            @Override public void setup(Looper looper)
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

            @Override public void update()
            {
                mgr.update(System.currentTimeMillis());
            }
        }).start();

        s.acquire();
    }

    @Test
    public void testRandomOrder() throws Exception
    {
        final Semaphore s = new Semaphore(0);

        new TestThread(new TestRunnable()
        {

            P_TaskManager mgr;

            @Override public void setup(Looper looper)
            {
                BleManagerConfig config = new BleManagerConfig();
                config.updateLooper = Looper.myLooper();
                BleManager bleMgr = BleManager.get(mActivity, config);
                mgr = new P_TaskManager(bleMgr);
                int taskCount = 1000;
                final TaskRandomOrderStateListener listener = new TaskRandomOrderStateListener(taskCount, s);
                int pri;
                Random r = new Random();
                for (int i = 0; i < taskCount; i++)
                {
                    pri = r.nextInt(5);
                    P_Task_Looper loop = new P_Task_Looper(mgr, listener, P_TaskPriority.values()[pri]);
                    mgr.add(loop);
                }
            }

            @Override public void update()
            {
                mgr.update(System.currentTimeMillis());
            }
        }).start();

        s.acquire();
    }

    @Test
    public void testRequeuing() throws Exception
    {
        final Semaphore s = new Semaphore(0);

        new TestThread(new TestRunnable()
        {

            P_TaskManager mgr;

            @Override public void setup(Looper looper)
            {
                BleManagerConfig config = new BleManagerConfig();
                config.updateLooper = Looper.myLooper();
                BleManager bleMgr = BleManager.get(mActivity, config);
                mgr = new P_TaskManager(bleMgr);
                P_Task_LongRunner longer = new P_Task_LongRunner(mgr, new P_Task.IStateListener()
                {

                    boolean executed, requeued, interrupted;

                    @Override public void onStateChanged(P_Task task, P_TaskState state)
                    {
                        if (state == P_TaskState.INTERRUPTED)
                        {
                            interrupted = true;
                            assertTrue(executed);
                            assertTrue(!requeued);
                        }
                        if (state == P_TaskState.REQUEUED)
                        {
                            requeued = true;
                            assertTrue(executed);
                            assertTrue(interrupted);
                            s.release();
                        }
                        if (state == P_TaskState.EXECUTING)
                        {
                            executed = true;
                            assertTrue(!requeued);
                            assertTrue(!interrupted);
                        }
                    }
                }, P_TaskPriority.MEDIUM);
                mgr.add(longer);
                int taskCount = 50;

                Random r = new Random();
                for (int i = 0; i < taskCount; i++)
                {
                    P_Task_Looper loop = new P_Task_Looper(mgr, null, P_TaskPriority.values()[r.nextInt(5)]);
                    mgr.add(loop);
                }

            }

            int count = 0;
            boolean addedHigherTasks = false;

            @Override public void update()
            {
                mgr.update(System.currentTimeMillis());
                count++;
                if (count > 25 && !addedHigherTasks)
                {
                    addedHigherTasks = true;
                    P_Task_Looper loop = new P_Task_Looper(mgr, new P_Task.IStateListener()
                    {
                        @Override public void onStateChanged(P_Task task, P_TaskState state)
                        {
                            if (state == P_TaskState.EXECUTING)
                            {
                                int i = 0;
                                i++;
                            }
                        }
                    }, P_TaskPriority.HIGH);
                    mgr.add(loop);
                }
            }
        }).start();

        s.acquire();
    }

    private class P_Task_LongRunner extends P_Task
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

    private class TestThread extends Thread
    {

        private final TestRunnable testRunnable;
        private boolean mRunning = true;

        public TestThread(TestRunnable testRunnable)
        {
            super("TestThread");
            this.testRunnable = testRunnable;
        }

        @Override public void run()
        {
            Looper.prepare();
            if (testRunnable != null)
            {
                testRunnable.setup(Looper.myLooper());
            }
            while (mRunning)
            {
                if (testRunnable != null)
                {
                    testRunnable.update();
                }
                try
                {
                    Thread.sleep(25);
                } catch (Exception e)
                {
                }
            }
            Looper.loop();
        }
    }

    private interface TestRunnable
    {
        void setup(Looper looper);

        void update();
    }

    private class TaskRandomOrderStateListener implements P_Task.IStateListener
    {

        private final Semaphore semaphore;
        private final int taskCount;
        private P_TaskPriority curPriority;
        private int curFinishedTasks;


        public TaskRandomOrderStateListener(int taskCount, Semaphore sem)
        {
            this.taskCount = taskCount;
            semaphore = sem;
        }

        @Override public void onStateChanged(P_Task task, P_TaskState state)
        {
            if (state == P_TaskState.SUCCEEDED || state == P_TaskState.FAILED)
            {
                if (curPriority != null)
                {
                    assertTrue(task.getPriority().ordinal() <= curPriority.ordinal());
                    if (task.getPriority().ordinal() < curPriority.ordinal())
                    {
                        curPriority = task.getPriority();
                    }
                }
                else
                {
                    curPriority = task.getPriority();
                }
                curFinishedTasks++;
                if (curFinishedTasks >= taskCount)
                {
                    semaphore.release();
                }
            }
        }
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
