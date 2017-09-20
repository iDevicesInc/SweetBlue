package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

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
        doWriteTest(ReadWriteListener.Type.WRITE_NO_RESPONSE, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
    }

    @Test
    public void writeSignedTest() throws Exception
    {
        startTest(false);
        doWriteTest(ReadWriteListener.Type.WRITE_SIGNED, BluetoothGattCharacteristic.WRITE_TYPE_SIGNED);
    }

    @Test
    public void defaultWriteTest() throws Exception
    {
        startTest(false);
        doWriteTest(ReadWriteListener.Type.WRITE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    }


    private void doWriteTest(final ReadWriteListener.Type writeType, final int checkType) throws Exception
    {
        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect(e12 ->
                {
                    assertTrue(e12.wasSuccess());
                    BleWrite write = new BleWrite(m_WriteService, m_WriteChar);
                    write.setBytes(new byte[]{0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9}).setWriteType(writeType)
                            .setReadWriteListener(e1 ->
                            {
                                assertTrue(e1.status().name(), e1.wasSuccess());
                                BluetoothGattCharacteristic ch = e1.characteristic();
                                assertTrue(ch.getWriteType() == checkType);
                                succeed();
                            });
                    e12.device().write(write);
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress());
        reacquire();
    }

    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        m_config.loggingOptions = LogOptions.ON;
        config.runOnMainThread = false;
        config.gattLayerFactory = device -> new UnitTestGatt(device, db);
        return config;
    }

}
