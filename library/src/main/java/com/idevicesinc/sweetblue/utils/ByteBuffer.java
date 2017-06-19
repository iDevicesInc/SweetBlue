package com.idevicesinc.sweetblue.utils;

/**
 * Convenience class to build up a byte array by appending bytes, or byte arrays.
 */
public final class ByteBuffer
{

    /**
     * The default size to initialize the {@link ByteBuffer} with.
     */
    public final static byte DEFAULT_BUFFER_SIZE = 10;

    private byte[] buffer;
    private int length;
    private int requestedSize = DEFAULT_BUFFER_SIZE;


    /**
     * Initialize this buffer with the given initial size
     */
    public ByteBuffer(int initialSize)
    {
        requestedSize = initialSize;
        buffer = newArray(requestedSize);
        length = 0;
    }

    /**
     * Create a new {@link ByteBuffer} with the given byte array. Note, the byte array is cloned before inserting
     * into the buffer.
     */
    public ByteBuffer(byte[] bytes)
    {
        requestedSize = bytes.length;
        buffer = bytes.clone();
        length = bytes.length;
    }

    /**
     * Construct a new {@link ByteBuffer} with the default of {@link #DEFAULT_BUFFER_SIZE}.
     */
    public ByteBuffer()
    {
        buffer = newArray(requestedSize);
        length = 0;
    }

    /**
     * Clears the buffer of any existing data.
     */
    public final void clear()
    {
        buffer = newArray(requestedSize);
        length = 0;
    }

    /**
     * Append a byte to the buffer.
     */
    public final void append(byte b)
    {
        if (length + 1 > buffer.length)
        {
            expand((length + 1) - buffer.length);
        }
        buffer[length] = b;
        length ++;
    }

    /**
     * Append a byte array to this buffer
     */
    public final void append(byte[] bytes)
    {
        if (length + bytes.length > buffer.length)
        {
            expand((length + bytes.length) - buffer.length);
        }
        System.arraycopy(bytes, 0, buffer, length, bytes.length);
        length += bytes.length;
    }

    /**
     * Append the given byte array (of the length given, so you can pass in an array 100 bytes, but specify 50 to be added. This operation always starts at the
     * first index of the array).
     */
    public final void append(byte[] data, int length)
    {
        if (this.length + length > buffer.length)
        {
            expand((this.length + length) - buffer.length);
        }
        System.arraycopy(data, 0, buffer, this.length, length);
        this.length += length;
    }

    /**
     * Update the buffer to only contain the current data from the given start and length. If the start or length are out-of-bounds, then
     * nothing will be done to the buffer.
     */
    public final void setToSubData(int start, int length)
    {
        if (start > this.length || start < 0 || length < 0 || this.length - start < length)
        {
            return;
        }
        byte[] sub = subData(start, length);
        buffer = sub.clone();
        this.length = buffer.length;
    }

    /**
     * Returns a byte array from this buffer from the given start index, and length
     * If length is less than 1, an empty byte array will be returned. An empty array
     * will be returned also if the start index is out of range.
     *
     * If the length is larger than the available bytes, then a byte array will be returned
     * with whatever bytes are available after the start index.
     */
    public final byte[] subData(int start, int length)
    {
        if (length < 1 || start >= this.length)
        {
            return P_Const.EMPTY_BYTE_ARRAY;
        }
        length = Math.min(length, this.length - start);
        byte[] subdata = newArray(length);
        System.arraycopy(buffer, start, subdata, 0, length);
        return subdata;
    }

    /**
     * Returns the current length of the buffer
     */
    public final int length()
    {
        return length;
    }

    /**
     * Returns the bytes currently in this buffer.
     */
    public final byte[] bytes()
    {
        byte[] b;
        if (length < buffer.length)
        {
            b = newArray(length);
            System.arraycopy(buffer, 0, b, 0, length);
        }
        else
        {
            b = buffer;
        }
        return b;
    }

    /**
     * Returns the bytes that were added to this buffer.
     * NOTE: This clears out the buffer, and starts with a fresh one.
     */
    public final byte[] bytesAndClear()
    {
        byte[] b;
        if (length < buffer.length)
        {
            b = newArray(length);
            System.arraycopy(buffer, 0, b, 0, length);
        }
        else
        {
            b = buffer;
        }
        clear();
        return b;
    }


    private void expand(int amount)
    {
        byte[] newbuffer = newArray(buffer.length + amount);
        System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
        buffer = newbuffer;
    }

    private static byte[] newArray(int size)
    {
        return new byte[size];
    }
}