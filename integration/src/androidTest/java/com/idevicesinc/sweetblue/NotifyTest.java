package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;

import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertFalse;


public class NotifyTest extends BaseTester<MainActivity>
{
    @Override Class<MainActivity> getActivityClass()
    {
        return MainActivity.class;
    }

    @Override BleManagerConfig getInitialConfig()
    {
        BleManagerConfig config = new BleManagerConfig();
        config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().contains("Switch-000000"));
            }
        };
        return config;
    }

    @Test
    public void isNotifyEnabledTest() throws Exception
    {
        final Semaphore s = new Semaphore(0);
        mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    mgr.stopScan();
                    boolean enabled = e.device().isNotifyEnabled(Uuids.BATTERY_SERVICE_UUID);
                    assertFalse(enabled);
                    s.release();
                }
            }
        });
        mgr.startScan();
        s.acquire();
    }
}
