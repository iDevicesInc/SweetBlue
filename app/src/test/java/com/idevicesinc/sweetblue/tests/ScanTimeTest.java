package com.idevicesinc.sweetblue.tests;


import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.PI_BleScanner;
import com.idevicesinc.sweetblue.PI_BleStatusHelper;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;


@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class ScanTimeTest extends BaseBleTest
{

    private static final int LEEWAY = 250;


    // While the scan itself is only 2 seconds, it takes a couple seconds for the test
    // to spin up the Java VM, so the timeout adds some padding to give it enough
    // time
    @Test(timeout = 4000)
    public void singleScanWithInterval() throws Exception
    {
        doSingleScanTest(2000);
    }

    @Test(timeout = 4000)
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
                final Pointer<Long> time = new Pointer<Long>();
                m_mgr.setListener_State(new BleManager.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didExit(BleManagerState.SCANNING))
                        {
                            if (didStop.get())
                            {
                                long diff = System.currentTimeMillis() - time.value;
                                // Make sure that our scan time is correct, this checks against
                                // 3x the scan amount (2 scans, 1 pause). We may need to add a bit
                                // to LEEWAY here, as it's going through 3 iterations, but for now
                                // it seems to be ok for the test
                                long targetTime = scanTime * 3;
                                assertTrue((diff - LEEWAY) < targetTime && targetTime < (diff + LEEWAY));
                                semaphore.release();
                            }
                            else
                            {
                                didStop.set(true);
                            }
                        }
                    }
                });
                time.value = System.currentTimeMillis();
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

    @Override PI_BleScanner getScanner()
    {
        return new DefaultBleScannerTest();
    }

    @Override PI_BleStatusHelper getStatusHelper()
    {
        return new DefaultStatusHelperTest();
    }
}
