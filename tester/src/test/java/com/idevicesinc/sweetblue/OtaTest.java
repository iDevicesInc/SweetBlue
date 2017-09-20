package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.UUID;
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
        BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress());

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            e.device().performOta(new TestOta());
        });
        startTest();
    }


    private final class TestOta extends BleTransaction.Ota
    {

        @Override
        protected void start(BleDevice device)
        {
            final BleWrite bleWrite = new BleWrite(m_serviceUuid, m_charUuid).setBytes(Util_Unit.randomBytes(10));
            device.write(bleWrite, e -> {

                assertTrue(e.wasSuccess());
                bleWrite.setBytes(Util_Unit.randomBytes(10));
                device.write(bleWrite, e1 -> {

                    assertTrue(e1.wasSuccess());
                    bleWrite.setBytes(Util_Unit.randomBytes(10));
                    device.write(bleWrite, e2 -> {

                        assertTrue(e2.wasSuccess());
                        OtaTest.this.succeed();
                    });
                });
            });
        }
    }

    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.loggingOptions = LogOptions.ON;
        config.runOnMainThread = false;
        config.gattLayerFactory = device -> new UnitTestGatt(device, db);
        return config;
    }

}
