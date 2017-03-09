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


    public P_NativeManagerLayer getManagerLayer()
    {
        return new UnitTestManagerLayer();
    }

    public P_NativeDeviceLayer getDeviceLayer(BleDevice device)
    {
        return new UnitTestDevice(device);
    }

    public P_GattLayer getGattLayer(BleDevice device)
    {
        return new UnitTestGatt(device);
    }


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
        m_config.nativeManagerLayer = getManagerLayer();
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return getGattLayer(device);
            }
        };
        m_config.nativeDeviceFactory = new P_NativeDeviceLayerFactory()
        {
            @Override public P_NativeDeviceLayer newInstance(BleDevice device)
            {
                return getDeviceLayer(device);
            }
        };
        m_config.logger = new UnitTestLogger();
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
