package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class NotifyTest extends BaseBleUnitTest
{

    private static final UUID mTestService = Uuids.fromShort("12BA");
    private static final UUID mTestChar = Uuids.fromShort("12BC");
    private static final UUID mTest2Char = Uuids.fromShort("12BD");
    private static final UUID mTestDesc = Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID;


    private BleDevice m_device;
    private GattDatabase dbNotifyWithDesc = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteNotify().setPermissions().read().build()
            .addDescriptor(mTestDesc).setPermissions().read().completeService();
    private GattDatabase dbNotify = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteNotify().setPermissions().read().completeChar()
            .addCharacteristic(mTest2Char).setProperties().readWriteNotify().setPermissions().read().completeService();
    private GattDatabase dbIndicateNoDesc = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteIndicate().setPermissions().read().completeService();

    @Test
    public void enableNotifyTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbNotifyWithDesc);

        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start(BleDevice device)
                    {
                        BleNotify notify = new BleNotify(mTestChar).setReadWriteListener(e1 ->
                        {
                            if (e1.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                NotifyTest.this.succeed();
                            }
                        });
                        m_device.enableNotify(notify);
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void enableNotifyNoDescriptorTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbNotify);

        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start(BleDevice device)
                    {
                        BleNotify notify = new BleNotify(mTestChar).setReadWriteListener(e1 ->
                        {
                            if (e1.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                NotifyTest.this.succeed();
                            }
                        });
                        m_device.enableNotify(notify);
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void multiNotifyTest() throws Exception
    {
        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbNotify);

        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "NotifyTesterererer");

        final boolean[] notifies = new boolean[2];

        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            BleNotify.Builder builder = new BleNotify.Builder(mTestService, mTestChar).setReadWriteListener(e1 ->
            {
                if (e1.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                {
                    assertTrue(e1.wasSuccess());
                    notifies[0] = true;
                }
            });
            builder.next().setCharacteristicUUID(mTest2Char).setReadWriteListener(e1 ->
            {
                if (e1.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                {
                    assertTrue(e1.wasSuccess());
                    succeed();
                }
            });
            device.enableNotifies(builder.build());
        });

        startTest();
    }

    @Test
    public void disableNotifyTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbNotify);

        m_mgr.setConfig(m_config);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start(BleDevice device)
                    {
                        final BleNotify notify = new BleNotify(mTestChar);
                        notify.setReadWriteListener(e1 ->
                        {
                            if (e1.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                notify.setReadWriteListener(e11 ->
                                {
                                    if (e11.type() == ReadWriteListener.Type.DISABLING_NOTIFICATION)
                                    {
                                        assertTrue("Disabling notification failed with status " + e11.status(), e11.wasSuccess());
                                        assertFalse(m_device.isNotifyEnabled(mTestChar));
                                        NotifyTest.this.succeed();
                                    }
                                });
                                m_device.disableNotify(notify);
                            }
                        });
                        m_device.enableNotify(notify);
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void disableNotifyNoDescriptorTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbNotify);

        m_mgr.setConfig(m_config);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start(BleDevice device)
                    {
                        final BleNotify notify = new BleNotify(mTestChar);
                        notify.setReadWriteListener(e1 ->
                        {
                            if (e1.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                notify.setReadWriteListener(e11 ->
                                {
                                    if (e11.type() == ReadWriteListener.Type.DISABLING_NOTIFICATION)
                                    {
                                        assertTrue("Disabling notification failed with status " + e11.status(), e11.wasSuccess());
                                        assertFalse(m_device.isNotifyEnabled(mTestChar));
                                        NotifyTest.this.succeed();
                                    }
                                });
                                m_device.disableNotify(notify);
                            }
                        });
                        m_device.enableNotify(notify);
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void enableAndReceiveNotifyUsingReadWriteListenerTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbNotifyWithDesc);

        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start(BleDevice device)
                    {
                        final BleNotify notify = new BleNotify(mTestChar).setReadWriteListener(e1 ->
                        {
                            if (e1.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                NativeUtil.sendNotification(m_device, e1.characteristic(), notifyData, Interval.millis(500));
                            }
                            else if (e1.type() == ReadWriteListener.Type.NOTIFICATION)
                            {
                                assertArrayEquals(notifyData, e1.data());
                                NotifyTest.this.succeed();
                            }
                        });
                        m_device.enableNotify(notify);
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void enableAndReceiveNotifyUsingNotificationListenerTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbNotifyWithDesc);

        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.setListener_Notification(e1 ->
                {
                    if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                    {
                        if (e1.wasSuccess())
                        {
                            NativeUtil.sendNotification(m_device, e1.characteristic(), notifyData, Interval.millis(500));
                        }
                    }
                    else if (e1.type() == NotificationListener.Type.NOTIFICATION)
                    {
                        assertArrayEquals(notifyData, e1.data());
                        succeed();
                    }
                });
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start(BleDevice device)
                    {
                        BleNotify notify = new BleNotify(mTestChar).setReadWriteListener(e12 ->
                        {
                            if (e12.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling failed with error " + e12.status(), e12.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                succeed();
                            }
                        });
                        m_device.enableNotify(notify);
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void enableAndReceiveIndicateTest() throws Exception
    {
        m_device = null;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbIndicateNoDesc);

        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start(BleDevice device)
                    {
                        BleNotify notify = new BleNotify(mTestChar).setReadWriteListener(e1 ->
                        {
                            if (e1.type() == ReadWriteListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling indication failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                NativeUtil.sendNotification(m_device, e1.characteristic(), notifyData, Interval.millis(500));
                            }
                            else if (e1.type() == ReadWriteListener.Type.INDICATION)
                            {
                                assertArrayEquals(notifyData, e1.data());
                                NotifyTest.this.succeed();
                            }
                        });
                        m_device.enableNotify(notify);
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void notifyStackTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, dbNotify);

        final byte[] first = Util_Unit.randomBytes(20);
        final byte[] second = Util_Unit.randomBytes(20);

        m_config.defaultInitFactory = () -> new BleTransaction.Init()
        {
            @Override
            protected void start(BleDevice device)
            {
                BleNotify notify = new BleNotify(mTestService, mTestChar)
                        .setReadWriteListener((e) ->
                        {
                            assertTrue(e.wasSuccess());
                            succeed();
                        });
                device.enableNotify(notify);
            }
        };

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "NotifMotif");

        final BluetoothGattCharacteristic ch = device.getNativeCharacteristic(mTestService, mTestChar);

        device.setListener_Notification((e) ->
        {
            if (e.type() == NotificationListener.Type.NOTIFICATION)
            {
                assertTrue(Arrays.equals(e.data(), first));
                device.pushListener_Notification((e1) ->
                {
                    assertTrue(Arrays.equals(e1.data(), second));
                    succeed();
                });
                NativeUtil.sendNotification(device, ch, second);
            }
        });

        device.connect((e) ->
        {
            assertTrue(e.wasSuccess());
            NativeUtil.sendNotification(device, ch, first);
        });

        startTest();
    }

}
