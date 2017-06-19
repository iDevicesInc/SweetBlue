package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class DuplicateCharTest extends BaseBleUnitTest
{

    private final static UUID mTestService = Uuids.fromShort("ABCD");
    private final static UUID mTestChar = Uuids.fromShort("1234");
    private final static UUID mTestDesc = Uuids.CHARACTERISTIC_PRESENTATION_FORMAT_DESCRIPTOR_UUID;

    private BleDevice m_device;

    private GattDatabase db = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setValue(new byte[] { 0x2, 0x3, 0x4, 0x5, 0x6 }).setProperties().readWriteNotify().setPermissions().readWrite().build()
            .addDescriptor(mTestDesc).setValue(new byte[] { 0x1 }).setPermissions().read().completeChar()
            .addCharacteristic(mTestChar).setValue(new byte[] { 0x2, 0x3, 0x4, 0x5, 0x6 }).setProperties().readWriteNotify().setPermissions().readWrite().build()
            .addDescriptor(mTestDesc).setValue(new byte[] { 0x2 }).setPermissions().read().completeService();


    @Test
    public void writeCharWhenMultipleExistTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                m_device.write(mTestChar, new byte[]{0x0, 0x1, 0x2, 0x3}, new DescriptorFilter()
                                {
                                    @Override public Please onEvent(DescriptorEvent event)
                                    {
                                        return Please.acceptIf(event.value()[0] == 0x2);
                                    }

                                    @Override public UUID descriptorUuid()
                                    {
                                        return mTestDesc;
                                    }
                                }, new BleDevice.ReadWriteListener()
                                {
                                    @Override public void onEvent(ReadWriteEvent e)
                                    {
                                        assertTrue(e.status().name(), e.wasSuccess());
                                        assertTrue(e.characteristic().getDescriptor(mTestDesc).getValue()[0] == 2);
                                        s.release();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test
    public void readCharWhenMultipleExistTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                m_device.read(mTestChar, new DescriptorFilter()
                                {
                                    @Override public Please onEvent(DescriptorEvent event)
                                    {
                                        return Please.acceptIf(event.value()[0] == 0x2);
                                    }

                                    @Override public UUID descriptorUuid()
                                    {
                                        return mTestDesc;
                                    }
                                }, new BleDevice.ReadWriteListener()
                                {
                                    @Override public void onEvent(ReadWriteEvent e)
                                    {
                                        assertTrue(e.status().name(), e.wasSuccess());
                                        assertTrue(e.characteristic().getDescriptor(mTestDesc).getValue()[0] == 2);
                                        assertTrue(Arrays.equals(e.data(), new byte[] { 0x2, 0x3, 0x4, 0x5, 0x6 }));
                                        s.release();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();

    }

    @Test
    public void enableNotifyMultipleExistTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                m_device.enableNotify(mTestService, mTestChar, Interval.DISABLED, new DescriptorFilter()
                                {
                                    @Override public Please onEvent(DescriptorEvent event)
                                    {
                                        return Please.acceptIf(event.value()[0] == 0x2);
                                    }

                                    @Override public UUID descriptorUuid()
                                    {
                                        return mTestDesc;
                                    }
                                }, new BleDevice.ReadWriteListener()
                                {
                                    @Override public void onEvent(ReadWriteEvent e)
                                    {
                                        if (e.type() == Type.ENABLING_NOTIFICATION)
                                        {
                                            assertTrue(e.wasSuccess());
                                            s.release();
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Override public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new UnitTestGatt(device, db);
            }
        };
        config.loggingEnabled = true;
        return config;
    }

}
