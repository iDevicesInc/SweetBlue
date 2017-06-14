package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class BondTest extends BaseBleUnitTest
{


    @Test(timeout = 20000)
    public void bondTest() throws Exception
    {
        final Semaphore s = new Semaphore(0);

        BleDevice device = m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test device #1");
        device.bond(new BleDevice.BondListener()
        {
            @Override
            public void onEvent(BondEvent e)
            {
                assertTrue(e.wasSuccess());
                s.release();
            }
        });

        s.acquire();
    }

    @Test(timeout = 20000)
    public void bondRetryTest() throws Exception
    {
        final Semaphore s = new Semaphore(0);

        m_config.nativeDeviceFactory = new P_NativeDeviceLayerFactory()
        {
            @Override
            public P_NativeDeviceLayer newInstance(BleDevice device)
            {
                return new BondFailACoupleTimesLayer(device, 3);
            }
        };

        m_mgr.setConfig(m_config);

        BleDevice device = m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test device #2");
        device.bond(new BleDevice.BondListener()
        {
            @Override
            public void onEvent(BondEvent e)
            {
                assertTrue(e.wasSuccess());
                s.release();
            }
        });

        s.acquire();
    }

    @Test(timeout = 20000)
    public void bondFilterTest() throws Exception
    {
        final Semaphore s = new Semaphore(0);

        m_config.nativeDeviceFactory = new P_NativeDeviceLayerFactory()
        {
            @Override
            public P_NativeDeviceLayer newInstance(BleDevice device)
            {
                return new BondFailACoupleTimesLayer(device, 1);
            }
        };

        m_config.bondFilter = new BleDeviceConfig.BondFilter()
        {
            @Override
            public Please onEvent(StateChangeEvent e)
            {
                return Please.bondIf(e.didEnter(BleDeviceState.DISCOVERED));
            }

            @Override
            public Please onEvent(CharacteristicEvent e)
            {
                return Please.doNothing();
            }
        };

        m_mgr.setConfig(m_config);

        m_mgr.setListener_Bond(new BleDevice.BondListener()
        {
            @Override
            public void onEvent(BondEvent e)
            {
                assertFalse(e.wasSuccess());
                s.release();
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test device #3");

        s.acquire();
    }


    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.nativeDeviceFactory = new P_NativeDeviceLayerFactory<UnitTestDevice>()
        {
            @Override
            public UnitTestDevice newInstance(BleDevice device)
            {
                return new UnitTestDevice(device);
            }
        };
        config.loggingEnabled = true;
        return config;
    }


    private final class BondFailACoupleTimesLayer extends UnitTestDevice
    {

        private int m_failsSoFar;
        private final int m_maxFails;


        public BondFailACoupleTimesLayer(BleDevice device, int maxFails)
        {
            super(device);
            m_maxFails = maxFails;
        }

        @Override
        public boolean createBond()
        {
            if (m_failsSoFar >= m_maxFails)
            {
                UnitTestUtils.bondSuccess(getBleDevice(), Interval.millis(250));
            }
            else
            {
                m_failsSoFar++;
                UnitTestUtils.bondFail(getBleDevice(), BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN, Interval.millis(250));
                System.out.println("Failing bond request. Fails so far: " + m_failsSoFar);
            }
            return true;
        }

        @Override
        public boolean createBondSneaky(String methodName, boolean loggingEnabled)
        {
            if (m_failsSoFar >= 2)
            {
                UnitTestUtils.bondSuccess(getBleDevice(), Interval.millis(250));
            }
            else
            {
                m_failsSoFar++;
                UnitTestUtils.bondFail(getBleDevice(), BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN, Interval.millis(250));
            }
            return true;
        }
    }
}
