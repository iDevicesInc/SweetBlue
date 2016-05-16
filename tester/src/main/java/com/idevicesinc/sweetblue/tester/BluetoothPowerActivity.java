package com.idevicesinc.sweetblue.tester;

import android.app.Activity;
import android.os.Bundle;

import com.idevicesinc.sweetblue.BleManager;

public class BluetoothPowerActivity extends Activity
{
    BleManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }

    public void turnBluetoothOn()
    {
        manager = BleManager.get(this);

        manager.turnOn();
    }

    public void turnBluetoothOff()
    {
        manager = BleManager.get(this);

        manager.turnOff();
    }
}


