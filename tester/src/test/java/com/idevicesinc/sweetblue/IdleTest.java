package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class IdleTest extends BaseBleUnitTest
{

    @Test(timeout = 4000)
    public void enterIdleTest() throws Exception
    {
        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;

        m_config.minTimeToIdle = Interval.secs(2.0);

        m_mgr.setConfig(m_config);

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(ManagerStateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.IDLE))
                {
                    assertTrue(m_mgr.getUpdateRate() == m_config.idleUpdateRate.millis());
                    succeed();
                }
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 6000)
    public void exitIdleTest() throws Exception
    {
        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.minTimeToIdle = Interval.ONE_SEC;

        m_mgr.setConfig(m_config);

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(ManagerStateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.IDLE))
                {
                    assertTrue(m_mgr.getUpdateRate() == m_config.idleUpdateRate.millis());
                    m_mgr.startScan();
                }
                else if (e.didExit(BleManagerState.IDLE))
                {
                    assertTrue(m_mgr.getUpdateRate() == m_config.autoUpdateRate.millis());
                    succeed();
                }
            }
        });

        startAsyncTest();
    }

}
