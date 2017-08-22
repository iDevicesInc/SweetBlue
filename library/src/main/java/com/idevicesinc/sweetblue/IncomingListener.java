package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Lambda;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.UUID;

/**
 * Provide an instance through {@link BleServer#setListener_Incoming(IncomingListener)}.
 * The return value of {@link IncomingListener#onEvent(IncomingEvent)} is used to decide if/how to respond to a given {@link IncomingEvent}.
 */
@Lambda
public interface IncomingListener extends ExchangeListener
{
    /**
     * Struct passed to {@link IncomingListener#onEvent(IncomingEvent)}} that provides details about the client and what it wants from us, the server.
     */
    @Immutable
    class IncomingEvent extends ExchangeEvent
    {
        IncomingEvent(BleServer server, BluetoothDevice nativeDevice, UUID serviceUuid_in, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_in, int requestId, int offset, final boolean responseNeeded)
        {
            super(server, nativeDevice, serviceUuid_in, charUuid_in, descUuid_in, type_in, target_in, data_in, requestId, offset, responseNeeded);
        }

        @Override public final String toString()
        {
            if( type().isRead() )
            {
                return Utils_String.toString
                (
                    this.getClass(),
                    "type", type(),
                    "target", target(),
                    "macAddress", macAddress(),
                    "charUuid", server().getManager().getLogger().uuidName(charUuid()),
                    "requestId", requestId()
                );
            }
            else
            {
                return Utils_String.toString
                (
                    this.getClass(),
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
     * Struct returned from {@link IncomingListener#onEvent(IncomingEvent)}.
     * Use the static constructor methods to create instances.
     */
    @Immutable
    class Please
    {
        final int m_gattStatus;
        final int m_offset;
        final FutureData m_futureData;
        final OutgoingListener m_outgoingListener;

        final boolean m_respond;

        private Please(final FutureData futureData, final int gattStatus, final int offset, final OutgoingListener outgoingListener)
        {
            m_respond = true;

            m_futureData = futureData != null ? futureData : P_Const.EMPTY_FUTURE_DATA;
            m_gattStatus = gattStatus;
            m_offset = offset;
            m_outgoingListener = outgoingListener != null ? outgoingListener : BleServer.NULL_OUTGOING_LISTENER;
        }

        private Please(final OutgoingListener outgoingListener)
        {
            m_respond = false;

            m_gattStatus = 0;
            m_offset = 0;
            m_futureData = P_Const.EMPTY_FUTURE_DATA;
            m_outgoingListener = outgoingListener != null ? outgoingListener : BleServer.NULL_OUTGOING_LISTENER;
        }

        /**
         * Use this as the return value of {@link IncomingListener#onEvent(IncomingEvent)}
         * when {@link IncomingEvent#responseNeeded()} is <code>true</code>.
         */
        public static Please doNotRespond()
        {
            return doNotRespond(null);
        }

        /**
         * Same as {@link #doNotRespond()} but allows you to provide a listener specific to this (non-)response.
         * Your {@link OutgoingListener#onEvent(OutgoingListener.OutgoingEvent)} will simply be called
         * with {@link OutgoingListener.Status#NO_RESPONSE_ATTEMPTED}.
         *
         * @see BleServer#setListener_Outgoing(OutgoingListener)
         */
        public static Please doNotRespond(final OutgoingListener listener)
        {
            return new Please(listener);
        }

        /**
         * Overload of {@link #respondWithSuccess(byte[])} - see {@link FutureData} for why/when you would want to use this.
         */
        public static Please respondWithSuccess(final FutureData futureData)
        {
            return respondWithSuccess(futureData, null);
        }

        /**
         * Overload of {@link #respondWithSuccess(byte[], OutgoingListener)} - see {@link FutureData} for why/when you would want to use this.
         */
        public static Please respondWithSuccess(final FutureData futureData, final OutgoingListener listener)
        {
            return new Please(futureData, BleStatuses.GATT_SUCCESS, /*offset=*/0, listener);
        }

        /**
         * Use this as the return value of {@link IncomingListener#onEvent(IncomingEvent)} when
         * {@link IncomingEvent#type()} {@link Type#isRead()} is <code>true</code> and you can respect
         * the read request and respond with data.
         */
        public static Please respondWithSuccess(final byte[] data)
        {
            return respondWithSuccess(data, null);
        }

        /**
         * Same as {@link #respondWithSuccess(byte[])} but allows you to provide a listener specific to this response.
         *
         * @see BleServer#setListener_Outgoing(OutgoingListener)
         */
        public static Please respondWithSuccess(final byte[] data, final OutgoingListener listener)
        {
            return respondWithSuccess(new PresentData(data), listener);
        }

        /**
         * Use this as the return value of {@link IncomingListener#onEvent(IncomingEvent)}
         * when {@link IncomingEvent#responseNeeded()} is <code>true</code> and {@link IncomingEvent#type()}
         * {@link Type#isWrite()} is <code>true</code> and you consider the write successful.
         */
        public static Please respondWithSuccess()
        {
            return respondWithSuccess((OutgoingListener)null);
        }

        /**
         * Same as {@link #respondWithSuccess()} but allows you to provide a listener specific to this response.
         *
         * @see BleServer#setListener_Outgoing(OutgoingListener)
         */
        public static Please respondWithSuccess(final OutgoingListener listener)
        {
            return new Please(P_Const.EMPTY_FUTURE_DATA, BleStatuses.GATT_SUCCESS, /*offset=*/0, listener);
        }

        /**
         * Send an error/status code back to the client. See <code>static final int</code>
         * members of {@link BleStatuses} starting with GATT_ for possible values.
         */
        public static Please respondWithError(final int gattStatus)
        {
            return respondWithError(gattStatus, null);
        }

        /**
         * Same as {@link #respondWithError(int)} but allows you to provide a listener specific to this response.
         *
         * @see BleServer#setListener_Outgoing(OutgoingListener)
         */
        public static Please respondWithError(final int gattStatus, final OutgoingListener listener)
        {
            return new Please(P_Const.EMPTY_FUTURE_DATA, gattStatus, /*offset=*/0, listener);
        }
    }

    /**
     * Called when a read or write from the client is requested.
     */
    Please onEvent(final IncomingEvent e);
}
