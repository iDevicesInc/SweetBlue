package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class WriteStripeTest extends BaseBleUnitTest
{

    private BleDevice m_device;

    private final static UUID tempServiceUuid = UUID.fromString("1234666a-1000-2000-8000-001199334455");
    private final static UUID tempUuid = UUID.fromString("1234666b-1000-2000-8000-001199334455");
    private final static UUID tempDescUuid = UUID.fromString("1234666d-1000-2000-8000-001199334455");


    @Test
    public void stripedWriteTest() throws Exception
    {
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
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                byte[] data = new byte[100];
                                new Random().nextBytes(data);
                                m_device.write(tempUuid, data, new BleDevice.ReadWriteListener()
                                {
                                    @Override public void onEvent(ReadWriteEvent e)
                                    {
                                        assertTrue(e.wasSuccess());
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
    public void stripedWriteDescriptorTest() throws Exception
    {
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
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                byte[] data = new byte[100];
                                new Random().nextBytes(data);
                                m_device.writeDescriptor(tempUuid, tempDescUuid, data, new BleDevice.ReadWriteListener()
                                {
                                    @Override public void onEvent(ReadWriteEvent e)
                                    {
                                        assertTrue(e.wasSuccess());
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



    @Override public P_GattLayer getGattLayer(BleDevice device)
    {
        return new StripedWriteGattLayer(device);
    }

    private class StripedWriteGattLayer extends UnitTestGatt
    {

        private BluetoothGattService mService;
        private BluetoothGattCharacteristic mChar;
        private BluetoothGattDescriptor mDesc;


        public StripedWriteGattLayer(BleDevice device)
        {
            super(device);
            mService = new BluetoothGattService(tempServiceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            mChar = new BluetoothGattCharacteristic(tempUuid, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
            mDesc = new BluetoothGattDescriptor(tempDescUuid, BluetoothGattDescriptor.PERMISSION_WRITE);
            mChar.addDescriptor(mDesc);
            mService.addCharacteristic(mChar);
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

        @Override public boolean writeDescriptor(final BluetoothGattDescriptor descriptor)
        {
            m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    getBleDevice().m_listeners.onDescriptorWrite(null, descriptor, BluetoothGatt.GATT_SUCCESS);
                }
            }, 150);
            return super.writeDescriptor(descriptor);
        }

        @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
        {
            return mService;
        }

        @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
        {
            List<BluetoothGattService> services = new ArrayList<>();
            services.add(mService);
            return services;
        }
    }
}
