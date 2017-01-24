package com.idevicesinc.sweetblue;


import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;

import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.Semaphore;


public class ConnectBlitzer extends BaseTester<MainActivity>
{

    private final static int MAX_COUNT = 50;

    final FailListener2 listener = new FailListener2();
    BleDevice device;
    private Semaphore s;
    private int connectCount;
    private boolean wasConnected = false;


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
                return Please.acknowledgeIf(e.name_native().equalsIgnoreCase("switch-000000"));
            }
        };
        config.useGattRefresh = false;
        config.loggingEnabled = true;
        config.runOnMainThread = false;
        return config;
    }

    @Test
    public void connectBlitz() throws Exception
    {
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UIUtil.handleBluetoothEnablerDialogs(uiDevice, activity);
        s = new Semaphore(0);

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
                    wasConnected = true;
                    connectCount++;
                    device.disconnect();
                }
                else if (e.didEnter(BleDeviceState.DISCONNECTED))
                {
                    if (e.device() == device && wasConnected)
                    {
                        wasConnected = false;
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
            }
        }, listener);
    }

    private class FailListener2 extends BleDevice.DefaultConnectionFailListener
    {
        @Override public Please onEvent(ConnectionFailEvent e)
        {
            Please p = super.onEvent(e);
            if (!p.isRetry())
            {
                Log.e("ConnectFail", "Connection failed with status " + e.status().name() + ". Connected successfully " + connectCount + " times. Retried " + e.failureCountSoFar() + " times.");
//                assertTrue("Connection failed with status " + e.status().name() + ". Connected successfully " + connectCount + " times. Retried " + e.failureCountSoFar() + " times.", false);
            }
            return p;
//            return Please.doNotRetry();
        }
    }

    private class FailListener implements BleDevice.ConnectionFailListener
    {

        @Override public Please onEvent(ConnectionFailEvent e)
        {
            if (e.device() == device)
            {
                //assertTrue("Connection failed with status " + e.status().name() + ". Connected successfully " + connectCount + " times. Retried " + e.failureCountSoFar() + " times.", false);
                return null;
            }
            else
            {
                return null;
            }
        }
    }

}
