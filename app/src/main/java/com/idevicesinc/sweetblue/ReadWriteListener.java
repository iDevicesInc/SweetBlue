package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.GenericListener_Void;


public interface ReadWriteListener extends GenericListener_Void<BleDevice.ReadWriteListener.ReadWriteEvent>
{


    // TODO - For version 3, we will need to uncomment this section. In fact, we should probably just copy it from BleDevice again, in the case that
    // TODO - changes have been made.

//    /**
//     * A value returned to {@link BleDevice.ReadWriteListener#onEvent(Event)}
//     * by way of {@link BleDevice.ReadWriteListener.ReadWriteEvent#status} that indicates success of the
//     * operation or the reason for its failure. This enum is <i>not</i>
//     * meant to match up with {@link BluetoothGatt}.GATT_* values in any way.
//     *
//     * @see BleDevice.ReadWriteListener.ReadWriteEvent#status()
//     */
//    enum Status implements UsesCustomNull
//    {
//        /**
//         * As of now, only used for {@link BleDevice.ConnectionFailListener.ConnectionFailEvent#txnFailReason()} in some cases.
//         */
//        NULL,
//
//        /**
//         * If {@link BleDevice.ReadWriteListener.ReadWriteEvent#type} {@link BleDevice.ReadWriteListener.Type#isRead()} then {@link BleDevice.ReadWriteListener.ReadWriteEvent#data} will contain some data returned from
//         * the device. If type is {@link BleDevice.ReadWriteListener.Type#WRITE} then {@link BleDevice.ReadWriteListener.ReadWriteEvent#data} was sent to the device.
//         */
//        SUCCESS,
//
//        /**
//         * {@link BleDevice#read(UUID, BleDevice.ReadWriteListener)}, {@link BleDevice#write(UUID, byte[])},
//         * {@link BleDevice#enableNotify(UUID, BleDevice.ReadWriteListener)}, etc. was called on {@link BleDevice#NULL}.
//         */
//        NULL_DEVICE,
//
//        /**
//         * Device is not {@link BleDeviceState#CONNECTED}.
//         */
//        NOT_CONNECTED,
//
//        /**
//         * Couldn't find a matching {@link BleDevice.ReadWriteListener.ReadWriteEvent#target} for the {@link BleDevice.ReadWriteListener.ReadWriteEvent#charUuid} (or
//         * {@link BleDevice.ReadWriteListener.ReadWriteEvent#descUuid} if {@link BleDevice.ReadWriteListener.ReadWriteEvent#target} is {@link BleDevice.ReadWriteListener.Target#DESCRIPTOR}) which was given to
//         * {@link BleDevice#read(UUID, BleDevice.ReadWriteListener)}, {@link BleDevice#write(UUID, byte[])}, etc. This most likely
//         * means that the internal call to {@link BluetoothGatt#discoverServices()} didn't find any
//         * {@link BluetoothGattService} that contained a {@link BluetoothGattCharacteristic} for {@link BleDevice.ReadWriteListener.ReadWriteEvent#charUuid()}.
//         * This can also happen if the internal call to get a BluetoothService(s) causes an exception (seen on some phones).
//         */
//        NO_MATCHING_TARGET,
//
//        /**
//         * Specific to {@link BleDevice.ReadWriteListener.Target#RELIABLE_WRITE}, this means the underlying call to {@link BluetoothGatt#beginReliableWrite()}
//         * returned <code>false</code>.
//         */
//        RELIABLE_WRITE_FAILED_TO_BEGIN,
//
//        /**
//         * Specific to {@link BleDevice.ReadWriteListener.Target#RELIABLE_WRITE}, this means {@link BleDevice#reliableWrite_begin(BleDevice.ReadWriteListener)} was
//         * called twice without an intervening call to either {@link BleDevice#reliableWrite_abort()} or {@link BleDevice#reliableWrite_execute()}.
//         */
//        RELIABLE_WRITE_ALREADY_BEGAN,
//
//        /**
//         * Specific to {@link BleDevice.ReadWriteListener.Target#RELIABLE_WRITE}, this means {@link BleDevice#reliableWrite_abort()} or {@link BleDevice#reliableWrite_execute()}
//         * was called without a previous call to {@link BleDevice#reliableWrite_begin(BleDevice.ReadWriteListener)}.
//         */
//        RELIABLE_WRITE_NEVER_BEGAN,
//
//        /**
//         * Specific to {@link BleDevice.ReadWriteListener.Target#RELIABLE_WRITE}, this means {@link BleDevice#reliableWrite_abort()} was called.
//         */
//        RELIABLE_WRITE_ABORTED,
//
//        /**
//         * You tried to do a read on a characteristic that is write-only, or
//         * vice-versa, or tried to read a notify-only characteristic, or etc., etc.
//         */
//        OPERATION_NOT_SUPPORTED,
//
//        /**
//         * The android api level doesn't support the lower level API call in the native stack. For example if you try to use
//         * {@link BleDevice#setMtu(int, BleDevice.ReadWriteListener)}, which requires API level 21, and you are at level 18.
//         */
//        ANDROID_VERSION_NOT_SUPPORTED,
//
//        /**
//         * {@link BluetoothGatt#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)}
//         * returned <code>false</code> for an unknown reason. This {@link BleDevice.ReadWriteListener.Status} is only relevant for calls to
//         * {@link BleDevice#enableNotify(UUID, BleDevice.ReadWriteListener)} and {@link BleDevice#disableNotify(UUID, BleDevice.ReadWriteListener)}
//         * (or the various overloads).
//         */
//        FAILED_TO_TOGGLE_NOTIFICATION,
//
//        /**
//         * {@link BluetoothGattCharacteristic#setValue(byte[])} (or one of its overloads) or
//         * {@link BluetoothGattDescriptor#setValue(byte[])} (or one of its overloads) returned <code>false</code>.
//         */
//        FAILED_TO_SET_VALUE_ON_TARGET,
//
//        /**
//         * The call to {@link BluetoothGatt#readCharacteristic(BluetoothGattCharacteristic)}
//         * or {@link BluetoothGatt#writeCharacteristic(BluetoothGattCharacteristic)}
//         * or etc. returned <code>false</code> and thus failed immediately
//         * for unknown reasons. No good remedy for this...perhaps try {@link BleManager#reset()}.
//         */
//        FAILED_TO_SEND_OUT,
//
//        /**
//         * The operation was cancelled by the device becoming {@link BleDeviceState#DISCONNECTED}.
//         */
//        CANCELLED_FROM_DISCONNECT,
//
//        /**
//         * The operation was cancelled because {@link BleManager} went {@link BleManagerState#TURNING_OFF} and/or
//         * {@link BleManagerState#OFF}. Note that if the user turns off BLE from their OS settings (airplane mode, etc.) then
//         * {@link BleDevice.ReadWriteListener.ReadWriteEvent#status} could potentially be {@link #CANCELLED_FROM_DISCONNECT} because SweetBlue might get
//         * the disconnect callback before the turning off callback. Basic testing has revealed that this is *not* the case, but you never know.
//         * <br><br>
//         * Either way, the device was or will be disconnected.
//         */
//        CANCELLED_FROM_BLE_TURNING_OFF,
//
//        /**
//         * Used either when {@link BleDevice.ReadWriteListener.ReadWriteEvent#type()} {@link BleDevice.ReadWriteListener.Type#isRead()} and the stack returned a <code>null</code>
//         * value for {@link BluetoothGattCharacteristic#getValue()} despite the operation being otherwise "successful", <i>or</i>
//         * {@link BleDevice#write(UUID, byte[])} (or overload(s) ) were called with a null data parameter. For the read case, the library
//         * will throw an {@link BleManager.UhOhListener.UhOh#READ_RETURNED_NULL}, but hopefully it was just a temporary glitch. If the problem persists try {@link BleManager#reset()}.
//         */
//        NULL_DATA,
//
//        /**
//         * Used either when {@link BleDevice.ReadWriteListener.ReadWriteEvent#type} {@link BleDevice.ReadWriteListener.Type#isRead()} and the operation was "successful" but
//         * returned a zero-length array for {@link BleDevice.ReadWriteListener.ReadWriteEvent#data}, <i>or</i> {@link BleDevice#write(UUID, byte[])} (or overload(s) )
//         * was called with a non-null but zero-length data parameter. Note that {@link BleDevice.ReadWriteListener.ReadWriteEvent#data} will be a zero-length array for
//         * all other error statuses as well, for example {@link #NO_MATCHING_TARGET}, {@link #NOT_CONNECTED}, etc. In other words it's never null.
//         */
//        EMPTY_DATA,
//
//        /**
//         * For now only used when giving a negative or zero value to {@link BleDevice#setMtu(int, BleDevice.ReadWriteListener)}.
//         */
//        INVALID_DATA,
//
//        /**
//         * The operation failed in a "normal" fashion, at least relative to all the other strange ways an operation can fail. This means for
//         * example that {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)}
//         * returned a status code that was not zero. This could mean the device went out of range, was turned off, signal was disrupted,
//         * whatever. Often this means that the device is about to become {@link BleDeviceState#DISCONNECTED}. {@link BleDevice.ReadWriteListener.ReadWriteEvent#gattStatus()}
//         * will most likely be non-zero, and you can check against the static fields in {@link BleStatuses} for more information.
//         *
//         * @see BleDevice.ReadWriteListener.ReadWriteEvent#gattStatus()
//         */
//        REMOTE_GATT_FAILURE,
//
//        /**
//         * Operation took longer than time specified in {@link BleNodeConfig#taskTimeoutRequestFilter} so we cut it loose.
//         */
//        TIMED_OUT;
//
//        /**
//         * Returns <code>true</code> for {@link #CANCELLED_FROM_DISCONNECT} or {@link #CANCELLED_FROM_BLE_TURNING_OFF}.
//         */
//        public boolean wasCancelled()
//        {
//            return this == CANCELLED_FROM_DISCONNECT || this == ReadWriteListener.Status.CANCELLED_FROM_BLE_TURNING_OFF;
//        }
//
//        @Override public boolean isNull()
//        {
//            return this == NULL;
//        }
//    }
//
//    /**
//     * The type of operation for a {@link BleDevice.ReadWriteListener.ReadWriteEvent} - read, write, poll, etc.
//     */
//    enum Type implements UsesCustomNull
//    {
//        /**
//         * As of now, only used for {@link BleDevice.ConnectionFailListener.ConnectionFailEvent#txnFailReason()} in some cases.
//         */
//        NULL,
//
//        /**
//         * Associated with {@link BleDevice#read(UUID, BleDevice.ReadWriteListener)} or {@link BleDevice#readRssi(BleDevice.ReadWriteListener)}.
//         */
//        READ,
//
//        /**
//         * Associated with {@link BleDevice#write(UUID, byte[])} or {@link BleDevice#write(UUID, byte[], BleDevice.ReadWriteListener)}
//         * or {@link BleDevice#setMtu(int)} or {@link BleDevice#setName(String, UUID, BleDevice.ReadWriteListener)}.
//         *
//         * @see #isWrite()
//         */
//        WRITE,
//
//        /**
//         * Similar to {@link #WRITE} but under the hood {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE} is used.
//         * See also {@link BluetoothGattCharacteristic#PROPERTY_WRITE_NO_RESPONSE}.
//         *
//         * @see #isWrite()
//         */
//        WRITE_NO_RESPONSE,
//
//        /**
//         * Similar to {@link #WRITE} but under the hood {@link BluetoothGattCharacteristic#WRITE_TYPE_SIGNED} is used.
//         * See also {@link BluetoothGattCharacteristic#PROPERTY_SIGNED_WRITE}.
//         *
//         * @see #isWrite()
//         */
//        WRITE_SIGNED,
//
//        /**
//         * Associated with {@link BleDevice#startPoll(UUID, Interval, BleDevice.ReadWriteListener)} or {@link BleDevice#startRssiPoll(Interval, BleDevice.ReadWriteListener)}.
//         */
//        POLL,
//
//        /**
//         * Associated with {@link BleDevice#enableNotify(UUID, BleDevice.ReadWriteListener)} when we  actually get a notification.
//         */
//        NOTIFICATION,
//
//        /**
//         * Similar to {@link #NOTIFICATION}, kicked off from {@link BleDevice#enableNotify(UUID, BleDevice.ReadWriteListener)}, but
//         * under the hood this is treated slightly differently.
//         */
//        INDICATION,
//
//        /**
//         * Associated with {@link BleDevice#startChangeTrackingPoll(UUID, Interval, BleDevice.ReadWriteListener)}
//         * or {@link BleDevice#enableNotify(UUID, Interval, BleDevice.ReadWriteListener)} where a force-read timeout is invoked.
//         */
//        PSUEDO_NOTIFICATION,
//
//        /**
//         * Associated with {@link BleDevice#enableNotify(UUID, BleDevice.ReadWriteListener)} and called when enabling the notification completes by writing to the
//         * Descriptor of the given {@link UUID}. {@link BleDevice.ReadWriteListener.Status#SUCCESS} doesn't <i>necessarily</i> mean that notifications will
//         * definitely now work (there may be other issues in the underlying stack), but it's a reasonable guarantee.
//         */
//        ENABLING_NOTIFICATION,
//
//        /**
//         * Opposite of {@link #ENABLING_NOTIFICATION}.
//         */
//        DISABLING_NOTIFICATION;
//
//        /**
//         * Returns <code>true</code> for every {@link BleDevice.ReadWriteListener.Type} except {@link #isWrite()}, {@link #ENABLING_NOTIFICATION}, and
//         * {@link #DISABLING_NOTIFICATION}. Overall this convenience method is meant to tell you when we've <i>received</i> something from
//         * the device as opposed to writing something to it.
//         */
//        public boolean isRead()
//        {
//            return !isWrite() && this != ENABLING_NOTIFICATION && this != DISABLING_NOTIFICATION;
//        }
//
//        /**
//         * Returns <code>true</code> if <code>this</code> is {@link #WRITE} or {@link #WRITE_NO_RESPONSE} or {@link #WRITE_SIGNED}.
//         */
//        public boolean isWrite()
//        {
//            return this == WRITE || this == WRITE_NO_RESPONSE || this == WRITE_SIGNED;
//        }
//
//        /**
//         * Returns true if <code>this</code> is {@link #NOTIFICATION}, {@link #PSUEDO_NOTIFICATION}, or {@link #INDICATION}.
//         */
//        public boolean isNotification()
//        {
//            return this.isNativeNotification() || this == PSUEDO_NOTIFICATION;
//        }
//
//        /**
//         * Subset of {@link #isNotification()}, returns <code>true</code> only for {@link #NOTIFICATION} and {@link #INDICATION}, i.e. only
//         * notifications who origin is an *actual* notification (or indication) sent from the remote BLE device.
//         */
//        public boolean isNativeNotification()
//        {
//            return this == NOTIFICATION || this == INDICATION;
//        }
//
//        /**
//         * Returns the {@link BleNodeConfig.HistoricalDataLogFilter.Source} equivalent
//         * for this {@link BleDevice.ReadWriteListener.Type}, or {@link BleNodeConfig.HistoricalDataLogFilter.Source#NULL}.
//         */
//        public BleNodeConfig.HistoricalDataLogFilter.Source toHistoricalDataSource()
//        {
//            switch (this)
//            {
//                case READ:
//                    return BleNodeConfig.HistoricalDataLogFilter.Source.READ;
//                case POLL:
//                    return BleNodeConfig.HistoricalDataLogFilter.Source.POLL;
//                case NOTIFICATION:
//                    return BleNodeConfig.HistoricalDataLogFilter.Source.NOTIFICATION;
//                case INDICATION:
//                    return BleNodeConfig.HistoricalDataLogFilter.Source.INDICATION;
//                case PSUEDO_NOTIFICATION:
//                    return BleNodeConfig.HistoricalDataLogFilter.Source.PSUEDO_NOTIFICATION;
//            }
//
//            return BleNodeConfig.HistoricalDataLogFilter.Source.NULL;
//        }
//
//        @Override public boolean isNull()
//        {
//            return this == NULL;
//        }
//    }
//
//    /**
//     * The type of GATT "object", provided by {@link BleDevice.ReadWriteListener.ReadWriteEvent#target()}.
//     */
//    enum Target implements UsesCustomNull
//    {
//        /**
//         * As of now, only used for {@link BleDevice.ConnectionFailListener.ConnectionFailEvent#txnFailReason()} in some cases.
//         */
//        NULL,
//
//        /**
//         * The {@link BleDevice.ReadWriteListener.ReadWriteEvent} returned has to do with a {@link BluetoothGattCharacteristic} under the hood.
//         */
//        CHARACTERISTIC,
//
//        /**
//         * The {@link BleDevice.ReadWriteListener.ReadWriteEvent} returned has to do with a {@link BluetoothGattDescriptor} under the hood.
//         */
//        DESCRIPTOR,
//
//        /**
//         * The {@link BleDevice.ReadWriteListener.ReadWriteEvent} is coming in from using {@link BleDevice#readRssi(BleDevice.ReadWriteListener)} or
//         * {@link BleDevice#startRssiPoll(Interval, BleDevice.ReadWriteListener)}.
//         */
//        RSSI,
//
//        /**
//         * The {@link BleDevice.ReadWriteListener.ReadWriteEvent} is coming in from using {@link BleDevice#setMtu(int, BleDevice.ReadWriteListener)} or overloads.
//         */
//        MTU,
//
//        /**
//         * The {@link BleDevice.ReadWriteListener.ReadWriteEvent} is coming in from using <code>reliableWrite_*()</code> overloads such as {@link BleDevice#reliableWrite_begin(BleDevice.ReadWriteListener)},
//         * {@link BleDevice#reliableWrite_execute()}, etc.
//         */
//        RELIABLE_WRITE,
//
//        /**
//         * The {@link BleDevice.ReadWriteListener.ReadWriteEvent} is coming in from using {@link BleDevice#setConnectionPriority(BleConnectionPriority, BleDevice.ReadWriteListener)} or overloads.
//         */
//        CONNECTION_PRIORITY;
//
//        @Override public boolean isNull()
//        {
//            return this == NULL;
//        }
//    }
//
//    /**
//     * Provides a bunch of information about a completed read, write, or notification.
//     */
//    @com.idevicesinc.sweetblue.annotations.Immutable
//    class ReadWriteEvent extends com.idevicesinc.sweetblue.utils.Event implements com.idevicesinc.sweetblue.utils.UsesCustomNull
//    {
//        /**
//         * Value used in place of <code>null</code>, either indicating that {@link #descUuid} isn't used for the {@link BleDevice.ReadWriteListener.ReadWriteEvent}
//         * because {@link #target} is {@link BleDevice.ReadWriteListener.Target#CHARACTERISTIC}, or that both {@link #descUuid} and {@link #charUuid} aren't applicable
//         * because {@link #target} is {@link BleDevice.ReadWriteListener.Target#RSSI}.
//         */
//        public static final UUID NON_APPLICABLE_UUID = Uuids.INVALID;
//
//        /**
//         * The {@link BleDevice} this {@link BleDevice.ReadWriteListener.ReadWriteEvent} is for.
//         */
//        public BleDevice device()
//        {
//            return m_device;
//        }
//
//        private final BleDevice m_device;
//
//        /**
//         * Convience to return the mac address of {@link #device()}.
//         */
//        public String macAddress()
//        {
//            return m_device.getMacAddress();
//        }
//
//        /**
//         * The type of operation, read, write, etc.
//         */
//        public BleDevice.ReadWriteListener.Type type()
//        {
//            return m_type;
//        }
//
//        private final BleDevice.ReadWriteListener.Type m_type;
//
//        /**
//         * The type of GATT object this {@link BleDevice.ReadWriteListener.ReadWriteEvent} is for, currently characteristic, descriptor, or rssi.
//         */
//        public BleDevice.ReadWriteListener.Target target()
//        {
//            return m_target;
//        }
//
//        private final BleDevice.ReadWriteListener.Target m_target;
//
//        /**
//         * The {@link UUID} of the service associated with this {@link BleDevice.ReadWriteListener.ReadWriteEvent}. This will always be a non-null {@link UUID},
//         * even if {@link #target} is {@link BleDevice.ReadWriteListener.Target#DESCRIPTOR}. If {@link #target} is {@link BleDevice.ReadWriteListener.Target#RSSI} then this will be referentially equal
//         * (i.e. you can use == to compare) to {@link #NON_APPLICABLE_UUID}.
//         */
//        public UUID serviceUuid()
//        {
//            return m_serviceUuid;
//        }
//
//        private final UUID m_serviceUuid;
//
//        /**
//         * The {@link UUID} of the characteristic associated with this {@link BleDevice.ReadWriteListener.ReadWriteEvent}. This will always be a non-null {@link UUID},
//         * even if {@link #target} is {@link BleDevice.ReadWriteListener.Target#DESCRIPTOR}. If {@link #target} is {@link BleDevice.ReadWriteListener.Target#RSSI} then this will be referentially equal
//         * (i.e. you can use == to compare) to {@link #NON_APPLICABLE_UUID}.
//         */
//        public UUID charUuid()
//        {
//            return m_charUuid;
//        }
//
//        private final UUID m_charUuid;
//
//        /**
//         * The {@link UUID} of the descriptor associated with this {@link BleDevice.ReadWriteListener.ReadWriteEvent}. If {@link #target} is
//         * {@link BleDevice.ReadWriteListener.Target#CHARACTERISTIC} or {@link BleDevice.ReadWriteListener.Target#RSSI} then this will be referentially equal
//         * (i.e. you can use == to compare) to {@link #NON_APPLICABLE_UUID}.
//         */
//        public UUID descUuid()
//        {
//            return m_descUuid;
//        }
//
//        private final UUID m_descUuid;
//
//        /**
//         * The data sent to the peripheral if {@link BleDevice.ReadWriteListener.ReadWriteEvent#type} {@link BleDevice.ReadWriteListener.Type#isWrite()}, otherwise the data received from the
//         * peripheral if {@link BleDevice.ReadWriteListener.ReadWriteEvent#type} {@link BleDevice.ReadWriteListener.Type#isRead()}. This will never be <code>null</code>. For error cases it will be a
//         * zero-length array.
//         */
//        public @Nullable(Nullable.Prevalence.NEVER) byte[] data()
//        {
//            return m_data;
//        }
//
//        private final byte[] m_data;
//
//        /**
//         * This value gets updated as a result of a {@link BleDevice#readRssi(BleDevice.ReadWriteListener)} call. It will
//         * always be equivalent to {@link BleDevice#getRssi()} but is included here for convenience.
//         *
//         * @see BleDevice#getRssi()
//         * @see BleDevice#getRssiPercent()
//         * @see BleDevice#getDistance()
//         */
//        public int rssi()
//        {
//            return m_rssi;
//        }
//
//        private final int m_rssi;
//
//        /**
//         * This value gets set as a result of a {@link BleDevice#setMtu(int, BleDevice.ReadWriteListener)} call. The value returned
//         * will be the same as that given to {@link BleDevice#setMtu(int, BleDevice.ReadWriteListener)}, which means it will be the
//         * same as {@link BleDevice#getMtu()} if {@link #status()} equals {@link BleDevice.ReadWriteListener.Status#SUCCESS}.
//         *
//         * @see BleDevice#getMtu()
//         */
//        public int mtu()
//        {
//            return m_mtu;
//        }
//
//        private final int m_mtu;
//
//        /**
//         * Indicates either success or the type of failure. Some values of {@link BleDevice.ReadWriteListener.Status} are not used for certain values of {@link BleDevice.ReadWriteListener.Type}.
//         * For example a {@link BleDevice.ReadWriteListener.Type#NOTIFICATION} cannot fail with {@link BleDevice.ReadWriteListener.Status#TIMED_OUT}.
//         */
//        public BleDevice.ReadWriteListener.Status status()
//        {
//            return m_status;
//        }
//
//        private final BleDevice.ReadWriteListener.Status m_status;
//
//        /**
//         * Time spent "over the air" - so in the native stack, processing in
//         * the peripheral's embedded software, what have you. This will
//         * always be slightly less than {@link #time_total()}.
//         */
//        public Interval time_ota()
//        {
//            return m_transitTime;
//        }
//
//        private final Interval m_transitTime;
//
//        /**
//         * Total time it took for the operation to complete, whether success
//         * or failure. This mainly includes time spent in the internal job
//         * queue plus {@link BleDevice.ReadWriteListener.ReadWriteEvent#time_ota()}. This will always be
//         * longer than {@link #time_ota()}, though usually only slightly so.
//         */
//        public Interval time_total()
//        {
//            return m_totalTime;
//        }
//
//        private final Interval m_totalTime;
//
//        /**
//         * The native gatt status returned from the stack, if applicable. If the {@link #status} returned is, for example,
//         * {@link BleDevice.ReadWriteListener.Status#NO_MATCHING_TARGET}, then the operation didn't even reach the point where a gatt status is
//         * provided, in which case this member is set to {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE} (value of
//         * {@value com.idevicesinc.sweetblue.BleStatuses#GATT_STATUS_NOT_APPLICABLE}). Otherwise it will be <code>0</code> for success or greater than
//         * <code>0</code> when there's an issue. <i>Generally</i> this value will only be meaningful when {@link #status} is
//         * {@link BleDevice.ReadWriteListener.Status#SUCCESS} or {@link BleDevice.ReadWriteListener.Status#REMOTE_GATT_FAILURE}. There are
//         * also some cases where this will be 0 for success but {@link #status} is for example
//         * {@link BleDevice.ReadWriteListener.Status#NULL_DATA} - in other words the underlying stack deemed the operation a success but SweetBlue
//         * disagreed. For this reason it's recommended to treat this value as a debugging tool and use {@link #status} for actual
//         * application logic if possible.
//         * <br><br>
//         * See {@link BluetoothGatt} for its static <code>GATT_*</code> status code members. Also see the source code of
//         * {@link BleStatuses} for SweetBlue's more comprehensive internal reference list of gatt status values. This list may not be
//         * totally accurate or up-to-date, nor may it match GATT_ values used by the bluetooth stack on your phone.
//         */
//        public int gattStatus()
//        {
//            return m_gattStatus;
//        }
//
//        private final int m_gattStatus;
//
//        /**
//         * This returns <code>true</code> if this event was the result of an explicit call through SweetBlue, e.g. through
//         * {@link BleDevice#read(UUID)}, {@link BleDevice#write(UUID, byte[])}, etc. It will return <code>false</code> otherwise,
//         * which can happen if for example you use {@link BleDevice#getNativeGatt()} to bypass SweetBlue for whatever reason.
//         * Another theoretical case is if you make an explicit call through SweetBlue, then you get {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status#TIMED_OUT},
//         * but then the native stack eventually *does* come back with something - this has never been observed, but it is possible.
//         */
//        public boolean solicited()
//        {
//            return m_solicited;
//        }
//
//        private final boolean m_solicited;
//
//        /**
//         * This value gets set as a result of a {@link BleDevice#setConnectionPriority(BleConnectionPriority, BleDevice.ReadWriteListener)} call. The value returned
//         * will be the same as that given to {@link BleDevice#setConnectionPriority(BleConnectionPriority, BleDevice.ReadWriteListener)}, which means it will be the
//         * same as {@link BleDevice#getConnectionPriority()} if {@link #status()} equals {@link BleDevice.ReadWriteListener.Status#SUCCESS}.
//         *
//         * @see BleDevice#getConnectionPriority()
//         */
//        public BleConnectionPriority connectionPriority()
//        {
//            return m_connectionPriority;
//        }
//
//        private final BleConnectionPriority m_connectionPriority;
//
//        ReadWriteEvent(BleDevice device, UUID serviceUuid, UUID charUuid, UUID descUuid, BleDevice.ReadWriteListener.Type type, BleDevice.ReadWriteListener.Target target, byte[] data, BleDevice.ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
//        {
//            this.m_device = device;
//            this.m_serviceUuid = serviceUuid != null ? serviceUuid : NON_APPLICABLE_UUID;
//            this.m_charUuid = charUuid != null ? charUuid : NON_APPLICABLE_UUID;
//            this.m_descUuid = descUuid != null ? descUuid : NON_APPLICABLE_UUID;
//            this.m_type = type;
//            this.m_target = target;
//            this.m_status = status;
//            this.m_gattStatus = gattStatus;
//            this.m_totalTime = Interval.secs(totalTime);
//            this.m_transitTime = Interval.secs(transitTime);
//            this.m_data = data != null ? data : BleDevice.EMPTY_BYTE_ARRAY;
//            this.m_rssi = device.getRssi();
//            this.m_mtu = device.getMtu();
//            this.m_solicited = solicited;
//            this.m_connectionPriority = device.getConnectionPriority();
//        }
//
//        ReadWriteEvent(BleDevice device, BleDevice.ReadWriteListener.Type type, int rssi, BleDevice.ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
//        {
//            this.m_device = device;
//            this.m_charUuid = NON_APPLICABLE_UUID;
//            this.m_descUuid = NON_APPLICABLE_UUID;
//            this.m_serviceUuid = NON_APPLICABLE_UUID;
//            this.m_type = type;
//            this.m_target = BleDevice.ReadWriteListener.Target.RSSI;
//            this.m_status = status;
//            this.m_gattStatus = gattStatus;
//            this.m_totalTime = Interval.secs(totalTime);
//            this.m_transitTime = Interval.secs(transitTime);
//            this.m_data = BleDevice.EMPTY_BYTE_ARRAY;
//            this.m_rssi = status == BleDevice.ReadWriteListener.Status.SUCCESS ? rssi : device.getRssi();
//            this.m_mtu = device.getMtu();
//            this.m_solicited = solicited;
//            this.m_connectionPriority = device.getConnectionPriority();
//        }
//
//        ReadWriteEvent(BleDevice device, int mtu, BleDevice.ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
//        {
//            this.m_device = device;
//            this.m_charUuid = NON_APPLICABLE_UUID;
//            this.m_descUuid = NON_APPLICABLE_UUID;
//            this.m_serviceUuid = NON_APPLICABLE_UUID;
//            this.m_type = BleDevice.ReadWriteListener.Type.WRITE;
//            this.m_target = BleDevice.ReadWriteListener.Target.MTU;
//            this.m_status = status;
//            this.m_gattStatus = gattStatus;
//            this.m_totalTime = Interval.secs(totalTime);
//            this.m_transitTime = Interval.secs(transitTime);
//            this.m_data = BleDevice.EMPTY_BYTE_ARRAY;
//            this.m_rssi = device.getRssi();
//            this.m_mtu = status == BleDevice.ReadWriteListener.Status.SUCCESS ? mtu : device.getMtu();
//            this.m_solicited = solicited;
//            this.m_connectionPriority = device.getConnectionPriority();
//        }
//
//        ReadWriteEvent(BleDevice device, BleConnectionPriority connectionPriority, BleDevice.ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
//        {
//            this.m_device = device;
//            this.m_charUuid = NON_APPLICABLE_UUID;
//            this.m_descUuid = NON_APPLICABLE_UUID;
//            this.m_serviceUuid = NON_APPLICABLE_UUID;
//            this.m_type = BleDevice.ReadWriteListener.Type.WRITE;
//            this.m_target = BleDevice.ReadWriteListener.Target.CONNECTION_PRIORITY;
//            this.m_status = status;
//            this.m_gattStatus = gattStatus;
//            this.m_totalTime = Interval.secs(totalTime);
//            this.m_transitTime = Interval.secs(transitTime);
//            this.m_data = BleDevice.EMPTY_BYTE_ARRAY;
//            this.m_rssi = device.getRssi();
//            this.m_mtu = device.getMtu();
//            this.m_solicited = solicited;
//            this.m_connectionPriority = connectionPriority;
//        }
//
//        static BleDevice.ReadWriteListener.ReadWriteEvent NULL(BleDevice device)
//        {
//            return new BleDevice.ReadWriteListener.ReadWriteEvent(device, NON_APPLICABLE_UUID, NON_APPLICABLE_UUID, NON_APPLICABLE_UUID, BleDevice.ReadWriteListener.Type.NULL, BleDevice.ReadWriteListener.Target.NULL, BleDevice.EMPTY_BYTE_ARRAY, BleDevice.ReadWriteListener.Status.NULL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Interval.ZERO.secs(), Interval.ZERO.secs(), /*solicited=*/true);
//        }
//
//        /**
//         * Forwards {@link BleDevice#getNativeService(UUID)}, which will be nonnull
//         * if {@link #target()} is {@link BleDevice.ReadWriteListener.Target#CHARACTERISTIC} or {@link BleDevice.ReadWriteListener.Target#DESCRIPTOR}.
//         */
//        public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattService service()
//        {
//            return device().getNativeService(serviceUuid());
//        }
//
//        /**
//         * Forwards {@link BleDevice#getNativeCharacteristic(UUID, UUID)}, which will be nonnull
//         * if {@link #target()} is {@link BleDevice.ReadWriteListener.Target#CHARACTERISTIC} or {@link BleDevice.ReadWriteListener.Target#DESCRIPTOR}.
//         */
//        public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattCharacteristic characteristic()
//        {
//            return device().getNativeCharacteristic(serviceUuid(), charUuid());
//        }
//
//        /**
//         * Forwards {@link BleDevice#getNativeDescriptor_inChar(UUID, UUID)}, which will be nonnull
//         * if {@link #target()} is {@link BleDevice.ReadWriteListener.Target#DESCRIPTOR}.
//         */
//        public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor descriptor()
//        {
//            return device().getNativeDescriptor_inChar(charUuid(), descUuid());
//        }
//
//        /**
//         * Convenience method for checking if {@link BleDevice.ReadWriteListener.ReadWriteEvent#status} equals {@link BleDevice.ReadWriteListener.Status#SUCCESS}.
//         */
//        public boolean wasSuccess()
//        {
//            return status() == BleDevice.ReadWriteListener.Status.SUCCESS;
//        }
//
//        /**
//         * Forwards {@link BleDevice.ReadWriteListener.Status#wasCancelled()}.
//         */
//        public boolean wasCancelled()
//        {
//            return status().wasCancelled();
//        }
//
//        /**
//         * Forwards {@link BleDevice.ReadWriteListener.Type#isNotification()}.
//         */
//        public boolean isNotification()
//        {
//            return type().isNotification();
//        }
//
//        /**
//         * Forwards {@link BleDevice.ReadWriteListener.Type#isRead()}.
//         */
//        public boolean isRead()
//        {
//            return type().isRead();
//        }
//
//        /**
//         * Forwards {@link BleDevice.ReadWriteListener.Type#isWrite()}.
//         */
//        public boolean isWrite()
//        {
//            return type().isWrite();
//        }
//
//        /**
//         * Returns the first byte from {@link #data()}, or 0x0 if not available.
//         */
//        public byte data_byte()
//        {
//            return data().length > 0 ? data()[0] : 0x0;
//        }
//
//        /**
//         * Convenience method that attempts to parse the data as a UTF-8 string.
//         */
//        public @Nullable(Nullable.Prevalence.NEVER) String data_utf8()
//        {
//            return data_string("UTF-8");
//        }
//
//        /**
//         * Best effort parsing of {@link #data()} as a {@link String}. For now simply forwards {@link #data_utf8()}.
//         * In the future may try to autodetect encoding first.
//         */
//        public @Nullable(Nullable.Prevalence.NEVER) String data_string()
//        {
//            return data_utf8();
//        }
//
//        /**
//         * Convenience method that attempts to parse {@link #data()} as a {@link String} with the given charset, for example <code>"UTF-8"</code>.
//         */
//        public @Nullable(Nullable.Prevalence.NEVER) String data_string(final String charset)
//        {
//            return Utils_String.getStringValue(data(), charset);
//        }
//
//        /**
//         * Convenience method that attempts to parse {@link #data()} as an int.
//         *
//         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
//         */
//        public @Nullable(Nullable.Prevalence.NEVER) int data_int(boolean reverse)
//        {
//            if (reverse)
//            {
//                byte[] data = data();
//                Utils_Byte.reverseBytes(data);
//                return Utils_Byte.bytesToInt(data);
//            }
//            else
//            {
//                return Utils_Byte.bytesToInt(data());
//            }
//        }
//
//        /**
//         * Convenience method that attempts to parse {@link #data()} as a short.
//         *
//         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
//         */
//        public @Nullable(Nullable.Prevalence.NEVER) short data_short(boolean reverse)
//        {
//            if (reverse)
//            {
//                byte[] data = data();
//                Utils_Byte.reverseBytes(data);
//                return Utils_Byte.bytesToShort(data);
//            }
//            else
//            {
//                return Utils_Byte.bytesToShort(data());
//            }
//        }
//
//        /**
//         * Convenience method that attempts to parse {@link #data()} as a long.
//         *
//         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
//         */
//        public @Nullable(Nullable.Prevalence.NEVER) long data_long(boolean reverse)
//        {
//            if (reverse)
//            {
//                byte[] data = data();
//                Utils_Byte.reverseBytes(data);
//                return Utils_Byte.bytesToLong(data);
//            }
//            else
//            {
//                return Utils_Byte.bytesToLong(data());
//            }
//        }
//
//        /**
//         * Forwards {@link BleDevice.ReadWriteListener.Type#isNull()}.
//         */
//        @Override public boolean isNull()
//        {
//            return type().isNull();
//        }
//
//        @Override public String toString()
//        {
//            if (isNull())
//            {
//                return BleDevice.ReadWriteListener.Type.NULL.toString();
//            }
//            else
//            {
//                if (target() == BleDevice.ReadWriteListener.Target.RSSI)
//                {
//                    return Utils_String.toString
//                            (
//                                    this.getClass(),
//                                    "status", status(),
//                                    "type", type(),
//                                    "target", target(),
//                                    "rssi", rssi(),
//                                    "gattStatus", device().getManager().getLogger().gattStatus(gattStatus())
//                            );
//                }
//                else if (target() == BleDevice.ReadWriteListener.Target.MTU)
//                {
//                    return Utils_String.toString
//                            (
//                                    this.getClass(),
//                                    "status", status(),
//                                    "type", type(),
//                                    "target", target(),
//                                    "mtu", mtu(),
//                                    "gattStatus", device().getManager().getLogger().gattStatus(gattStatus())
//                            );
//                }
//                else if (target() == BleDevice.ReadWriteListener.Target.CONNECTION_PRIORITY)
//                {
//                    return Utils_String.toString
//                            (
//                                    this.getClass(),
//                                    "status", status(),
//                                    "type", type(),
//                                    "target", target(),
//                                    "connectionPriority", connectionPriority(),
//                                    "gattStatus", device().getManager().getLogger().gattStatus(gattStatus())
//                            );
//                }
//                else
//                {
//                    return Utils_String.toString
//                            (
//                                    this.getClass(),
//                                    "status", status(),
//                                    "data", Arrays.toString(data()),
//                                    "type", type(),
//                                    "charUuid", device().getManager().getLogger().uuidName(charUuid()),
//                                    "gattStatus", device().getManager().getLogger().gattStatus(gattStatus())
//                            );
//                }
//            }
//        }
//    }

/**
 * Called when a read or write is complete or when a notification comes in or when a notification is enabled/disabled.
 */
//		void onEvent(final ReadWriteEvent e);
}