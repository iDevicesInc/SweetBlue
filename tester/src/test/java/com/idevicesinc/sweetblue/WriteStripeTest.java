package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.utils.ByteBuffer;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class WriteStripeTest extends BaseBleUnitTest
{

    private final static UUID tempServiceUuid = UUID.fromString("1234666a-1000-2000-8000-001199334455");
    private final static UUID tempUuid = UUID.fromString("1234666b-1000-2000-8000-001199334455");
    private final static UUID tempDescUuid = UUID.fromString("1234666d-1000-2000-8000-001199334455");


    private BleDevice m_device;

    private GattDatabase db = new GattDatabase().addService(tempServiceUuid)
            .addCharacteristic(tempUuid).setProperties().write().setPermissions().write().build()
            .addDescriptor(tempDescUuid).setPermissions().write().completeService();

    private ByteBuffer m_buffer;


    @Test
    public void stripedWriteTest() throws Exception
    {
        m_config.loggingEnabled = true;

        m_buffer = new ByteBuffer();

        m_mgr.setConfig(m_config);

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
                                final byte[] data = new byte[100];
                                new Random().nextBytes(data);
                                m_device.write(tempUuid, data, new BleDevice.ReadWriteListener()
                                {
                                    @Override public void onEvent(ReadWriteEvent e)
                                    {
                                        assertTrue(e.wasSuccess());
                                        assertArrayEquals(data, m_buffer.bytesAndClear());
                                        succeed();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(Util.randomMacAddress(), "Test Device");

        startTest();
    }

    @Test
    public void stripedWriteDescriptorTest() throws Exception
    {
        m_config.loggingEnabled = true;

        m_buffer = new ByteBuffer();

        m_mgr.setConfig(m_config);

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
                                final byte[] data = new byte[100];
                                new Random().nextBytes(data);
                                m_device.writeDescriptor(tempUuid, tempDescUuid, data, new BleDevice.ReadWriteListener()
                                {
                                    @Override public void onEvent(ReadWriteEvent e)
                                    {
                                        assertTrue(e.wasSuccess());
                                        assertArrayEquals(data, m_buffer.bytesAndClear());
                                        succeed();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(Util.randomMacAddress(), "Test Device");

        startTest();
    }

    @Override public P_GattLayer getGattLayer(BleDevice device)
    {
        return new StripeGatt(device);
    }

    private final class StripeGatt extends UnitTestGatt
    {


        public StripeGatt(BleDevice device)
        {
            super(device, db);
        }

        @Override
        public boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data)
        {
            m_buffer.append(data);
            return super.setCharValue(characteristic, data);
        }

        @Override
        public boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data)
        {
            m_buffer.append(data);
            return super.setDescValue(descriptor, data);
        }
    }
}
