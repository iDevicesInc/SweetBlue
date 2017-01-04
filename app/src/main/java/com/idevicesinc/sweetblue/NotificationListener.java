package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.Arrays;
import java.util.UUID;

/**
 * Convenience interface for listening for Notifications/Indications only.
 */
public interface NotificationListener extends GenericListener_Void<NotificationListener.NotificationEvent>
{

    /**
     * A value returned to {@link NotificationListener#onEvent(Event)}
     * by way of {@link NotificationListener.NotificationEvent#status} that indicates success of the
     * operation or the reason for its failure. This enum is <i>not</i>
     * meant to match up with {@link BluetoothGatt}.GATT_* values in any way.
     *
     * @see NotificationListener.NotificationEvent#status()
     */
    enum Status implements UsesCustomNull
    {
        /**
         * As of now, not used.
         */
        NULL,

        /**
         * This is used to indicate that toggling a notification/indication was successful.
         */
        SUCCESS,

        /**
         * {@link BluetoothGatt#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)}
         * returned <code>false</code> for an unknown reason.
         */
        FAILED_TO_TOGGLE_NOTIFICATION,

        /**
         * The operation failed in a "normal" fashion, at least relative to all the other strange ways an operation can fail. This means for
         * example that {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)}
         * returned a status code that was not zero. This could mean the device went out of range, was turned off, signal was disrupted,
         * whatever. Often this means that the device is about to become {@link BleDeviceState#DISCONNECTED}. {@link BleDevice.ReadWriteListener.ReadWriteEvent#gattStatus()}
         * will most likely be non-zero, and you can check against the static fields in {@link BleStatuses} for more information.
         *
         * @see BleDevice.ReadWriteListener.ReadWriteEvent#gattStatus()
         */
        REMOTE_GATT_FAILURE;

        @Override public boolean isNull()
        {
            return this == NULL;
        }
    }

    /**
     * The type of operation for a {@link BleDevice.ReadWriteListener.ReadWriteEvent} - read, write, poll, etc.
     */
    enum Type implements UsesCustomNull
    {
        /**
         * As of now, only used for {@link BleDevice.ConnectionFailListener.ConnectionFailEvent#txnFailReason()} in some cases.
         */
        NULL,

        /**
         * Associated with {@link BleDevice#enableNotify(UUID, BleDevice.ReadWriteListener)} when we  actually get a notification.
         */
        NOTIFICATION,

        /**
         * Similar to {@link #NOTIFICATION}, kicked off from {@link BleDevice#enableNotify(UUID, BleDevice.ReadWriteListener)}, but
         * under the hood this is treated slightly differently.
         */
        INDICATION,

        /**
         * Associated with {@link BleDevice#startChangeTrackingPoll(UUID, Interval, BleDevice.ReadWriteListener)}
         * or {@link BleDevice#enableNotify(UUID, Interval, BleDevice.ReadWriteListener)} where a force-read timeout is invoked.
         */
        PSUEDO_NOTIFICATION,

        /**
         * Associated with {@link BleDevice#enableNotify(UUID, BleDevice.ReadWriteListener)} and called when enabling the notification completes by writing to the
         * Descriptor of the given {@link UUID}. {@link BleDevice.ReadWriteListener.Status#SUCCESS} doesn't <i>necessarily</i> mean that notifications will
         * definitely now work (there may be other issues in the underlying stack), but it's a reasonable guarantee.
         */
        ENABLING_NOTIFICATION,

        /**
         * Opposite of {@link #ENABLING_NOTIFICATION}.
         */
        DISABLING_NOTIFICATION;


        /**
         * Returns <code>true</code> only for {@link #NOTIFICATION} and {@link #INDICATION}, i.e. only
         * notifications whose origin is an *actual* notification (or indication) sent from the remote BLE device (as opposed to
         * a {@link #PSUEDO_NOTIFICATION}).
         */
        public boolean isNativeNotification()
        {
            return this == NOTIFICATION || this == INDICATION;
        }

        /**
         * Returns the {@link BleNodeConfig.HistoricalDataLogFilter.Source} equivalent
         * for this {@link NotificationListener.Type}, or {@link BleNodeConfig.HistoricalDataLogFilter.Source#NULL}.
         */
        public BleNodeConfig.HistoricalDataLogFilter.Source toHistoricalDataSource()
        {
            switch (this)
            {
                case NOTIFICATION:
                    return BleNodeConfig.HistoricalDataLogFilter.Source.NOTIFICATION;
                case INDICATION:
                    return BleNodeConfig.HistoricalDataLogFilter.Source.INDICATION;
                case PSUEDO_NOTIFICATION:
                    return BleNodeConfig.HistoricalDataLogFilter.Source.PSUEDO_NOTIFICATION;
            }

            return BleNodeConfig.HistoricalDataLogFilter.Source.NULL;
        }

        @Override public boolean isNull()
        {
            return this == NULL;
        }
    }

    /**
     * Provides a bunch of information about a notification.
     */
    @com.idevicesinc.sweetblue.annotations.Immutable
    class NotificationEvent extends com.idevicesinc.sweetblue.utils.Event implements com.idevicesinc.sweetblue.utils.UsesCustomNull
    {

        /**
         * Value used in place of <code>null</code>.
         */
        public static final UUID NON_APPLICABLE_UUID = Uuids.INVALID;

        /**
         * The {@link BleDevice} this {@link BleDevice.ReadWriteListener.ReadWriteEvent} is for.
         */
        public BleDevice device()
        {
            return m_device;
        }

        private final BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public String macAddress()
        {
            return m_device.getMacAddress();
        }

        /**
         * The type of operation, read, write, etc.
         */
        public Type type()
        {
            return m_type;
        }

        private final Type m_type;

        /**
         * The {@link UUID} of the service associated with this {@link BleDevice.ReadWriteListener.ReadWriteEvent}. This will always be a non-null {@link UUID}.
         */
        public UUID serviceUuid()
        {
            return m_serviceUuid;
        }

        private final UUID m_serviceUuid;

        /**
         * The {@link UUID} of the characteristic associated with this {@link BleDevice.ReadWriteListener.ReadWriteEvent}. This will always be a non-null {@link UUID}.
         */
        public UUID charUuid()
        {
            return m_charUuid;
        }

        private final UUID m_charUuid;

        /**
         * The data received from the peripheral. This will never be <code>null</code>. For error cases it will be a
         * zero-length array.
         */
        public @Nullable(Nullable.Prevalence.NEVER) byte[] data()
        {
            return m_data;
        }

        private final byte[] m_data;

        /**
         * Indicates either success or the type of failure.
         */
        public Status status()
        {
            return m_status;
        }

        private final Status m_status;

        /**
         * Time spent "over the air" - so in the native stack, processing in
         * the peripheral's embedded software, what have you. This will
         * always be slightly less than {@link #time_total()}.
         */
        public Interval time_ota()
        {
            return m_transitTime;
        }

        private final Interval m_transitTime;

        /**
         * Total time it took for the operation to complete, whether success
         * or failure. This mainly includes time spent in the internal job
         * queue plus {@link BleDevice.ReadWriteListener.ReadWriteEvent#time_ota()}. This will always be
         * longer than {@link #time_ota()}, though usually only slightly so.
         */
        public Interval time_total()
        {
            return m_totalTime;
        }

        private final Interval m_totalTime;

        /**
         * The native gatt status returned from the stack, if applicable. If the {@link #status} returned is, for example,
         * {@link BleDevice.ReadWriteListener.Status#NO_MATCHING_TARGET}, then the operation didn't even reach the point where a gatt status is
         * provided, in which case this member is set to {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE} (value of
         * {@value com.idevicesinc.sweetblue.BleStatuses#GATT_STATUS_NOT_APPLICABLE}). Otherwise it will be <code>0</code> for success or greater than
         * <code>0</code> when there's an issue. <i>Generally</i> this value will only be meaningful when {@link #status} is
         * {@link BleDevice.ReadWriteListener.Status#SUCCESS} or {@link BleDevice.ReadWriteListener.Status#REMOTE_GATT_FAILURE}. There are
         * also some cases where this will be 0 for success but {@link #status} is for example
         * {@link BleDevice.ReadWriteListener.Status#NULL_DATA} - in other words the underlying stack deemed the operation a success but SweetBlue
         * disagreed. For this reason it's recommended to treat this value as a debugging tool and use {@link #status} for actual
         * application logic if possible.
         * <br><br>
         * See {@link BluetoothGatt} for its static <code>GATT_*</code> status code members. Also see the source code of
         * {@link BleStatuses} for SweetBlue's more comprehensive internal reference list of gatt status values. This list may not be
         * totally accurate or up-to-date, nor may it match GATT_ values used by the bluetooth stack on your phone.
         */
        public int gattStatus()
        {
            return m_gattStatus;
        }

        private final int m_gattStatus;

        /**
         * This returns <code>true</code> if this event was the result of an explicit call through SweetBlue, e.g. through
         * {@link BleDevice#read(UUID)}, {@link BleDevice#write(UUID, byte[])}, etc. It will return <code>false</code> otherwise,
         * which can happen if for example you use {@link BleDevice#getNativeGatt()} to bypass SweetBlue for whatever reason.
         * Another theoretical case is if you make an explicit call through SweetBlue, then you get {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status#TIMED_OUT},
         * but then the native stack eventually *does* come back with something - this has never been observed, but it is possible.
         */
        public boolean solicited()
        {
            return m_solicited;
        }

        private final boolean m_solicited;


        NotificationEvent(BleDevice device, UUID serviceUuid, UUID charUuid, NotificationListener.Type type, byte[] data, NotificationListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
        {
            this.m_device = device;
            this.m_serviceUuid = serviceUuid != null ? serviceUuid : NON_APPLICABLE_UUID;
            this.m_charUuid = charUuid != null ? charUuid : NON_APPLICABLE_UUID;
            this.m_type = type;
            this.m_status = status;
            this.m_gattStatus = gattStatus;
            this.m_totalTime = Interval.secs(totalTime);
            this.m_transitTime = Interval.secs(transitTime);
            this.m_data = data != null ? data : BleDevice.EMPTY_BYTE_ARRAY;
            this.m_solicited = solicited;
        }


        static NotificationEvent NULL(BleDevice device)
        {
            return new NotificationEvent(device, NON_APPLICABLE_UUID, NON_APPLICABLE_UUID, NotificationListener.Type.NULL, BleDevice.EMPTY_BYTE_ARRAY, NotificationListener.Status.NULL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Interval.ZERO.secs(), Interval.ZERO.secs(), /*solicited=*/true);
        }

        /**
         * Forwards {@link BleDevice#getNativeService(UUID)}.
         */
        public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattService service()
        {
            return device().getNativeService(serviceUuid());
        }

        /**
         * Forwards {@link BleDevice#getNativeCharacteristic(UUID, UUID)}.
         */
        public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattCharacteristic characteristic()
        {
            return device().getNativeCharacteristic(serviceUuid(), charUuid());
        }

        /**
         * Convenience method for checking if {@link NotificationListener.NotificationEvent#status} equals {@link NotificationListener.Status#SUCCESS}.
         */
        public boolean wasSuccess()
        {
            return status() == NotificationListener.Status.SUCCESS;
        }

        /**
         * Returns the first byte from {@link #data()}, or 0x0 if not available.
         */
        public byte data_byte()
        {
            return data().length > 0 ? data()[0] : 0x0;
        }

        /**
         * Convenience method that attempts to parse the data as a UTF-8 string.
         */
        public @Nullable(Nullable.Prevalence.NEVER) String data_utf8()
        {
            return data_string("UTF-8");
        }

        /**
         * Best effort parsing of {@link #data()} as a {@link String}. For now simply forwards {@link #data_utf8()}.
         * In the future may try to autodetect encoding first.
         */
        public @Nullable(Nullable.Prevalence.NEVER) String data_string()
        {
            return data_utf8();
        }

        /**
         * Convenience method that attempts to parse {@link #data()} as a {@link String} with the given charset, for example <code>"UTF-8"</code>.
         */
        public @Nullable(Nullable.Prevalence.NEVER) String data_string(final String charset)
        {
            return Utils_String.getStringValue(data(), charset);
        }

        /**
         * Convenience method that attempts to parse {@link #data()} as an int.
         *
         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
         */
        public @Nullable(Nullable.Prevalence.NEVER) int data_int(boolean reverse)
        {
            if (reverse)
            {
                byte[] data = data();
                Utils_Byte.reverseBytes(data);
                return Utils_Byte.bytesToInt(data);
            }
            else
            {
                return Utils_Byte.bytesToInt(data());
            }
        }

        /**
         * Convenience method that attempts to parse {@link #data()} as a short.
         *
         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
         */
        public @Nullable(Nullable.Prevalence.NEVER) short data_short(boolean reverse)
        {
            if (reverse)
            {
                byte[] data = data();
                Utils_Byte.reverseBytes(data);
                return Utils_Byte.bytesToShort(data);
            }
            else
            {
                return Utils_Byte.bytesToShort(data());
            }
        }

        /**
         * Convenience method that attempts to parse {@link #data()} as a long.
         *
         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
         */
        public @Nullable(Nullable.Prevalence.NEVER) long data_long(boolean reverse)
        {
            if (reverse)
            {
                byte[] data = data();
                Utils_Byte.reverseBytes(data);
                return Utils_Byte.bytesToLong(data);
            }
            else
            {
                return Utils_Byte.bytesToLong(data());
            }
        }

        /**
         * Forwards {@link NotificationListener.Type#isNull()}.
         */
        @Override public boolean isNull()
        {
            return type().isNull();
        }

        @Override public String toString()
        {
            if (isNull())
            {
                return BleDevice.ReadWriteListener.Type.NULL.toString();
            }
            else
            {
                return Utils_String.toString
                        (
                                this.getClass(),
                                "status", status(),
                                "data", Arrays.toString(data()),
                                "type", type(),
                                "charUuid", device().getManager().getLogger().uuidName(charUuid()),
                                "gattStatus", device().getManager().getLogger().gattStatus(gattStatus())
                        );

            }
        }
    }

}
