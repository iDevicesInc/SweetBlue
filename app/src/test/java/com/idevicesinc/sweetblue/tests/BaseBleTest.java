package com.idevicesinc.sweetblue.tests;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.BleScanPower;
import com.idevicesinc.sweetblue.PI_BleScanner;
import com.idevicesinc.sweetblue.PI_BleStatusHelper;
import com.idevicesinc.sweetblue.PI_UpdateLoop;
import com.idevicesinc.sweetblue.UnitLoop;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;

import org.junit.Before;
import org.robolectric.Robolectric;

import java.lang.reflect.Method;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertNotNull;


public abstract class BaseBleTest
{

    BleManager m_mgr;
    BleManagerConfig m_config;
    Activity m_activity;


    abstract PI_BleScanner getScanner();

    abstract PI_BleStatusHelper getStatusHelper();

    @Before
    public void setup()
    {
        m_activity = Robolectric.setupActivity(Activity.class);
        m_config = new BleManagerConfig();
        m_config.postCallbacksToMainThread = false;
        m_config.autoUpdateRate = Interval.DISABLED;
        m_config.bleScanner = getScanner();
        m_config.bleStatusHelper = getStatusHelper();
        final BleManager mgr = BleManager.get(m_activity, m_config);
        assertNotNull(mgr);
        m_mgr = mgr;
        m_mgr.onResume();
        UnitLoop looper = new UnitLoop(m_mgr);
    }

    void doTestOperation(final TestOp action) throws Exception
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

    interface TestOp
    {
        void run(Semaphore semaphore);
    }


    public static class DefaultBleScannerTest implements PI_BleScanner
    {

        @Override public boolean startClassicDiscovery()
        {
            return true;
        }

        @Override public void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
        {

        }

        @Override public void startMScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
        {

        }

        @Override public boolean startLeScan(BluetoothAdapter.LeScanCallback callback)
        {
            return true;
        }

        @Override public void stopLeScan(BluetoothAdapter.LeScanCallback callback)
        {

        }
    }

    public static class DefaultStatusHelperTest implements PI_BleStatusHelper
    {

        @Override public boolean isLocationEnabledForScanning_byOsServices()
        {
            return true;
        }

        @Override public boolean isLocationEnabledForScanning_byRuntimePermissions()
        {
            return true;
        }

        @Override public boolean isLocationEnabledForScanning()
        {
            return true;
        }

        @Override public boolean isBluetoothEnabled()
        {
            return true;
        }
    }

}
