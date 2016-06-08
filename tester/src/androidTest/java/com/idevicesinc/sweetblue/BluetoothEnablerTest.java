package com.idevicesinc.sweetblue;

import android.test.ActivityInstrumentationTestCase2;

import org.junit.Test;

import java.util.concurrent.Semaphore;

public class BluetoothEnablerTest extends ActivityInstrumentationTestCase2<BluetoothEnablerActivity>
{
    BluetoothEnablerActivity testActivity;

    public BluetoothEnablerTest()
    {
        super(BluetoothEnablerActivity.class);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        testActivity = getActivity();
    }

    @Test
    public void testBluetoothEnabler() throws InterruptedException
    {
        //This needs to do something in order for the enabler to work properly. This might need to be connected into
        //The state listener of the enabler directly so that we can see the progression of states. Otherwise, there isn't
        //Any callback to be use to determine when a stage is complete.
        Semaphore finishedSemaphore = new Semaphore(0);

        BleManager manager = BleManager.get(testActivity);

        BluetoothEnabler.testInit(testActivity, new BleManagerConfig(){
            {

            }
        }.bluetoothEnablerController, manager);

        BluetoothEnabler.setTestSemaphore(finishedSemaphore);

        manager.enableBluetoothAndMarshmallowPrerequisites(testActivity);

        finishedSemaphore.acquire();
    }
}
