package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Util;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ScanTest extends BaseBleUnitTest
{

    private static final int LEEWAY = 500;

    @Test(timeout = 10000)
    public void scanApiClassicTest() throws Exception
    {
        startTest(false);
        m_config.scanApi = BleScanApi.CLASSIC;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.CLASSIC);
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test(timeout = 10000)
    @Deprecated
    public void scanApiClassicBackwardsCompatTest() throws Exception
    {
        startTest(false);
        m_config.scanMode = BleScanMode.CLASSIC;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.CLASSIC);
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    public void scanClassicOneTimeTest() throws Exception
    {
        m_config.scanApi = BleScanApi.CLASSIC;
        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);
        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override
            public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.CLASSIC);
                }
                else if (e.didExit(BleManagerState.SCANNING))
                {
                    assertFalse("Scan task is in the queue, when it should not be!", m_mgr.getTaskQueue().isInQueue(P_Task_Scan.class, m_mgr) || m_mgr.getTaskQueue().isCurrent(P_Task_Scan.class, m_mgr));
                    succeed();
                }
            }
        });

        m_mgr.startScan(Interval.FIVE_SECS);
        startTest();
    }

    @Test(timeout = 10000)
    public void scanApiPreLollipop() throws Exception
    {
        startTest(false);
        m_config.scanApi = BleScanApi.PRE_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test(timeout = 10000)
    @Deprecated
    public void scanApiPreLollipopBackwardsCompatTest() throws Exception
    {
        startTest(false);
        m_config.scanMode = BleScanMode.PRE_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test(timeout = 10000)
    public void scanApiPostLollipop() throws Exception
    {
        startTest(false);
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test(timeout = 20000)
    public void inifiteScanWithPauseTest() throws Exception
    {
        startTest(false);
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override
            public void run()
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    boolean scanStarted = false;
                    boolean scanPaused = false;

                    @Override
                    public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            if (!scanPaused)
                            {
                                scanStarted = true;
                            }
                            else
                            {
                                succeed();
                            }
                        }
                        else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
                        {
                            assertTrue(scanStarted);
                            scanPaused = true;
                        }
                    }
                });
                m_mgr.startScan(Interval.INFINITE);
            }
        });
    }

    @Test(timeout = 30000)
    public void inifiteScanForcedTest() throws Exception
    {
        startTest(false);
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.updateLoopCallback = new PI_UpdateLoop.Callback()
        {
            @Override
            public void onUpdate(double timestep_seconds)
            {
                double timeScanning = m_mgr.getTimeInState(BleManagerState.SCANNING).secs();
                if (timeScanning >= 15.0)
                {
                    succeed();
                }
            }
        };
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override
            public void run()
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {

                    @Override
                    public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                        }
                        else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
                        {
                            assertFalse("Scanning paused for a forced infinite scan!", true);
                        }
                    }
                });

                m_mgr.startScan(new ScanOptions().scanFor(Interval.INFINITE).forceIndefinite(true));
            }
        });
    }

    @Test(timeout = 10000)
    @Deprecated
    public void scanApiPostLollipopBackwardsCompatTest() throws Exception
    {
        startTest(false);
        m_config.scanMode = BleScanMode.POST_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test(timeout = 10000)
    public void scanApiAuto() throws Exception
    {
        startTest(false);
        m_config.scanApi = BleScanApi.AUTO;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test(timeout = 30000)
    public void scanApiAutoSwitchApiTest() throws Exception
    {
        startTest(false);
        m_config.scanApi = BleScanApi.AUTO;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    boolean paused = false;

                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            if (!paused)
                            {
                                assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                            }
                            else
                            {
                                paused = true;
                                assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                                m_mgr.stopScan();
                                succeed();
                            }
                        }
                        else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
                        {
                            paused = true;
                        }
                    }
                });
                m_mgr.startScan();
            }
        });
    }

    @Test(timeout = 10000)
    @Deprecated
    public void scanApiAutoBackwardsCompatTest() throws Exception
    {
        startTest(false);
        m_config.scanMode = BleScanMode.AUTO;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test(timeout = 10000)
    public void scanClassicBoostTest() throws Exception
    {
        startTest(false);
        m_config.scanApi = BleScanApi.AUTO;
        m_config.scanClassicBoostLength = Interval.secs(BleManagerConfig.DEFAULT_CLASSIC_SCAN_BOOST_TIME);
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {

                    boolean boosted = false;

                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.BOOST_SCANNING))
                        {
                            boosted = true;
                        }
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue(boosted);
                            assertFalse(m_mgr.is(BleManagerState.BOOST_SCANNING));
                            m_mgr.stopScan();
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    // While the scan itself is only 2 seconds, it takes a couple seconds for the test
    // to spin up the Java VM, so the timeout adds some padding to give it enough
    // time
    @Test(timeout = 4000)
    public void singleScanWithInterval() throws Exception
    {
        startTest(false);
        doSingleScanTest(2000);
    }

    @Test(timeout = 7000)
    public void periodicScanTest() throws Exception
    {
        startTest(false);
        doPeriodicScanTest(1000);
    }

    @Test(timeout = 7000)
    public void periodicScanWithOptionsTest() throws Exception
    {
        startTest(false);
        doPeriodicScanOptionsTest(1000);
    }

    @Test(timeout = 14000)
    public void highPriorityScanTest() throws Exception
    {
        final Pointer<Integer> connected = new Pointer<>(0);

        final BleDevice device2;

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    e.device().connect();
                    if (connected.value == 1)
                    {
                        m_mgr.startScan(new ScanOptions().scanFor(Interval.TEN_SECS).asHighPriority(true));
                    }
                    connected.value += 1;
                }
            }
        });

        m_mgr.newDevice(Util.randomMacAddress(), "Test Device");
        device2 = m_mgr.newDevice(Util.randomMacAddress(), "Test Device 2");

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_TaskQueue queue = m_mgr.getTaskQueue();
                    assertTrue(queue.isInQueue(P_Task_Connect.class, device2));
                    int position = queue.positionInQueue(P_Task_Connect.class, device2);
                    assertTrue(position != -1);
                    succeed();
                }
            }
        });
        startTest();
    }

    @Test
    public void scanRecordUpdateTest() throws Exception
    {
        // TODO - Get this working in v3. This would require too many internal changes to get working right now. As we've refactored quite a lot
        // in v3, it doesnt make sense to do it here. Will have to manually test to make sure things are working correctly.
//        final int START_FLAGS = 15;
//
//        final int CHANGED_FLAGS = 28;
//
//        m_config.loggingEnabled = true;
//
//        m_mgr.setConfig(m_config);
//
//        byte[] record = Utils_ScanRecord.newScanRecord((byte) START_FLAGS, null, "Testerino", false, (byte) 1, (short) 0, null);
//
//        m_mgr.setListener_Discovery(e ->
//        {
//            if (e.was(BleManager.DiscoveryListener.LifeCycle.DISCOVERED))
//            {
//                assertTrue("Advertising flags don't match! Expected: " + START_FLAGS + " Got: " + e.device().getAdvertisingFlags(), e.device().getAdvertisingFlags() == START_FLAGS);
//                byte[] newRecord = Utils_ScanRecord.newScanRecord((byte) CHANGED_FLAGS, null, "Testerino4", false, (byte) 10, (short) 57, null);
//                NativeUtil.advertiseNewDevice(m_mgr, -35, newRecord, Interval.millis(250));
//            }
//            else if (e.was(BleManager.DiscoveryListener.LifeCycle.REDISCOVERED))
//            {
//                assertTrue(e.device().getAdvertisingFlags() == CHANGED_FLAGS);
//                assertTrue(e.device().getName_native().contains("4"));
//                assertTrue(e.device().getTxPower() == 10);
//                assertTrue(e.device().getManufacturerId() == 57);
//                succeed();
//            }
//        });
//
//        m_mgr.setListener_State((ManagerStateListener) e ->
//        {
//            if (e.didEnter(BleManagerState.SCANNING))
//            {
//                NativeUtil.advertiseNewDevice(m_mgr, -45, record);
//            }
//        });
//
//        m_mgr.startScan();
//
//        startTest();
    }








    private void doPeriodicScanTest(final long scanTime) throws Exception
    {
        final AtomicBoolean didStop = new AtomicBoolean(false);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                final AtomicLong time = new AtomicLong();
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didExit(BleManagerState.SCANNING))
                        {
                            if (didStop.get())
                            {
                                long diff = m_mgr.currentTime() - time.get();
                                // Make sure that our scan time is correct, this checks against
                                // 3x the scan amount (2 scans, 1 pause). We may need to add a bit
                                // to LEEWAY here, as it's going through 3 iterations, but for now
                                // it seems to be ok for the test

                                // We also need to account for boost scan time here

                                long boostTime = getBoostTime();

                                long targetTime = (scanTime * 3) + boostTime;
                                assertTrue("Diff: " + diff, (diff - LEEWAY) < targetTime && targetTime < (diff + LEEWAY));
                                succeed();
                            }
                            else
                            {
                                didStop.set(true);
                            }
                        }
                    }
                });
                time.set(m_mgr.currentTime());
                m_mgr.startPeriodicScan(Interval.millis(scanTime), Interval.millis(scanTime));
            }
        });
    }

    private void doPeriodicScanOptionsTest(final long scanTime) throws Exception
    {
        final AtomicBoolean didStop = new AtomicBoolean(false);
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                final AtomicLong time = new AtomicLong();
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didExit(BleManagerState.SCANNING))
                        {
                            if (didStop.get())
                            {
                                long diff = m_mgr.currentTime() - time.get();
                                // Make sure that our scan time is correct, this checks against
                                // 3x the scan amount (2 scans, 1 pause). We may need to add a bit
                                // to LEEWAY here, as it's going through 3 iterations, but for now
                                // it seems to be ok for the test

                                // We also need to account for boost scan time here

                                long boostTime = getBoostTime();

                                long targetTime = (scanTime * 3) + boostTime;


                                assertTrue("Target time: " + targetTime + " Diff: " + diff, (diff - LEEWAY) < targetTime && targetTime < (diff + LEEWAY));
                                succeed();
                            }
                            else
                            {
                                didStop.set(true);
                            }
                        }
                    }
                });
                time.set(m_mgr.currentTime());
                ScanOptions options = new ScanOptions().scanPeriodically(Interval.millis(scanTime), Interval.millis(scanTime));
                m_mgr.startScan(options);
            }
        });
    }

    private long getBoostTime()
    {
        return Interval.isEnabled(m_mgr.m_config.scanClassicBoostLength.millis()) ? m_mgr.m_config.scanClassicBoostLength.millis() * 2 : 0;
    }

    private void doSingleScanTest(final long scanTime) throws Exception
    {
        doTestOperation(new TestOp()
        {
            @Override public void run()
            {
                final Pointer<Long> time = new Pointer<Long>();
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            time.value = System.currentTimeMillis();
                        }
                        if (e.didExit(BleManagerState.SCANNING))
                        {
                            // Make sure our scan time is approximately correct
                            long diff = System.currentTimeMillis() - time.value;
                            assertTrue("Scan didn't run the appropriate amount of time. Requested time = " + scanTime +  " Diff = " + diff, ((diff - LEEWAY) < scanTime && scanTime < (diff + LEEWAY)));
                            succeed();
                        }
                    }
                });
                m_mgr.startScan(Interval.millis(scanTime));
            }
        });
    }

}
