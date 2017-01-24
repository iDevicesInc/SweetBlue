package com.idevicesinc.sweetblue;


import android.util.Log;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
import org.junit.Test;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;



public class BleScanTest extends BaseTester<BleScanActivity>
{


    @Test
    public void testFiveSecondScanMain() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        mgr.m_config.runOnMainThread = true;
        mgr.setConfig(mgr.m_config);

        mgr.setListener_State(new ManagerStateListener() {

            long timeStarted;

            @Override
            public void onEvent(BleManager.StateListener.StateEvent e) {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    timeStarted = e.manager().currentTime();
                }
                else if (e.didExit(BleManagerState.SCANNING))
                {
                    long now = e.manager().currentTime();
                    long diff = now - timeStarted;
                    assertTrue("Diff: " + diff, diff <= 5250 && diff > 4999);
                    shutdown(finishedSemaphore);
                }
            }
        });

        mgr.startScan(Interval.FIVE_SECS);

        finishedSemaphore.acquire();
    }

    @Test
    public void testFiveSecondScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        mgr.m_config.runOnMainThread = false;
        mgr.setConfig(mgr.m_config);

        mgr.setListener_State(new ManagerStateListener() {
            boolean hasStartedScan = false;
            long timeStarted;

            @Override
            public void onEvent(BleManager.StateListener.StateEvent e) {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    hasStartedScan = true;
                    timeStarted = e.manager().currentTime();
                }
                else if (e.didExit(BleManagerState.SCANNING))
                {
                    Interval time = e.manager().getTimeInState(BleManagerState.SCANNING);
                    long now = e.manager().currentTime();
                    long diff = now - timeStarted;
                    assertTrue("Diff: " + diff, diff <= 5250 && diff > 4999);
                    shutdown(finishedSemaphore);
                }
            }
        });

        mgr.startScan(Interval.FIVE_SECS);

        finishedSemaphore.acquire();
    }

    @Test(timeout = 30000)
    public void testInfiniteScanMain() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        final AtomicBoolean started = new AtomicBoolean(false);

        mgr.m_config.runOnMainThread = true;
        mgr.m_config.updateLoopCallback = new PI_UpdateLoop.Callback()
        {
            @Override public void onUpdate(double timestep_seconds)
            {
                if (started.get())
                {
                    assertTrue("BleManager is not SCANNING!", mgr.is(BleManagerState.SCANNING));
                    if (mgr.getTimeInState(BleManagerState.SCANNING).secs() >= 20.0)
                    {
                        shutdown(finishedSemaphore);
                    }
                }
            }
        };
        mgr.setConfig(mgr.m_config);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    started.set(true);
                }
            }
        });

        checkState();

        mgr.startScan();

        finishedSemaphore.acquire();
    }

    @Test(timeout = 30000)
    public void testInfiniteScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        final AtomicBoolean started = new AtomicBoolean(false);

        mgr.m_config.runOnMainThread = false;
        mgr.m_config.updateLoopCallback = new PI_UpdateLoop.Callback()
        {
            @Override public void onUpdate(double timestep_seconds)
            {
                if (started.get())
                {
                    assertTrue("BleManager is not SCANNING!", mgr.is(BleManagerState.SCANNING));
                    if (mgr.getTimeInState(BleManagerState.SCANNING).secs() >= 20.0)
                    {
                        shutdown(finishedSemaphore);
                    }
                }
            }
        };
        mgr.setConfig(mgr.m_config);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                started.set(true);
            }
        });

        checkState();

        mgr.startScan();

        finishedSemaphore.acquire();
    }

    @Test(timeout = 20000)
    public void testPeriodicScanMain() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        mgr.m_config.runOnMainThread = true;
        mgr.setConfig(mgr.m_config);

        mgr.setListener_State(new ManagerStateListener()
        {
            boolean checkedScan = false;
            long timeStopped;

            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (!checkedScan)
                {
                    if (e.didExit(BleManagerState.SCANNING))
                    {
                        checkedScan = true;
                        long diff = mgr.getTimeInState(BleManagerState.SCANNING).millis();
                        assertTrue("Diff: " + diff, (diff - 500) < 5000 && (diff + 500) > 5000);
                        timeStopped = mgr.currentTime();
                    }
                }
                else
                {
                    if (e.didExit(BleManagerState.SCANNING))
                    {
                        long diff = mgr.getTimeInState(BleManagerState.SCANNING).millis();
                        long diff2 = mgr.currentTime() - timeStopped - diff;
                        assertTrue("Time not scanning: " + diff2, (diff2 - 250) < 5000 && (diff2 + 250) > 5000);
                        assertTrue("Time Scanning: " + diff, (diff - 250) < 5000 && (diff + 250) > 5000);
                        shutdown(finishedSemaphore);
                    }
                }
            }
        });

        mgr.startPeriodicScan(Interval.FIVE_SECS, Interval.FIVE_SECS);

        finishedSemaphore.acquire();
    }

    @Test(timeout = 20000)
    public void testPeriodicScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        mgr.m_config.runOnMainThread = false;
        mgr.setConfig(mgr.m_config);

        mgr.setListener_State(new ManagerStateListener()
        {
            boolean checkedScan = false;
            long timeStopped;

            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (!checkedScan)
                {
                    if (e.didExit(BleManagerState.SCANNING))
                    {
                        checkedScan = true;
                        long diff = mgr.getTimeInState(BleManagerState.SCANNING).millis();
                        assertTrue("Diff: " + diff, (diff - 500) < 5000 && (diff + 500) > 5000);
                        timeStopped = mgr.currentTime();
                    }
                }
                else
                {
                    if (e.didExit(BleManagerState.SCANNING))
                    {
                        long diff = mgr.getTimeInState(BleManagerState.SCANNING).millis();
                        long diff2 = mgr.currentTime() - timeStopped - diff;
                        assertTrue("Time not scanning: " + diff2, (diff2 - 250) < 5000 && (diff2 + 250) > 5000);
                        assertTrue("Time Scanning: " + diff, (diff - 250) < 5000 && (diff + 250) > 5000);
                        shutdown(finishedSemaphore);
                    }
                }
            }
        });

        mgr.startPeriodicScan(Interval.FIVE_SECS, Interval.FIVE_SECS);

        finishedSemaphore.acquire();
    }

    @Test
    public void testScanAPIPostLollipopMain() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = true;
        config.scanApi = BleScanApi.POST_LOLLIPOP;

        mgr.setConfig(config);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = mgr.getScanManager();

                    assertTrue(Utils.isLollipop());

                    assertTrue(scanManager.isPostLollipopScan());

                    shutdown(semaphore);
                }
            }
        });

        mgr.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIPostLollipop() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = false;
        config.scanApi = BleScanApi.POST_LOLLIPOP;

        mgr.setConfig(config);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = mgr.getScanManager();

                    assertTrue(Utils.isLollipop());

                    assertTrue(scanManager.isPostLollipopScan());

                    shutdown(semaphore);
                }
            }
        });

        mgr.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIPreLollipopMain() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = true;
        config.scanApi = BleScanApi.PRE_LOLLIPOP;

        mgr.setConfig(config);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = mgr.getScanManager();

                    assertTrue(scanManager.isPreLollipopScan());

                    shutdown(semaphore);
                }
            }
        });

        mgr.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIPreLollipop() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = false;
        config.scanApi = BleScanApi.PRE_LOLLIPOP;

        mgr.setConfig(config);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = mgr.getScanManager();

                    assertTrue(scanManager.isPreLollipopScan());

                    shutdown(semaphore);
                }
            }
        });

        mgr.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIClassicMain() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = true;
        config.scanApi = BleScanApi.CLASSIC;

        mgr.setConfig(config);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = mgr.getScanManager();

                    assertTrue(scanManager.isClassicScan());

                    shutdown(semaphore);
                }
            }
        });

        mgr.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIClassic() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = false;
        config.scanApi = BleScanApi.CLASSIC;

        mgr.setConfig(config);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = mgr.getScanManager();

                    assertTrue(scanManager.isClassicScan());

                    shutdown(semaphore);
                }
            }
        });

        mgr.startScan();

        semaphore.acquire();
    }

    @Test
    public void testStopScan() throws Exception
    {

        mgr.startScan();

        mgr.stopScan();

        assertFalse(mgr.is(BleManagerState.SCANNING));

        shutdown(null);
    }

    private void shutdown(Semaphore semaphore)
    {
        if (semaphore != null)
        {
            semaphore.release();
        }
    }


    public void checkState()
    {
        Log.e("BLE_SCANNING", mgr.is(BleManagerState.SCANNING) + "");
        Log.e("BLE_OFF", mgr.is(BleManagerState.OFF) + "");
        Log.e("BLE_TURNING_OFF", mgr.is(BleManagerState.TURNING_OFF) + "");
        Log.e("BLE_STARTING_SCAN", mgr.is(BleManagerState.STARTING_SCAN) + "");
        Log.e("BLE_RESETTING", mgr.is(BleManagerState.RESETTING) + "");
        Log.e("BLE_ON", mgr.is(BleManagerState.ON) + "");
        Log.e("BLE_TURNING_ON", mgr.is(BleManagerState.TURNING_ON) + "");
    }


    @Override Class<BleScanActivity> getActivityClass()
    {
        return BleScanActivity.class;
    }

    @Override BleManagerConfig getInitialConfig()
    {
        BleManagerConfig config = new BleManagerConfig();
        config.loggingEnabled = true;
        return config;
    }
}
