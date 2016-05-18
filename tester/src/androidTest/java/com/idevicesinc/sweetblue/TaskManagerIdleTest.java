package com.idevicesinc.sweetblue;

import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.idevicesinc.sweetblue.listeners.ManagerStateListener;

import org.junit.Test;

import java.util.concurrent.Semaphore;

public class TaskManagerIdleTest extends ActivityInstrumentationTestCase2<TaskManagerIdleActivity>
{
    TaskManagerIdleActivity testActivity;

    BleManager bleManager;

    Handler handler;

    public TaskManagerIdleTest()
    {
        super(TaskManagerIdleActivity.class);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        testActivity = getActivity();

        bleManager = testActivity.getBleManager();

        handler = testActivity.getHandler();
    }

    @Test
    public void testTaskManagerIdleUsingDelays() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                /**
                 * After waiting the appropriate time check to see if the current speed is the idle speed
                 */
                assertTrue(bleManager.getUpdateSpeed() == bleManager.mConfig.updateThreadIdleIntervalMs);

                bleManager.startScan(); //Kick off scanning -> put something in the queue

                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        /**
                         * After waiting about 1 cycle check to see if the current speed is no longer idling
                         */
                        long updateSpeed = bleManager.getUpdateSpeed();

                        assertTrue(updateSpeed == bleManager.mConfig.updateThreadSpeed.getMilliseconds());

                        bleManager.stopScan(); //Stop scanning -> empty the queue

                        handler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                /**
                                 * After waiting again check to see that the speed is idling again
                                 */
                                long updateSpeed = bleManager.getUpdateSpeed();

                                assertTrue(updateSpeed == bleManager.mConfig.updateThreadIdleIntervalMs);

                                bleManager.startScan(); //Kick off scanning -> put something in the queue once more

                                handler.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        /**
                                         * Check to see one last time that the the thread woke up
                                         */
                                        long updateSpeed = bleManager.getUpdateSpeed();

                                        assertTrue(updateSpeed == bleManager.mConfig.updateThreadSpeed.getMilliseconds());

                                        finishedSemaphore.release();
                                    }

                                }, bleManager.mConfig.updateThreadSpeed.getMilliseconds());
                            }

                        }, 2 * bleManager.mConfig.delayBeforeIdleMs);/*Go a couple more ticks and wait for idle*/
                    }

                }, bleManager.mConfig.updateThreadSpeed.getMilliseconds() + 5);
            }

        }, 2 * bleManager.mConfig.delayBeforeIdleMs);

        finishedSemaphore.acquire();
    }

    @Test
    public void testTaskManagerIdleUsingStateChange() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.setManagerStateListener(new ManagerStateListener()
        {
            @Override
            public void onEvent(StateEvent event)
            {
                if(event.didEnter(BleManagerState.IDLE))
                {
                    assertTrue(bleManager.getUpdateSpeed() == bleManager.mConfig.updateThreadIdleIntervalMs);

                    finishedSemaphore.release();
                }
                else
                {
                    assertTrue(bleManager.getUpdateSpeed() == bleManager.mConfig.updateThreadSpeed.getMilliseconds());

                    bleManager.stopScan();
                }
            }
        });

        bleManager.startScan();

        finishedSemaphore.acquire();
    }

}
