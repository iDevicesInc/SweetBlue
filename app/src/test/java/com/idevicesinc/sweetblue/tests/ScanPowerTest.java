package com.idevicesinc.sweetblue.tests;


import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanMode;
import com.idevicesinc.sweetblue.BleScanPower;
import com.idevicesinc.sweetblue.PI_BleScanner;
import com.idevicesinc.sweetblue.PI_BleStatusHelper;
import com.idevicesinc.sweetblue.utils.Interval;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 21)
@RunWith(RobolectricTestRunner.class)
public class ScanPowerTest extends BaseBleTest
{

    @Test
    public void scanPowerLow() throws Exception
    {
        m_config.scanPower = BleScanPower.LOW_POWER;
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
                            // TODO - Uncomment the following line once the lollipop scanning API
                            // actually works at least as well as the pre-lollipop API.
                            //assertTrue(getScanPower() == BleScanPower.LOW_POWER);
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
    public void scanPowerMedium() throws Exception
    {
        m_config.scanPower = BleScanPower.MEDIUM_POWER;
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
                            // TODO - Uncomment the following line once the lollipop scanning API
                            // actually works at least as well as the pre-lollipop API.
                            //assertTrue(getScanPower() == BleScanPower.MEDIUM_POWER);
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
    public void scanPowerHigh() throws Exception
    {
        m_config.scanPower = BleScanPower.HIGH_POWER;
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
                            // TODO - Uncomment the following line once the lollipop scanning API
                            // actually works at least as well as the pre-lollipop API.
                            //assertTrue(getScanPower() == BleScanPower.HIGH_POWER);
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
    public void scanPowerAutoForeground() throws Exception
    {
        m_config.scanPower = BleScanPower.AUTO;
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
                            // We're in the foreground, and NOT running an infinite scan, so this should be High power here
                            // TODO - Uncomment the following line once the lollipop scanning API
                            // actually works at least as well as the pre-lollipop API.
                            //assertTrue(getScanPower() == BleScanPower.HIGH_POWER);
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
    public void scanPowerAutoInfinite() throws Exception
    {
        m_config.scanPower = BleScanPower.AUTO;
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
                            // We're in the foreground, and running an infinite scan, so this should be Medium power here
                            // TODO - Uncomment the following line once the lollipop scanning API
                            // actually works at least as well as the pre-lollipop API.
                            //assertTrue(getScanPower() == BleScanPower.MEDIUM_POWER);
                            m_mgr.stopScan();
                            semaphore.release();
                        }
                    }
                });
                m_mgr.startScan();
            }
        });
    }

    @Test
    public void scanPowerAutoBackground() throws Exception
    {
        m_config.scanPower = BleScanPower.AUTO;
        m_mgr.setConfig(m_config);
        m_mgr.onPause();
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
                            // We're in the background, so this should be low power here
                            // TODO - Uncomment the following line once the lollipop scanning API
                            // actually works at least as well as the pre-lollipop API.
                            //assertTrue(getScanPower() == BleScanPower.LOW_POWER);
                            m_mgr.stopScan();
                            m_mgr.onResume();
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
