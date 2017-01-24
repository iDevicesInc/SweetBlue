package com.idevicesinc.sweetblue;


import org.junit.Test;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.Semaphore;


public class TaskManagerIdleTest extends BaseTester<TestActivity>
{


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
                else if (event.didEnter(BleManagerState.SCANNING))
                {
                    assertTrue(mgr.getUpdateRate() == mgr.m_config.autoUpdateRate.millis());

                    mgr.stopScan();
                }
            }
        });

        mgr.startScan();

        finishedSemaphore.acquire();
    }

    @Override Class<TestActivity> getActivityClass()
    {
        return TestActivity.class;
    }

    @Override BleManagerConfig getInitialConfig()
    {
        BleManagerConfig config = new BleManagerConfig();
        config.runOnMainThread = false;
        return config;
    }
}
