package com.idevicesinc.sweetblue;

import android.os.Handler;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.idevicesinc.sweetblue.listeners.ManagerStateListener;

import junit.framework.Assert;

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
public class TaskManagerIdleTest
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
    public void testTaskManagerIdleMultipleTimes() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        final AtomicInteger idleCount = new AtomicInteger(0);

        final AtomicBoolean stateToggle = new AtomicBoolean(false);

        final AtomicLong workingStartTime = new AtomicLong(0);

        final long buffer = bleManager.mConfig.updateThreadSpeed.getMilliseconds() * 2;

        bleManager.setManagerStateListener(new ManagerStateListener()
        {
            @Override
            public void onEvent(StateEvent event)
            {
                if(event.didEnter(BleManagerState.SCANNING))
                {
                    if(idleCount.get() >= 4)
                        finishedSemaphore.release();

                    Assert.assertFalse("State never changed before going to scanning", stateToggle.get());

                    stateToggle.set(true);

                    bleManager.stopScan();

                    workingStartTime.set(System.currentTimeMillis());
                }

                if(event.didEnter(BleManagerState.IDLE))
                {
                    long timeInState = System.currentTimeMillis() - workingStartTime.get();

                    Assert.assertTrue("BleManager took too long to enter idling", timeInState + buffer >= bleManager.mConfig.delayBeforeIdleMs);

                    idleCount.set(idleCount.get() + 1);

                    Assert.assertTrue("State never changed before going to idle", stateToggle.get());

                    stateToggle.set(false);

                    bleManager.startScan();
                }
            }
        });

        bleManager.startScan();

        finishedSemaphore.acquire();
    }

    @Test
    public void testTaskManagerIdleOnce() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.setManagerStateListener(new ManagerStateListener()
        {
            @Override
            public void onEvent(StateEvent event)
            {
                if(event.didEnter(BleManagerState.IDLE))
                {
                    Assert.assertTrue(bleManager.getUpdateSpeed() == bleManager.mConfig.updateThreadIdleIntervalMs);

                    finishedSemaphore.release();
                }
                else
                {
                    Assert.assertTrue(bleManager.getUpdateSpeed() == bleManager.mConfig.updateThreadSpeed.getMilliseconds());

                    bleManager.stopScan();
                }
            }
        });

        bleManager.startScan();

        finishedSemaphore.acquire();
    }

}
