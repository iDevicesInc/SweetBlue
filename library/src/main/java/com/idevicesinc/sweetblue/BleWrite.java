package com.idevicesinc.sweetblue;

import android.util.Log;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Builder-type class for sending a write over BLE. Use this class to set the service and/or characteristic
 * UUIDs, and the data you'd like to write. This class provides convenience methods for sending
 * booleans, ints, shorts, longs, and Strings. Use with {@link BleDevice#write(BleWrite)},
 * or {@link BleDevice#write(BleWrite, ReadWriteListener)}. Note that all {@link BleOp} classes assume
 * the device you're talking to is {@link java.nio.ByteOrder#BIG_ENDIAN}. While Java is {@link java.nio.ByteOrder#BIG_ENDIAN} by
 * default, android actually is {@link java.nio.ByteOrder#LITTLE_ENDIAN}, so the convenience methods take this into
 * account by reversing the bytes. If you don't want this to happen, the call {@link #setIsBigEndian(boolean)} to
 * <code>false</code>.
 */
public final class BleWrite extends BleOp<BleWrite>
{

    FutureData data = null;
    ReadWriteListener.Type writeType = null;
    boolean bigEndian = true;


    public BleWrite(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleWrite(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }

    public BleWrite(boolean bigEndian)
    {
        this.bigEndian = bigEndian;
    }


    @Override
    public final boolean isValid()
    {
        return charUuid != null && data != null && data.getData() != null;
    }

    /**
     * Tells SweetBlue the endianness of the BLE device you want to perform a write to. This value only matters if you are using any of the
     * convenience methods: <br><br>
     * {@link #setBoolean(boolean)} <br>
     * {@link #setInt(int)} <br>
     * {@link #setLong(long)} <br>
     * {@link #setShort(short)} <br>
     * {@link #setString(String)} <br>
     * {@link #setString(String, String)} <br>
     */
    public final BleWrite setIsBigEndian(boolean isBigEndian)
    {
        bigEndian = isBigEndian;
        return this;
    }

    /**
     * Set the {@link com.idevicesinc.sweetblue.ReadWriteListener.Type} of the write to perform. This is here in the case that the
     * characteristic you are writing to has more than one write type associated with it eg. {@link android.bluetooth.BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE},
     * {@link android.bluetooth.BluetoothGattCharacteristic#WRITE_TYPE_SIGNED} along with standard writes.
     */
    @Advanced
    public final BleWrite setWriteType(ReadWriteListener.Type writeType)
    {
        this.writeType = writeType;
        if (writeType != ReadWriteListener.Type.WRITE && writeType != ReadWriteListener.Type.WRITE_NO_RESPONSE && writeType != ReadWriteListener.Type.WRITE_SIGNED)
        {
            Log.e(TAG, "Tried to set a write type of " + writeType.toString() + ". Only " + ReadWriteListener.Type.WRITE + ", " + ReadWriteListener.Type.WRITE_NO_RESPONSE +
            ", or " + ReadWriteListener.Type.WRITE_SIGNED + " is allowed here. " + ReadWriteListener.Type.WRITE + " will be used by default.");
        }
        return this;
    }

    /**
     * Set the raw bytes to write.
     */
    public final BleWrite setBytes(byte[] data)
    {
        this.data = new PresentData(data);
        return this;
    }

    /**
     * Set the {@link FutureData} to write.
     */
    public final BleWrite setData(FutureData data)
    {
        this.data = data;
        return this;
    }

    /**
     * Set the boolean to write.
     */
    public final BleWrite setBoolean(boolean value)
    {
        data = new PresentData(value ? new byte[]{0x1} : new byte[]{0x0});
        return this;
    }

    /**
     * Set an int to be written.
     */
    public final BleWrite setInt(int val)
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
    public final BleWrite setShort(short val)
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
    public final BleWrite setLong(long val)
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
    public final BleWrite setString(String value, String stringEncoding)
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
    public final BleWrite setString(String value)
    {
        return setString(value, "UTF-8");
    }

}
