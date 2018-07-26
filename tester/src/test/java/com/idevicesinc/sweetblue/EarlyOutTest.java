package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Util;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class EarlyOutTest extends BaseBleUnitTest
{

    @Test
    public void notConnectedEarlyOutTest() throws Exception
    {
        startTest(false);
        BleDevice device = m_mgr.newDevice(Util.randomMacAddress());
        BleDevice.ReadWriteListener.ReadWriteEvent event = device.read(Uuids.BATTERY_SERVICE_UUID);
        assertNotNull("ReadWriteEvent was null!", event);
        assertTrue(event.status() == BleDevice.ReadWriteListener.Status.NOT_CONNECTED);
        succeed();
    }

    @Test
    public void noMatchingTargetTest() throws Exception
    {
        BleDevice device = m_mgr.newDevice(Util.randomMacAddress());
        device.connect(e ->
        {
            if (e.didEnter(BleDeviceState.INITIALIZED))
            {
                BleDevice.ReadWriteListener.ReadWriteEvent event = device.read(Uuids.BATTERY_SERVICE_UUID);
                assertTrue(event.status() == BleDevice.ReadWriteListener.Status.NO_MATCHING_TARGET);
                succeed();
            }
        });
        startTest(true);
    }

}
