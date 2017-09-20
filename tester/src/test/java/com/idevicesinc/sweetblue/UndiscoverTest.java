package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class UndiscoverTest extends BaseBleUnitTest
{


    @Test
    public void undiscoverConnectedDeviceTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Undiscover Me!");

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.UNDISCOVERED))
            {
                assertFalse(m_mgr.m_deviceMngr.has(device));
                assertFalse(device.is(BleDeviceState.CONNECTED));
                succeed();
            }
        });

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            device.undiscover();
        });

        startTest();

    }


}
