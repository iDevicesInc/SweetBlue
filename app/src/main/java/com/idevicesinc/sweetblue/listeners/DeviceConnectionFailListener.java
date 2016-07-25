package com.idevicesinc.sweetblue.listeners;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.P_Gateway;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;


public interface DeviceConnectionFailListener extends P_BaseConnectionFailListener
{

    /**
     * The reason for the connection failure.
     */
    public static enum Status implements UsesCustomNull
    {
        /**
         * Used in place of Java's built-in <code>null</code> wherever needed. As of now, the {@link ConnectionFailEvent#status()} given
         * to {@link DeviceConnectionFailListener#onEvent(ConnectionFailEvent)} will *never* be {@link DeviceConnectionFailListener.Status#NULL}.
         */
        NULL,

        /**
         * A call was made to {@link BleDevice#connect()} or its overloads
         * but {@link ConnectionFailEvent#device()} is already
         * {@link BleDeviceState#CONNECTING} or {@link BleDeviceState#CONNECTED}.
         */
        ALREADY_CONNECTING_OR_CONNECTED,

        /**
         * {@link BleDevice#connect()} (or various overloads) was called on {@link BleDevice#NULL}.
         */
        NULL_DEVICE,

        /**
         * Couldn't connect through {@link BluetoothDevice#connectGatt(android.content.Context, boolean, BluetoothGattCallback)}
         * because it (a) {@link Timing#IMMEDIATELY} returned <code>null</code>, (b) {@link Timing#EVENTUALLY} returned a bad
         * {@link ConnectionFailEvent#gattStatus()}, or (c) {@link Timing#TIMED_OUT}.
         */
        NATIVE_CONNECTION_FAILED,

        /**
         * {@link BluetoothGatt#discoverServices()} either (a) {@link Timing#IMMEDIATELY} returned <code>false</code>,
         * (b) {@link Timing#EVENTUALLY} returned a bad {@link ConnectionFailEvent#gattStatus()}, or (c) {@link Timing#TIMED_OUT}.
         */
        DISCOVERING_SERVICES_FAILED,

        /**
         * {@link BluetoothDevice#createBond()} either (a) {@link Timing#IMMEDIATELY} returned <code>false</code>,
         * (b) {@link Timing#EVENTUALLY} returned a bad {@link ConnectionFailEvent#bondFailReason()}, or (c) {@link Timing#TIMED_OUT}.
         * <br><br>
         * NOTE: {@link BleDeviceConfig#bondingFailFailsConnection} must be <code>true</code> for this {@link Status} to be applicable.
         *
         * @see BondListener
         */
        BONDING_FAILED,

        /**
         * The {@link BleTransaction} instance passed to {@link BleDevice#connect(BleTransaction.Auth)} or
         * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} failed through {@link BleTransaction#fail()}.
         *
         */
        AUTHENTICATION_FAILED,

        /**
         * {@link BleTransaction} instance passed to {@link BleDevice#connect(BleTransaction.Init)} or
         * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} failed through {@link BleTransaction#fail()}.
         *
         */
        INITIALIZATION_FAILED,

        /**
         * Remote peripheral randomly disconnected sometime during the connection process. Similar to {@link #NATIVE_CONNECTION_FAILED}
         * but only occurs after the device is {@link BleDeviceState#CONNECTED} and we're going through
         * {@link BleDeviceState#DISCOVERING_SERVICES}, or {@link BleDeviceState#AUTHENTICATING}, or what have you. It might
         * be from the device turning off, or going out of range, or any other random reason.
         */
        ROGUE_DISCONNECT,

        /**
         * {@link BleDevice#disconnect()} was called sometime during the connection process.
         */
        EXPLICIT_DISCONNECT,

        /**
         * {@link BleManager#reset()} or {@link BleManager#turnOff()} (or
         * overloads) were called sometime during the connection process.
         * Basic testing reveals that this value will also be used when a
         * user turns off BLE by going through their OS settings, airplane
         * mode, etc., but it's not absolutely *certain* that this behavior
         * is consistent across phones. For example there might be a phone
         * that kills all connections before going through the ble turn-off
         * process, in which case SweetBlue doesn't know the difference and
         * {@link #ROGUE_DISCONNECT} will be used.
         */
        BLE_TURNING_OFF;

        /**
         * Returns true for {@link #EXPLICIT_DISCONNECT} or {@link #BLE_TURNING_OFF}.
         */
        public boolean wasCancelled()
        {
            return this == EXPLICIT_DISCONNECT || this == BLE_TURNING_OFF;
        }

        /**
         * Same as {@link #wasCancelled()}, at least for now, but just being more "explicit", no pun intended.
         */
        boolean wasExplicit()
        {
            return wasCancelled();
        }

        /**
         * Whether this status honors a {@link P_BaseConnectionFailListener.Please#isRetry()}. Returns <code>false</code> if {@link #wasCancelled()} or
         * <code>this</code> is {@link #ALREADY_CONNECTING_OR_CONNECTED}.
         */
        public boolean allowsRetry()
        {
            return !this.wasCancelled() && this != ALREADY_CONNECTING_OR_CONNECTED;
        }

        @Override public boolean isNull()
        {
            return this == NULL;
        }

        /**
         * Convenience method that returns whether this status is something that your app user would usually care about.
         * If this returns <code>true</code> then perhaps you should pop up a {@link android.widget.Toast} or something of that nature.
         */
        public boolean shouldBeReportedToUser()
        {
            return	this == NATIVE_CONNECTION_FAILED		||
                    this == DISCOVERING_SERVICES_FAILED		||
                    this == BONDING_FAILED					||
                    this == AUTHENTICATION_FAILED			||
                    this == INITIALIZATION_FAILED			||
                    this == ROGUE_DISCONNECT				 ;
        }
    }

    /**
     * For {@link Status#NATIVE_CONNECTION_FAILED}, {@link Status#DISCOVERING_SERVICES_FAILED}, and
     * {@link Status#BONDING_FAILED}, gives further timing information on when the failure took place.
     * For all other reasons, {@link ConnectionFailEvent#timing()} will be {@link #NOT_APPLICABLE}.
     */
    public static enum Timing
    {
        /**
         * For reasons like {@link DeviceConnectionFailListener.Status#BLE_TURNING_OFF}, {@link DeviceConnectionFailListener.Status#AUTHENTICATION_FAILED}, etc.
         */
        NOT_APPLICABLE,

        /**
         * The operation failed immediately, for example by the native stack method returning <code>false</code> from a method call.
         */
        IMMEDIATELY,

        /**
         * The operation failed in the native stack. {@link DeviceConnectionFailListener.ConnectionFailEvent#gattStatus()}
         * will probably be a positive number if {@link DeviceConnectionFailListener.ConnectionFailEvent#status()} is
         * {@link DeviceConnectionFailListener.Status#NATIVE_CONNECTION_FAILED} or {@link DeviceConnectionFailListener.Status#DISCOVERING_SERVICES_FAILED}.
         * {@link DeviceConnectionFailListener.ConnectionFailEvent#bondFailReason()} will probably be a positive number if
         * {@link DeviceConnectionFailListener.ConnectionFailEvent#status()} is {@link DeviceConnectionFailListener.Status#BONDING_FAILED}.
         */
        EVENTUALLY,

        /**
         * The operation took longer than the time dictated by {@link com.idevicesinc.sweetblue.BleManagerConfig#taskTimeout}.
         */
        TIMED_OUT;
    }

    /**
     * Structure passed to {@link DeviceConnectionFailListener#onEvent(ConnectionFailEvent)} to provide more info about how/why the connection failed.
     */
    @Immutable
    public static class ConnectionFailEvent extends P_BaseConnectionFailListener.ConnectionFailEvent implements UsesCustomNull
    {
        /**
         * The {@link BleDevice} this {@link ConnectionFailEvent} is for.
         */
        public BleDevice device() {  return m_device;  }
        private final BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public String macAddress()  {  return m_device.getMacAddress();  }

        /**
         * General reason why the connection failed.
         */
        public Status status() {  return m_status;  }
        private final Status m_status;

        /**
         * See {@link BondEvent#failReason()}.
         */
        public int bondFailReason() {  return m_bondFailReason;  }
        private final int m_bondFailReason;

        /**
         * The highest state reached by the latest connection attempt.
         */
        public BleDeviceState highestStateReached_latest() {  return m_highestStateReached_latest;  }
        private final BleDeviceState m_highestStateReached_latest;

        /**
         * The highest state reached during the whole connection attempt cycle.
         * <br><br>
         * TIP: You can use this to keep the visual feedback in your connection progress UI "bookmarked" while the connection retries
         * and goes through previous states again.
         */
        public BleDeviceState highestStateReached_total() {  return m_highestStateReached_total;  }
        private final BleDeviceState m_highestStateReached_total;

        /**
         * Further timing information for {@link Status#NATIVE_CONNECTION_FAILED}, {@link Status#BONDING_FAILED}, and {@link Status#DISCOVERING_SERVICES_FAILED}.
         */
        public Timing timing() {  return m_timing;  }
        private final Timing m_timing;

        /**
         * If {@link ConnectionFailEvent#status()} is {@link Status#AUTHENTICATION_FAILED} or
         * {@link Status#INITIALIZATION_FAILED} and {@link BleTransaction#fail()} was called somewhere in or
         * downstream of {@link ReadWriteListener#onEvent(ReadWriteEvent)}, then the {@link ReadWriteEvent} passed there will be returned
         * here. Otherwise, this will return a {@link ReadWriteEvent} for which {@link ReadWriteEvent#isNull()} returns <code>true</code>.
         */
        public ReadWriteListener.ReadWriteEvent txnFailReason() {  return m_txnFailReason;  }
        private final ReadWriteListener.ReadWriteEvent m_txnFailReason;

        /**
         * Returns a chronologically-ordered list of all {@link ConnectionFailEvent} instances returned through
         * {@link DeviceConnectionFailListener#onEvent(ConnectionFailEvent)} since the first call to {@link BleDevice#connect()},
         * including the current instance. Thus this list will always have at least a length of one (except if {@link #isNull()} is <code>true</code>).
         * The list length is "reset" back to one whenever a {@link BleDeviceState#CONNECTING_OVERALL} operation completes, either
         * through becoming {@link BleDeviceState#INITIALIZED}, or {@link BleDeviceState#DISCONNECTED} for good.
         */
        public ConnectionFailEvent[] history()  {  return m_history;  }
        private final ConnectionFailEvent[] m_history;

        ConnectionFailEvent(BleDevice device, Status reason, Timing timing, int failureCountSoFar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, BleDeviceState highestStateReached, BleDeviceState highestStateReached_total, AutoConnectUsage autoConnectUsage, int bondFailReason, ReadWriteListener.ReadWriteEvent txnFailReason, ArrayList<ConnectionFailEvent> history)
        {
            super(failureCountSoFar, latestAttemptTime, totalAttemptTime, gattStatus, autoConnectUsage);

            this.m_device = device;
            this.m_status = reason;
            this.m_timing = timing;
            this.m_highestStateReached_latest = highestStateReached != null ? highestStateReached : BleDeviceState.NULL;
            this.m_highestStateReached_total = highestStateReached_total != null ? highestStateReached_total : BleDeviceState.NULL;
            this.m_bondFailReason = bondFailReason;
            this.m_txnFailReason = txnFailReason;

            if( history == null )
            {
                this.m_history = EMPTY_HISTORY();
            }
            else
            {
                this.m_history = new ConnectionFailEvent[history.size()+1];
                for( int i = 0; i < history.size(); i++ )
                {
                    this.m_history[i] = history.get(i);
                }

                this.m_history[this.m_history.length-1] = this;
            }

            // TODO - Decide how to deal with this. Implement asserts? or something else?
//            m_device.getManager().ASSERT(highestStateReached != null, "highestState_latest shouldn't be null.");
//            m_device.getManager().ASSERT(highestStateReached_total != null, "highestState_total shouldn't be null.");
        }

        private static ConnectionFailEvent[] s_emptyHistory = null;
        static ConnectionFailEvent[] EMPTY_HISTORY()
        {
            s_emptyHistory = s_emptyHistory != null ? s_emptyHistory : new ConnectionFailEvent[]{};

            return s_emptyHistory;
        }

        static ConnectionFailEvent NULL(BleDevice device)
        {
            return new ConnectionFailEvent(device, Status.NULL, Timing.NOT_APPLICABLE, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, BleDeviceState.NULL, AutoConnectUsage.NOT_APPLICABLE, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, device.NULL_READWRITE_EVENT(), null);
        }

        static ConnectionFailEvent EARLY_OUT(BleDevice device, Status reason)
        {
            return new DeviceConnectionFailListener.ConnectionFailEvent(device, reason, Timing.TIMED_OUT, 0, Interval.ZERO, Interval.ZERO, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, BleDeviceState.NULL, AutoConnectUsage.NOT_APPLICABLE, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, device.NULL_READWRITE_EVENT(), null);
        }

        /**
         * Returns whether this {@link ConnectionFailEvent} instance is a "dummy" value. For now used for
         * {@link BleNodeConfig.ReconnectFilter.ReconnectEvent#connectionFailEvent()} in certain situations.
         */
        @Override public boolean isNull()
        {
            return status().isNull();
        }

        /**
         * Forwards {@link DeviceConnectionFailListener.Status#shouldBeReportedToUser()} using {@link #status()}.
         */
        public boolean shouldBeReportedToUser()
        {
            return status().shouldBeReportedToUser();
        }

        @Override public String toString()
        {
            if (isNull())
            {
                return Status.NULL.name();
            }
            else
            {
                if( status() == Status.BONDING_FAILED )
                {
                    return Utils_String.toString
                            (
                                    this.getClass(),
                                    "device",				device().getName(),
                                    "status", 				status(),
                                    "timing",				timing(),
                                    "bondFailReason",       P_Gateway.gattUnbondReason(device().getManager(), bondFailReason()),
                                    "failureCountSoFar",	failureCountSoFar()
                            );
                }
                else
                {
                    return Utils_String.toString
                            (
                                    this.getClass(),
                                    "device",				device().getName(),
                                    "status", 				status(),
                                    "timing",				timing(),
                                    "gattStatus",			P_Gateway.gattStatus(device().getManager(), gattStatus()),
                                    "failureCountSoFar",	failureCountSoFar()
                            );
                }
            }
        }
    }

    /**
     * Return value is ignored if device is either {@link BleDeviceState#RECONNECTING_LONG_TERM} or reason
     * {@link Status#allowsRetry()} is <code>false</code>. If the device is {@link BleDeviceState#RECONNECTING_LONG_TERM}
     * then authority is deferred to {@link BleNodeConfig.ReconnectFilter}.
     * <br><br>
     * Otherwise, this method offers a more convenient way of retrying a connection, as opposed to manually doing it yourself. It also lets
     * the library handle things in a slightly more optimized/cleaner fashion and so is recommended for that reason also.
     * <br><br>
     * NOTE that this callback gets fired *after* {@link StateListener} lets you know that the device is {@link BleDeviceState#DISCONNECTED}.
     * <br><br>
     * The time parameters like {@link ConnectionFailEvent#attemptTime_latest()} are of optional use to you to decide if connecting again
     * is worth it. For example if you've been trying to connect for 10 seconds already, chances are that another connection attempt probably won't work.
     */
    Please onEvent(final ConnectionFailEvent e);

}
