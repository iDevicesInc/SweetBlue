package com.idevicesinc.sweetblue;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.DebugLogger;
import com.idevicesinc.sweetblue.utils.Interval;

import java.util.UUID;


public class MyBleService extends Service
{
    @Override public IBinder onBind(Intent intent)
    {
        return null;
    }


    private BleManager mgr;

    @Override public int onStartCommand(Intent intent, int flags, int startId)
    {
        startForeground(1, new Notification());
        BleManagerConfig config = new BleManagerConfig();
        config.loggingEnabled = true;
        config.scanReportDelay = Interval.DISABLED;
        config.stopScanOnPause = false;
        config.bondRetryFilter = new BondRetryFilter.DefaultBondRetryFilter(5);
        config.scanApi = BleScanApi.POST_LOLLIPOP;
        config.defaultScanFilter = new BleManagerConfig.DefaultScanFilter(UUID.fromString("ba067250-9b6e-11e4-bd06-0800200c9a66"));
        config.runOnMainThread = false;
        mgr = BleManager.get(this, config);
        mgr.setListener_Discovery(e ->
        {
            if (e.was(BleManager.DiscoveryListener.LifeCycle.DISCOVERED))
            {
                Log.e("+++", "Discovered: " + e.device().getName_native());
                e.device().connect(e1 -> {
                    if (e1.didEnter(BleDeviceState.INITIALIZED))
                    {
                        Log.e("+++", "Connected to " + e1.device().getName_native());
                        e1.device().disconnect();
                    }
                });
            }
            else if (e.was(BleManager.DiscoveryListener.LifeCycle.REDISCOVERED))
            {

            }
        });
        mgr.getPostManager().getUIHandler().postDelayed(() -> mgr.startScan(), 1000);
        return super.onStartCommand(intent, flags, startId);
    }
}
