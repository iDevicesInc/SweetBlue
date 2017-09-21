package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.impl.DefaultScanFilter;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.UUID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ScanFilterTest extends BaseBleUnitTest
{

    @Test
    public void uuidFilterTest() throws Exception
    {
        byte[] record = Utils_ScanRecord.newScanRecord("FilterTesterer", Uuids.BATTERY_SERVICE_UUID);
        BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer", record, null);

        ScanFilter filter = new DefaultScanFilter(Uuids.BATTERY_SERVICE_UUID);
        ScanFilter.ScanEvent event = newEvent(device);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        filter = new DefaultScanFilter(Uuids.GLUCOSE_SERVICE_UUID);
        event = newEvent(device);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test
    public void uuidListTest() throws Exception
    {
        byte[] record = Utils_ScanRecord.newScanRecord("FilterTesterer", Uuids.BATTERY_SERVICE_UUID);
        BleDevice device1 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer", record, null);

        byte[] record2 = Utils_ScanRecord.newScanRecord("Milano", Uuids.CURRENT_TIME_SERVICE);
        BleDevice device2 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Milano", record2, null);

        byte[] record3 = Utils_ScanRecord.newScanRecord("Wretched", Uuids.BLOOD_PRESSURE_SERVICE_UUID);
        BleDevice device3 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Wretched", record3, null);

        ArrayList<UUID> list = new ArrayList<>();
        list.add(Uuids.BATTERY_SERVICE_UUID);
        list.add(Uuids.CURRENT_TIME_SERVICE);
        ScanFilter filter = new DefaultScanFilter(list);
        ScanFilter.ScanEvent event = newEvent(device1);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device2);
        please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device3);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test
    public void nameFilterTest() throws Exception
    {
        byte[] record = Utils_ScanRecord.newScanRecord("FilterTesterer", Uuids.BATTERY_SERVICE_UUID);
        BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer", record, null);

        ScanFilter filter = new DefaultScanFilter("TERtes");
        ScanFilter.ScanEvent event = newEvent(device);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        filter = new DefaultScanFilter("testing");
        event = newEvent(device);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test
    public void nameListTest() throws Exception
    {
        BleDevice device1 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer");
        BleDevice device2 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Milano");
        BleDevice device3 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Wretched");

        ScanFilter filter = new DefaultScanFilter("TESter", "laNo");
        ScanFilter.ScanEvent event = newEvent(device1);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device2);
        please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device3);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test
    public void uuidAndNameListTest() throws Exception
    {
        byte[] record = Utils_ScanRecord.newScanRecord("FilterTesterer", Uuids.BATTERY_SERVICE_UUID);
        BleDevice device1 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer", record, null);

        byte[] record2 = Utils_ScanRecord.newScanRecord("Milano", Uuids.CURRENT_TIME_SERVICE);
        BleDevice device2 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Milano", record2, null);

        byte[] record3 = Utils_ScanRecord.newScanRecord("Wretched", Uuids.BLOOD_PRESSURE_SERVICE_UUID);
        BleDevice device3 = m_mgr.newDevice(Util_Unit.randomMacAddress(), "Wretched", record3, null);

        ArrayList<UUID> list = new ArrayList<>();
        list.add(Uuids.BATTERY_SERVICE_UUID);
        ScanFilter filter = new DefaultScanFilter(list, "ilano");
        ScanFilter.ScanEvent event = newEvent(device1);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device2);
        please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device3);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }



    private ScanFilter.ScanEvent newEvent(BleDevice device)
    {
        return ScanFilter.ScanEvent.fromScanRecord(device.getNative(), device.getName_native(), device.getName_normalized(), device.getRssi(), State.ChangeIntent.INTENTIONAL, device.getScanRecord());
    }

}
