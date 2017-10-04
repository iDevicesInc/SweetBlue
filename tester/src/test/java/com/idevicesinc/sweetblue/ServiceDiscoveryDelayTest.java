package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ServiceDiscoveryDelayTest extends BaseBleUnitTest
{

    @Test
    public void delayedServiceDiscoveryTest() throws Exception
    {
        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.useGattRefresh = false;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override
            public P_GattLayer newInstance(BleDevice device)
            {
                return new Gatt(device);
            }
        };
        m_config.serviceDiscoveryDelay = Interval.ONE_SEC;

        m_mgr.setConfig(m_config);

        BleDevice fake = m_mgr.newDevice(Util.randomMacAddress(), "Fakey Fakerson");

        fake.setListener_State(new DeviceStateListener()
        {
            @Override
            public void onEvent(BleDevice.StateListener.StateEvent e)
            {
                if (e.didExit(BleDeviceState.DISCOVERING_SERVICES))
                {
                    Interval time = e.device().getTimeInState(BleDeviceState.DISCOVERING_SERVICES);
                    assertTrue(time.secs() >= 1.0);
                    succeed();
                }
            }
        });

        fake.connect();

        startTest();
    }


    private static final class Gatt extends UnitTestGatt
    {

        public Gatt(BleDevice device)
        {
            super(device);
        }

        @Override
        public Interval getDelayTime()
        {
            return Interval.millis(5);
        }
    }

}
