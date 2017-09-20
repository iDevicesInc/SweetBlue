package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Random;

import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class PollingTest extends BaseBleUnitTest
{

    private GattDatabase db = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID)
            .addCharacteristic(Uuids.BATTERY_LEVEL).setValue(new byte[]{100}).setPermissions().read().setProperties().read().completeService();


    @Test(timeout = 30000)
    public void rssiPollTest() throws Exception
    {
        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Rssi Poll Tester");
        final Pointer<Integer> counter = new Pointer<>(0);
        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            device.startRssiPoll(Interval.ONE_SEC, e1 ->
            {
                assertTrue(e1.wasSuccess());
                counter.value++;
                if (counter.value >= 5)
                {
                    succeed();
                }
            });
        });
        startTest();
    }

    @Test(timeout = 30000)
    public void batteryPollTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_mgr.setConfig(m_config);

        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Battery Poll Tester");
        final Pointer<Integer> counter = new Pointer<>(0);
        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            device.startPoll(Uuids.BATTERY_LEVEL, Interval.ONE_SEC, e1 ->
            {
                assertTrue(e1.status().name(), e1.wasSuccess());
                counter.value++;
                if (counter.value >= 5)
                {
                    succeed();
                }
            });
        });
        startTest();
    }

    @Override
    public P_GattLayer getGattLayer(BleDevice device)
    {
        return new BatteryGatt(device);
    }

    private final class BatteryGatt extends UnitTestGatt
    {

        public BatteryGatt(BleDevice device)
        {
            super(device, db);
        }

        @Override
        public void sendReadResponse(BluetoothGattCharacteristic characteristic, byte[] data)
        {
            Random r = new Random();
            int level = r.nextInt(99) + 1;
            super.sendReadResponse(characteristic, new byte[]{(byte) level});
        }
    }
}
