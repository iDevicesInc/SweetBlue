package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;

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
public class ReadTest extends BaseBleUnitTest
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
    public void simpleReadTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "DeviceOfRead-ness");

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            BleRead read = new BleRead(firstServiceUuid, firstCharUuid).setReadWriteListener(r ->
            {
                assertTrue(r.wasSuccess());
                assertNotNull(r.data());
                succeed();
            });
            device.read(read);
        });

        startTest();
    }

    @Test
    public void multiReadTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "DeviceOfRead-nes");

        final boolean[] reads = new boolean[4];

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            BleRead.Builder builder = new BleRead.Builder(firstServiceUuid, firstCharUuid);
            builder.setReadWriteListener(r ->
            {
                assertTrue(r.status().name(), r.wasSuccess());
                assertNotNull(r.data());
                if (r.isFor(firstCharUuid))
                    reads[0] = true;
                else if (r.isFor(secondCharUuid))
                    reads[1] = true;
                else if (r.isFor(thirdCharUuid))
                    reads[2] = true;
                else if (r.isFor(fourthCharUuid))
                {
                    assertTrue(reads[0] && reads[1] && reads[2]);
                    succeed();
                }
                else
                    // We should never get to this option
                    assertTrue(false);
            })
                    .next().setServiceUUID(secondSeviceUuid).setCharacteristicUUID(secondCharUuid)
                    .next().setServiceUUID(thirdServiceUuid).setCharacteristicUUID(thirdCharUuid)
                    .next().setCharacteristicUUID(fourthCharUuid);
            device.readMany(builder.build());
        });

        startTest();
    }

    @Test
    public void readListenerStackTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "SomeBLEdevice");

        final BleRead read = new BleRead(firstServiceUuid, firstCharUuid);

        device.setListener_ReadWrite((e) ->
        {
            assertTrue(e.wasSuccess());
            device.pushListener_ReadWrite((e1) ->
            {
                assertTrue(e.wasSuccess());
                succeed();
            });
            device.read(read);
        });

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            device.read(read);
        });

        startTest();
    }

    @Override
    public P_GattLayer getGattLayer(BleDevice device)
    {
        return new ReadGatt(device, db);
    }

    private class ReadGatt extends UnitTestGatt
    {

        public ReadGatt(BleDevice device, GattDatabase gattDb)
        {
            super(device, gattDb);
        }

        @Override
        public boolean readCharacteristic(BluetoothGattCharacteristic characteristic)
        {
            characteristic.setValue(Util_Unit.randomBytes(20));
            return super.readCharacteristic(characteristic);
        }
    }

}
