package com.idevicesinc.sweetblue;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;


@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class DeviceManagerTest extends BaseBleUnitTest
{


    @Test(timeout = 20000)
    public void removeDevicesFromCacheTest() throws Exception
    {
        final long m_timeStarted = System.currentTimeMillis();
        final Semaphore s = new Semaphore(0);
        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);
        new Thread(new Runnable()
        {
            @Override public void run()
            {
                while (m_timeStarted + 10000 > System.currentTimeMillis())
                {
                    m_mgr.newDevice(UnitTestUtils.randomMacAddress());
                    try
                    {
                        Thread.sleep(25);
                    } catch (Exception e)
                    {
                    }
                }
            }
        }).start();
        new Thread(new Runnable()
        {
            @Override public void run()
            {
                while (m_timeStarted + 10000 > System.currentTimeMillis())
                {
                    try
                    {
                        Thread.sleep(500);
                    } catch (Exception e)
                    {
                    }
                    m_mgr.removeAllDevicesFromCache();
                }
                s.release();
            }
        }).start();
        s.acquire();
    }

}
