package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;


@Config(manifest = Config.NONE, sdk = 21)
@RunWith(RobolectricTestRunner.class)
public class ScanApiTest extends BaseBleTest
{

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
                            assertTrue(getScanApi() == BleScanApi.CLASSIC);
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
                            assertTrue(getScanApi() == BleScanApi.CLASSIC);
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
                            assertTrue(getScanApi() == BleScanApi.PRE_LOLLIPOP);
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
                            assertTrue(getScanApi() == BleScanApi.PRE_LOLLIPOP);
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
                            assertTrue(getScanApi() == BleScanApi.POST_LOLLIPOP);
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
                            assertTrue(getScanApi() == BleScanApi.POST_LOLLIPOP);
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
                            assertTrue(getScanApi() == BleScanApi.PRE_LOLLIPOP);
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
                            assertTrue(getScanApi() == BleScanApi.PRE_LOLLIPOP);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Override public PI_BleScanner getScanner()
    {
        return new DefaultBleScannerTest();
    }

    @Override public PI_BleStatusHelper getStatusHelper()
    {
        return new DefaultStatusHelperTest();
    }
}
