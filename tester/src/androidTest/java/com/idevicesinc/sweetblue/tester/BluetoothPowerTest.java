package com.idevicesinc.sweetblue.tester;

import android.bluetooth.BluetoothAdapter;
import android.test.ActivityInstrumentationTestCase2;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;

import org.junit.Test;

public class BluetoothPowerTest extends ActivityInstrumentationTestCase2<BluetoothPowerActivity>
{
    BluetoothPowerActivity testActivity;

    BleManager bleManager;

    BluetoothAdapter bleAdapter;

    public BluetoothPowerTest()
    {
        super(BluetoothPowerActivity.class);
    }


    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        testActivity = getActivity();

        bleManager = BleManager.get(testActivity);
    }

    @Test
    public void testBluetoothPower()
    {
        assertNotNull(testActivity);

        bleAdapter = BluetoothAdapter.getDefaultAdapter();

        assertEquals(bleAdapter.isEnabled(), bleManager.is(BleManagerState.ON));

        if(bleAdapter.isEnabled())
        {
            toggleBLEOff();
        }
        else
        {
            toggleBLEOn();
        }
    }

    private void toggleBLEOff()
    {
        bleManager.turnOff();

        assertEquals(!bleAdapter.isEnabled(), bleManager.is(BleManagerState.OFF));
    }

    private void toggleBLEOn()
    {
        bleManager.turnOn();

        assertEquals(bleAdapter.isEnabled(), bleManager.is(BleManagerState.ON));
    }

}
