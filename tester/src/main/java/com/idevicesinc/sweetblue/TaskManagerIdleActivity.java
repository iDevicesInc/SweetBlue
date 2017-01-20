package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.idevicesinc.sweetblue.utils.BluetoothEnabler;

public class TaskManagerIdleActivity extends Activity
{

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        BluetoothEnabler.start(this);
    }

    Handler getHandler()
    {
        return handler;
    }
}
