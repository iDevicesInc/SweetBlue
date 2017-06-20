package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class DeviceManagerTest extends BaseBleUnitTest
{


    @Test(timeout = 20000)
    public void removeDevicesFromCacheTest() throws Exception
    {
        final long m_timeStarted = System.currentTimeMillis();
        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);
        new Thread(new Runnable()
        {
            @Override public void run()
            {
                while (m_timeStarted + 10000 > System.currentTimeMillis())
                {
                    m_mgr.newDevice(Util.randomMacAddress());
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
                succeed();
            }
        }).start();
        startTest();
    }

}
