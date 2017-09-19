package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.content.Context;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;


public interface ServerReconnectFilter extends ReconnectFilter<ServerReconnectFilter.ConnectFailEvent>
{

    /**
     * The reason for the connection failure.
     */
    enum Status implements UsesCustomNull
    {
        /**
         * Used in place of Java's built-in <code>null</code> wherever needed. As of now, the {@link ConnectFailEvent#status()} given
         * to {@link ServerReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)} will *never* be {@link Status#NULL}.
         */
        NULL,

        /**
         * A call was made to {@link BleServer#connect(String)} or its overloads
         * but {@link ConnectFailEvent#server()} is already
         * {@link BleServerState#CONNECTING} or {@link BleServerState#CONNECTED} for the given client.
         */
        ALREADY_CONNECTING_OR_CONNECTED,

        /**
         * {@link BleServer#connect(String)} (or various overloads) was called on {@link BleServer#NULL}.
         */
        NULL_SERVER,

        /**
         * The call to {@link android.bluetooth.BluetoothManager#openGattServer(Context, BluetoothGattServerCallback)} returned
         * a <code>null</code> object instance, so we could not proceed.
         */
        SERVER_OPENING_FAILED,

        /**
         * Couldn't connect through {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}
         * because it returned <code>false</code>.
         */
        NATIVE_CONNECTION_FAILED_IMMEDIATELY,

        /**
         * Couldn't connect through {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}
         * because we eventually got a bad status code through {@link BluetoothGattServerCallback#onConnectionStateChange(BluetoothDevice, int, int)}.
         */
        NATIVE_CONNECTION_FAILED_EVENTUALLY,

        /**
         * Couldn't connect through {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}
         * because the operation took longer than the time dictated by {@link BleNodeConfig#taskTimeoutRequestFilter}.
         */
        TIMED_OUT,

        /**
         * {@link BleServer#disconnect()} or overloads was called sometime during the connection process.
         */
        CANCELLED_FROM_DISCONNECT,

        /**
         * {@link BleManager#reset()} or {@link BleManager#turnOff()} (or
         * overloads) were called sometime during the connection process.
         * Basic testing reveals that this value will also be used when a
         * user turns off BLE by going through their OS settings, airplane
         * mode, etc., but it's not absolutely *certain* that this behavior
         * is consistent across phones. For example there might be a phone
         * that kills all connections *before* going through the ble turn-off
         * process, thus {@link #NATIVE_CONNECTION_FAILED_EVENTUALLY} would probably be seen.
         */
        CANCELLED_FROM_BLE_TURNING_OFF;

        /**
         * Returns true for {@link #CANCELLED_FROM_DISCONNECT} or {@link #CANCELLED_FROM_BLE_TURNING_OFF}.
         */
        public final boolean wasCancelled()
        {
            return this == CANCELLED_FROM_DISCONNECT || this == CANCELLED_FROM_BLE_TURNING_OFF;
        }

        /**
         * Same as {@link #wasCancelled()}, at least for now, but just being more "explicit", no pun intended.
         */
        final boolean wasExplicit()
        {
            return wasCancelled();
        }

        /**
         * Whether this reason honors a {@link com.idevicesinc.sweetblue.ReconnectFilter.ConnectFailPlease#isRetry()}. Returns <code>false</code> if {@link #wasCancelled()} or
         * <code>this</code> is {@link #ALREADY_CONNECTING_OR_CONNECTED}.
         */
        public final boolean allowsRetry()
        {
            return !this.wasCancelled() && this != ALREADY_CONNECTING_OR_CONNECTED;
        }

        @Override public final boolean isNull()
        {
            return this == NULL;
        }

        /**
         * Convenience method that returns whether this status is something that your app user would usually care about.
         * If this returns <code>true</code> then perhaps you should pop up a {@link android.widget.Toast} or something of that nature.
         */
        public final boolean shouldBeReportedToUser()
        {
            return	this == SERVER_OPENING_FAILED					||
                    this == NATIVE_CONNECTION_FAILED_IMMEDIATELY	||
                    this == NATIVE_CONNECTION_FAILED_EVENTUALLY		||
                    this == TIMED_OUT;
        }
    }

    /**
     * Structure passed to {@link #onConnectFailed(ReconnectFilter.ConnectFailEvent)} to provide more info about how/why the connection failed.
     */
    @Immutable
    class ConnectFailEvent extends ReconnectFilter.ConnectFailEvent implements UsesCustomNull
    {
        /**
         * The {@link BleServer} this {@link ConnectFailEvent} is for.
         */
        public final BleServer server() {  return m_server;  }
        private final BleServer m_server;

        /**
         * The native {@link BluetoothDevice} client this {@link ConnectFailEvent} is for.
         */
        public final BluetoothDevice nativeDevice() {  return m_nativeDevice;  }
        private final BluetoothDevice m_nativeDevice;

        /**
         * Returns the mac address of the client that's undergoing the state change with this {@link #server()}.
         */
        public final String macAddress()  {  return m_nativeDevice.getAddress();  }

        /**
         * General reason why the connection failed.
         */
        public final Status status() {  return m_status;  }
        private final Status m_status;

        /**
         * Returns a chronologically-ordered list of all {@link ConnectFailEvent} instances returned through
         * {@link ServerReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)} since the first call to {@link BleDevice#connect()},
         * including the current instance. Thus this list will always have at least a length of one (except if {@link #isNull()} is <code>true</code>).
         * The list length is "reset" back to one whenever a {@link BleDeviceState#CONNECTING_OVERALL} operation completes, either
         * through becoming {@link BleDeviceState#INITIALIZED}, or {@link BleDeviceState#DISCONNECTED} for good.
         */
        public final ConnectFailEvent[] history()  {  return m_history;  }
        private final ConnectFailEvent[] m_history;

        ConnectFailEvent(BleServer server, final BluetoothDevice nativeDevice, Status status, int failureCountSoFar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, AutoConnectUsage autoConnectUsage, ArrayList<ConnectFailEvent> history)
        {
            super(failureCountSoFar, latestAttemptTime, totalAttemptTime, gattStatus, autoConnectUsage);

            this.m_server = server;
            this.m_nativeDevice = nativeDevice;
            this.m_status = status;

            if( history == null )
            {
                this.m_history = EMPTY_HISTORY();
            }
            else
            {
                this.m_history = new ConnectFailEvent[history.size()+1];
                for( int i = 0; i < history.size(); i++ )
                {
                    this.m_history[i] = history.get(i);
                }

                this.m_history[this.m_history.length-1] = this;
            }
        }

        private static ConnectFailEvent[] s_emptyHistory = null;
        /*package*/ static ConnectFailEvent[] EMPTY_HISTORY()
        {
            s_emptyHistory = s_emptyHistory != null ? s_emptyHistory : new ConnectFailEvent[0];

            return s_emptyHistory;
        }

        /*package*/ static ConnectFailEvent NULL(BleServer server, BluetoothDevice nativeDevice)
        {
            return new ConnectFailEvent(server, nativeDevice, Status.NULL, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, AutoConnectUsage.NOT_APPLICABLE, null);
        }

        /*package*/ static ConnectFailEvent EARLY_OUT(BleServer server, BluetoothDevice nativeDevice, Status status)
        {
            return new ConnectFailEvent(server, nativeDevice, status, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, AutoConnectUsage.NOT_APPLICABLE, null);
        }

        /**
         * Returns whether this {@link ConnectFailEvent} instance is a "dummy" value. For now used for
         * {@link ReconnectFilter.ConnectionLostEvent#connectionFailEvent()} in certain situations.
         */
        @Override public final boolean isNull()
        {
            return status().isNull();
        }

        /**
         * Forwards {@link DeviceReconnectFilter.Status#shouldBeReportedToUser()}
         * using {@link #status()}.
         */
        public final boolean shouldBeReportedToUser()
        {
            return status().shouldBeReportedToUser();
        }

        @Override public final String toString()
        {
            if (isNull())
            {
                return Status.NULL.name();
            }
            else
            {
                return Utils_String.toString
                        (
                                this.getClass(),
                                "server",				server(),
                                "macAddress",			macAddress(),
                                "status", 				status(),
                                "gattStatus",			server().getManager().getLogger().gattStatus(gattStatus()),
                                "failureCountSoFar",	failureCountSoFar()
                        );
            }
        }
    }

}
