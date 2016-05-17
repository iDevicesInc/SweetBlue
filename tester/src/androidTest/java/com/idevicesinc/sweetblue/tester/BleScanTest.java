package com.idevicesinc.sweetblue.tester;

import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.listeners.ManagerStateListener;

import org.junit.Test;

import java.util.concurrent.Semaphore;

public class BleScanTest extends ActivityInstrumentationTestCase2<BleScanActivity>
{
    BleScanActivity testActivity;

    BleManager bleManager;

    public BleScanTest()
    {
        super(BleScanActivity.class);
    }


    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        testActivity = getActivity();

        bleManager = testActivity.getBleManager();

        testActivity.eventListener = new BleScanActivity.EventStateInterface()
        {
            @Override
            public void onEvent(ManagerStateListener.StateEvent event)
            {
                if(event.didEnter(BleManagerState.SCANNING))
                {
                    Log.e("YAY", "NOW WE ARE SCANNING");
                }
                else if(event.didExit(BleManagerState.SCANNING))
                {
                    Log.e("BOO", "WE HAVE LEFT SCANNING");
                }
            }
        };
    }

    @Test
    public void testFiveSecondScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        testActivity.startFiveSecondScan();

        Handler handler = testActivity.getHandler();

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                assertTrue(bleManager.is(BleManagerState.SCANNING));
            }
        }, 1000);

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                assertFalse(bleManager.is(BleManagerState.SCANNING));

                shutdown(finishedSemaphore);
            }
        }, 6000);

        finishedSemaphore.acquire();
    }

    @Test
    public void testInfiniteScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        testActivity.startInfiniteScan();


        Handler handler = testActivity.getHandler();

        testActivity.checkState();

        for(int i = 1; i < 30; i++)
        {
            final int iteration = i;
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    assertTrue(bleManager.is(BleManagerState.SCANNING));

                    if(iteration == 29)
                    {
                        shutdown(finishedSemaphore);
                    }
                }
            }, i * 1000);
        }

        finishedSemaphore.acquire();
    }

    @Test
    public void testPeriodicScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        testActivity.startPeriodicScan();

        Handler handler = testActivity.getHandler();

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                assertTrue(bleManager.is(BleManagerState.SCANNING));
            }
        }, 1000);

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                assertTrue(bleManager.is(BleManagerState.SCAN_PAUSED));
            }
        }, 6000);

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                assertTrue(bleManager.is(BleManagerState.SCANNING));

                shutdown(finishedSemaphore);
            }
        }, 11000);

        finishedSemaphore.acquire();
    }

    @Test
    public void testStopScan() throws Exception
    {
        testActivity.stopScan();

        assertFalse(bleManager.is(BleManagerState.SCANNING));
    }

    private void shutdown(Semaphore semaphore)
    {
        bleManager.shutdown();

        semaphore.release();
    }

}
