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
import java.util.Arrays;
import java.util.List;
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
                return new MultipleCharGatt(device);
            }
        };
        config.loggingEnabled = true;
        return config;
    }

    private class MultipleCharGatt extends UnitTestGatt
    {

        private final List<BluetoothGattService> mServices;

        public MultipleCharGatt(BleDevice device)
        {
            super(device);
            mServices = new ArrayList<>();
            BluetoothGattService service = new BluetoothGattService(mTestService, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic char1 = new BluetoothGattCharacteristic(mTestChar, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattCharacteristic char2 = new BluetoothGattCharacteristic(mTestChar, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattDescriptor desc1 = new BluetoothGattDescriptor(mTestDesc, BluetoothGattDescriptor.PERMISSION_READ);
            desc1.setValue(new byte[] { 0x1 });
            BluetoothGattDescriptor desc2 = new BluetoothGattDescriptor(mTestDesc, BluetoothGattDescriptor.PERMISSION_READ);
            desc1.setValue(new byte[] { 0x2 });
            char1.addDescriptor(desc1);
            char2.addDescriptor(desc2);
            service.addCharacteristic(char1);
            service.addCharacteristic(char2);
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

        @Override public boolean readDescriptor(final BluetoothGattDescriptor descriptor)
        {
            m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    getBleDevice().m_listeners.onDescriptorRead(null, descriptor, BluetoothGatt.GATT_SUCCESS);
                }
            }, 150);
            return super.readDescriptor(descriptor);
        }

        @Override public boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic)
        {
            m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    getBleDevice().m_listeners.onCharacteristicWrite(null, characteristic, BluetoothGatt.GATT_SUCCESS);
                }
            }, 150);
            return super.writeCharacteristic(characteristic);
        }

        @Override public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic)
        {
            characteristic.setValue(new byte[] { 0x2, 0x3, 0x4, 0x5, 0x6 });
            m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    getBleDevice().m_listeners.onCharacteristicRead(null, characteristic, BluetoothGatt.GATT_SUCCESS);
                }
            }, 150);
            return super.readCharacteristic(characteristic);
        }
    }

}
