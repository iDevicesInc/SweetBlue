package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class NotifyTest extends BaseBleUnitTest
{

    private static final UUID mTestService = Uuids.fromShort("12BA");
    private static final UUID mTestChar = Uuids.fromShort("12BC");
    private static final UUID mTestDesc = Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID;


    private BleDevice m_device;


    @Test
    public void enableNotifyTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new NotifyGattWithDescLayer(device);
            }
        };

        m_config.loggingEnabled = true;

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleTransaction.Init()
                    {
                        @Override protected void start(BleDevice device)
                        {
                            m_device.enableNotify(mTestChar, new BleDevice.ReadWriteListener()
                            {
                                @Override public void onEvent(ReadWriteEvent e)
                                {
                                    if (e.type() == Type.ENABLING_NOTIFICATION)
                                    {
                                        assertTrue(e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        s.release();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test
    public void enableNotifyNoDescriptorTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new NotifyGattWithNoDescLayer(device);
            }
        };

        m_config.loggingEnabled = true;

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleTransaction.Init()
                    {
                        @Override protected void start(BleDevice device)
                        {
                            m_device.enableNotify(mTestChar, new BleDevice.ReadWriteListener()
                            {
                                @Override public void onEvent(ReadWriteEvent e)
                                {
                                    if (e.type() == Type.ENABLING_NOTIFICATION)
                                    {
                                        assertTrue(e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        s.release();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test
    public void disableNotifyTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new NotifyGattWithDescLayer(device);
            }
        };

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleTransaction.Init()
                    {
                        @Override protected void start(BleDevice device)
                        {
                            m_device.enableNotify(mTestChar, new BleDevice.ReadWriteListener()
                            {
                                @Override public void onEvent(ReadWriteEvent e)
                                {
                                    if (e.type() == Type.ENABLING_NOTIFICATION)
                                    {
                                        assertTrue(e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        m_device.disableNotify(mTestChar, new BleDevice.ReadWriteListener()
                                        {
                                            @Override public void onEvent(ReadWriteEvent e)
                                            {
                                                if (e.type() == Type.DISABLING_NOTIFICATION)
                                                {
                                                    assertTrue(e.wasSuccess());
                                                    assertFalse(m_device.isNotifyEnabled(mTestChar));
                                                    s.release();
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test
    public void disableNotifyNoDescriptorTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new NotifyGattWithNoDescLayer(device);
            }
        };

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleTransaction.Init()
                    {
                        @Override protected void start(BleDevice device)
                        {
                            m_device.enableNotify(mTestChar, new BleDevice.ReadWriteListener()
                            {
                                @Override public void onEvent(ReadWriteEvent e)
                                {
                                    if (e.type() == Type.ENABLING_NOTIFICATION)
                                    {
                                        assertTrue(e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        m_device.disableNotify(mTestChar, new BleDevice.ReadWriteListener()
                                        {
                                            @Override public void onEvent(ReadWriteEvent e)
                                            {
                                                if (e.type() == Type.DISABLING_NOTIFICATION)
                                                {
                                                    assertTrue(e.wasSuccess());
                                                    assertFalse(m_device.isNotifyEnabled(mTestChar));
                                                    s.release();
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test
    public void enableAndReceiveNotifyUsingReadWriteListenerTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new NotifyGattWithDescLayer(device);
            }
        };

        m_config.loggingEnabled = true;

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleTransaction.Init()
                    {
                        @Override protected void start(BleDevice device)
                        {
                            m_device.enableNotify(mTestChar, new BleDevice.ReadWriteListener()
                            {
                                @Override public void onEvent(ReadWriteEvent e)
                                {
                                    if (e.type() == Type.ENABLING_NOTIFICATION)
                                    {
                                        assertTrue(e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        UnitTestUtils.sendNotification(m_device, e.characteristic(), notifyData, 250);
                                    }
                                    else if (e.type() == Type.NOTIFICATION)
                                    {
                                        assertArrayEquals(notifyData, e.data());
                                        s.release();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test
    public void enableAndReceiveNotifyUsingNotificationListenerTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new NotifyGattWithDescLayer(device);
            }
        };

        m_config.loggingEnabled = true;

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.setListener_Notification(new NotificationListener()
                    {
                        @Override public void onEvent(NotificationEvent e)
                        {
                            if (e.type() == Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue(e.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                UnitTestUtils.sendNotification(m_device, e.characteristic(), notifyData, 250);
                            }
                            else if (e.type() == Type.NOTIFICATION)
                            {
                                assertArrayEquals(notifyData, e.data());
                                s.release();
                            }
                        }
                    });
                    m_device.connect(new BleTransaction.Init()
                    {
                        @Override protected void start(BleDevice device)
                        {
                            m_device.enableNotify(mTestChar);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test
    public void enableAndReceiveIndicateTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new IndicateGattWithNoDescLayer(device);
            }
        };

        m_config.loggingEnabled = true;

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleTransaction.Init()
                    {
                        @Override protected void start(BleDevice device)
                        {
                            m_device.enableNotify(mTestChar, new BleDevice.ReadWriteListener()
                            {
                                @Override public void onEvent(ReadWriteEvent e)
                                {
                                    if (e.type() == Type.ENABLING_NOTIFICATION)
                                    {
                                        assertTrue(e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        UnitTestUtils.sendNotification(m_device, e.characteristic(), notifyData, 250);
                                    }
                                    else if (e.type() == Type.INDICATION)
                                    {
                                        assertArrayEquals(notifyData, e.data());
                                        s.release();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    private class NotifyGattWithDescLayer extends UnitTestGatt
    {

        private final List<BluetoothGattService> mServices;


        public NotifyGattWithDescLayer(BleDevice device)
        {
            super(device);
            mServices = new ArrayList<>();
            BluetoothGattService service = new BluetoothGattService(mTestService, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(mTestChar, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattDescriptor desc = new BluetoothGattDescriptor(mTestDesc, BluetoothGattDescriptor.PERMISSION_READ);
            characteristic.addDescriptor(desc);
            service.addCharacteristic(characteristic);
            mServices.add(service);
        }

        @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
        {
            return mServices;
        }

        @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
        {
            if (serviceUuid.equals(mTestService))
            {
                return mServices.get(0);
            }
            else
            {
                return null;
            }
        }

        @Override public boolean writeDescriptor(final BluetoothGattDescriptor descriptor)
        {
            if (descriptor.getUuid().equals(mTestDesc))
            {
                m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
                {
                    @Override public void run()
                    {
                        getBleDevice().m_listeners.onDescriptorWrite(null, descriptor, BluetoothGatt.GATT_SUCCESS);
                    }
                }, 150);
                return true;
            }
            return false;
        }
    }

    private class NotifyGattWithNoDescLayer extends UnitTestGatt
    {

        private final List<BluetoothGattService> mServices;


        public NotifyGattWithNoDescLayer(BleDevice device)
        {
            super(device);
            mServices = new ArrayList<>();
            BluetoothGattService service = new BluetoothGattService(mTestService, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(mTestChar, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
            service.addCharacteristic(characteristic);
            mServices.add(service);
        }

        @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
        {
            return mServices;
        }

        @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
        {
            if (serviceUuid.equals(mTestService))
            {
                return mServices.get(0);
            }
            else
            {
                return null;
            }
        }

        @Override public boolean writeDescriptor(final BluetoothGattDescriptor descriptor)
        {
            if (descriptor.getUuid().equals(mTestDesc))
            {
                m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
                {
                    @Override public void run()
                    {
                        getBleDevice().m_listeners.onDescriptorWrite(null, descriptor, BluetoothGatt.GATT_SUCCESS);
                    }
                }, 150);
                return true;
            }
            return false;
        }
    }

    private class IndicateGattWithNoDescLayer extends UnitTestGatt
    {

        private final List<BluetoothGattService> mServices;


        public IndicateGattWithNoDescLayer(BleDevice device)
        {
            super(device);
            mServices = new ArrayList<>();
            BluetoothGattService service = new BluetoothGattService(mTestService, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(mTestChar, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PERMISSION_READ);
            service.addCharacteristic(characteristic);
            mServices.add(service);
        }

        @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
        {
            return mServices;
        }

        @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
        {
            if (serviceUuid.equals(mTestService))
            {
                return mServices.get(0);
            }
            else
            {
                return null;
            }
        }

        @Override public boolean writeDescriptor(final BluetoothGattDescriptor descriptor)
        {
            if (descriptor.getUuid().equals(mTestDesc))
            {
                m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
                {
                    @Override public void run()
                    {
                        getBleDevice().m_listeners.onDescriptorWrite(null, descriptor, BluetoothGatt.GATT_SUCCESS);
                    }
                }, 150);
                return true;
            }
            return false;
        }
    }


}
