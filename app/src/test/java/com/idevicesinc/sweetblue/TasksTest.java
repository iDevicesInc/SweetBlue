package com.idevicesinc.sweetblue;


import android.app.Activity;
import android.os.Looper;
import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 23)
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
    public void testRandomOrder() throws Exception
    {
        final Semaphore mSemaphore = new Semaphore(0);
        final AtomicBoolean mKeepRunning = new AtomicBoolean(true);

        new UnitTestThread(new UnitTestRunnable()
        {

            P_TaskManager mManager;

            @Override public BleManager setup(Looper looper)
            {
                BleManager bleMgr = BleManager.get(mActivity, getConfig());
                mManager = bleMgr.mTaskManager;
                int taskCount = 1000;
                final TaskRandomOrderStateListener listener = new TaskRandomOrderStateListener(taskCount, mSemaphore, mKeepRunning);
                Random r = new Random();
                for (int i = 0; i < taskCount; i++)
                {
                    P_Task_Looper loop = new P_Task_Looper(mManager, listener, P_TaskPriority.values()[r.nextInt(6)]);
                    mManager.add(loop);
                }
                return bleMgr;
            }

            @Override public boolean update()
            {
                return mKeepRunning.get();
            }
        }).start();

        mSemaphore.acquire();
    }

    @Test
    public void testRequeuing() throws Exception
    {
        final Semaphore mSemaphore = new Semaphore(0);
        final AtomicBoolean mKeepRunning = new AtomicBoolean(true);

        new UnitTestThread(new UnitTestRunnable()
        {

            P_TaskManager mManager;

            @Override public BleManager setup(Looper looper)
            {
                BleManager bleMgr = BleManager.get(mActivity, getConfig());
                mManager = bleMgr.mTaskManager;
                P_Task_LongRunner longer = new P_Task_LongRunner(mManager, new P_Task.IStateListener()
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
                            mSemaphore.release();
                            mKeepRunning.set(false);
                        }
                        if (state == P_TaskState.EXECUTING)
                        {
                            executed = true;
                            assertTrue(!requeued);
                            assertTrue(!interrupted);
                        }
                    }
                }, P_TaskPriority.MEDIUM);
                mManager.add(longer);
                int taskCount = 50;

                Random r = new Random();
                for (int i = 0; i < taskCount; i++)
                {
                    P_Task_Looper loop = new P_Task_Looper(mManager, null, P_TaskPriority.values()[r.nextInt(5)]);
                    mManager.add(loop);
                }
                return bleMgr;
            }

            int count = 0;
            boolean addedHigherTasks = false;

            @Override public boolean update()
            {
                count++;
                if (count > 25 && !addedHigherTasks)
                {
                    addedHigherTasks = true;
                    P_Task_Looper loop = new P_Task_Looper(mManager, null, P_TaskPriority.HIGH);
                    mManager.add(loop);
                }
                return mKeepRunning.get();
            }
        }).start();

        mSemaphore.acquire();
    }

    @Test
    public void testTimeOut() throws Exception
    {
        final Semaphore mSemaphore = new Semaphore(0);

        final Interval TASK_TIMEOUT = Interval.FIVE_SECS;
        final int MIN_GOAL = (int) TASK_TIMEOUT.millis() - 50;
        final int MAX_GOAL = (int) TASK_TIMEOUT.millis() + 50;

        new UnitTestThread(new UnitTestRunnable()
        {

            P_TaskManager mManager;
            boolean mKeepRunning = true;

            @Override public BleManager setup(Looper looper)
            {
                BleManagerConfig config = getConfig();
                config.taskTimeout = TASK_TIMEOUT;
                BleManager bleMgr = BleManager.get(mActivity, config);
                mManager = bleMgr.mTaskManager;
                P_Task_LongRunner task = new P_Task_LongRunner(mManager, new P_Task.IStateListener()
                {
                    long time;

                    @Override public void onStateChanged(P_Task task, P_TaskState state)
                    {
                        if (state == P_TaskState.EXECUTING)
                        {
                            time = System.currentTimeMillis();
                        }
                        if (state == P_TaskState.TIMED_OUT)
                        {
                            time = System.currentTimeMillis() - time;
                            assertTrue(time > MIN_GOAL && time < MAX_GOAL);
                            mSemaphore.release();
                            mKeepRunning = false;
                        }
                    }
                }, P_TaskPriority.HIGH);
                mManager.add(task);

                return bleMgr;
            }

            @Override public boolean update()
            {
                return mKeepRunning;
            }
        }).start();

        mSemaphore.acquire();
    }

    private static BleManagerConfig getConfig()
    {
        BleManagerConfig config = new BleManagerConfig();
        config.updateLooper = Looper.myLooper();
        return config;
    }

    private class TaskRandomOrderStateListener implements P_Task.IStateListener
    {

        private final Semaphore mSemaphore;
        private final int mTaskCount;
        private P_TaskPriority mCurPriority;
        private int mCurFinishedTasks;
        private final AtomicBoolean mKeepRunning;


        public TaskRandomOrderStateListener(int taskCount, Semaphore sem, AtomicBoolean keepRunning)
        {
            this.mTaskCount = taskCount;
            mSemaphore = sem;
            this.mKeepRunning = keepRunning;
        }

        @Override public void onStateChanged(P_Task task, P_TaskState state)
        {
            if (state == P_TaskState.SUCCEEDED || state == P_TaskState.FAILED)
            {
                if (mCurPriority != null)
                {
                    assertTrue(task.getPriority().ordinal() <= mCurPriority.ordinal());
                    if (task.getPriority().ordinal() < mCurPriority.ordinal())
                    {
                        mCurPriority = task.getPriority();
                    }
                }
                else
                {
                    mCurPriority = task.getPriority();
                }
                mCurFinishedTasks++;
                if (mCurFinishedTasks >= mTaskCount)
                {
                    mSemaphore.release();
                    mKeepRunning.set(false);
                }
            }
        }
    }

}
