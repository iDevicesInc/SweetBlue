package com.idevicesinc.sweetblue;

import android.os.Handler;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.Semaphore;


public class TaskManagerIdleTest extends BaseTester<TaskManagerIdleActivity>
{

    Handler handler;


    @Override
    public void additionalSetup()
    {
        handler = activity.getHandler();
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
                assertTrue(mgr.getUpdateRate() == mgr.m_config.idleUpdateRate.millis());

                mgr.startScan(); //Kick off scanning -> put something in the queue

                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        /**
                         * After waiting about 1 cycle check to see if the current speed is no longer idling
                         */
                        long updateSpeed = mgr.getUpdateRate();

                        assertTrue(updateSpeed == mgr.m_config.autoUpdateRate.millis());

                        mgr.stopScan(); //Stop scanning -> empty the queue

                        handler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                /**
                                 * After waiting again check to see that the speed is idling again
                                 */
                                long updateSpeed = mgr.getUpdateRate();

                                assertTrue(updateSpeed == mgr.m_config.idleUpdateRate.millis());

                                mgr.startScan(); //Kick off scanning -> put something in the queue once more

                                handler.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        /**
                                         * Check to see one last time that the the thread woke up
                                         */
                                        long updateSpeed = mgr.getUpdateRate();

                                        assertTrue(updateSpeed == mgr.m_config.autoUpdateRate.millis());

                                        finishedSemaphore.release();
                                    }

                                }, mgr.m_config.autoUpdateRate.millis() + 50);
                            }

                        }, 2 * mgr.m_config.minTimeToIdle.millis());/*Go a couple more ticks and wait for idle*/
                    }

                }, mgr.m_config.autoUpdateRate.millis() + 50);
            }

        }, 2 * mgr.m_config.minTimeToIdle.millis());

        finishedSemaphore.acquire();
    }

    @Test
    public void testTaskManagerIdleUsingStateChange() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override
            public void onEvent(BleManager.StateListener.StateEvent event)
            {
                if (event.didEnter(BleManagerState.IDLE))
                {
                    assertTrue(mgr.getUpdateRate() == mgr.m_config.idleUpdateRate.millis());

                    finishedSemaphore.release();
                }
                else
                {
                    assertTrue(mgr.getUpdateRate() == mgr.m_config.autoUpdateRate.millis());

                    mgr.stopScan();
                }
            }
        });

        mgr.startScan();

        finishedSemaphore.acquire();
    }

    @Override Class<TaskManagerIdleActivity> getActivityClass()
    {
        return TaskManagerIdleActivity.class;
    }

    @Override BleManagerConfig getInitialConfig()
    {
        return new BleManagerConfig();
    }
}
