package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BleScanInfo;
import com.idevicesinc.sweetblue.utils.BleUuid;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.assertTrue;


public class BleScanInfoTest extends BaseTest
{


    @Test
    public void scanRecordTestWithServiceData() throws Exception
    {
        startTest(false);
        final UUID uuid = Uuids.BATTERY_SERVICE_UUID;
        final short manId = (short) 16454;
        final byte[] manData = new byte[] { 0x5,(byte) 0xAA, 0x44, (byte) 0xB3, 0x66 };
        BleScanInfo bleRecord = new BleScanInfo()
                .setName("Johnny 5")
                .setAdvFlags((byte) 1, (byte) 0x2)
                .setTxPower((byte) 10)
                .addServiceData(uuid, new byte[] { 100 })
                .setManufacturerId(manId)
                .setManufacturerData(manData);
        byte[] record = bleRecord.buildPacket();
        BleScanInfo info = Utils_ScanRecord.parseScanRecord(record);
        assertTrue(info.getName().equals("Johnny 5"));
        assertTrue(info.getAdvFlags().value == 3);
        assertTrue(info.getManufacturerId() == manId);
        assertTrue(Arrays.equals(info.getManufacturerData(), manData));
        assertTrue(info.getTxPower().value == 10);
        Map<UUID, byte[]> services = info.getServiceData();
        assertTrue(services.size() == 1);
        assertTrue(Arrays.equals(services.get(uuid), new byte[] {100}));
        succeed();
    }

    @Test
    public void scanRecordTestWithShort() throws Exception
    {
        startTest(false);
        final UUID uuid = Uuids.BATTERY_SERVICE_UUID;
        final short manId = (short) 16454;
        final byte[] manData = new byte[] { 0x5,(byte) 0xAA, 0x44, (byte) 0xB3, 0x66 };
        BleScanInfo bleRecord = new BleScanInfo()
                .setName("Johnny 5")
                .setAdvFlags((byte) 0x3)
                .setTxPower((byte) 10)
                .addServiceUuid(uuid, BleUuid.UuidSize.SHORT)
                .setManufacturerId(manId)
                .setManufacturerData(manData);
        byte[] record = bleRecord.buildPacket();
        BleScanInfo info = Utils_ScanRecord.parseScanRecord(record);
        assertTrue(info.getName().equals("Johnny 5"));
        assertTrue(info.getAdvFlags().value == 3);
        assertTrue(info.getManufacturerId() == manId);
        assertTrue(Arrays.equals(info.getManufacturerData(), manData));
        assertTrue(info.getTxPower().value == 10);
        List<UUID> services = info.getServiceUUIDS();
        assertTrue(services.size() == 1);
        assertTrue(services.get(0).equals(uuid));
        succeed();
    }

    @Test
    public void scanRecordTestWithFull() throws Exception
    {
        startTest(false);
        final short manId = (short) 16454;
        final byte[] manData = new byte[] { 0x5,(byte) 0xAA, 0x44, (byte) 0xB3, 0x66 };
        BleScanInfo bleRecord = new BleScanInfo()
                .setName("Johnny 5")
                .setAdvFlags((byte) 1, (byte) 0x2)
                .setTxPower((byte) 10)
                .addServiceUuid(Uuids.BATTERY_SERVICE_UUID)
                .addServiceUuid(Uuids.DEVICE_INFORMATION_SERVICE_UUID)
                .setManufacturerId(manId)
                .setManufacturerData(manData);
        byte[] record = bleRecord.buildPacket();
        BleScanInfo info = Utils_ScanRecord.parseScanRecord(record);
        assertTrue(info.getName().equals("Johnny 5"));
        assertTrue(info.getAdvFlags().value == 3);
        assertTrue(info.getManufacturerId() == manId);
        assertTrue(Arrays.equals(info.getManufacturerData(), manData));
        assertTrue(info.getTxPower().value == 10);
        List<UUID> services = info.getServiceUUIDS();
        assertTrue(services.size() == 2);
        assertTrue(services.contains(Uuids.BATTERY_SERVICE_UUID));
        assertTrue(services.contains(Uuids.DEVICE_INFORMATION_SERVICE_UUID));
        succeed();
    }

    @Test
    public void scanRecordTestWithMedium() throws Exception
    {
        startTest(false);
        UUID myUuid = Uuids.fromInt("ABABCDCD");
        final short manId = (short) 16454;
        final byte[] manData = new byte[] { 0x5,(byte) 0xAA, 0x44, (byte) 0xB3, 0x66 };
        BleScanInfo bleRecord = new BleScanInfo()
                .setName("Johnny 5")
                .setAdvFlags((byte) 1, (byte) 0x2)
                .setTxPower((byte) 10)
                .addServiceUuid(Uuids.CURRENT_TIME_SERVICE, BleUuid.UuidSize.MEDIUM)
                .addServiceUuid(Uuids.CURRENT_TIME_SERVICE__CURRENT_TIME, BleUuid.UuidSize.MEDIUM)
                .addServiceUuid(myUuid, BleUuid.UuidSize.MEDIUM)
                .setManufacturerId(manId)
                .setManufacturerData(manData);
        byte[] record = bleRecord.buildPacket();
        BleScanInfo info = Utils_ScanRecord.parseScanRecord(record);
        assertTrue(info.getName().equals("Johnny 5"));
        assertTrue(info.getAdvFlags().value == 3);
        assertTrue(info.getManufacturerId() == manId);
        assertTrue(Arrays.equals(info.getManufacturerData(), manData));
        assertTrue(info.getTxPower().value == 10);
        List<UUID> services = info.getServiceUUIDS();
        assertTrue(services.size() == 3);
        assertTrue(services.contains(Uuids.CURRENT_TIME_SERVICE));
        assertTrue(services.contains(Uuids.CURRENT_TIME_SERVICE__CURRENT_TIME));
        assertTrue(services.contains(myUuid));
        succeed();
    }

}
