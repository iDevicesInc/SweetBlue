package com.idevicesinc.sweetblue.tester;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.listeners.ManagerStateListener;

public class MainActivity extends AppCompatActivity
{

    BleManager mgr;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BleManagerConfig config = new BleManagerConfig();
        config.loggingEnabled = true;
        mgr = BleManager.get(this, config);
        mgr.setManagerStateListener(new ManagerStateListener()
        {
            @Override public void onEvent(StateEvent event)
            {
                int i = 0;
                i++;
            }
        });
        if (mgr.is(BleManagerState.OFF))
        {
            mgr.turnOn();
        }
    }
}
