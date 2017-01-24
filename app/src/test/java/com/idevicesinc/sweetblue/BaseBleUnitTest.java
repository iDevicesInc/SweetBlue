package com.idevicesinc.sweetblue;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.After;
import org.junit.Before;
import org.robolectric.Robolectric;
import java.lang.reflect.Method;
import java.util.concurrent.Semaphore;


public abstract class BaseBleUnitTest
{

    public BleManager m_mgr;
    public BleManagerConfig m_config;
    Activity m_activity;


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
        BleScanApi mode = BleScanApi.AUTO;
        try
        {
            Method getMode = BleManagerState.SCANNING.getClass().getDeclaredMethod("getScanApi", (Class[]) null);
            getMode.setAccessible(true);
            mode = (BleScanApi) getMode.invoke(BleManagerState.SCANNING, (Object[]) null);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return mode;
    }

    public BleScanPower getScanPower()
    {
        BleScanPower power = BleScanPower.AUTO;
        try
        {
            Method getPower = BleManagerState.SCANNING.getClass().getDeclaredMethod("getScanPower", (Class[]) null);
            getPower.setAccessible(true);
            power = (BleScanPower) getPower.invoke(BleManagerState.SCANNING, (Object[]) null);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return power;
    }

    public interface TestOp
    {
        void run(Semaphore semaphore);
    }

}
