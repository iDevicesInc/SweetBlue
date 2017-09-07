package com.idevicesinc.sweetblue;


import android.util.Log;
import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;

import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class ScanTest extends BaseTester<MainActivity>
{

    private static final String TAG = "ScanTest";


    @Test
    public void preLollipopScanTest() throws Exception
    {
        doScanTest("preL", Interval.TEN_SECS, BleScanApi.PRE_LOLLIPOP, BleScanPower.AUTO, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY), Interval.secs(.5));
    }

    @Test
    public void postLollipopMediumTest() throws Exception
    {
        doScanTest("postLMed", Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.MEDIUM_POWER, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY), Interval.secs(.5));
    }

    @Test
    public void postLollipopMediumNoBatchDelayTest() throws Exception
    {
        doScanTest("postLMedNoBatch", Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.MEDIUM_POWER, Interval.DISABLED, Interval.secs(.5));
    }

    @Test
    public void postLollipopHighTest() throws Exception
    {
        doScanTest("postLHigh", Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.HIGH_POWER, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY), Interval.secs(.5));
    }

    @Test
    public void postLollipopHighNoBatchDelayTest() throws Exception
    {
        doScanTest("postLHighNoBatch", Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.HIGH_POWER, Interval.DISABLED, Interval.secs(.5));
    }

    @Test
    public void preLollipopScanTestNoBoost() throws Exception
    {
        doScanTest("preLNoBoost", Interval.TEN_SECS, BleScanApi.PRE_LOLLIPOP, BleScanPower.AUTO, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY), Interval.DISABLED);
    }

    @Test
    public void postLollipopMediumTestNoBoost() throws Exception
    {
        doScanTest("postLMedNoBoost", Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.MEDIUM_POWER, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY), Interval.DISABLED);
    }

    @Test
    public void postLollipopMediumNoBatchDelayTestNoBoost() throws Exception
    {
        doScanTest("postLMedNoBatchNoBoost", Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.MEDIUM_POWER, Interval.DISABLED, Interval.DISABLED);
    }

    @Test
    public void postLollipopHighTestNoBoost() throws Exception
    {
        doScanTest("postLHighNoBoost", Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.HIGH_POWER, Interval.secs(BleManagerConfig.DEFAULT_SCAN_REPORT_DELAY), Interval.DISABLED);
    }

    @Test
    public void postLollipopHighNoBatchDelayTestNoBoost() throws Exception
    {
        doScanTest("postLHighNoBatchNoBoost", Interval.TEN_SECS, BleScanApi.POST_LOLLIPOP, BleScanPower.HIGH_POWER, Interval.DISABLED, Interval.DISABLED);
    }

    private void doScanTest(final String name, Interval scanLength, BleScanApi scanApi, BleScanPower scanPower, Interval batchDelay, Interval boostScanTime) throws Exception
    {
        final Semaphore s = new Semaphore(0);

        final AtomicInteger deviceCount = new AtomicInteger(0);
        final AtomicLong timeStarted = new AtomicLong(0);
        final AtomicLong timeBeforeFirstDeviceDiscovered = new AtomicLong(0);
        final AtomicInteger weakestRSSI = new AtomicInteger(0);
        mConfig.scanApi = scanApi;
        mConfig.scanPower = scanPower;
        mConfig.scanReportDelay = batchDelay;
        mConfig.scanClassicBoostLength = boostScanTime;
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

                    if(e.rssi() < weakestRSSI.get())
                    {
                        weakestRSSI.set(e.rssi());
                    }
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
                    Log.e(name+TAG, "\t" +
                            (System.currentTimeMillis() - timeStarted.get()) + "\t" +  // Total time
                            timeBeforeFirstDeviceDiscovered.get() + "\t" +  // Time to discover 1st device
                            deviceCount.get() + "\t" +  // # of devices found
                            weakestRSSI.get());  // Weakest RSSI discovered
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
        return config;
    }
}
