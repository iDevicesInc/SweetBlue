package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

public class TaskManagerIdleActivity extends Activity
{
    BleManager bleManager;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BleManagerConfig config = new BleManagerConfig();

        config.updateThreadSpeed = UpdateThreadSpeed.TWO_HUNDRED_FIFTY_MS;

        config.delayBeforeIdleMs = 1000;

        config.updateThreadIdleIntervalMs = 1000;

        bleManager = BleManager.get(this, config);

        handler = new Handler();
    }

    BleManager getBleManager()
    {
        return bleManager;
    }

    Handler getHandler()
    {
        return handler;
    }
}
