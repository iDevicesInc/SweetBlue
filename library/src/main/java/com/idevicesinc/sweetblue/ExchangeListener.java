package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Lambda;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.UUID;

/**
 * Tagging interface, not to be implemented directly as this is just the base interface to statically tie together
 * {@link IncomingListener} and {@link OutgoingListener} with common enums/structures.
 */
@Lambda
public interface ExchangeListener
{
    /**
     * The type of GATT object, provided by {@link ExchangeEvent#target()}.
     */
    enum Target
    {
        /**
         * The {@link ExchangeEvent} returned has to do with a {@link BluetoothGattCharacteristic} under the hood.
         */
        CHARACTERISTIC,

        /**
         * The {@link ExchangeEvent} returned has to do with a {@link BluetoothGattDescriptor} under the hood.
         */
        DESCRIPTOR;
    }

    /**
     * The type of exchange being executed, read, write, or notify.
     */
    enum Type
    {
        /**
         * The client is requesting a read of some data from us, the server.
         */
        READ,

        /**
         * The client is requesting acceptance of a write.
         */
        WRITE,

        /**
         * The client is requesting acceptance of a prepared write.
         */
        PREPARED_WRITE,

        /**
         * Only for {@link BleServer#sendNotification(String, UUID, byte[])} or overloads.
         */
        NOTIFICATION,

        /**
         * Only for {@link BleServer#sendIndication(String, UUID, byte[])} or overloads.
         */
        INDICATION;

        /**
         * Shorthand for checking if this equals {@link #READ}.
         */
        public final boolean isRead()
        {
            return this == READ;
        }

        /**
         * Shorthand for checking if this equals {@link #NOTIFICATION} or {@link #INDICATION}.
         */
        public final boolean isNotificationOrIndication()
        {
            return this == NOTIFICATION || this == INDICATION;
        }

        /**
         * Shorthand for checking if this equals {@link #WRITE} or {@link #PREPARED_WRITE}.
         */
        public final boolean isWrite()
        {
            return this == WRITE || this == PREPARED_WRITE;
        }
    }

    /**
     * Like {@link ExchangeListener}, this class should not be used directly as this is just a base class to statically tie together
     * {@link IncomingListener.IncomingEvent} and {@link OutgoingListener.OutgoingEvent} with a common API.
     */
    @Immutable
    abstract class ExchangeEvent extends Event
    {
        /**
         * Value used in place of <code>null</code>, either indicating that {@link #descUuid()}
         * isn't used for the {@link ExchangeEvent} because {@link #target()} is {@link Target#CHARACTERISTIC}.
         */
        public static final UUID NON_APPLICABLE_UUID = Uuids.INVALID;

        /**
         * Return value of {@link #requestId()} if {@link #type()} is {@link Type#NOTIFICATION}.
         */
        public static final int NON_APPLICABLE_REQUEST_ID = -1;

        /**
         * The {@link BleServer} this {@link ExchangeEvent} is for.
         */
        public final BleServer server() {  return m_server;  }
        private final BleServer m_server;

        /**
         * Returns the mac address of the client peripheral that we are exchanging data with.
         */
        public final String macAddress()  {  return m_nativeDevice.getAddress(); }

        /**
         * Returns the native bluetooth device object representing the client making the request.
         */
        public final BluetoothDevice nativeDevice()  {  return m_nativeDevice;  };
        private final BluetoothDevice m_nativeDevice;

        /**
         * The type of operation, read or write.
         */
        public final Type type() {  return m_type;  }
        private final Type m_type;

        /**
         * The type of GATT object this {@link ExchangeEvent} is for, characteristic or descriptor.
         */
        public final Target target() {  return m_target; }
        private final Target m_target;

        /**
         * The {@link UUID} of the service associated with this {@link ExchangeEvent}.
         */
        public final UUID serviceUuid() {  return m_serviceUuid; }
        private final UUID m_serviceUuid;

        /**
         * The {@link UUID} of the characteristic associated with this {@link ExchangeEvent}. This will always be
         * a valid {@link UUID}, even if {@link #target()} is {@link Target#DESCRIPTOR}.
         */
        public final @Nullable(Nullable.Prevalence.NEVER) UUID charUuid() {  return m_charUuid; }
        private final UUID m_charUuid;

        /**
         * The {@link UUID} of the descriptor associated with this {@link ExchangeEvent}. If {@link #target} is
         * {@link Target#CHARACTERISTIC} then this will be referentially equal (i.e. you can use == to compare)
         * to {@link #NON_APPLICABLE_UUID}.
         */
        public final @Nullable(Nullable.Prevalence.NEVER) UUID descUuid() {  return m_descUuid; }
        private final UUID m_descUuid;

        /**
         * The data received from the client if {@link #type()} is {@link Type#isWrite()}, otherwise an empty byte array.
         * This is in contrast to {@link OutgoingListener.OutgoingEvent#data_sent()} if
         * {@link #type()} is {@link Type#isRead()}.
         *
         */
        public final @Nullable(Nullable.Prevalence.NEVER) byte[] data_received() {  return m_data_received; }
        private final byte[] m_data_received;

        /**
         * The request id forwarded from the native stack. See various methods of {@link android.bluetooth.BluetoothGattServerCallback} for explanation.
         * Not relevant if {@link #type()} {@link Type#isNotificationOrIndication()} is <code>true</code>.
         */
        public final int requestId()  {  return m_requestId;  }
        private final int m_requestId;

        /**
         * The offset forwarded from the native stack. See various methods of {@link android.bluetooth.BluetoothGattServerCallback} for explanation.
         * Not relevant if {@link #type()} {@link Type#isNotificationOrIndication()} is <code>true</code>.
         */
        public final int offset()  {  return m_offset;  }
        private final int m_offset;

        /**
         * Dictates whether a response is needed.
         * Not relevant if {@link #type()} {@link Type#isNotificationOrIndication()} is <code>true</code>.
         */
        public final boolean responseNeeded()  {  return m_responseNeeded;  }
        private final boolean m_responseNeeded;

        ExchangeEvent(BleServer server, BluetoothDevice nativeDevice, UUID serviceUuid_in, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_in, int requestId, int offset, final boolean responseNeeded)
        {
            m_server = server;
            m_nativeDevice = nativeDevice;
            m_serviceUuid = serviceUuid_in != null ? serviceUuid_in: NON_APPLICABLE_UUID;;
            m_charUuid = charUuid_in != null ? charUuid_in : NON_APPLICABLE_UUID;;
            m_descUuid = descUuid_in != null ? descUuid_in : NON_APPLICABLE_UUID;
            m_type = type_in;
            m_target = target_in;
            m_requestId = requestId;
            m_offset = offset;
            m_responseNeeded = responseNeeded;

            m_data_received = data_in != null ? data_in : P_Const.EMPTY_BYTE_ARRAY;
        }

        public final boolean isFor(final String macAddress)  {  return macAddress().equals(macAddress);  }
        public final boolean isFor(final UUID uuid)  {  return uuid.equals(serviceUuid()) || uuid.equals(charUuid()) || uuid.equals(descUuid());  }
    }
}
