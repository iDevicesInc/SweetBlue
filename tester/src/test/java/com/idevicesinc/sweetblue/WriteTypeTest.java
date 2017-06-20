package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class WriteTypeTest extends BaseBleUnitTest
{

    private final static UUID m_WriteService = UUID.randomUUID();
    private final static UUID m_WriteChar = UUID.randomUUID();


    private GattDatabase db = new GattDatabase().addService(m_WriteService)
            .addCharacteristic(m_WriteChar).setProperties().write().write_no_response().signed_write().setPermissions().write().signed_write().completeService();

    @Test
    public void writeNoResponseTest() throws Exception
    {
        startTest(false);
        doWriteTest(BleDevice.ReadWriteListener.Type.WRITE_NO_RESPONSE, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
    }

    @Test
    public void writeSignedTest() throws Exception
    {
        startTest(false);
        doWriteTest(BleDevice.ReadWriteListener.Type.WRITE_SIGNED, BluetoothGattCharacteristic.WRITE_TYPE_SIGNED);
    }

    @Test
    public void defaultWriteTest() throws Exception
    {
        startTest(false);
        doWriteTest(BleDevice.ReadWriteListener.Type.WRITE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    }



    private void doWriteTest(final BleDevice.ReadWriteListener.Type writeType, final int checkType) throws Exception
    {
        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    e.device().connect(new BleDevice.StateListener()
                    {
                        @Override
                        public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                WriteBuilder write = new WriteBuilder(m_WriteService, m_WriteChar);
                                write.setBytes(new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9 }).setWriteType(writeType)
                                        .setReadWriteListener(new ReadWriteListener()
                                        {
                                            @Override
                                            public void onEvent(BleDevice.ReadWriteListener.ReadWriteEvent e)
                                            {
                                                assertTrue(e.wasSuccess());
                                                BluetoothGattCharacteristic ch = e.characteristic();
                                                assertTrue(ch.getWriteType() == checkType);
                                                succeed();
                                            }
                                        });
                                e.device().write(write);
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(Util.randomMacAddress());
        reacquire();
    }

    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.loggingEnabled = true;
        config.runOnMainThread = false;
        config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override
            public P_GattLayer newInstance(BleDevice device)
            {
                return new UnitTestGatt(device, db);
            }
        };
        return config;
    }

}
