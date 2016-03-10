package com.idevicesinc.sweetblue.tests;


import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanMode;
import com.idevicesinc.sweetblue.PI_BleScanner;
import com.idevicesinc.sweetblue.PI_BleStatusHelper;
import com.idevicesinc.sweetblue.utils.Interval;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Semaphore;


@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class ScanModeTest extends BaseBleTest
{

    @Test
    public void scanModeClassicTest() throws Exception
    {
        m_config.scanMode = BleScanMode.CLASSIC;
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
                            assertTrue(getScanMode(BleManagerState.SCANNING) == BleScanMode.CLASSIC);
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
