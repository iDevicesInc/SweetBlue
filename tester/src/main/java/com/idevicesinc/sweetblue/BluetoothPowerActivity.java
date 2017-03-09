package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.os.Bundle;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;


public class BluetoothPowerActivity extends Activity
{
    BleManager manager;
    public boolean ready = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter() {
            @Override public Please onEvent(BluetoothEnablerEvent e)
            {
                if (e.isDone())
                {
                    ready = true;
                }
                return super.onEvent(e);
            }
        });
    }

    public void turnBluetoothOn()
    {
        getManager();

        manager.turnOn();
    }

    public BleManager getManager()
    {
        if (manager == null)
        {
            BleManagerConfig config = new BleManagerConfig();
            config.runOnMainThread = false;
            manager = BleManager.get(this, config);
        }
        return manager;
    }

    public void turnBluetoothOff()
    {
        getManager();

        manager.turnOff();
    }
}


