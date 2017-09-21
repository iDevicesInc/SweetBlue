package com.idevicesinc.sweetblue;


import android.app.Activity;
import org.junit.After;
import org.junit.Before;
import org.robolectric.Robolectric;


public abstract class BaseBleUnitTest extends BaseTest
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
        new Thread(() -> action.run()).start();

        reacquire();
    }

    public BleManagerConfig getConfig()
    {
        m_config = new BleManagerConfig();
        m_config.nativeManagerLayer = getManagerLayer();
        m_config.gattLayerFactory = this::getGattLayer;
        m_config.nativeDeviceFactory = this::getDeviceLayer;
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

    @Override
    int getTraceIndex()
    {
        return 3;
    }

    public interface TestOp
    {
        void run();
    }

}
