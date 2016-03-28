package com.idevicesinc.sweetblue.tests;


import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanMode;
import com.idevicesinc.sweetblue.PI_BleScanner;
import com.idevicesinc.sweetblue.PI_BleStatusHelper;
import com.idevicesinc.sweetblue.utils.Interval;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;


@Config(manifest = Config.NONE, sdk = 21)
@RunWith(RobolectricTestRunner.class)
public class ScanModeTest extends BaseBleTest
{

    @Test
    public void scanModeClassicTest() throws Exception
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
                            assertTrue(getScanMode() == BleScanMode.CLASSIC);
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
    public void scanModePreLollipop() throws Exception
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
                            assertTrue(getScanMode() == BleScanMode.PRE_LOLLIPOP);
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
    public void scanModeAuto() throws Exception
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
                            // TODO - Swap the following 2 lines once the lollipop scanning API
                            // actually works at least as well as the pre-lollipop API.
                            //assertTrue(getScanMode() == BleScanMode.AUTO);
                            assertTrue(getScanMode() == BleScanMode.PRE_LOLLIPOP);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan(Interval.FIVE_SECS);
            }
        });
    }

    @Override PI_BleScanner getScanner()
    {
        return new DefaultBleScannerTest();
    }

    @Override PI_BleStatusHelper getStatusHelper()
    {
        return new DefaultStatusHelperTest();
    }
}
