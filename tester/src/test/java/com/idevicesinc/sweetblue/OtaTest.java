package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class OtaTest extends BaseBleUnitTest
{

    private final static UUID m_serviceUuid = UUID.randomUUID();
    private final static UUID m_charUuid = UUID.randomUUID();

    private GattDatabase db = new GattDatabase().addService(m_serviceUuid)
            .addCharacteristic(m_charUuid).setValue(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0}).setProperties().write().setPermissions().write().completeService();

    @Test
    public void otaTest() throws Exception
    {
        BleDevice device = m_mgr.newDevice(Util.randomMacAddress());

        device.connect(new BleDevice.StateListener()
        {
            @Override
            public void onEvent(StateEvent e)
            {
                if (e.didEnter(BleDeviceState.INITIALIZED))
                {
                    e.device().performOta(new TestOta());
                }
            }
        });
        startTest();
    }


    private final class TestOta extends BleTransaction.Ota
    {

        @Override
        protected void start(BleDevice device)
        {
            device.write(m_serviceUuid, m_charUuid, Util.randomBytes(10), new BleDevice.ReadWriteListener()
            {
                @Override
                public void onEvent(ReadWriteEvent e)
                {
                    assertTrue(e.wasSuccess());
                    e.device().write(m_serviceUuid, m_charUuid, Util.randomBytes(10), new BleDevice.ReadWriteListener()
                    {
                        @Override
                        public void onEvent(ReadWriteEvent e)
                        {
                            assertTrue(e.wasSuccess());
                            OtaTest.this.succeed();
                        }
                    });
                }
            });
        }
    }

    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.loggingEnabled = true;
        config.runOnMainThread = false;
        config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override
            public P_GattLayer newInstance(BleDevice device)
            {
                return new UnitTestGatt(device, db);
            }
        };
        return config;
    }

}
