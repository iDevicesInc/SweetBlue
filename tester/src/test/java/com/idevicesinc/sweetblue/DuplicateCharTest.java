package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class DuplicateCharTest extends BaseBleUnitTest
{

    private final static UUID mTestService = Uuids.fromShort("ABCD");
    private final static UUID mTestChar = Uuids.fromShort("1234");
    private final static UUID mTestDesc = Uuids.CHARACTERISTIC_PRESENTATION_FORMAT_DESCRIPTOR_UUID;
    private final static UUID mNotifyDesc = Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID;

    private BleDevice m_device;

    private GattDatabase db = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setValue(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}).setProperties().readWriteNotify().setPermissions().readWrite().build()
            .addDescriptor(mTestDesc).setValue(new byte[]{0x1}).setPermissions().read().completeChar()
            .addCharacteristic(mTestChar).setValue(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}).setProperties().readWriteNotify().setPermissions().readWrite().build()
            .addDescriptor(mTestDesc).setValue(new byte[]{0x2}).setPermissions().read().completeService();

    private GattDatabase db2 = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setValue(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}).setProperties().readWriteNotify().setPermissions().readWrite().build()
            .addDescriptor(mNotifyDesc).setPermissions().readWrite().completeChar()
            .addCharacteristic(mTestChar).setValue(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}).setProperties().readWrite().setPermissions().readWrite().completeService();


    @Test
    public void writeCharWhenMultipleExistTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    assertTrue(e1.wasSuccess());
                    final BleWrite bleWrite = new BleWrite(mTestChar).setBytes(new byte[]{0x0, 0x1, 0x2, 0x3});
                    bleWrite.setDescriptorFilter(new DescriptorFilter()
                    {
                        @Override
                        public Please onEvent(DescriptorEvent event)
                        {
                            return Please.acceptIf(event.value()[0] == 0x2);
                        }

                        @Override
                        public UUID descriptorUuid()
                        {
                            return mTestDesc;
                        }
                    });
                    m_device.write(bleWrite, e11 ->
                    {
                        assertTrue(e11.status().name(), e11.wasSuccess());
                        assertTrue(e11.characteristic().getDescriptor(mTestDesc).getValue()[0] == 2);
                        succeed();
                    });
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void readCharWhenMultipleExistTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    assertTrue(e1.wasSuccess());
                    BleRead read = new BleRead(mTestChar).setDescriptorFilter(new DescriptorFilter()
                    {
                        @Override
                        public Please onEvent(DescriptorEvent event)
                        {
                            return Please.acceptIf(event.value()[0] == 0x2);
                        }

                        @Override
                        public UUID descriptorUuid()
                        {
                            return mTestDesc;
                        }
                    }).setReadWriteListener(e11 ->
                    {
                        assertTrue(e11.status().name(), e11.wasSuccess());
                        assertTrue(e11.characteristic().getDescriptor(mTestDesc).getValue()[0] == 2);
                        assertTrue("Goal: " + Arrays.toString(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}) + " Return: " + Arrays.toString(e11.data()), Arrays.equals(e11.data(), new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}));
                        succeed();
                    });
                    m_device.read(read);
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();

    }

    @Test
    public void readCharWhenMultipleExistOutOfSpecTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, db2);

        m_mgr.setConfig(m_config);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    assertTrue(e1.wasSuccess());
                    BleRead read = new BleRead(mTestChar).setDescriptorFilter(new DescriptorFilter()
                    {
                        @Override
                        public Please onEvent(DescriptorEvent event)
                        {
                            // we're looking to read the char withOUT the notification descriptor
                            return Please.acceptIf(event.characteristic().getDescriptor(mNotifyDesc) == null);
                        }

                        @Override
                        public UUID descriptorUuid()
                        {
                            return null;
                        }
                    }).setReadWriteListener(e11 ->
                    {
                        assertTrue(e11.status().name(), e11.wasSuccess());
                        assertNull(e11.characteristic().getDescriptor(mNotifyDesc));
                        assertTrue(Arrays.equals(e11.data(), new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}));
                        succeed();
                    });
                    m_device.read(read);
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();

    }

    @Test
    public void enableNotifyMultipleExistTest() throws Exception
    {
        m_device = null;

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    assertTrue(e1.wasSuccess());
                    BleNotify notify = new BleNotify(mTestService, mTestChar).setDescriptorFilter(new DescriptorFilter()
                    {
                        @Override
                        public Please onEvent(DescriptorEvent event)
                        {
                            return Please.acceptIf(event.value()[0] == 0x2);
                        }

                        @Override
                        public UUID descriptorUuid()
                        {
                            return mTestDesc;
                        }
                    }).setReadWriteListener(e11 ->
                    {
                        if (e11.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                        {
                            assertTrue(e11.wasSuccess());
                            succeed();
                        }
                    }).setForceReadTimeout(Interval.DISABLED);
                    m_device.enableNotify(notify);
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.gattLayerFactory = device -> new UnitTestGatt(device, db);
        config.loggingOptions = LogOptions.ON;
        return config;
    }

}
