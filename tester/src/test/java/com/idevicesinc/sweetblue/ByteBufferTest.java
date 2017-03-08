package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;


public class ByteBufferTest
{

    private final static byte[] array1 = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
    private final static byte[] array2 = new byte[] { 0x06, 0x07, 0x08, 0x09, 0x0A };


    @Test
    public void appendBytesTest() throws Exception
    {
        ByteBuffer buff = new ByteBuffer();

        buff.append(array1);
        buff.append(array2);
        buff.append((byte) 0x0B);

        assertArrayEquals(buff.bytes(), new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B });
    }

    @Test
    public void appendThenCheckLengthTest() throws Exception
    {
        ByteBuffer buff = new ByteBuffer(array1);

        buff.append(array2);

        assertArrayEquals(buff.bytes(), new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A });
        assertTrue(buff.length() == 10);
    }

    @Test
    public void bytesAndClearTest() throws Exception
    {
        ByteBuffer buff = new ByteBuffer(2);

        buff.append(array1);
        buff.append(array2);

        assertArrayEquals(buff.bytesAndClear(), new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A });
        assertTrue(buff.length() == 0);
    }

    @Test
    public void subDataTest() throws Exception
    {
        final byte[] empty = new byte[0];

        ByteBuffer buff = new ByteBuffer(2);

        buff.append(array1);
        buff.append(array2);

        byte[] sub = buff.subData(2, 5);

        assertArrayEquals(sub, new byte[] { 0x03, 0x04, 0x05, 0x06, 0x07 });

        assertArrayEquals(buff.subData(11, 20), empty);

        byte[] sub2 = buff.subData(8, 5);

        assertArrayEquals(sub2, new byte[] { 0x09, 0x0A });

        buff.setToSubData(2, 5);

        assertArrayEquals(sub, buff.bytes());

        buff.setToSubData(11, 10);

        assertArrayEquals(sub, buff.bytes());
    }

    @Test
    public void clearTest() throws Exception
    {
        ByteBuffer buff = new ByteBuffer(array1);

        buff.append(array2);

        buff.clear();

        assertTrue(buff.length() == 0);
    }

}
