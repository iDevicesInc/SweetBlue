package com.idevicesinc.sweetblue;

import android.util.Log;

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.util.UUID;

/**
 * Builder class for sending a write over BLE. Use this class to set the service and/or characteristic
 * UUIDs, and the data you'd like to write. This class provides convenience methods for sending
 * booleans, ints, shorts, longs, and Strings. Use with {@link BleDevice#write(WriteBuilder)},
 * or {@link BleDevice#write(WriteBuilder, BleDevice.ReadWriteListener)}.
 */
public final class WriteBuilder
{

    private final static String TAG = WriteBuilder.class.getSimpleName();

    UUID serviceUuid = null;
    UUID charUuid = null;
    UUID descriptorUuid = null;
    FutureData data = null;
    BleDevice.ReadWriteListener.Type writeType = null;
    BleDevice.ReadWriteListener readWriteListener = null;
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
     * Overload of {@link WriteBuilder#WriteBuilder(boolean, UUID, UUID, DescriptorFilter)}.
     *
     * @param isBigEndian - if <code>true</code>, then when using {@link #setInt(int)}, {@link #setShort(short)},
     *                    or {@link #setLong(long)}, SweetBlue will reverse the bytes for you.
     */
    public WriteBuilder(boolean isBigEndian)
    {
        this(isBigEndian, null, null, null);
    }

    /**
     * Overload of {@link  WriteBuilder#WriteBuilder(boolean, UUID, UUID, DescriptorFilter)}.
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
        this.serviceUuid = serviceUUID;
        charUuid = characteristicUUID;
        this.descriptorFilter = descriptorFilter;
    }


    /**
     * Set the service UUID for this write. This is only needed when you have characteristics with identical uuids under different services.
     */
    public final WriteBuilder setServiceUUID(UUID uuid)
    {
        serviceUuid = uuid;
        return this;
    }

    /**
     * Set the characteristic UUID to write to.
     */
    public final WriteBuilder setCharacteristicUUID(UUID uuid)
    {
        charUuid = uuid;
        return this;
    }

    /**
     * Set the descriptor UUID to write to (if writing to a descriptor).
     */
    public final WriteBuilder setDescriptorUUID(UUID uuid)
    {
        descriptorUuid = uuid;
        return this;
    }

    /**
     * Set the {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type} of the write to perform. This is here in the case that the
     * characteristic you are writing to has more than one write type associated with it eg. {@link android.bluetooth.BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE},
     * {@link android.bluetooth.BluetoothGattCharacteristic#WRITE_TYPE_SIGNED} along with standard writes.
     */
    @Advanced
    public final WriteBuilder setWriteType(BleDevice.ReadWriteListener.Type writeType)
    {
        this.writeType = writeType;
        if (writeType != BleDevice.ReadWriteListener.Type.WRITE && writeType != BleDevice.ReadWriteListener.Type.WRITE_NO_RESPONSE && writeType != BleDevice.ReadWriteListener.Type.WRITE_SIGNED)
        {
            Log.e(TAG, "Tried to set a write type of " + writeType.toString() + ". Only " + BleDevice.ReadWriteListener.Type.WRITE + ", " + BleDevice.ReadWriteListener.Type.WRITE_NO_RESPONSE +
            ", or " + BleDevice.ReadWriteListener.Type.WRITE_SIGNED + " is allowed here. " + BleDevice.ReadWriteListener.Type.WRITE + " will be used by default.");
        }
        return this;
    }

    /**
     * Set the {@link ReadWriteListener} for listening to the callback of the write you wish to perform.
     */
    public final WriteBuilder setReadWriteListener(final ReadWriteListener listener)
    {
        readWriteListener = new BleDevice.ReadWriteListener()
        {
            @Override
            public void onEvent(ReadWriteEvent e)
            {
                if (listener != null)
                {
                    listener.onEvent(e);
                }
            }
        };
        return this;
    }

    /**
     * Set the {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener} for listening to the callback of the write.
     *
     * @deprecated This will be removed in v3. It's safe, and ok to use this method until then.
     */
    @Deprecated
    public final WriteBuilder setReadWriteListener_dep(final BleDevice.ReadWriteListener listener)
    {
        readWriteListener = listener;
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
     * Set the {@link FutureData} to write.
     */
    public final WriteBuilder setData(FutureData data)
    {
        this.data = data;
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
        wbuilder.charUuid = builder.charUUID;
        wbuilder.data = builder.data;
        wbuilder.serviceUuid = builder.serviceUUID;
        return wbuilder;
    }

}
