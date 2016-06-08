package com.idevicesinc.sweetblue;

import android.os.Handler;
import android.os.SystemClock;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.idevicesinc.sweetblue.listeners.ManagerStateListener;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UpdateCallback;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_State;
import com.idevicesinc.sweetblue.utils.Utils_String;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BleScanTest
{
    TestActivity testActivity;

    BleManager bleManager;

    @Rule
    public ActivityTestRule<TestActivity> startActivity = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void init() throws Exception
    {
        testActivity = startActivity.getActivity();

        bleManager = BleManager.get(testActivity);

        bleManager.turnOn();
    }

    @Test
    public void testFiveSecondScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        final AtomicLong stateStartTime = new AtomicLong(0);

        final long buffer = bleManager.mConfig.updateThreadSpeed.getMilliseconds() * 2;

        bleManager.setManagerStateListener(new ManagerStateListener()
        {
            @Override
            public void onEvent(StateEvent event)
            {
                if(event.didEnter(BleManagerState.SCANNING))
                {
                    stateStartTime.set(System.currentTimeMillis());
                }
                else if(event.didExit(BleManagerState.SCANNING))
                {
                    long timeInState = System.currentTimeMillis() - stateStartTime.get();

                    Assert.assertTrue("Manager was scanning for longer than the correct amount of time", timeInState - Interval.FIVE_SECS.millis() <= buffer);

                    Assert.assertTrue("Manager didn't scan for the right amount of time", timeInState >= 0);

                    finishedSemaphore.release();
                }
            }
        });

        bleManager.startScan(Interval.FIVE_SECS);

        finishedSemaphore.acquire();
    }

    @Test
    public void testInfiniteScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        Handler handler = testActivity.getHandler();

        bleManager.startScan();

        for(int i = 1; i < 30; i++)
        {
            final int iteration = i;
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    Assert.assertTrue(bleManager.is(BleManagerState.SCANNING));

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

        final AtomicInteger stateCounter = new AtomicInteger(0);

        final AtomicLong scanningStartTime = new AtomicLong(0);

        final AtomicLong scanPausedStartTime = new AtomicLong(0);

        final AtomicBoolean stateToggle = new AtomicBoolean(false);

        final long buffer = bleManager.mConfig.updateThreadSpeed.getMilliseconds() * 2;

        bleManager.setManagerStateListener(new ManagerStateListener()
        {
            @Override
            public void onEvent(StateEvent event)
            {
                if(event.didEnter(BleManagerState.SCANNING))
                {
                    if(stateCounter.get() >= 4)
                        finishedSemaphore.release();

                    scanningStartTime.set(System.currentTimeMillis());

                    Assert.assertFalse("Scanning state wasn't entered from Scan_Paused, it came from another state", stateToggle.get());

                    stateToggle.set(true);

                    stateCounter.set(stateCounter.get() + 1);
                }

                if(event.didExit(BleManagerState.SCANNING))
                {
                    long timeInState = System.currentTimeMillis() - scanningStartTime.get();

                    Assert.assertTrue("Time in SCANNING: " + timeInState + " exceeded 5 sec + buffer time (" + buffer + " mSec)",  timeInState - Interval.FIVE_SECS.millis() <= buffer);

                    Assert.assertTrue("Time in SCANNING: " + timeInState + " didn't satisfy the 5 second periodic scan time", timeInState - Interval.FIVE_SECS.millis() >= 0);
                }

                if(event.didEnter(BleManagerState.SCAN_PAUSED))
                {
                    scanPausedStartTime.set(System.currentTimeMillis());

                    Assert.assertTrue("Paused state wasn't entered from the SCANNING state, it came from another state", stateToggle.get());

                    stateCounter.set(stateCounter.get() + 1);

                    stateToggle.set(false);
                }

                if(event.didExit(BleManagerState.SCAN_PAUSED))
                {
                    long timeInState = System.currentTimeMillis() - scanPausedStartTime.get();

                    Assert.assertTrue("Time in SCAN_PAUSED: " + timeInState + " exceeded 5 sec + buffer time (" + buffer + " mSec)",  timeInState - Interval.FIVE_SECS.millis() <= buffer);

                    Assert.assertTrue("Time in SCAN_PAUSED: " + timeInState + " didn't satisfy the 5 second periodic scan time", timeInState - Interval.FIVE_SECS.millis() >= 0);
                }
            }
        });

        bleManager.startPeriodicScan(Interval.FIVE_SECS, Interval.FIVE_SECS);

        finishedSemaphore.acquire();
    }

    @Test
    public void testScanAPIPostLollipop()
    {
        BleManagerConfig config = new BleManagerConfig();

        config.scanApi = BleScanAPI.POST_LOLLIPOP;

        bleManager = BleManager.get(testActivity, config);

        bleManager.startScan();

        P_ScanManager scanManager = bleManager.mScanManager;

        Assert.assertTrue(Utils.isLollipop());

        Assert.assertTrue(scanManager.isPostLollipopScan());
    }

    @Test
    public void testScanAPIPreLollipop()
    {
        BleManagerConfig config = new BleManagerConfig();

        config.scanApi = BleScanAPI.PRE_LOLLIPOP;

        bleManager = BleManager.get(testActivity, config);

        bleManager.startScan();

        P_ScanManager scanManager = bleManager.mScanManager;

        Assert.assertTrue(scanManager.isPreLollipopScan());
    }

    @Test
    public void testScanAPIClassic()
    {
        BleManagerConfig config = new BleManagerConfig();

        config.scanApi = BleScanAPI.CLASSIC;

        bleManager = BleManager.get(testActivity, config);

        bleManager.startScan();

        P_ScanManager scanManager = bleManager.mScanManager;

        Assert.assertTrue(scanManager.isClassicScan());
    }

    @Test
    public void testStopScan() throws Exception
    {
        bleManager.setManagerStateListener(new ManagerStateListener()
        {
            @Override
            public void onEvent(StateEvent event)
            {
                if(event.didEnter(BleManagerState.SCANNING))
                {
                    bleManager.stopScan();

                    Assert.assertFalse(bleManager.is(BleManagerState.SCANNING));
                }
            }
        });

        bleManager.startScan();
    }

    private void shutdown(Semaphore semaphore)
    {
        bleManager.shutdown();

        semaphore.release();
    }

}
