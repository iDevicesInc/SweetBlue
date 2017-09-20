package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Arrays;
import java.util.UUID;
import com.idevicesinc.sweetblue.utils.GattDatabase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class GetServiceExceptionTest extends BaseBleUnitTest
{

    private GattDatabase db = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID).
            addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().read().setProperties().read().completeService();

    private GattDatabase db2 = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID).
            addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().read().setProperties().read().build().
            addDescriptor(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).setValue(new byte[] { 0x05 }).setPermissions().read().completeChar().
            addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().read().setProperties().read().build().
            addDescriptor(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).setValue(new byte[] { 0x08 }).setPermissions().read().completeService();

    @Test
    public void getServiceExceptionTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "ImaBlowUp");

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(e1 ->
            {
                assertTrue(e1.status() == ReadWriteListener.Status.GATT_CONCURRENT_EXCEPTION);
                succeed();
            });
            device.read(read);
        });

        startTest();
    }

    @Test
    public void getServiceFromDuplicateExceptionTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_config.gattLayerFactory = device -> new ConcurrentGatt(device, db2);

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "ImaBlowUp");

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setDescriptorFilter(new DescriptorFilter()
            {
                @Override
                public Please onEvent(DescriptorEvent event)
                {
                    return Please.acceptIf(Arrays.equals(event.value(), new byte[] { 0x08 }));
                }

                @Override
                public UUID descriptorUuid()
                {
                    return Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID;
                }
            }).setReadWriteListener(e1 ->
            {
                assertTrue(e1.status() == ReadWriteListener.Status.GATT_CONCURRENT_EXCEPTION);
                succeed();
            });
            device.read(read);
        });

        startTest();
    }

    @Override
    public P_GattLayer getGattLayer(BleDevice device)
    {
        return new ConcurrentGatt(device);
    }

    private class ConcurrentGatt extends UnitTestGatt
    {

        public ConcurrentGatt(BleDevice device)
        {
            super(device, db);
        }

        public ConcurrentGatt(BleDevice device, GattDatabase db)
        {
            super(device, db);
        }

        @Override
        public BleServiceWrapper getBleService(UUID serviceUuid, P_Logger logger)
        {
            return new BleServiceWrapper(UhOhListener.UhOh.CONCURRENT_EXCEPTION);
        }
    }

}
