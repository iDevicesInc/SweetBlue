package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ABluetoothPowerTest
{
    TestActivity testActivity;

    BleManager bleManager;

    BluetoothAdapter bleAdapter;

    @Rule
    public ActivityTestRule<TestActivity> loadedActivity = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void init() throws Exception
    {
        testActivity = loadedActivity.getActivity();

        bleManager = BleManager.get(testActivity);

        bleAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Test
    public void testBleOn() throws Exception
    {
        bleManager.turnOn();

        Thread.sleep(2000); //Pause to wait for the device to update its state

        Assert.assertTrue("Bluetooth isn't on", bleAdapter.isEnabled());

        Assert.assertEquals(bleAdapter.isEnabled(), bleManager.is(BleManagerState.ON));
    }

    @Test
    public void testBleOff() throws Exception
    {
        bleManager.turnOff();

        Thread.sleep(2000); //Pause to wait for the device to update its state

        Assert.assertEquals("Bluetooth is off", !bleAdapter.isEnabled(), bleManager.is(BleManagerState.OFF));
    }

}
