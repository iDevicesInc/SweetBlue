package com.idevicesinc.sweetblue;


import android.app.Activity;
import org.junit.After;
import org.junit.Before;
import org.robolectric.Robolectric;
import java.util.concurrent.Semaphore;


public abstract class BaseBleUnitTest
{

    public BleManager m_mgr;
    public BleManagerConfig m_config;

    public Activity m_activity;

    public abstract P_NativeManagerLayer getManagerLayer();


    @Before
    public void setup() throws Exception
    {
        m_activity = Robolectric.setupActivity(Activity.class);
        m_mgr = BleManager.get(m_activity, getConfig());
        m_mgr.forceOn();
        m_mgr.onResume();
    }

    @After
    public void tearDown() throws Exception
    {
        m_mgr.shutdown();
        m_activity.finish();
        m_activity = null;
    }

    public void doTestOperation(final TestOp action) throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        new Thread(new Runnable()
        {
            @Override public void run()
            {
                action.run(semaphore);
            }
        }).start();

        semaphore.acquire();
    }

    public BleManagerConfig getConfig()
    {
        m_config = new BleManagerConfig();
        m_config.unitTest = true;
        m_config.nativeManagerLayer = getManagerLayer();
        return m_config;
    }


    public BleScanApi getScanApi()
    {
        return m_mgr.getScanManager().getCurrentApi();
    }

    public BleScanPower getScanPower()
    {
        return m_mgr.getScanManager().getCurrentPower();
    }

    public interface TestOp
    {
        void run(Semaphore semaphore);
    }

}
