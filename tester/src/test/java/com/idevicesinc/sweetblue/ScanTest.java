package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class ScanTest extends BaseBleUnitTest
{

    private static final int LEEWAY = 500;

    @Test
    public void scanApiClassicTest() throws Exception
    {
        m_config.scanApi = BleScanApi.CLASSIC;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.CLASSIC);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    @Deprecated
    public void scanApiClassicBackwardsCompatTest() throws Exception
    {
        m_config.scanMode = BleScanMode.CLASSIC;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.CLASSIC);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    public void scanApiPreLollipop() throws Exception
    {
        m_config.scanApi = BleScanApi.PRE_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    @Deprecated
    public void scanApiPreLollipopBackwardsCompatTest() throws Exception
    {
        m_config.scanMode = BleScanMode.PRE_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    public void scanApiPostLollipop() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    @Deprecated
    public void scanApiPostLollipopBackwardsCompatTest() throws Exception
    {
        m_config.scanMode = BleScanMode.POST_LOLLIPOP;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    public void scanApiAuto() throws Exception
    {
        m_config.scanApi = BleScanApi.AUTO;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                m_mgr.setListener_State(new ManagerStateListener()
                {
                    @Override public void onEvent(BleManager.StateListener.StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    @Deprecated
    public void scanApiAutoBackwardsCompatTest() throws Exception
    {
        m_config.scanMode = BleScanMode.AUTO;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleManagerState.SCANNING))
                        {
                            assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Test
    public void scanClassicBoostTest() throws Exception
    {
        m_config.scanApi = BleScanApi.AUTO;
        m_mgr.setConfig(m_config);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
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
                            semaphore.release();
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
        doSingleScanTest(2000);
    }

    @Test(timeout = 7000)
    public void periodicScanTest() throws Exception
    {
        doPeriodicScanTest(1000);
    }





    private void doPeriodicScanTest(final long scanTime) throws Exception
    {
        final AtomicBoolean didStop = new AtomicBoolean(false);
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
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
                                long targetTime = scanTime * 3;
                                assertTrue("Diff: " + diff, (diff - LEEWAY) < targetTime && targetTime < (diff + LEEWAY));
                                semaphore.release();
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

    private void doSingleScanTest(final long scanTime) throws Exception
    {
        doTestOperation(new TestOp()
        {
            @Override public void run(final Semaphore semaphore)
            {
                final Pointer<Long> time = new Pointer<Long>();
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didExit(BleManagerState.SCANNING))
                        {
                            // Make sure our scan time is approximately correct
                            long diff = System.currentTimeMillis() - time.value;
                            assertTrue(((diff - LEEWAY) < scanTime && scanTime < (diff + LEEWAY)));
                            semaphore.release();
                        }
                    }
                });
                time.value = System.currentTimeMillis();
                m_mgr.startScan(Interval.millis(scanTime));
            }
        });
    }

}
