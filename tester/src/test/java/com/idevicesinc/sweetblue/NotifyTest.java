package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.Interval;
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
    private GattDatabase dbNotifyWithDesc = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteNotify().setPermissions().read().build()
            .addDescriptor(mTestDesc).setPermissions().read().completeService();
    private GattDatabase dbNotify = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteNotify().setPermissions().read().completeService();
    private GattDatabase dbIndicateNoDesc = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteIndicate().setPermissions().read().completeService();

    @Test
    public void enableNotifyTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new UnitTestGatt(device, dbNotifyWithDesc);
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
                                        assertTrue("Enabling notification failed with status " + e.status(), e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        succeed();
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
                return new UnitTestGatt(device, dbNotify);
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
                                        assertTrue("Enabling notification failed with status " + e.status(), e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        succeed();
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
                return new UnitTestGatt(device, dbNotify);
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
                                        assertTrue("Enabling notification failed with status " + e.status(), e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        succeed();
                                        m_device.disableNotify(mTestChar, new BleDevice.ReadWriteListener()
                                        {
                                            @Override public void onEvent(ReadWriteEvent e)
                                            {
                                                if (e.type() == Type.DISABLING_NOTIFICATION)
                                                {
                                                    assertTrue("Disabling notification failed with status " + e.status(), e.wasSuccess());
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
                return new UnitTestGatt(device, dbNotify);
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
                                        assertTrue("Enabling notification failed with status " + e.status(), e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        succeed();
                                        m_device.disableNotify(mTestChar, new BleDevice.ReadWriteListener()
                                        {
                                            @Override public void onEvent(ReadWriteEvent e)
                                            {
                                                if (e.type() == Type.DISABLING_NOTIFICATION)
                                                {
                                                    assertTrue("Disabling notification failed with status " + e.status(), e.wasSuccess());
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
                return new UnitTestGatt(device, dbNotifyWithDesc);
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
                                        assertTrue("Enabling notification failed with status " + e.status(), e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        succeed();
                                        UnitTestUtils.sendNotification(m_device, e.characteristic(), notifyData, Interval.millis(500));
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
                return new UnitTestGatt(device, dbNotifyWithDesc);
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
                                if (e.wasSuccess())
                                {
                                    UnitTestUtils.sendNotification(m_device, e.characteristic(), notifyData, Interval.millis(500));
                                }
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
                            m_device.enableNotify(mTestChar, new BleDevice.ReadWriteListener()
                            {
                                @Override
                                public void onEvent(ReadWriteEvent e)
                                {
                                    if (e.type() == Type.ENABLING_NOTIFICATION)
                                    {
                                        assertTrue("Enabling failed with error " + e.status(), e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        succeed();
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
    public void enableAndReceiveIndicateTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new UnitTestGatt(device, dbIndicateNoDesc);
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
                                        assertTrue("Enabling indication failed with status " + e.status(), e.wasSuccess());
                                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                                        succeed();
                                        UnitTestUtils.sendNotification(m_device, e.characteristic(), notifyData, Interval.millis(500));
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

}
