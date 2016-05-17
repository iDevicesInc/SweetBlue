package com.idevicesinc.sweetblue.tester;

import android.bluetooth.BluetoothAdapter;
import android.test.ActivityInstrumentationTestCase2;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;

import org.junit.Test;

public class ABluetoothPowerTest extends ActivityInstrumentationTestCase2<BluetoothPowerActivity>
{
    BluetoothPowerActivity testActivity;

    BleManager bleManager;

    BluetoothAdapter bleAdapter;

    public ABluetoothPowerTest()
    {
        super(BluetoothPowerActivity.class);
    }


    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        testActivity = getActivity();

        bleManager = BleManager.get(testActivity);

        bleAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Test
    public void testBleOff()
    {
        testActivity.turnBluetoothOff();

        assertEquals(!bleAdapter.isEnabled(), bleManager.is(BleManagerState.OFF));
    }

    @Test
    public void testBleOn() throws Exception
    {
        testActivity.turnBluetoothOn();

        assertTrue(bleAdapter.isEnabled());

        assertEquals(bleAdapter.isEnabled(), bleManager.is(BleManagerState.ON));
    }

}
