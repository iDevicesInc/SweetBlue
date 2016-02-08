package com.idevicesinc.sweetblue.tests;

import com.idevicesinc.sweetblue.utils.Utils_Byte;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class ByteUtilTest
{

    @Test
    public void intTest() {
        int value = 424242;
        byte[] bytes = Utils_Byte.intToBytes(value);
        assertArrayEquals(new byte[] { 0x0, 0x6, 0x79, 0x32 }, bytes);
        assertEquals(value, Utils_Byte.bytesToInt(bytes));
        byte[] tmpBytes = bytes.clone();
        Utils_Byte.reverseBytes(bytes);
        assertFalse(Arrays.equals(bytes, tmpBytes));
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, tmpBytes);
    }

    @Test
    public void shortTest() {
        short value = 8724;
        byte[] bytes = Utils_Byte.shortToBytes(value);
        assertArrayEquals(new byte[] { 0x22, 0x14 }, bytes);
        assertEquals(value, Utils_Byte.bytesToShort(bytes));
        byte[] tmpBytes = bytes.clone();
        Utils_Byte.reverseBytes(bytes);
        assertFalse(Arrays.equals(bytes, tmpBytes));
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, tmpBytes);
    }

    @Test
    public void longTest() {
        long value = 8279580351934L;
        byte[] bytes = Utils_Byte.longToBytes(value);
        assertArrayEquals(new byte[] { 0x0, 0x0, 0x7, (byte) 0x87, (byte) 0xBD, 0x72, 0x1D, (byte) 0xBE }, bytes);
        assertEquals(value, Utils_Byte.bytesToLong(bytes));
        byte[] tmpBytes = bytes.clone();
        Utils_Byte.reverseBytes(bytes);
        assertFalse(Arrays.equals(bytes, tmpBytes));
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, tmpBytes);
    }

    @Test
    public void hexTest() {
        String hex = "A7B244FFBA5C";
        byte[] bytes = Utils_Byte.hexStringToBytes(hex);
        assertArrayEquals(new byte[] { (byte) 0xA7, (byte) 0xB2, 0x44, (byte) 0xFF, (byte) 0xBA, 0x5C }, bytes);
    }

}