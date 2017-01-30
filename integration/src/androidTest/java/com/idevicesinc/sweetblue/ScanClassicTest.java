package com.idevicesinc.sweetblue;


import android.support.test.filters.FlakyTest;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.Interval;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class ScanClassicTest extends BaseTester<MainActivity>
{
    @Override Class getActivityClass()
    {
        return MainActivity.class;
    }

    @Override BleManagerConfig getInitialConfig()
    {
        BleManagerConfig config = new BleManagerConfig();
        config.runOnMainThread = false;
        config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().contains("Tag"));
            }
        };
        return config;
    }

    @Test
    public void scanWithOutClassicBoost() throws Exception
    {
        final Semaphore s = new Semaphore(0);
        final AtomicLong mScanStarted = new AtomicLong();
        final List<BleDevice> devices = new ArrayList<>();
        mConfig.scanClassicBoostLength = Interval.DISABLED;
        mgr.setConfig(mConfig);
        mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    if (!devices.contains(e.device()))
                    {
                        devices.add(e.device());
                        if (devices.size() == 5)
                        {
                            long diff = System.currentTimeMillis() - mScanStarted.get();
                            mgr.stopScan();
                            Log.e("ScanTest", "Found all 5 devices in " + diff + " ms.\n");
                            s.release();
                        }
                    }
                }
            }
        });
        mScanStarted.set(System.currentTimeMillis());
        mgr.startScan();
        s.acquire();
    }

    @Test
    public void scanWithClassicBoost() throws Exception
    {
        final Semaphore s = new Semaphore(0);
        final AtomicLong mScanStarted = new AtomicLong();
        final List<BleDevice> devices = new ArrayList<>();
        mConfig.scanClassicBoostLength = Interval.secs(0.5);
        mgr.setConfig(mConfig);
        mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    if (!devices.contains(e.device()))
                    {
                        devices.add(e.device());
                        if (devices.size() == 5)
                        {
                            long diff = System.currentTimeMillis() - mScanStarted.get();
                            mgr.stopScan();
                            Log.e("ScanTest", "Found all 5 devices in " + diff + " ms.\n");
                            s.release();
                        }
                    }
                }
            }
        });
        mScanStarted.set(System.currentTimeMillis());
        mgr.startScan();
        s.acquire();
    }

}
