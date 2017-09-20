package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class WriteTest extends BaseBleUnitTest
{

    private final static UUID firstServiceUuid = UUID.randomUUID();
    private final static UUID secondSeviceUuid = UUID.randomUUID();
    private final static UUID thirdServiceUuid = UUID.randomUUID();

    private final static UUID firstCharUuid = UUID.randomUUID();
    private final static UUID secondCharUuid = UUID.randomUUID();
    private final static UUID thirdCharUuid = UUID.randomUUID();
    private final static UUID fourthCharUuid = UUID.randomUUID();


    private GattDatabase db =
            new GattDatabase().addService(firstServiceUuid).addCharacteristic(firstCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService()
                    .addService(secondSeviceUuid).addCharacteristic(secondCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService()
                    .addService(thirdServiceUuid).addCharacteristic(thirdCharUuid).setProperties().readWrite().setPermissions().readWrite().completeChar()
                    .addCharacteristic(fourthCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService();

    @Test
    public void simpleWriteTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "DeviceOfWrite-ness");

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            BleWrite write = new BleWrite(firstServiceUuid, firstCharUuid).setBytes(Util_Unit.randomBytes(20)).setReadWriteListener(r ->
            {
                assertTrue(r.status().name(), r.wasSuccess());
                assertNotNull(r.data());
                succeed();
            });
            device.write(write);
        });

        startTest();
    }

    @Test
    public void multiWriteTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "DeviceOfRead-nes");

        final boolean[] writes = new boolean[4];

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            BleWrite.Builder builder = new BleWrite.Builder(firstServiceUuid, firstCharUuid);
            builder.setBytes(Util_Unit.randomBytes(20)).setReadWriteListener(r ->
            {
                assertTrue(r.status().name(), r.wasSuccess());
                assertNotNull(r.data());
                if (r.isFor(firstCharUuid))
                    writes[0] = true;
                else if (r.isFor(secondCharUuid))
                    writes[1] = true;
                else if (r.isFor(thirdCharUuid))
                    writes[2] = true;
                else if (r.isFor(fourthCharUuid))
                {
                    assertTrue(writes[0] && writes[1] && writes[2]);
                    succeed();
                }
                else
                    // We should never get to this option
                    assertTrue(false);
            })
                    .next().setServiceUUID(secondSeviceUuid).setCharacteristicUUID(secondCharUuid).setBytes(Util_Unit.randomBytes(20))
                    .next().setServiceUUID(thirdServiceUuid).setCharacteristicUUID(thirdCharUuid).setBytes(Util_Unit.randomBytes(20))
                    .next().setCharacteristicUUID(fourthCharUuid).setBytes(Util_Unit.randomBytes(20));
            device.writeMany(builder.build());
        });

        startTest();
    }

    @Test
    public void writeStackTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "WriteMonster");

        final BleWrite write = new BleWrite(firstServiceUuid, firstCharUuid).setBytes(Util_Unit.randomBytes(20));

        device.setListener_ReadWrite((e) ->
        {
            assertTrue(e.wasSuccess());
            device.pushListener_ReadWrite((e1) ->
            {
                assertTrue(e.wasSuccess());
                succeed();
            });
            write.setBytes(Util_Unit.randomBytes(20));
            device.write(write);
        });

        device.connect((e) ->
        {
            assertTrue(e.wasSuccess());
            device.write(write);
        });

        startTest();
    }

    @Override
    public P_GattLayer getGattLayer(BleDevice device)
    {
        return new UnitTestGatt(device, db);
    }
}
