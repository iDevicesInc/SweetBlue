package com.idevicesinc.sweetblue.tests;


import com.idevicesinc.sweetblue.utils.*;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class UtilsTest
{


    @Test
    public void getStringValueTest() {
        String s = "This is just a test";
        byte[] sb = s.getBytes();
        assertEquals(s, Utils_String.getStringValue(sb));
        assertEquals(s, Utils_String.getStringValue(sb, "UTF-8"));
    }

    @Test
    public void normalizeNameTest() {
        String name = "This Is A Test-To make sure it works";
        assertEquals("this_is_a_test", Utils_String.normalizeDeviceName(name));
    }

    @Test
    public void matchingUUIDTest() {
        List<UUID> adIds = new ArrayList<UUID>();
        adIds.add(Uuids.BATTERY_LEVEL);
        adIds.add(Uuids.BATTERY_SERVICE_UUID);
        List<UUID> looking = new ArrayList<UUID>();
        looking.add(Uuids.BATTERY_LEVEL);
        assert Utils.haveMatchingIds(adIds, looking);
        looking.clear();
        looking.add(Uuids.DEVICE_INFORMATION_SERVICE_UUID);
        assertFalse(Utils.haveMatchingIds(adIds, looking));
    }

}
