package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.UUID;

/**
 * Provide an instance to various static methods of {@link IncomingListener.Please} such as
 * {@link IncomingListener.Please#respondWithSuccess(OutgoingListener)}, or {@link BleServer#setListener_Outgoing(OutgoingListener)},
 * or {@link BleManager#setListener_Outgoing(OutgoingListener)}. Also used to callback the success or failure of
 * notifications through {@link BleServer#sendNotification(String, UUID, UUID, FutureData, OutgoingListener)},
 * {@link BleServer#sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}, or various overloads thereof.
 */
public interface OutgoingListener extends ExchangeListener
{
    /**
     * Struct passed to {@link OutgoingListener#onEvent(OutgoingEvent)}
     * that provides details of what was sent to the client and if it succeeded.
     */
    @Immutable
    class OutgoingEvent extends ExchangeEvent implements UsesCustomNull
    {
        /**
         * Returns the result of the response, or {@link Status#NO_RESPONSE_ATTEMPTED} if
         * for example {@link IncomingListener.Please#doNotRespond(OutgoingListener)} was used.
         */
        public final Status status()  {  return m_status;  }
        private final Status m_status;

        /**
         * The data that was attempted to be sent back to the client if {@link #type()} {@link Type#isRead()} is <code>true</code>.
         */
        public final byte[] data_sent()  {  return m_data_sent;  }
        private final byte[] m_data_sent;

        /**
         * The gattStatus sent to the client, provided to static methods of {@link IncomingListener.Please}
         * if {@link #type()} is {@link Type#READ} or {@link Type#WRITE} - otherwise this will equal {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
         */
        public final int gattStatus_sent()  {  return m_gattStatus_sent;  }
        private final int m_gattStatus_sent;

        /**
         * The gattStatus received from an attempted communication with the client. For now this is only relevant if {@link #type()}
         * {@link Type#isNotificationOrIndication()} is <code>true</code> - otherwise this will equal {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
         */
        public final int gattStatus_received()  {  return m_gattStatus_received;  }
        private final int m_gattStatus_received;

        /**
         * This returns <code>true</code> if this event was the result of an explicit call through SweetBlue, e.g. through
         * {@link BleServer#sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}. It will return <code>false</code> otherwise,
         * which can happen if for example you use {@link BleServer#getNativeLayer()} to bypass SweetBlue for whatever reason.
         * Another theoretical case is if you make an explicit call through SweetBlue, then you get {@link Status#TIMED_OUT},
         * but then the native stack eventually *does* come back with something - this has never been observed, but it is possible.
         */
        public final boolean solicited()  {  return m_solicited;  }
        private final boolean m_solicited;

        OutgoingEvent(BleServer server, BluetoothDevice nativeDevice, UUID serviceUuid_in, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_received, byte[] data_sent, int requestId, int offset, final boolean responseNeeded, final Status status, final int gattStatus_sent, final int gattStatus_received, final boolean solicited)
        {
            super(server, nativeDevice, serviceUuid_in, charUuid_in, descUuid_in, type_in, target_in, data_received, requestId, offset, responseNeeded);

            m_status = status;
            m_data_sent = data_sent;
            m_gattStatus_received = gattStatus_received;
            m_gattStatus_sent = gattStatus_sent;
            m_solicited = solicited;
        }

        OutgoingEvent(final IncomingListener.IncomingEvent e, final byte[] data_sent, final Status status, final int gattStatus_sent, final int gattStatus_received)
        {
            super(e.server(), e.nativeDevice(), e.serviceUuid(), e.charUuid(), e.descUuid(), e.type(), e.target(), e.data_received(), e.requestId(), e.offset(), e.responseNeeded());

            m_status = status;
            m_data_sent = data_sent;
            m_gattStatus_received = gattStatus_received;
            m_gattStatus_sent = gattStatus_sent;
            m_solicited = true;
        }

        static OutgoingEvent EARLY_OUT__NOTIFICATION(final BleServer server, final BluetoothDevice nativeDevice, final UUID serviceUuid, final UUID charUuid, final FutureData data, final Status status)
        {
            return new OutgoingEvent
            (
                server, nativeDevice, serviceUuid, charUuid, NON_APPLICABLE_UUID, Type.NOTIFICATION, Target.CHARACTERISTIC,
                P_Const.EMPTY_BYTE_ARRAY, data.getData(), NON_APPLICABLE_REQUEST_ID, 0, false, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE,
                BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true
            );
        }

        static OutgoingEvent NULL__NOTIFICATION(final BleServer server, final BluetoothDevice nativeDevice, final UUID serviceUuid, final UUID charUuid)
        {
            return EARLY_OUT__NOTIFICATION(server, nativeDevice, serviceUuid, charUuid, P_Const.EMPTY_FUTURE_DATA, Status.NULL);
        }

        /**
         * Checks if {@link #status()} is {@link Status#SUCCESS}.
         */
        public final boolean wasSuccess()
        {
            return status() == Status.SUCCESS;
        }

        /**
         * Will return true in certain early-out cases when there is no issue and the response can continue.
         * See {@link BleServer#sendNotification(String, UUID, UUID, byte[], OutgoingListener)} for more information.
         */
        @Override public final boolean isNull()
        {
            return status().isNull();
        }

        @Override public final String toString()
        {
            if( type().isRead() )
            {
                return Utils_String.toString
                (
                    this.getClass(),
                    "status",			status(),
                    "type",				type(),
                    "target",			target(),
                    "macAddress",		macAddress(),
                    "charUuid",			server().getManager().getLogger().uuidName(charUuid()),
                    "requestId",		requestId()
                );
            }
            else
            {
                return Utils_String.toString
                (
                    this.getClass(),
                    "status",			status(),
                    "type",				type(),
                    "target",			target(),
                    "data_received",	data_received(),
                    "macAddress",		macAddress(),
                    "charUuid",			server().getManager().getLogger().uuidName(charUuid()),
                    "requestId",		requestId()
                );
            }
        }
    }

    /**
     * Enumeration of the various success and error statuses possible for an outgoing message.
     */
    enum Status implements UsesCustomNull
    {
        /**
         * Fulfills the soft contract of {@link UsesCustomNull}.
         */
        NULL,

        /**
         * The outgoing message to the client was successfully sent.
         */
        SUCCESS,

        /**
         * {@link BleServer#sendNotification(String, UUID, UUID, FutureData, OutgoingListener)} or
         * {@link BleServer#sendIndication(String, UUID, byte[])} (or various overloads) was called
         * on {@link BleServer#NULL}.
         */
        NULL_SERVER,

        /**
         * {@link IncomingListener.Please#doNotRespond(OutgoingListener)} (or overloads)
         * were called or {@link IncomingListener.IncomingEvent#responseNeeded()} was <code>false</code>.
         */
        NO_RESPONSE_ATTEMPTED,

        /**
         * The server does not have a {@link IncomingListener} set so no valid response
         * could be sent. Please set a listener through {@link BleServer#setListener_Incoming(IncomingListener)}.
         */
        NO_REQUEST_LISTENER_SET,

        /**
         * Couldn't find a matching {@link OutgoingEvent#target()} for {@link OutgoingEvent#charUuid()}.
         */
        NO_MATCHING_TARGET,

        /**
         * For now only relevant if {@link OutgoingEvent#type()} is {@link Type#NOTIFICATION} -
         * {@link BluetoothGattCharacteristic#setValue(byte[])} (or one of its overloads) returned <code>false</code>.
         */
        FAILED_TO_SET_VALUE_ON_TARGET,

        /**
         * The underlying call to {@link BluetoothGattServer#sendResponse(BluetoothDevice, int, int, int, byte[])}
         * or {@link BluetoothGattServer#notifyCharacteristicChanged(BluetoothDevice, BluetoothGattCharacteristic, boolean)}
         * failed for reasons unknown.
         */
        FAILED_TO_SEND_OUT,

        /**
         * The operation failed in a "normal" fashion, at least relative to all the other strange ways an operation can fail. This means for
         * example that {@link BluetoothGattServer#notifyCharacteristicChanged(BluetoothDevice, BluetoothGattCharacteristic, boolean)}
         * returned a status code through {@link BluetoothGattServerCallback#onNotificationSent(BluetoothDevice, int)} that was not zero.
         * This could mean the device went out of range, was turned off, signal was disrupted, whatever. Often this means that the
         * client is about to become {@link BleServerState#DISCONNECTED}.
         */
        REMOTE_GATT_FAILURE,

        /**
         * The operation was cancelled by the client/server becoming {@link BleServerState#DISCONNECTED}.
         */
        CANCELLED_FROM_DISCONNECT,

        /**
         * The operation was cancelled because {@link BleManager} went {@link BleManagerState#TURNING_OFF} and/or
         * {@link BleManagerState#OFF}. Note that if the user turns off BLE from their OS settings (airplane mode, etc.) then
         * {@link OutgoingEvent#status()} could potentially be {@link #CANCELLED_FROM_DISCONNECT} because SweetBlue might get
         * the disconnect callback before the turning off callback. Basic testing has revealed that this is *not* the case, but you never know.
         * <br><br>
         * Either way, the client was or will be disconnected.
         */
        CANCELLED_FROM_BLE_TURNING_OFF,

        /**
         * Couldn't send out the data because the operation took longer than the time dictated by {@link BleNodeConfig#taskTimeoutRequestFilter}
         * so we had to cut her loose.
         */
        TIMED_OUT,

        /**
         * Could not communicate with the client device because the server is not currently {@link BleServerState#CONNECTED}.
         */
        NOT_CONNECTED;

        /**
         * Returns true if <code>this==</code> {@link #NULL}.
         */
        @Override public final boolean isNull()
        {
            return this == NULL;
        }
    }

    /**
     * Called when a notification or a response to a request is fulfilled or failed.
     */
    void onEvent(final OutgoingEvent e);
}
