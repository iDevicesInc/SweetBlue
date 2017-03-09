package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.test.ActivityInstrumentationTestCase2;

import org.junit.Test;

public class ABluetoothPowerTest extends ActivityInstrumentationTestCase2<BluetoothPowerActivity>
{
    BluetoothPowerActivity testActivity;

    BleManager bleManager;

    BluetoothAdapter bleAdapter;
    UiDevice uiDevice;


    public ABluetoothPowerTest()
    {
        super(BluetoothPowerActivity.class);
    }


    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        testActivity = getActivity();

        bleManager = testActivity.getManager();

        bleAdapter = BluetoothAdapter.getDefaultAdapter();

        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void testBleOff() throws Exception
    {
        testActivity.turnBluetoothOff();

        Thread.sleep(2000); //Pause to wait for the device to update its state

        assertEquals(!bleAdapter.isEnabled(), bleManager.is(BleManagerState.OFF));
    }

    @Test
    public void testBleOn() throws Exception
    {
        UIUtil.handleBluetoothEnablerDialogs(uiDevice, testActivity);

        testActivity.turnBluetoothOn();

        Thread.sleep(2000); //Pause to wait for the device to update its state

        assertTrue(bleAdapter.isEnabled());

        assertEquals(bleAdapter.isEnabled(), bleManager.is(BleManagerState.ON));
    }

}
