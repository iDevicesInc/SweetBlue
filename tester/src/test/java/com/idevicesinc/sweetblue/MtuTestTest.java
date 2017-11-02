package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;
import com.idevicesinc.sweetblue.utils.Util;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Random;
import java.util.UUID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class MtuTestTest extends BaseBleUnitTest
{

    @Test
    public void doNotTestMTUResultTest() throws Exception
    {

        m_config.loggingEnabled = true;
        m_config.mtuTestCallback = new MtuTestCallback()
        {
            @Override
            public Please onTestRequest(MtuTestEvent event)
            {
                return Please.doNothing();
            }

            @Override
            public void onResult(TestResult result)
            {
                release();
            }
        };

        m_mgr.setConfig(m_config);

        BleDevice device = m_mgr.newDevice(Util.randomMacAddress(), "Mtu-inator");

        device.connect(new BleTransaction.Init()
        {
            @Override
            protected void start(BleDevice device)
            {
                device.setMtu(100);
            }
        });

        startTest();
    }

    @Test
    public void mtuTestFailTest() throws Exception
    {

        final int mtuSize = 100;
        final UUID serviceUuid = Uuids.random();
        final UUID charUuid = Uuids.random();
        final byte[] data = new byte[mtuSize];
        new Random().nextBytes(data);

        final GattDatabase db = new GattDatabase()
                .addService(serviceUuid)
                .addCharacteristic(charUuid).setProperties().readWrite().setPermissions().readWrite().completeService();

        m_config.loggingEnabled = true;

        m_config.gattLayerFactory = device -> new MtuFailGatt(device, db);

        m_config.mtuTestCallback = new MtuTestCallback()
        {
            @Override
            public Please onTestRequest(MtuTestEvent event)
            {
                return Please.doWriteTest(serviceUuid, charUuid, data);
            }

            @Override
            public void onResult(TestResult result)
            {
                assertFalse(result.wasSuccess());
                assertTrue(result.result() == TestResult.Result.WRITE_TIMED_OUT);
                release();
            }
        };

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util.randomMacAddress(), "Mtu-inator_the_revenge");

        device.connect(new BleTransaction.Init()
        {
            @Override
            protected void start(BleDevice device)
            {
                device.setMtu(mtuSize);
            }
        });

        startTest();
    }

    @Test
    public void testMtuSuccessTest() throws Exception
    {

        final int mtuSize = 100;
        final UUID serviceUuid = Uuids.random();
        final UUID charUuid = Uuids.random();
        final byte[] data = new byte[mtuSize];
        new Random().nextBytes(data);

        final GattDatabase db = new GattDatabase()
                .addService(serviceUuid)
                .addCharacteristic(charUuid).setProperties().readWrite().setPermissions().readWrite().completeService();

        m_config.loggingEnabled = true;

        m_config.gattLayerFactory = device -> new UnitTestGatt(device, db);

        m_config.mtuTestCallback = new MtuTestCallback()
        {
            @Override
            public Please onTestRequest(MtuTestEvent event)
            {
                return Please.doWriteTest(serviceUuid, charUuid, data);
            }

            @Override
            public void onResult(TestResult result)
            {
                assertTrue(result.wasSuccess());
                release();
            }
        };

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util.randomMacAddress(), "Mtu-inator_the_revenge");

        device.connect(new BleTransaction.Init()
        {
            @Override
            protected void start(BleDevice device)
            {
                device.setMtu(mtuSize);
            }
        });

        startTest();

    }

    private static class MtuFailGatt extends UnitTestGatt
    {

        public MtuFailGatt(BleDevice device, GattDatabase db)
        {
            super(device, db);
        }

        @Override
        public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic)
        {
            return true;
        }
    }

}
