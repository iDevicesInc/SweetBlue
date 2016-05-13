package com.idevicesinc.sweetblue.listeners;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.content.Context;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleNode;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.P_Gateway;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.ArrayList;

public interface ServerConnectionFailListener extends P_BaseConnectionFailListener
{

    /**
     * The reason for the connection failure.
     */
    public static enum Status implements UsesCustomNull
    {
        /**
         * Used in place of Java's built-in <code>null</code> wherever needed. As of now, the {@link ConnectionFailEvent#status()} given
         * to {@link ServerConnectionFailListener#onEvent(ConnectionFailEvent)} will *never* be {@link ServerConnectionFailListener.Status#NULL}.
         */
        NULL,

        /**
         * A call was made to {@link BleServer#connect(String)} or its overloads
         * but {@link ConnectionFailEvent#server()} is already
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
        public boolean wasCancelled()
        {
            return this == CANCELLED_FROM_DISCONNECT || this == CANCELLED_FROM_BLE_TURNING_OFF;
        }

        /**
         * Same as {@link #wasCancelled()}, at least for now, but just being more "explicit", no pun intended.
         */
        boolean wasExplicit()
        {
            return wasCancelled();
        }

        /**
         * Whether this reason honors a {@link P_BaseConnectionFailListener.Please#isRetry()}. Returns <code>false</code> if {@link #wasCancelled()} or
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
            return	this == SERVER_OPENING_FAILED					||
                    this == NATIVE_CONNECTION_FAILED_IMMEDIATELY	||
                    this == NATIVE_CONNECTION_FAILED_EVENTUALLY		||
                    this == TIMED_OUT;
        }
    }

    /**
     * Structure passed to {@link ServerConnectionFailListener#onEvent(ConnectionFailEvent)} to provide more info about how/why the connection failed.
     */
    @Immutable
    public static class ConnectionFailEvent extends P_BaseConnectionFailListener.ConnectionFailEvent implements UsesCustomNull
    {
        /**
         * The {@link BleServer} this {@link ConnectionFailEvent} is for.
         */
        public BleServer server() {  return m_server;  }
        private final BleServer m_server;

        /**
         * The native {@link BluetoothDevice} client this {@link ConnectionFailEvent} is for.
         */
        public BluetoothDevice nativeDevice() {  return m_nativeDevice;  }
        private final BluetoothDevice m_nativeDevice;

        /**
         * Returns the mac address of the client that's undergoing the state change with this {@link #server()}.
         */
        public String macAddress()  {  return m_nativeDevice.getAddress();  }

        /**
         * General reason why the connection failed.
         */
        public Status status() {  return m_status;  }
        private final Status m_status;

        /**
         * Returns a chronologically-ordered list of all {@link ConnectionFailEvent} instances returned through
         * {@link ServerConnectionFailListener#onEvent(ConnectionFailEvent)} since the first call to {@link BleDevice#connect()},
         * including the current instance. Thus this list will always have at least a length of one (except if {@link #isNull()} is <code>true</code>).
         * The list length is "reset" back to one whenever a {@link BleDeviceState#CONNECTING_OVERALL} operation completes, either
         * through becoming {@link BleDeviceState#INITIALIZED}, or {@link BleDeviceState#DISCONNECTED} for good.
         */
        public ConnectionFailEvent[] history()  {  return m_history;  }
        private final ConnectionFailEvent[] m_history;

        ConnectionFailEvent(BleServer server, final BluetoothDevice nativeDevice, Status status, int failureCountSoFar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, AutoConnectUsage autoConnectUsage, ArrayList<ConnectionFailEvent> history)
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
                this.m_history = new ConnectionFailEvent[history.size()+1];
                for( int i = 0; i < history.size(); i++ )
                {
                    this.m_history[i] = history.get(i);
                }

                this.m_history[this.m_history.length-1] = this;
            }
        }

        private static ConnectionFailEvent[] s_emptyHistory = null;
        /*package*/ static ConnectionFailEvent[] EMPTY_HISTORY()
        {
            s_emptyHistory = s_emptyHistory != null ? s_emptyHistory : new ConnectionFailEvent[0];

            return s_emptyHistory;
        }

        /*package*/ static ConnectionFailEvent NULL(BleServer server, BluetoothDevice nativeDevice)
        {
            return new ConnectionFailEvent(server, nativeDevice, Status.NULL, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, AutoConnectUsage.NOT_APPLICABLE, null);
        }

        /*package*/ static ConnectionFailEvent EARLY_OUT(BleServer server, BluetoothDevice nativeDevice, Status status)
        {
            return new ServerConnectionFailListener.ConnectionFailEvent(server, nativeDevice, status, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, AutoConnectUsage.NOT_APPLICABLE, null);
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
         * Forwards {@link DeviceConnectionFailListener.Status#shouldBeReportedToUser()}
         * using {@link #status()}.
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
                return Utils_String.toString
                        (
                                this.getClass(),
                                "server",				server(),
                                "macAddress",			macAddress(),
                                "status", 				status(),
                                "gattStatus",           P_Gateway.gattStatus(server().getManager(), gattStatus()),
                                "failureCountSoFar",	failureCountSoFar()
                        );
            }
        }
    }

    Please onEvent(final ConnectionFailEvent e);
}
