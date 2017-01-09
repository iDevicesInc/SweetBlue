package com.idevicesinc.sweetblue;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.Semaphore;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class ConnectBlitzer
{

    private final static int MAX_COUNT = 500;

    TaskManagerIdleActivity activity;
    BleManager mgr;
    BleDevice device;
    private Semaphore s;
    private int connectCount;


    @Rule
    public ActivityTestRule<TaskManagerIdleActivity> mRule = new ActivityTestRule<>(TaskManagerIdleActivity.class);


    @Before
    public void setup()
    {
        activity = mRule.getActivity();
    }

    @Test
    public void connectBlitz() throws Exception
    {
        s = new Semaphore(0);
        mgr = activity.getBleManager();
        BleManagerConfig config = new BleManagerConfig();
        config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().equalsIgnoreCase("switch-000000"));
            }
        };
//        config.useGattRefresh = true;
        config.runOnMainThread = false;
        mgr.setConfig(config);
        mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    mgr.stopScan();
                    device = e.device();
                    doConnect();
                }
            }
        });
        mgr.setListener_ConnectionFail(new BleDevice.ConnectionFailListener()
        {
            @Override public Please onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent e)
            {
                assertTrue("Connection failed with status " + e.status().name() + ". Connected successfully " + connectCount + " times.", false);
                return Please.doNotRetry();
            }
        });
        mgr.startScan();
        s.acquire();
    }

    public void doConnect()
    {
        device.connect(new BleDevice.StateListener()
        {
            @Override public void onEvent(StateEvent e)
            {
                if (e.didEnter(BleDeviceState.INITIALIZED))
                {
                    connectCount++;
                    device.disconnect();
                }
                else if (e.didEnter(BleDeviceState.DISCONNECTED))
                {
                    if (connectCount < MAX_COUNT)
                    {
                        doConnect();
                    }
                    else
                    {
                        Log.d("ConnectBlitzer", "Successfully connected to device " + connectCount + " times.");
                        s.release();
                    }
                }
            }
        });
    }

}
