package com.idevicesinc.sweetblue;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.DebugLogger;


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
        config.stopScanOnPause = false;
        config.bondRetryFilter = new BondRetryFilter.DefaultBondRetryFilter(5);
        config.scanApi = BleScanApi.POST_LOLLIPOP;
        config.defaultScanFilter = e -> BleManagerConfig.ScanFilter.Please.acknowledgeIf(e.name_normalized().contains("wall"));
        config.runOnMainThread = false;
        mgr = BleManager.get(this, config);
        mgr.setListener_Discovery(e ->
        {
            if (e.was(BleManager.DiscoveryListener.LifeCycle.DISCOVERED))
            {
                Log.e("+++", "Discovery Event: " + e);
            }
            else if (e.was(BleManager.DiscoveryListener.LifeCycle.REDISCOVERED))
            {

            }
        });
        mgr.getPostManager().getUIHandler().postDelayed(() -> mgr.startScan(), 1000);
        return super.onStartCommand(intent, flags, startId);
    }
}
