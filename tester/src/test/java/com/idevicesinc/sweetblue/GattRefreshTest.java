package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class GattRefreshTest extends BaseBleUnitTest
{

    @Test(timeout = 12000)
    public void connectThenRefreshGattTest() throws Exception
    {

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final Pointer<Boolean> refreshingGatt = new Pointer<>(false);


        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED) || e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                final BleDevice device = e.device();
                device.setListener_State(e1 ->
                {
                    if (e1.didEnter(BleDeviceState.SERVICES_DISCOVERED))
                    {
                        if (refreshingGatt.value)
                        {
                            succeed();
                        }
                    }
                    if (e1.didEnter(BleDeviceState.INITIALIZED))
                    {
                        refreshingGatt.value = true;
                        e1.device().refreshGattDatabase();
                    }
                });
                device.connect();
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();

    }

}
