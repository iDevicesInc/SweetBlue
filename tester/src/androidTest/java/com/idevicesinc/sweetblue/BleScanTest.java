package com.idevicesinc.sweetblue;

import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
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

        bleManager.onResume();

    }

    @Test
    public void testFiveSecondScanMain() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = true;
        bleManager.setConfig(bleManager.m_config);

        bleManager.setListener_State(new ManagerStateListener() {

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

        bleManager.startScan(Interval.FIVE_SECS);

        finishedSemaphore.acquire();
    }

    @Test
    public void testFiveSecondScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = false;
        bleManager.setConfig(bleManager.m_config);

        bleManager.setListener_State(new ManagerStateListener() {
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

        bleManager.startScan(Interval.FIVE_SECS);


//        Handler handler = testActivity.getHandler();

//        handler.postDelayed(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                assertTrue(bleManager.is(BleManagerState.SCANNING));
//            }
//        }, 1000);
//
//        handler.postDelayed(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                assertFalse(bleManager.is(BleManagerState.SCANNING));
//
//                shutdown(finishedSemaphore);
//            }
//        }, 6000);

        finishedSemaphore.acquire();
    }

    @Test
    public void testInfiniteScanMain() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = true;
        bleManager.setConfig(bleManager.m_config);

        bleManager.stopScan();


        Handler handler = testActivity.getHandler();

        checkState();

        for(int i = 1; i < 30; i++)
        {
            final int iteration = i;
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    checkState();
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
    public void testInfiniteScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = false;
        bleManager.setConfig(bleManager.m_config);

        bleManager.startScan();


        Handler handler = testActivity.getHandler();

        checkState();

        for(int i = 1; i < 30; i++)
        {
            final int iteration = i;
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    checkState();
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
    public void testPeriodicScanMain() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = true;
        bleManager.setConfig(bleManager.m_config);

        bleManager.startPeriodicScan(Interval.FIVE_SECS, Interval.FIVE_SECS);

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
                assertTrue(!bleManager.is(BleManagerState.SCANNING));
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
    public void testPeriodicScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = false;
        bleManager.setConfig(bleManager.m_config);

        bleManager.startPeriodicScan(Interval.FIVE_SECS, Interval.FIVE_SECS);

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
                assertTrue(!bleManager.is(BleManagerState.SCANNING));
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
    public void testScanAPIPostLollipopMain() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = true;
        config.scanApi = BleScanApi.POST_LOLLIPOP;

        bleManager = BleManager.get(testActivity, config);

        bleManager.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = bleManager.getScanManager();

                    assertTrue(Utils.isLollipop());

                    assertTrue(scanManager.isPostLollipopScan());

                    shutdown(semaphore);
                }
            }
        });

        bleManager.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIPostLollipop() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = false;
        config.scanApi = BleScanApi.POST_LOLLIPOP;

        bleManager = BleManager.get(testActivity, config);

        bleManager.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = bleManager.getScanManager();

                    assertTrue(Utils.isLollipop());

                    assertTrue(scanManager.isPostLollipopScan());

                    shutdown(semaphore);
                }
            }
        });

        bleManager.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIPreLollipopMain() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = true;
        config.scanApi = BleScanApi.PRE_LOLLIPOP;

        bleManager = BleManager.get(testActivity, config);

        bleManager.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = bleManager.getScanManager();

                    assertTrue(scanManager.isPreLollipopScan());

                    shutdown(semaphore);
                }
            }
        });

        bleManager.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIPreLollipop() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = false;
        config.scanApi = BleScanApi.PRE_LOLLIPOP;

        bleManager = BleManager.get(testActivity, config);

        bleManager.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = bleManager.getScanManager();

                    assertTrue(scanManager.isPreLollipopScan());

                    shutdown(semaphore);
                }
            }
        });

        bleManager.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIClassicMain() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = true;
        config.scanApi = BleScanApi.CLASSIC;

        bleManager = BleManager.get(testActivity, config);

        bleManager.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = bleManager.getScanManager();

                    assertTrue(scanManager.isClassicScan());

                    shutdown(semaphore);
                }
            }
        });

        bleManager.startScan();

        semaphore.acquire();
    }

    @Test
    public void testScanAPIClassic() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);

        BleManagerConfig config = new BleManagerConfig();

        config.runOnMainThread = false;
        config.scanApi = BleScanApi.CLASSIC;

        bleManager = BleManager.get(testActivity, config);

        bleManager.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    P_ScanManager scanManager = bleManager.getScanManager();

                    assertTrue(scanManager.isClassicScan());

                    shutdown(semaphore);
                }
            }
        });

        bleManager.startScan();

        semaphore.acquire();
    }

    @Test
    public void testStopScan() throws Exception
    {
        bleManager = BleManager.get(testActivity);

        bleManager.startScan();

        bleManager.stopScan();

        assertFalse(bleManager.is(BleManagerState.SCANNING));

        shutdown(null);
    }

    private void shutdown(Semaphore semaphore)
    {
        bleManager.stopScan();
        bleManager.shutdown();

        if (semaphore != null)
        {
            semaphore.release();
        }
    }


    public void checkState()
    {
        Log.e("BLE_SCANNING", bleManager.is(BleManagerState.SCANNING) + "");
        Log.e("BLE_OFF", bleManager.is(BleManagerState.OFF) + "");
        Log.e("BLE_TURNING_OFF", bleManager.is(BleManagerState.TURNING_OFF) + "");
        Log.e("BLE_STARTING_SCAN", bleManager.is(BleManagerState.STARTING_SCAN) + "");
        Log.e("BLE_RESETTING", bleManager.is(BleManagerState.RESETTING) + "");
        Log.e("BLE_ON", bleManager.is(BleManagerState.ON) + "");
        Log.e("BLE_TURNING_ON", bleManager.is(BleManagerState.TURNING_ON) + "");
    }


}
