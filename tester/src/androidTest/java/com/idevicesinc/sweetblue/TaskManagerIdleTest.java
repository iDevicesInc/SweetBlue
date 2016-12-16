package com.idevicesinc.sweetblue;

import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
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
                assertTrue(bleManager.getUpdateRate() == bleManager.m_config.idleUpdateRate.millis());

                bleManager.startScan(); //Kick off scanning -> put something in the queue

                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        /**
                         * After waiting about 1 cycle check to see if the current speed is no longer idling
                         */
                        long updateSpeed = bleManager.getUpdateRate();

                        assertTrue(updateSpeed == bleManager.m_config.autoUpdateRate.millis());

                        bleManager.stopScan(); //Stop scanning -> empty the queue

                        handler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                /**
                                 * After waiting again check to see that the speed is idling again
                                 */
                                long updateSpeed = bleManager.getUpdateRate();

                                assertTrue(updateSpeed == bleManager.m_config.idleUpdateRate.millis());

                                bleManager.startScan(); //Kick off scanning -> put something in the queue once more

                                handler.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        /**
                                         * Check to see one last time that the the thread woke up
                                         */
                                        long updateSpeed = bleManager.getUpdateRate();

                                        assertTrue(updateSpeed == bleManager.m_config.autoUpdateRate.millis());

                                        finishedSemaphore.release();
                                    }

                                }, bleManager.m_config.autoUpdateRate.millis());
                            }

                        }, 2 * bleManager.m_config.minTimeToIdle.millis());/*Go a couple more ticks and wait for idle*/
                    }

                }, bleManager.m_config.autoUpdateRate.millis() + 5);
            }

        }, 2 * bleManager.m_config.minTimeToIdle.millis());

        finishedSemaphore.acquire();
    }

    @Test
    public void testTaskManagerIdleUsingStateChange() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.setListener_State(new ManagerStateListener()
        {
            @Override
            public void onEvent(BleManager.StateListener.StateEvent event)
            {
                if(event.didEnter(BleManagerState.IDLE))
                {
                    assertTrue(bleManager.getUpdateRate() == bleManager.m_config.idleUpdateRate.millis());

                    finishedSemaphore.release();
                }
                else
                {
                    assertTrue(bleManager.getUpdateRate() == bleManager.m_config.autoUpdateRate.millis());

                    bleManager.stopScan();
                }
            }
        });

        bleManager.startScan();

        finishedSemaphore.acquire();
    }

}
