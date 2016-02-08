package com.idevicesinc.sweetblue.tests;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.PI_BleScanner;
import com.idevicesinc.sweetblue.PI_BleStatusHelper;
import com.idevicesinc.sweetblue.PI_UpdateLoop;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Before;
import org.robolectric.Robolectric;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertNotNull;


public abstract class BaseBleTest
{

    BleManager m_mgr;

    @Before
    public void setup()
    {
        Activity activity = Robolectric.setupActivity(Activity.class);
        BleManagerConfig sConfig = new BleManagerConfig();
        sConfig.allowCallsFromAllThreads = true;
        sConfig.updateLoopFactory = new TestUpdateLoopFactory();
        sConfig.bleScanner = getScanner();
        sConfig.bleStatusHelper = getStatusHelper();
        final BleManager mgr = BleManager.get(activity, sConfig);
        assertNotNull(mgr);
        m_mgr = mgr;
        m_mgr.onResume();
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

    abstract PI_BleScanner getScanner();
    abstract PI_BleStatusHelper getStatusHelper();

    interface TestOp
    {
        void run(Semaphore semaphore);
    }

    private static class TestUpdateLoopFactory implements PI_UpdateLoop.IUpdateLoopFactory {

        @Override public PI_UpdateLoop newAnonThreadLoop()
        {
            return new UnitLoop(new PI_UpdateLoop.Callback()
            {
                @Override public void onUpdate(double timestep_seconds)
                {
                }
            });
        }

        @Override public PI_UpdateLoop newMainThreadLoop(PI_UpdateLoop.Callback callback)
        {
            return new UnitLoop(callback);
        }

        @Override public PI_UpdateLoop newAnonThreadLoop(PI_UpdateLoop.Callback callback)
        {
            return new UnitLoop(callback);
        }
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
