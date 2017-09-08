package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Util;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertFalse;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class UndiscoverTest extends BaseBleUnitTest
{


    @Test
    public void undiscoverConnectedDeviceTest() throws Exception
    {
        m_config.loggingEnabled = true;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util.randomMacAddress(), "Undiscover Me!");

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.UNDISCOVERED))
                {
                    assertFalse(m_mgr.m_deviceMngr.has(device));
                    assertFalse(device.is(BleDeviceState.CONNECTED));
                    succeed();
                }
            }
        });

        device.connect(new BleDevice.StateListener() {
            @Override
            public void onEvent(BleDevice.StateListener.StateEvent e)
            {
                if (e.didEnter(BleDeviceState.INITIALIZED))
                {
                    device.undiscover();
                }
            }
        });

        startTest();

    }


}
