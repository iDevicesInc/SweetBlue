package com.idevicesinc.sweetblue;

import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
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

        testActivity.eventListener = new BleScanActivity.EventStateInterface()
        {
            @Override
            public void onEvent(BleManager.StateListener.StateEvent event)
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
    public void testFiveSecondScanMain() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = true;
        bleManager.setConfig(bleManager.m_config);

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
    public void testFiveSecondScan() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = false;
        bleManager.setConfig(bleManager.m_config);

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
    public void testInfiniteScanMain() throws Exception
    {
        final Semaphore finishedSemaphore = new Semaphore(0);

        bleManager.m_config.runOnMainThread = true;
        bleManager.setConfig(bleManager.m_config);

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
                    testActivity.checkState();
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
                    testActivity.checkState();
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

}
