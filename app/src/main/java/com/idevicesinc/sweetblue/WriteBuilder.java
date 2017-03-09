package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Builder class for sending a write over BLE. Use this class to set the service and/or characteristic
 * UUIDs, and the data you'd like to write. This class provides convenience methods for sending
 * booleans, ints, shorts, longs, and Strings. Use with {@link BleDevice#write(WriteBuilder)},
 * or {@link BleDevice#write(WriteBuilder, BleDevice.ReadWriteListener)}.
 */
public final class WriteBuilder
{

    UUID serviceUUID = null;
    UUID charUUID = null;
    FutureData data = null;
    DescriptorFilter descriptorFilter = null;
    boolean bigEndian = true;


    /**
     * Basic constructor. You must at the very least call {@link #setCharacteristicUUID(UUID)}, and one of the
     * methods that add data ({@link #setBytes(byte[])}, {@link #setInt(int)}, etc..) before attempting to
     * send the write.
     */
    public WriteBuilder()
    {
        this(/*bigEndian*/true, null, null, null);
    }

    /**
     * Overload of {@link WriteBuilder#WriteBuilder(boolean, UUID, UUID, DescriptorFilter)}. If @param isBigEndian is true,
     *
     * @param isBigEndian - if <code>true</code>, then when using {@link #setInt(int)}, {@link #setShort(short)},
     *                    or {@link #setLong(long)}, SweetBlue will reverse the bytes for you.
     */
    public WriteBuilder(boolean isBigEndian)
    {
        this(isBigEndian, null, null, null);
    }

    /**
     * Overload of {@link  WriteBuilder#WriteBuilder(boolean, UUID, UUID, DescriptorFilter)}. If @param isBigEndian is true,
     *
     * @param isBigEndian - if <code>true</code>, then when using {@link #setInt(int)}, {@link #setShort(short)},
     *                    or {@link #setLong(long)}, SweetBlue will reverse the bytes for you.
     */
    public WriteBuilder(boolean isBigEndian, UUID characteristicUUID)
    {
        this(isBigEndian, null, characteristicUUID, null);
    }

    /**
     * Overload of {@link WriteBuilder#WriteBuilder(boolean, UUID, UUID, DescriptorFilter)}.
     */
    public WriteBuilder(UUID characteristicUUID)
    {
        this(/*bigendian*/true, null, characteristicUUID, null);
    }

    /**
     * Overload of {@link WriteBuilder#WriteBuilder(boolean, UUID, UUID, DescriptorFilter)}.
     */
    public WriteBuilder(UUID serviceUUID, UUID characteristicUUID)
    {
        this(/*bigendian*/true, serviceUUID, characteristicUUID, null);
    }

    /**
     * Main constructor to use. All other constructors overload this one.
     *
     * @param isBigEndian - if <code>true</code>, then when using {@link #setInt(int)}, {@link #setShort(short)},
     *                    or {@link #setLong(long)}, SweetBlue will reverse the bytes for you.
     */
    public WriteBuilder(boolean isBigEndian, UUID serviceUUID, UUID characteristicUUID, DescriptorFilter descriptorFilter)
    {
        bigEndian = isBigEndian;
        this.serviceUUID = serviceUUID;
        charUUID = characteristicUUID;
        this.descriptorFilter = descriptorFilter;
    }


    /**
     * Set the service UUID for this write. This is only needed when you have characteristics with identical uuids under different services.
     */
    public final WriteBuilder setServiceUUID(UUID uuid)
    {
        serviceUUID = uuid;
        return this;
    }

    /**
     * Set the characteristic UUID to write to.
     */
    public final WriteBuilder setCharacteristicUUID(UUID uuid)
    {
        charUUID = uuid;
        return this;
    }

    /**
     * Set the {@link DescriptorFilter} to determine which characteristic to write to, if there are multiple with the same {@link UUID} in the same
     * {@link android.bluetooth.BluetoothGattService}.
     */
    public final WriteBuilder setDescriptorFilter(DescriptorFilter filter)
    {
        descriptorFilter = filter;
        return this;
    }

    /**
     * Set the raw bytes to write.
     */
    public final WriteBuilder setBytes(byte[] data)
    {
        this.data = new PresentData(data);
        return this;
    }

    /**
     * Set the boolean to write.
     */
    public final WriteBuilder setBoolean(boolean value)
    {
        data = new PresentData(value ? new byte[]{0x1} : new byte[]{0x0});
        return this;
    }

    /**
     * Set an int to be written.
     */
    public final WriteBuilder setInt(int val)
    {
        final byte[] d = Utils_Byte.intToBytes(val);
        if (bigEndian)
        {
            Utils_Byte.reverseBytes(d);
        }
        data = new PresentData(d);
        return this;
    }

    /**
     * Set a short to be written.
     */
    public final WriteBuilder setShort(short val)
    {
        final byte[] d = Utils_Byte.shortToBytes(val);
        if (bigEndian)
        {
            Utils_Byte.reverseBytes(d);
        }
        data = new PresentData(d);
        return this;
    }

    /**
     * Set a long to be written.
     */
    public final WriteBuilder setLong(long val)
    {
        final byte[] d = Utils_Byte.longToBytes(val);
        if (bigEndian)
        {
            Utils_Byte.reverseBytes(d);
        }
        data = new PresentData(d);
        return this;
    }

    /**
     * Set a string to be written. This method also allows you to specify the string encoding. If the encoding
     * fails, then {@link String#getBytes()} is used instead, which uses "UTF-8" by default.
     */
    public final WriteBuilder setString(String value, String stringEncoding)
    {
        byte[] bytes;
        try
        {
            bytes = value.getBytes(stringEncoding);
        } catch (UnsupportedEncodingException e)
        {
            bytes = value.getBytes();
        }
        data = new PresentData(bytes);
        return this;
    }

    /**
     * Set a string to be written. This defaults to "UTF-8" encoding.
     */
    public final WriteBuilder setString(String value)
    {
        return setString(value, "UTF-8");
    }

    static final WriteBuilder fromDeprecatedWriteBuilder(BleDevice.WriteBuilder builder)
    {
        WriteBuilder wbuilder = new WriteBuilder();
        wbuilder.bigEndian = builder.bigEndian;
        wbuilder.charUUID = builder.charUUID;
        wbuilder.data = builder.data;
        wbuilder.serviceUUID = builder.serviceUUID;
        return wbuilder;
    }

}
