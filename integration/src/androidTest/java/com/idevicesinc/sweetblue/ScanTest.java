package com.idevicesinc.sweetblue;


import android.util.Log;
import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class ScanTest extends BaseTester<MainActivity>
{

    private static final String TAG = "ScanTest";


    @Test
    public void preLollipopScanTest() throws Exception
    {
        doScanTest(Interval.TEN_SECS, BleScanApi.PRE_LOLLIPOP, BleScanPower.AUTO, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY));
    }

    @Test
    public void postLollipopMediumTest() throws Exception
    {
        doScanTest(Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.MEDIUM_POWER, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY));
    }

    @Test
    public void postLollipopMediumNoBatchDelayTest() throws Exception
    {
        doScanTest(Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.MEDIUM_POWER, Interval.DISABLED);
    }

    @Test
    public void postLollipopHighTest() throws Exception
    {
        doScanTest(Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.HIGH_POWER, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY));
    }

    @Test
    public void postLollipopHighNoBatchDelayTest() throws Exception
    {
        doScanTest(Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.HIGH_POWER, Interval.DISABLED);
    }



    private void doScanTest(Interval scanLength, BleScanApi scanApi, BleScanPower scanPower, Interval batchDelay) throws Exception
    {
        final Semaphore s = new Semaphore(0);

        final AtomicInteger deviceCount = new AtomicInteger(0);
        final AtomicLong timeStarted = new AtomicLong(0);
        final AtomicLong timeBeforeFirstDeviceDiscovered = new AtomicLong(0);
        mConfig.scanApi = scanApi;
        mConfig.scanPower = scanPower;
        mConfig.scanReportDelay = batchDelay;
        mgr.setConfig(mConfig);

        mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    if (deviceCount.get() == 0)
                    {
                        timeBeforeFirstDeviceDiscovered.set(System.currentTimeMillis() - timeStarted.get());
                    }
                    deviceCount.incrementAndGet();
                }
            }
        });

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override
            public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didExit(BleManagerState.SCANNING))
                {
                    Log.e(TAG, "Pre-Lollipop scan:\nTime to find first device: " + timeBeforeFirstDeviceDiscovered.get() + "ms\n" +
                            "Scan finished with " + deviceCount.get() + " devices found.");
                    s.release();
                }
                else if (e.didEnter(BleManagerState.SCANNING))
                {
                    timeStarted.set(System.currentTimeMillis());
                }
            }
        });

        mgr.startScan(scanLength);
        s.acquire();
    }


    @Override
    Class<MainActivity> getActivityClass()
    {
        return MainActivity.class;
    }

    @Override
    BleManagerConfig getInitialConfig()
    {
        BleManagerConfig config = new BleManagerConfig();
        config.runOnMainThread = false;
        config.loggingEnabled = true;
        return null;
    }
}
