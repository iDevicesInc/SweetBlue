package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class BondTest extends BaseBleUnitTest
{


    @Test(timeout = 20000)
    public void bondTest() throws Exception
    {
        BleDevice device = m_mgr.newDevice(Util.randomMacAddress(), "Test device #1");
        device.bond(new BleDevice.BondListener()
        {
            @Override
            public void onEvent(BondEvent e)
            {
                assertTrue(e.wasSuccess());
                succeed();
            }
        });

        startTest();
    }

    @Test(timeout = 20000)
    public void bondRetryTest() throws Exception
    {
        m_config.nativeDeviceFactory = new P_NativeDeviceLayerFactory()
        {
            @Override
            public P_NativeDeviceLayer newInstance(BleDevice device)
            {
                return new BondFailACoupleTimesLayer(device, 3);
            }
        };

        m_mgr.setConfig(m_config);

        BleDevice device = m_mgr.newDevice(Util.randomMacAddress(), "Test device #2");
        device.bond(new BleDevice.BondListener()
        {
            @Override
            public void onEvent(BondEvent e)
            {
                assertTrue(e.wasSuccess());
                succeed();
            }
        });

        startTest();
    }

    @Test(timeout = 20000)
    public void bondFilterTest() throws Exception
    {
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
                succeed();
            }
        });

        m_mgr.newDevice(Util.randomMacAddress(), "Test device #3");

        startTest();
    }


    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
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
                return super.createBond();
            }
            else
            {
                m_failsSoFar++;
                NativeUtil.bondFail(getBleDevice(), BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN, Interval.millis(250));
                System.out.println("Failing bond request. Fails so far: " + m_failsSoFar);
            }
            return true;
        }

        @Override
        public boolean createBondSneaky(String methodName, boolean loggingEnabled)
        {
            if (m_failsSoFar >= 2)
            {
                return super.createBondSneaky(methodName, loggingEnabled);
            }
            else
            {
                m_failsSoFar++;
                NativeUtil.bondFail(getBleDevice(), BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN, Interval.millis(250));
            }
            return true;
        }
    }
}
