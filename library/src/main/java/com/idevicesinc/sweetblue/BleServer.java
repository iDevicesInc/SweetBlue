package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.content.Context;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Lambda;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode;
import static com.idevicesinc.sweetblue.BleManagerState.ON;
import static com.idevicesinc.sweetblue.BleServerState.*;


/**
 * Get an instance from {@link BleManager#getServer()}. Wrapper for functionality exposed by {@link BluetoothGattServer}. For OS levels less than 5.0, this
 * is only useful by piggybacking on an existing {@link BleDevice} that is currently {@link BleDeviceState#CONNECTED}.
 * For OS levels 5.0 and up a {@link BleServer} is capable of acting as an independent peripheral.
 */
public final class BleServer extends BleNode
{
	/**
	 * Special value that is used in place of Java's built-in <code>null</code>.
	 */
	@Immutable
	public static final BleServer NULL = new BleServer(null, /*isNull=*/true);

	/**
	 * Tagging interface, not to be implemented directly as this is just the base interface to statically tie together
	 * {@link IncomingListener} and {@link OutgoingListener} with common enums/structures.
	 */
	@Lambda
	public static interface ExchangeListener
	{
		/**
		 * The type of GATT object, provided by {@link ExchangeEvent#target()}.
		 */
		public static enum Target
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
		public static enum Type
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
		 * Like {@link BleServer.ExchangeListener}, this class should not be used directly as this is just a base class to statically tie together
		 * {@link IncomingListener.IncomingEvent} and {@link OutgoingListener.OutgoingEvent} with a common API.
		 */
		@Immutable
		public abstract static class ExchangeEvent extends Event
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

	/**
	 * Provide an instance through {@link BleServer#setListener_Incoming(IncomingListener)}.
	 * The return value of {@link IncomingListener#onEvent(IncomingEvent)} is used to decide if/how to respond to a given {@link IncomingEvent}.
	 */
	@Lambda
	public static interface IncomingListener extends ExchangeListener
	{
		/**
		 * Struct passed to {@link BleServer.IncomingListener#onEvent(BleServer.IncomingListener.IncomingEvent)}} that provides details about the client and what it wants from us, the server.
		 */
		@Immutable
		public static class IncomingEvent extends ExchangeEvent
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
		public static class Please
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
				m_outgoingListener = outgoingListener != null ? outgoingListener : NULL_OUTGOING_LISTENER;
			}

			private Please(final OutgoingListener outgoingListener)
			{
				m_respond = false;

				m_gattStatus = 0;
				m_offset = 0;
				m_futureData = P_Const.EMPTY_FUTURE_DATA;
				m_outgoingListener = outgoingListener != null ? outgoingListener : NULL_OUTGOING_LISTENER;
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
			 * Your {@link BleServer.OutgoingListener#onEvent(BleServer.OutgoingListener.OutgoingEvent)} will simply be called
			 * with {@link BleServer.OutgoingListener.Status#NO_RESPONSE_ATTEMPTED}.
			 *
			 * @see BleServer#setListener_Outgoing(BleServer.OutgoingListener)
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
			 * Overload of {@link #respondWithSuccess(byte[], BleServer.OutgoingListener)} - see {@link FutureData} for why/when you would want to use this.
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

	/**
	 * Provide an instance to various static methods of {@link IncomingListener.Please} such as
	 * {@link BleServer.IncomingListener.Please#respondWithSuccess(BleServer.OutgoingListener)}, or {@link BleServer#setListener_Outgoing(OutgoingListener)},
	 * or {@link BleManager#setListener_Outgoing(BleServer.OutgoingListener)}. Also used to callback the success or failure of
	 * notifications through {@link BleServer#sendNotification(String, UUID, UUID, FutureData, OutgoingListener)},
	 * {@link BleServer#sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}, or various overloads thereof.
	 */
	public static interface OutgoingListener extends ExchangeListener
	{
		/**
		 * Struct passed to {@link BleServer.OutgoingListener#onEvent(BleServer.OutgoingListener.OutgoingEvent)}
		 * that provides details of what was sent to the client and if it succeeded.
		 */
		@Immutable
		public static class OutgoingEvent extends ExchangeEvent implements UsesCustomNull
		{
			/**
			 * Returns the result of the response, or {@link BleServer.OutgoingListener.Status#NO_RESPONSE_ATTEMPTED} if
			 * for example {@link BleServer.IncomingListener.Please#doNotRespond(BleServer.OutgoingListener)} was used.
			 */
			public final Status status()  {  return m_status;  }
			private final Status m_status;

			/**
			 * The data that was attempted to be sent back to the client if {@link #type()} {@link Type#isRead()} is <code>true</code>.
			 */
			public final byte[] data_sent()  {  return m_data_sent;  }
			private final byte[] m_data_sent;

			/**
			 * The gattStatus sent to the client, provided to static methods of {@link BleServer.IncomingListener.Please}
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
			 * Another theoretical case is if you make an explicit call through SweetBlue, then you get {@link com.idevicesinc.sweetblue.BleServer.OutgoingListener.Status#TIMED_OUT},
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
		public static enum Status implements UsesCustomNull
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
			 * {@link BleServer.IncomingListener.Please#doNotRespond(BleServer.OutgoingListener)} (or overloads)
			 * were called or {@link BleServer.IncomingListener.IncomingEvent#responseNeeded()} was <code>false</code>.
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

	/**
	 * Provide an implementation to {@link BleServer#setListener_Advertising(BleServer.AdvertisingListener)}, and
	 * {@link BleManager#setListener_Advertising(BleServer.AdvertisingListener)} to receive a callback
	 * when using {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public static interface AdvertisingListener
	{

		/**
		 * Enumeration describing the m_status of calling {@link #startAdvertising(BleAdvertisingPacket)}.
		 */
		public static enum Status implements UsesCustomNull
		{
			SUCCESS(BleStatuses.ADVERTISE_SUCCESS),
			DATA_TOO_LARGE(AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE),
			TOO_MANY_ADVERTISERS(AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS),
			ALREADY_STARTED(AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED),
			INTERNAL_ERROR(AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR),
			ANDROID_VERSION_NOT_SUPPORTED(BleStatuses.ADVERTISE_ANDROID_VERSION_NOT_SUPPORTED),
			CHIPSET_NOT_SUPPORTED(AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED),
			BLE_NOT_ON(-1),
			NULL_SERVER(-2),
			NULL(-3);

			private final int m_nativeStatus;

			Status(int nativeStatus)
			{
				m_nativeStatus = nativeStatus;
			}

			public final int getNativeStatus()
			{
				return m_nativeStatus;
			}

			public static Status fromNativeStatus(int bit)
			{
				for (Status res : values())
				{
					if (res.m_nativeStatus == bit)
					{
						return res;
					}
				}
				return SUCCESS;
			}

			@Override
			public final boolean isNull() {
				return this == NULL;
			}
		}

		/**
		 * Sub class representing the Advertising Event
		 */
		public static class AdvertisingEvent extends Event implements UsesCustomNull
		{
			private final BleServer m_server;
			private final Status m_status;

			AdvertisingEvent(BleServer server, Status m_status)
			{
				m_server = server;
				this.m_status = m_status;
			}

			/**
			 * The backing {@link BleManager} which is attempting to start advertising.
			 */
			public final BleServer server()
			{
				return m_server;
			}

			/**
			 * Whether or not {@link #startAdvertising(BleAdvertisingPacket)} was successful or not. If false,
			 * then call {@link #m_status} to get the error code.
			 */
			public final boolean wasSuccess()
			{
				return m_status == Status.SUCCESS;
			}

			/**
			 * Returns {@link Status} describing
			 * the m_status of calling {@link #startAdvertising(BleAdvertisingPacket)}
			 */
			public final Status status()
			{
				return m_status;
			}

			@Override
			public final boolean isNull() {
				return status() == Status.NULL;
			}

			@Override
			public final String toString() {
				return Utils_String.toString(this.getClass(),
						"server", server().getClass().getSimpleName(),
						"status", status());
			}
		}

		/**
		 * Called upon the m_status of calling {@link #startAdvertising(BleAdvertisingPacket)}
		 */
		void onEvent(AdvertisingEvent e);

	}

	/**
	 * Provide an implementation to {@link BleServer#setListener_State(StateListener)} and/or
	 * {@link BleManager#setListener_ServerState(BleServer.StateListener)} to receive state change events.
	 *
	 * @see BleServerState
	 * @see BleServer#setListener_State(StateListener)
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface StateListener
	{
		/**
		 * Subclass that adds the {@link #server()}, {@link #macAddress()}, and {@link #gattStatus()} fields.
		 */
		@Immutable
		public static class StateEvent extends State.ChangeEvent<BleServerState>
		{
			/**
			 * The server undergoing the state change.
			 */
			public final BleServer server() {  return m_server;  }
			private final BleServer m_server;

			/**
			 * Returns the mac address of the client that's undergoing the state change with this {@link #server()}.
			 */
			public final String macAddress()  {  return m_macAddress;  }
			private final String m_macAddress;

			/**
			 * The change in gattStatus that may have precipitated the state change, or {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
			 * For example if {@link #didEnter(State)} with {@link BleServerState#DISCONNECTED} is <code>true</code> and
			 * {@link #didExit(State)} with {@link BleServerState#CONNECTING} is also <code>true</code> then {@link #gattStatus()} may be greater
			 * than zero and give some further hint as to why the connection failed.
			 */
			public final int gattStatus() {  return m_gattStatus;  }
			private final int m_gattStatus;

			/*package*/ StateEvent(BleServer server, String macAddress, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
			{
				super(oldStateBits, newStateBits, intentMask);

				m_server = server;
				m_gattStatus = gattStatus;
				m_macAddress = macAddress;
			}

			@Override public final String toString()
			{
				return Utils_String.toString
				(
					this.getClass(),
//					"server",			server().getName_debug(),
					"entered",			Utils_String.toString(enterMask(),						BleServerState.VALUES()),
					"exited", 			Utils_String.toString(exitMask(),						BleServerState.VALUES()),
					"current",			Utils_String.toString(newStateBits(),					BleServerState.VALUES()),
					"gattStatus",		server().getManager().getLogger().gattStatus(gattStatus())
				);
			}
		}

		/**
		 * Called when a server's bitwise {@link BleServerState} changes. As many bits as possible are flipped at the same time.
		 */
		void onEvent(final StateEvent e);
	}

	/**
	 * Provide an implementation of this callback to {@link BleServer#setListener_ConnectionFail(ConnectionFailListener)}.
	 *
	 * @see BleServer#setListener_ConnectionFail(ConnectionFailListener)
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ConnectionFailListener extends BleNode.ConnectionFailListener
	{
		/**
		 * The reason for the connection failure.
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Used in place of Java's built-in <code>null</code> wherever needed. As of now, the {@link ConnectionFailEvent#status()} given
			 * to {@link ConnectionFailListener#onEvent(ConnectionFailEvent)} will *never* be {@link ConnectionFailListener.Status#NULL}.
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
			 * Whether this reason honors a {@link BleNode.ConnectionFailListener.Please#isRetry()}. Returns <code>false</code> if {@link #wasCancelled()} or
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
		 * Structure passed to {@link ConnectionFailListener#onEvent(ConnectionFailEvent)} to provide more info about how/why the connection failed.
		 */
		@Immutable
		public static class ConnectionFailEvent extends BleNode.ConnectionFailListener.ConnectionFailEvent implements UsesCustomNull
		{
			/**
			 * The {@link BleServer} this {@link ConnectionFailEvent} is for.
			 */
			public final BleServer server() {  return m_server;  }
			private final BleServer m_server;

			/**
			 * The native {@link BluetoothDevice} client this {@link ConnectionFailEvent} is for.
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
			 * Returns a chronologically-ordered list of all {@link ConnectionFailEvent} instances returned through
			 * {@link ConnectionFailListener#onEvent(ConnectionFailEvent)} since the first call to {@link BleDevice#connect()},
			 * including the current instance. Thus this list will always have at least a length of one (except if {@link #isNull()} is <code>true</code>).
			 * The list length is "reset" back to one whenever a {@link BleDeviceState#CONNECTING_OVERALL} operation completes, either
			 * through becoming {@link BleDeviceState#INITIALIZED}, or {@link BleDeviceState#DISCONNECTED} for good.
			 */
			public final ConnectionFailEvent[] history()  {  return m_history;  }
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
				return new ConnectionFailListener.ConnectionFailEvent(server, nativeDevice, status, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, AutoConnectUsage.NOT_APPLICABLE, null);
			}

			/**
			 * Returns whether this {@link ConnectionFailEvent} instance is a "dummy" value. For now used for
			 * {@link BleNodeConfig.ReconnectFilter.ReconnectEvent#connectionFailEvent()} in certain situations.
			 */
			@Override public final boolean isNull()
			{
				return status().isNull();
			}

			/**
			 * Forwards {@link BleDevice.ConnectionFailListener.Status#shouldBeReportedToUser()}
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

		Please onEvent(final ConnectionFailEvent e);
	}

	/**
	 * Default implementation of {@link ConnectionFailListener} that attempts a certain number of retries. An instance of this class is set by default
	 * for all new {@link BleServer} instances using {@link BleServer.DefaultConnectionFailListener#DEFAULT_CONNECTION_FAIL_RETRY_COUNT}.
	 * Use {@link BleServer#setListener_ConnectionFail(ConnectionFailListener)} to override the default behavior.
	 *
	 * @see ConnectionFailListener
	 * @see BleServer#setListener_ConnectionFail(ConnectionFailListener)
	 */
	@Immutable
	public static class DefaultConnectionFailListener implements ConnectionFailListener
	{
		/**
		 * The default retry count provided to {@link DefaultConnectionFailListener}.
		 * So if you were to call {@link BleDevice#connect()} and all connections failed, in total the
		 * library would try to connect {@value #DEFAULT_CONNECTION_FAIL_RETRY_COUNT}+1 times.
		 *
		 * @see DefaultConnectionFailListener
		 */
		public static final int DEFAULT_CONNECTION_FAIL_RETRY_COUNT = 2;

		/**
		 * The default connection fail limit past which {@link DefaultConnectionFailListener} will start returning {@link BleNode.ConnectionFailListener.Please#retryWithAutoConnectTrue()}.
		 */
		public static final int DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT = 2;

		/**
		 * The maximum amount of time to keep trying if connection is failing due to (what usually are) transient bonding failures
		 */
		public static final Interval MAX_RETRY_TIME_FOR_BOND_FAILURE = Interval.secs(120.0);

		private final int m_retryCount;
		private final int m_failCountBeforeUsingAutoConnect;

		public DefaultConnectionFailListener()
		{
			this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT);
		}

		public DefaultConnectionFailListener(int retryCount, int failCountBeforeUsingAutoConnect)
		{
			m_retryCount = retryCount;
			m_failCountBeforeUsingAutoConnect = failCountBeforeUsingAutoConnect;
		}

		public final int getRetryCount()
		{
			return m_retryCount;
		}

		@Override public final Please onEvent(ConnectionFailEvent e)
		{
			//--- DRK > Not necessary to check this ourselves, just being explicit.
			if (!e.status().allowsRetry() )
			{
				return Please.doNotRetry();
			}
			else if (e.failureCountSoFar() <= m_retryCount)
			{
				return Please.retry();
			}
			else
			{
				return Please.doNotRetry();
			}
		}
	}

	/**
	 * Provide an implementation of this callback to {@link BleServer#setListener_ServiceAdd(ServiceAddListener)}.
	 *
	 * @see BleServer#setListener_ServiceAdd(ServiceAddListener)
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ServiceAddListener
	{
		/**
		 * Enumeration of the different ways that service addition can fail (and one way for it to succeed),
		 * provided through {@link OutgoingListener.OutgoingEvent#status()}.
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Fulfills the soft contract of {@link UsesCustomNull}.
			 */
			NULL,

			/**
			 * Service was added successfully.
			 */
			SUCCESS,

			/**
			 * Tried to add a service to {@link BleServer#NULL}.
			 */
			NULL_SERVER,

			/**
			 * Tried to add the same service reference twice.
			 */
			DUPLICATE_SERVICE,

			/**
			 * Adding this service required that a native {@link BluetoothGattServer} to be created,
			 * but it could not be created for some reason.
			 */
			SERVER_OPENING_FAILED,

			/**
			 * The call to {@link BluetoothGattServer#addService(BluetoothGattService)} returned <code>false</code>.
			 */
			FAILED_IMMEDIATELY,

			/**
			 * {@link BluetoothGattServerCallback#onServiceAdded(int, BluetoothGattService)} reported a bad gatt status
			 * for the service addition, which is provided through {@link OutgoingListener.OutgoingEvent#gattStatus_received()}.
			 */
			FAILED_EVENTUALLY,

			/**
			 * Couldn't add the service because the operation took longer than the time dictated by {@link BleNodeConfig#taskTimeoutRequestFilter}.
			 */
			TIMED_OUT,

			/**
			 * {@link #removeService(UUID)} or {@link #removeAllServices()} was called before the service could be fully added.
			 */
			CANCELLED_FROM_REMOVAL,

			/**
			 * The operation was cancelled because {@link BleServer#disconnect()} was called before the operation completed.
			 */
			CANCELLED_FROM_DISCONNECT,

			/**
			 * The operation was cancelled because {@link BleManager} went {@link BleManagerState#TURNING_OFF} and/or
			 * {@link BleManagerState#OFF}. Note that if the user turns off BLE from their OS settings (airplane mode, etc.) then
			 * {@link ServiceAddEvent#status()} could potentially be {@link #CANCELLED_FROM_DISCONNECT} because SweetBlue might get
			 * the disconnect callback before the turning off callback. Basic testing has revealed that this is *not* the case, but you never know.
			 * <br><br>
			 * Either way, the device was or will be disconnected.
			 */
			CANCELLED_FROM_BLE_TURNING_OFF,

			/**
			 * {@link BleManager} is not {@link BleManagerState#ON} so we can't add a service.
			 */
			BLE_NOT_ON;

			/**
			 * Returns true if <code>this</code> equals {@link #SUCCESS}.
			 */
			public final boolean wasSuccess()
			{
				return this == Status.SUCCESS;
			}

			@Override public final boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Event struct passed to {@link #onEvent(ServiceAddEvent)} to give you information about the success
		 * of a service addition or the reason(s) for its failure.
		 */
		@Immutable
		public static class ServiceAddEvent extends Event
		{
			/**
			 * The server to which the service is being added.
			 */
			public final BleServer server()  {  return m_server;  }
			private final BleServer m_server;

			/**
			 * The service being added to {@link #server()}.
			 */
			public final BluetoothGattService service()  {  return m_service;  }
			private final BluetoothGattService m_service;

			/**
			 * Convenience to return the {@link UUID} of {@link #service()}.
			 */
			public final UUID serviceUuid() {  return service().getUuid();  }

			/**
			 * Should only be relevant if {@link #status()} is {@link BleServer.ServiceAddListener.Status#FAILED_EVENTUALLY}.
			 */
			public final int gattStatus()  {  return m_gattStatus;  }
			private final int m_gattStatus;

			/**
			 * Indicates the success or reason for failure for adding the service.
			 */
			public final Status status()  {  return m_status;  }
			private final Status m_status;

			/**
			 * This returns <code>true</code> if this event was the result of an explicit call through SweetBlue, e.g. through
			 * {@link BleServer#addService(BleService, ServiceAddListener)}. It will return <code>false</code> otherwise,
			 * which can happen if for example you use {@link BleServer#getNativeLayer()} to bypass SweetBlue for whatever reason.
			 * Another theoretical case is if you make an explicit call through SweetBlue, then you get {@link com.idevicesinc.sweetblue.BleServer.ServiceAddListener.Status#TIMED_OUT},
			 * but then the native stack eventually *does* come back with something - this has never been observed, but it is possible.
			 */
			public final boolean solicited()  {  return m_solicited;  }
			private final boolean m_solicited;

			/*package*/ ServiceAddEvent(final BleServer server, final BluetoothGattService service, final Status status, final int gattStatus, final boolean solicited)
			{
				m_server = server;
				m_service = service;
				m_status = status;
				m_gattStatus = gattStatus;
				m_solicited = solicited;
			}

			/**
			 * Convenience forwarding of {@link BleServer.ServiceAddListener.Status#wasSuccess()}.
			 */
			public final boolean wasSuccess()
			{
				return status().wasSuccess();
			}

			/*package*/ static ServiceAddEvent NULL(BleServer server, BluetoothGattService service)
			{
				return EARLY_OUT(server, service, Status.NULL);
			}

			/*package*/ static ServiceAddEvent EARLY_OUT(BleServer server, BluetoothGattService service, Status status)
			{
				return new ServiceAddEvent(server, service, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
			}

			public final String toString()
			{
				return Utils_String.toString
				(
					this.getClass(),
					"status",			status(),
					"service",			server().getManager().getLogger().serviceName(service().getUuid()),
					"gattStatus",		server().getManager().getLogger().gattStatus(gattStatus())
				);
			}
		}

		/**
		 * Called when a service has finished being added or failed to be added.
		 */
		void onEvent(final ServiceAddEvent e);
	}

	private static final OutgoingListener NULL_OUTGOING_LISTENER = new OutgoingListener()
	{
		@Override public void onEvent(OutgoingEvent e)
		{
		}
	};

	private final P_ServerStateTracker m_stateTracker;
	final P_BleServer_Listeners m_listeners;
	final P_NativeServerWrapper m_nativeWrapper;
	private AdvertisingListener m_advertisingListener;
	private IncomingListener m_incomingListener;
	private OutgoingListener m_outgoingListener_default;
	private final boolean m_isNull;
	private BleNodeConfig m_config = null;
	private final P_ServerConnectionFailManager m_connectionFailMngr;
	private final P_ClientManager m_clientMngr;

	/*package*/ BleServer(final BleManager mngr, final boolean isNull)
	{
		super(mngr);

		m_isNull = isNull;

		if( isNull )
		{
			m_stateTracker = new P_ServerStateTracker(this);
			m_listeners = null;
			m_nativeWrapper = new P_NativeServerWrapper(this);
			m_connectionFailMngr = new P_ServerConnectionFailManager(this);
			m_clientMngr = new P_ClientManager(this);
		}
		else
		{
			m_stateTracker = new P_ServerStateTracker(this);
			m_listeners = new P_BleServer_Listeners(this);
			m_nativeWrapper = new P_NativeServerWrapper(this);
			m_connectionFailMngr = new P_ServerConnectionFailManager(this);
			m_clientMngr = new P_ClientManager(this);
		}
	}

	@Override protected final PA_ServiceManager newServiceManager()
	{
		return new P_ServerServiceManager(this);
	}

	/**
	 * Optionally sets overrides for any custom options given to {@link BleManager#get(android.content.Context, BleManagerConfig)}
	 * for this individual server.
	 */
	public final void setConfig(final BleNodeConfig config_nullable)
	{
		m_config = config_nullable == null ? null : config_nullable.clone();
	}

	@Override /*package*/ final BleNodeConfig conf_node()
	{
		return m_config != null ? m_config : conf_mngr();
	}
	
	/**
	 * Set a listener here to be notified whenever this server's state changes in relation to a specific client.
	 */
	public final void setListener_State(@Nullable(Nullable.Prevalence.NORMAL) final BleServer.StateListener listener_nullable)
	{
		m_stateTracker.setListener(listener_nullable);
	}

	/**
	 * Set a listener here to override any listener provided previously.
	 */
	public final void setListener_Incoming(@Nullable(Nullable.Prevalence.NORMAL) final IncomingListener listener_nullable)
	{
		m_incomingListener = listener_nullable;
	}

	/**
	 * Set a listener here to override any listener provided previously and provide a default backup that will be called
	 * after any listener provided to {@link #addService(BleService, ServiceAddListener)}.
	 */
	public final void setListener_ServiceAdd(@Nullable(Nullable.Prevalence.NORMAL) final ServiceAddListener listener_nullable)
	{
		serviceMngr_server().setListener(listener_nullable);
	}

	public final void setListener_Advertising(@Nullable(Nullable.Prevalence.NORMAL) final AdvertisingListener listener_nullable)
	{
		m_advertisingListener = listener_nullable;
	}

	public final @Nullable(Nullable.Prevalence.RARE)
	AdvertisingListener getListener_Advertise()
	{
		return m_advertisingListener;
	}

	/**
	 * Returns the listener provided to {@link #setListener_Incoming(IncomingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.RARE) IncomingListener getListener_Incoming()
	{
		return m_incomingListener;
	}

	/**
	 * This is a default catch-all convenience listener that will be called after any listener provided through
	 * the static methods of {@link BleServer.IncomingListener.Please} such as {@link BleServer.IncomingListener.Please#respondWithSuccess(BleServer.OutgoingListener)}.
	 *
	 * @see BleManager#setListener_Outgoing(BleServer.OutgoingListener)
	 */
	public final void setListener_Outgoing(final OutgoingListener listener)
	{
		m_outgoingListener_default = listener;
	}

	/**
	 * Set a listener here to override any listener provided previously.
	 */
	public final void setListener_ConnectionFail(final ConnectionFailListener listener)
	{
		m_connectionFailMngr.setListener(listener);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID charUuid, byte[] data)
	{
		return sendIndication(macAddress, null, charUuid, data, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID charUuid, byte[] data, OutgoingListener listener)
	{
		return sendIndication(macAddress, (UUID) null, charUuid, data, listener);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data)
	{
		return sendIndication(macAddress, serviceUuid, charUuid, data, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data, OutgoingListener listener)
	{
		return sendIndication(macAddress, serviceUuid, charUuid, new PresentData(data), listener);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID charUuid, final FutureData futureData)
	{
		return sendIndication(macAddress, null, charUuid, futureData, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return sendIndication(macAddress, (UUID) null, charUuid, futureData, listener);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData)
	{
		return sendIndication(macAddress, serviceUuid, charUuid, futureData, (OutgoingListener) null);
	}

	/**
	 * Same as {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)} but sends an indication instead.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return sendNotification_private(macAddress, serviceUuid, charUuid, futureData, listener, /*isIndication=*/true);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID charUuid, byte[] data)
	{
		return sendNotification(macAddress, null, charUuid, data, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID charUuid, byte[] data, OutgoingListener listener)
	{
		return sendNotification(macAddress, (UUID) null, charUuid, data, listener);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data)
	{
		return sendNotification(macAddress, serviceUuid, charUuid, data, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data, OutgoingListener listener)
	{
		return sendNotification(macAddress, serviceUuid, charUuid, new PresentData(data), listener);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID charUuid, final FutureData futureData)
	{
		return sendNotification(macAddress, null, charUuid, futureData, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return sendNotification(macAddress, (UUID) null, charUuid, futureData, listener);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData)
	{
		return sendNotification(macAddress, serviceUuid, charUuid, futureData, (OutgoingListener) null);
	}

	/**
	 * Use this method to send a notification to the client device with the given mac address to the given characteristic {@link UUID}.
	 * If there is any kind of "early-out" issue then this method will return a {@link OutgoingListener.OutgoingEvent} in addition
	 * to passing it through the listener. Otherwise this method will return an instance with {@link OutgoingListener.OutgoingEvent#isNull()} being
	 * <code>true</code>.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return sendNotification_private(macAddress, serviceUuid, charUuid, futureData, listener, /*isIndication=*/false);
	}

	private OutgoingListener.OutgoingEvent sendNotification_private(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData, final OutgoingListener listener, final boolean isIndication)
	{
		final String macAddress_normalized = getManager().normalizeMacAddress(macAddress);

		final BluetoothDevice nativeDevice = newNativeDevice(macAddress_normalized).getNativeDevice();

		if( isNull() )
		{
			final OutgoingListener.OutgoingEvent e = OutgoingListener.OutgoingEvent.EARLY_OUT__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid, futureData, OutgoingListener.Status.NULL_SERVER);

			invokeOutgoingListeners(e, listener);

			return e;
		}

		if( !is(macAddress_normalized, CONNECTED ) )
		{
			final OutgoingListener.OutgoingEvent e = OutgoingListener.OutgoingEvent.EARLY_OUT__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid, futureData, OutgoingListener.Status.NOT_CONNECTED);

			invokeOutgoingListeners(e, listener);

			return e;
		}

		final BluetoothGattCharacteristic char_native = getNativeCharacteristic(serviceUuid, charUuid);

		if( char_native == null )
		{
			final OutgoingListener.OutgoingEvent e = OutgoingListener.OutgoingEvent.EARLY_OUT__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid, futureData, OutgoingListener.Status.NO_MATCHING_TARGET);

			invokeOutgoingListeners(e, listener);

			return e;
		}

		final boolean confirm = isIndication;
		final P_Task_SendNotification task = new P_Task_SendNotification(this, nativeDevice, serviceUuid, charUuid, futureData, confirm, listener);
		queue().add(task);

		return OutgoingListener.OutgoingEvent.NULL__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid);
	}

	/**
	 * Checks to see if the device is running an Android OS which supports
	 * advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByAndroidVersion()}.
	 */
	public final boolean isAdvertisingSupportedByAndroidVersion()
	{
		return getManager().isAdvertisingSupportedByAndroidVersion();
	}

	/**
	 * Checks to see if the device supports advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByChipset()}.
	 */
	public final boolean isAdvertisingSupportedByChipset()
	{
		return getManager().isAdvertisingSupportedByChipset();
	}

	/**
	 * Checks to see if the device supports advertising BLE services. This is forwarded from {@link BleManager#isAdvertisingSupported()}.
	 */
	public final boolean isAdvertisingSupported()
	{
		return getManager().isAdvertisingSupported();
	}

	/**
	 * Checks to see if the device is currently advertising.
	 */
	public final boolean isAdvertising()
	{
		return getManager().getTaskQueue().isCurrentOrInQueue(P_Task_Advertise.class, getManager());
	}

	/**
	 * Checks to see if the device is currently advertising the given {@link UUID}.
	 */
	public final boolean isAdvertising(UUID serviceUuid)
	{
		if (Utils.isLollipop())
		{
			P_Task_Advertise adtask = getManager().getTaskQueue().get(P_Task_Advertise.class, getManager());
			if (adtask != null)
			{
				return adtask.getPacket().hasUuid(serviceUuid);
			}
		}
		return false;
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, AdvertisingListener listener)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid), listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuids));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, AdvertisingListener listener)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuids), listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, byte[] serviceData)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid, serviceData));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, byte[] serviceData, BleAdvertisingPacket.Option... options)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid, serviceData, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, BleAdvertisingPacket.Option... options)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, BleAdvertisingPacket.Option... options)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuids, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, BleAdvertisingSettings, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid), settings, listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, BleAdvertisingSettings, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuids), settings, listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, BleAdvertisingSettings, AdvertisingListener)}. This sets
	 * the {@link BleAdvertisingMode} to {@link BleAdvertisingMode#AUTO}, and {@link BleTransmissionPower} to {@link BleTransmissionPower#MEDIUM}, and
	 * no timeout for the advertisement.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleAdvertisingPacket advPacket)
	{
		return startAdvertising(advPacket, null);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, BleAdvertisingSettings, AdvertisingListener)}. This sets
	 * the {@link BleAdvertisingMode} to {@link BleAdvertisingMode#AUTO}, and {@link BleTransmissionPower} to {@link BleTransmissionPower#MEDIUM}, and
	 * no timeout for the advertisement.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleAdvertisingPacket advPacket, AdvertisingListener listener)
	{
		return startAdvertising(advPacket, new BleAdvertisingSettings(BleAdvertisingMode.AUTO, BleTransmissionPower.MEDIUM, Interval.ZERO), listener);
	}

	/**
	 * Starts advertising serviceUuids with the information supplied in {@link BleAdvertisingPacket}. Note that this will
	 * only work for devices on Lollipop, or above. Even then, not every device supports advertising. Use
	 * {@link BleManager#isAdvertisingSupported()} to check to see if the phone supports it.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleAdvertisingPacket advertisePacket, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		if (isNull())
		{
			getManager().getLogger().e(BleServer.class.getSimpleName() + " is null!");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.NULL_SERVER);
		}

		if (!isAdvertisingSupportedByAndroidVersion())
		{
			getManager().getLogger().e("Advertising NOT supported on android OS's less than Lollipop!");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.ANDROID_VERSION_NOT_SUPPORTED);
		}

		if (!isAdvertisingSupportedByChipset())
		{
			getManager().getLogger().e("Advertising NOT supported by current device's chipset!");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.CHIPSET_NOT_SUPPORTED);
		}

		if (!getManager().is(BleManagerState.ON))
		{
			getManager().getLogger().e(BleManager.class.getSimpleName() + " is not " + ON + "! Please use the turnOn() method first.");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.BLE_NOT_ON);
		}

		final P_Task_Advertise adTask = getManager().getTaskQueue().get(P_Task_Advertise.class, getManager());
		if (adTask != null)
		{
			getManager().getLogger().w(BleServer.class.getSimpleName() + " is already advertising!");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.ALREADY_STARTED);
		}
		else
		{
			getManager().ASSERT(!getManager().getTaskQueue().isCurrentOrInQueue(P_Task_Advertise.class, getManager()));

			getManager().getTaskQueue().add(new P_Task_Advertise(this, advertisePacket, settings, listener));
			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.NULL);
		}
	}

	/**
	 * Stops the server from advertising.
	 */
	public final void stopAdvertising()
	{
		if (Utils.isLollipop())
		{

			final P_Task_Advertise adTask = getManager().getTaskQueue().get(P_Task_Advertise.class, getManager());
			if (adTask != null)
			{
				adTask.stopAdvertising();
				adTask.clearFromQueue();
			}
			getManager().ASSERT(!getManager().getTaskQueue().isCurrentOrInQueue(P_Task_Advertise.class, getManager()));
		}
	}

	/**
	 * Returns the name this {@link BleServer} is using (and will be advertised as, if applicable).
	 */
	public final String getName()
	{
		return getManager().managerLayer().getName();
	}

	/**
	 * Set the name you wish this {@link BleServer} to be known as. This will affect how other devices see this server, and sets the name
	 * on the lower level {@link BluetoothAdapter}. If you DO change this, please be aware this will affect everything, including apps outside
	 * of your own. It's probably best NOT to use this, but it's here for flexibility.
	 */
	@Advanced
	public final void setName(String name)
	{
		getManager().managerLayer().setName(name);
	}

    /**
     * Provides just-in-case lower-level access to the native server instance.
     * See similar warning for {@link BleDevice#getNative()}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.RARE) BluetoothGattServer getNative()
    {
        return m_nativeWrapper.getNative().getNativeServer();
    }

	/**
	 * Provides just-in-case access to the abstracted server instance.
	 * See similar warning for {@link BleDevice#getNative()}.
	 */
	@Advanced
	public final @Nullable(Nullable.Prevalence.RARE) P_NativeServerLayer getNativeLayer()
	{
		return m_nativeWrapper.getNative();
	}

	/**
	 * Returns the bitwise state mask representation of {@link BleServerState} for the given client mac address.
	 *
	 * @see BleServerState
	 */
	@Advanced
	public final int getStateMask(final String macAddress)
	{
		final String macAddress_normalized = getManager().normalizeMacAddress(macAddress);

		return m_stateTracker.getStateMask(macAddress_normalized);
	}

	/**
	 * Returns <code>true</code> if there is any bitwise overlap between the provided value and {@link #getStateMask(String)}.
	 *
	 * @see #isAll(String, int)
	 */
	public final boolean isAny(final String macAddress, final int mask_BleServerState)
	{
		return (getStateMask(macAddress) & mask_BleServerState) != 0x0;
	}

	/**
	 * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask(String)}.
	 *
	 * @see #isAny(String, int)
	 *
	 */
	public final boolean isAll(final String macAddress, final int mask_BleServerState)
	{
		return (getStateMask(macAddress) & mask_BleServerState) == mask_BleServerState;
	}

	/**
	 * Returns true if the given client is in the state provided.
	 */
	public final boolean is(final String macAddress, final BleServerState state)
	{
		return state.overlaps(getStateMask(macAddress));
	}

	/**
	 * Returns true if the given client is in any of the states provided.
	 */
	public final boolean isAny(final String macAddress, final BleServerState ... states )
	{
		final int stateMask = getStateMask(macAddress);

		for( int i = 0; i < states.length; i++ )
		{
			if( states[i].overlaps(stateMask) )  return true;
		}

		return false;
	}

	/**
	 * Overload of {@link #connect(String, StateListener, ConnectionFailListener)} with no listeners.
	 */
	public final ConnectionFailListener.ConnectionFailEvent connect(final String macAddress)
	{
		return connect(macAddress, null, null);
	}

	/**
	 * Overload of {@link #connect(String, StateListener, ConnectionFailListener)} with only one listener.
	 */
	public final ConnectionFailListener.ConnectionFailEvent connect(final String macAddress, final StateListener stateListener)
	{
		return connect(macAddress, stateListener, null);
	}

	/**
	 * Overload of {@link #connect(String, StateListener, ConnectionFailListener)} with only one listener.
	 */
	public final ConnectionFailListener.ConnectionFailEvent connect(final String macAddress, final ConnectionFailListener connectionFailListener)
	{
		return connect(macAddress, null, connectionFailListener);
	}

	/**
	 * Connect to the given client mac address and provided listeners that are shorthand for calling {@link #setListener_State(StateListener)}
	 * {@link #setListener_ConnectionFail(ConnectionFailListener)}.
	 */
	public final ConnectionFailListener.ConnectionFailEvent connect(final String macAddress, final StateListener stateListener, final ConnectionFailListener connectionFailListener)
	{
		final String macAddress_normalized = getManager().normalizeMacAddress(macAddress);

		return connect_internal(newNativeDevice(macAddress_normalized).getNativeDevice(), stateListener, connectionFailListener);
	}

	/*package*/ final ConnectionFailListener.ConnectionFailEvent connect_internal(final BluetoothDevice nativeDevice)
	{
		return connect_internal(nativeDevice, null, null);
	}

	/*package*/ final ConnectionFailListener.ConnectionFailEvent connect_internal(final BluetoothDevice nativeDevice, final StateListener stateListener, final ConnectionFailListener connectionFailListener)
	{
		m_nativeWrapper.clearImplicitDisconnectIgnoring(nativeDevice.getAddress());

		if( stateListener != null )
		{
			setListener_State(stateListener);
		}

		if( connectionFailListener != null )
		{
			setListener_ConnectionFail(connectionFailListener);
		}

		if( isNull() )
		{
			final ConnectionFailListener.ConnectionFailEvent e = ConnectionFailListener.ConnectionFailEvent.EARLY_OUT(this, nativeDevice, ConnectionFailListener.Status.NULL_SERVER);

			m_connectionFailMngr.invokeCallback(e);

			return e;
		}

		m_connectionFailMngr.onExplicitConnectionStarted(nativeDevice.getAddress());

		if( isAny(nativeDevice.getAddress(), CONNECTING, CONNECTED) )
		{
			final ConnectionFailListener.ConnectionFailEvent e = ConnectionFailListener.ConnectionFailEvent.EARLY_OUT(this, nativeDevice, ConnectionFailListener.Status.ALREADY_CONNECTING_OR_CONNECTED);

			m_connectionFailMngr.invokeCallback(e);

			return e;
		}

		m_clientMngr.onConnecting(nativeDevice.getAddress());

		final P_Task_ConnectServer task = new P_Task_ConnectServer(this, nativeDevice, m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
		queue().add(task);

		m_stateTracker.doStateTransition(nativeDevice.getAddress(), BleServerState.DISCONNECTED /* ==> */, BleServerState.CONNECTING, ChangeIntent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		return ConnectionFailListener.ConnectionFailEvent.NULL(this, nativeDevice);
	}

	private P_NativeDeviceLayer newNativeDevice(final String macAddress)
	{
		final BleManager mngr = getManager();

		return mngr == null ? null : mngr.newNativeDevice(macAddress);
	}

	public final boolean disconnect(final String macAddress)
	{
		final String macAddress_normalized = getManager().normalizeMacAddress(macAddress);

		return disconnect_private(macAddress_normalized, ConnectionFailListener.Status.CANCELLED_FROM_DISCONNECT, ChangeIntent.INTENTIONAL);
	}

	private boolean disconnect_private(final String macAddress, final ConnectionFailListener.Status status_connectionFail, final ChangeIntent intent)
	{
		final boolean addTask = true;

		m_connectionFailMngr.onExplicitDisconnect(macAddress);

		if( is(macAddress, DISCONNECTED) )  return false;

		final BleServerState oldConnectionState = m_stateTracker.getOldConnectionState(macAddress);

		final P_NativeDeviceLayer nativeDevice = newNativeDevice(macAddress);

		if( addTask )
		{
			//--- DRK > Purposely doing explicit=true here without regarding the intent.
			final boolean explicit = true;
			final P_Task_DisconnectServer task = new P_Task_DisconnectServer(this, nativeDevice.getNativeDevice(), m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
			queue().add(task);
		}

		m_stateTracker.doStateTransition(macAddress, oldConnectionState /* ==> */, BleServerState.DISCONNECTED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		if( oldConnectionState == CONNECTING )
		{
			m_connectionFailMngr.onNativeConnectFail(nativeDevice.getNativeDevice(), status_connectionFail, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}

		return true;
	}

	/*package*/ final void disconnect_internal(final ServiceAddListener.Status status_serviceAdd, final ConnectionFailListener.Status status_connectionFail, final ChangeIntent intent)
	{
		stopAdvertising();

		getClients(new ForEach_Void<String>()
		{
			@Override public void next(final String next)
			{
				disconnect_private(next, status_connectionFail, intent);

				m_nativeWrapper.ignoreNextImplicitDisconnect(next);
			}

		}, CONNECTING, CONNECTED);

		m_nativeWrapper.closeServer();

		serviceMngr_server().removeAll(status_serviceAdd);
	}

	/**
	 * Disconnects this server completely, disconnecting all connected clients and shutting things down.
	 * To disconnect individual clients use {@link #disconnect(String)}.
	 */
	public final void disconnect()
	{
		disconnect_internal(ServiceAddListener.Status.CANCELLED_FROM_DISCONNECT, ConnectionFailListener.Status.CANCELLED_FROM_DISCONNECT, ChangeIntent.INTENTIONAL);
	}

	@Override public final boolean isNull()
	{
		return m_isNull;
	}

	/*package*/ final void onNativeConnecting_implicit(final String macAddress)
	{
		m_clientMngr.onConnecting(macAddress);

		m_stateTracker.doStateTransition(macAddress, BleServerState.DISCONNECTED /* ==> */, BleServerState.CONNECTING, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	/*package*/ final void onNativeConnect(final String macAddress, final boolean explicit)
	{
		m_clientMngr.onConnected(macAddress);

		final ChangeIntent intent = explicit ? ChangeIntent.INTENTIONAL : ChangeIntent.UNINTENTIONAL;

		//--- DRK > Testing and source code inspection reveals that it's impossible for the native stack to report server->client CONNECTING.
		//---		In other words for both implicit and explicit connects it always jumps from DISCONNECTED to CONNECTED.
		//---		For explicit connects through SweetBlue we can thus fake the CONNECTING state cause we know a task was in the queue, etc.
		//---		For implicit connects the decision is made here to reflect what happens in the native stack, cause as far as SweetBlue
		//---		is concerned we were never in the CONNECTING state either.
		final BleServerState previousState = explicit ? BleServerState.CONNECTING : BleServerState.DISCONNECTED;

		m_stateTracker.doStateTransition(macAddress, previousState /* ==> */, BleServerState.CONNECTED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	/*package*/ final void onNativeConnectFail(final BluetoothDevice nativeDevice, final ConnectionFailListener.Status status, final int gattStatus)
	{
		if( status == ConnectionFailListener.Status.TIMED_OUT )
		{
			final P_Task_DisconnectServer task = new P_Task_DisconnectServer(this, nativeDevice, m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
			queue().add(task);
		}

		m_stateTracker.doStateTransition(nativeDevice.getAddress(), BleServerState.CONNECTING /* ==> */, BleServerState.DISCONNECTED, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		m_connectionFailMngr.onNativeConnectFail(nativeDevice, status, gattStatus);
	}

	/*package*/ final void onNativeDisconnect( final String macAddress, final boolean explicit, final int gattStatus)
	{
		final boolean ignore = m_nativeWrapper.shouldIgnoreImplicitDisconnect(macAddress);

		if( explicit == false && ignore == false )
		{
			m_stateTracker.doStateTransition(macAddress, BleServerState.CONNECTED /* ==> */, BleServerState.DISCONNECTED, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else
		{
			// explicit case gets handled immediately by the disconnect method.
		}
	}

	final void invokeAdvertiseListeners(AdvertisingListener.Status result, AdvertisingListener listener)
	{
		final AdvertisingListener.AdvertisingEvent event = new AdvertisingListener.AdvertisingEvent(this, result);
		if (listener != null)
		{
			listener.onEvent(event);
		}
		if (m_advertisingListener != null)
		{
			m_advertisingListener.onEvent(event);
		}
		if (getManager().m_advertisingListener != null)
		{
			getManager().m_advertisingListener.onEvent(event);
		}
	}

	/**
	 * Does a referential equality check on the two servers.
	 */
	public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final BleServer server_nullable)
	{
		if (server_nullable == null)												return false;
		if (server_nullable == this)												return true;
		if (server_nullable.getNativeLayer().isServerNull() || this.getNativeLayer().isServerNull() )		return false;
		if( this.isNull() && server_nullable.isNull() )								return true;

		return server_nullable == this;
	}

	/**
	 * Returns {@link #equals(BleServer)} if object is an instance of {@link BleServer}. Otherwise calls super.
	 *
	 * @see BleServer#equals(BleServer)
	 */
	@Override public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable)
	{
		if( object_nullable == null )  return false;

		if (object_nullable instanceof BleServer)
		{
			final BleServer object_cast = (BleServer) object_nullable;

			return this.equals(object_cast);
		}

		return false;
	}

	/*package*/ final void invokeOutgoingListeners(final OutgoingListener.OutgoingEvent e, final OutgoingListener listener_specific_nullable)
	{
		if( listener_specific_nullable != null )
		{
			getManager().getPostManager().postCallback(new Runnable()
			{
				@Override public void run()
				{
					if (listener_specific_nullable != null)
					{
						listener_specific_nullable.onEvent(e);
					}
				}
			});
		}

		if( m_outgoingListener_default != null )
		{
			getManager().getPostManager().postCallback(new Runnable()
			{
				@Override public void run()
				{
					if (m_outgoingListener_default != null)
					{
						m_outgoingListener_default.onEvent(e);
					}
				}
			});
		}

		if( getManager().m_defaultServerOutgoingListener != null )
		{
			getManager().getPostManager().postCallback(new Runnable()
			{
				@Override public void run()
				{
					if (getManager().m_defaultServerOutgoingListener != null)
					{
						getManager().m_defaultServerOutgoingListener.onEvent(e);
					}
				}
			});
		}
	}

	/**
	 * Overload of {@link #addService(BleService, ServiceAddListener)} without the listener.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) ServiceAddListener.ServiceAddEvent addService(final BleService service)
	{
		return this.addService(service, null);
	}

	/**
	 * Starts the process of adding a service to this server. The provided listener will be called when the service is added or there is a problem.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) ServiceAddListener.ServiceAddEvent addService(final BleService service, final ServiceAddListener listener)
	{
		return serviceMngr_server().addService(service, listener);
	}

	// TODO - This should become public for v3. For now, it will stay package private. (We may want to use BleService, etc in the GattDatabase class)
	final ServiceAddListener.ServiceAddEvent addService(final BluetoothGattService service, final ServiceAddListener listener)
	{
		return serviceMngr_server().addService_native(service, listener);
	}

	/**
	 * Remove any service previously provided to {@link #addService(BleService, ServiceAddListener)} or overloads. This can be safely called
	 * even if the call to {@link #addService(BleService, ServiceAddListener)} hasn't resulted in a callback to the provided listener yet, in which
	 * case it will be called with {@link BleServer.ServiceAddListener.Status#CANCELLED_FROM_REMOVAL}.
	 */
	public final @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattService removeService(final UUID serviceUuid)
	{
		return serviceMngr_server().remove(serviceUuid);
	}

	/**
	 * Convenience to remove all services previously added with {@link #addService(BleService, ServiceAddListener)} (or overloads). This is slightly more performant too.
	 */
	public final void removeAllServices()
	{
		serviceMngr_server().removeAll(ServiceAddListener.Status.CANCELLED_FROM_REMOVAL);
	}

	/**
	 * Offers a more "functional" means of iterating through the internal list of clients instead of
	 * using {@link #getClients()} or {@link #getClients_List()}.
	 */
	public final void getClients(final ForEach_Void<String> forEach)
	{
		m_clientMngr.getClients(forEach, 0x0);
	}

	/**
	 * Same as {@link #getClients(ForEach_Void)} but will only return clients
	 * in the given state provided.
	 */
	public final void getClients(final ForEach_Void<String> forEach, final BleServerState state)
	{
		m_clientMngr.getClients(forEach, state.bit());
	}

	/**
	 * Same as {@link #getClients(ForEach_Void)} but will only return clients
	 * in any of the given states provided.
	 */
	public final void getClients(final ForEach_Void<String> forEach, final BleServerState ... states)
	{
		m_clientMngr.getClients(forEach, BleServerState.toBits(states));
	}

	/**
	 * Overload of {@link #getClients(ForEach_Void)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach)
	{
		m_clientMngr.getClients(forEach, 0x0);
	}

	/**
	 * Overload of {@link #getClients(ForEach_Void, BleServerState)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach, final BleServerState state)
	{
		m_clientMngr.getClients(forEach, state.bit());
	}

	/**
	 * Same as {@link #getClients(ForEach_Breakable)} but will only return clients
	 * in any of the given states provided.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach, final BleServerState ... states)
	{
		m_clientMngr.getClients(forEach, BleServerState.toBits(states));
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients()
	{
		return m_clientMngr.getClients(0x0);
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients(final BleServerState state)
	{
		return m_clientMngr.getClients(state.bit());
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients(final BleServerState ... states)
	{
		return m_clientMngr.getClients(BleServerState.toBits(states));
	}

	/**
	 * Overload of {@link #getClients()} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List()
	{
		return m_clientMngr.getClients_List(0x0);
	}

	/**
	 * Overload of {@link #getClients(BleServerState)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List(final BleServerState state)
	{
		return m_clientMngr.getClients_List(state.bit());
	}

	/**
	 * Overload of {@link #getClients(BleServerState[])} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List(final BleServerState ... states)
	{
		return m_clientMngr.getClients_List(BleServerState.toBits(states));
	}

	/**
	 * Returns the total number of clients this server is connecting or connected to (or previously so).
	 */
	public final int getClientCount()
	{
		return m_clientMngr.getClientCount();
	}

	/**
	 * Returns the number of clients that are in the current state.
	 */
	public final int getClientCount(final BleServerState state)
	{
		return m_clientMngr.getClientCount(state.bit());
	}

	/**
	 * Returns the number of clients that are in any of the given states.
	 */
	public final int getClientCount(final BleServerState ... states)
	{
		return m_clientMngr.getClientCount(BleServerState.toBits(states));
	}

	/**
	 * Returns <code>true</code> if this server has any connected or connecting clients (or previously so).
	 */
	public final boolean hasClients()
	{
		return getClientCount() > 0;
	}

	/**
	 * Returns <code>true</code> if this server has any clients in the given state.
	 */
	public final boolean hasClient(final BleServerState state)
	{
		return getClientCount(state) > 0;
	}

	/**
	 * Returns <code>true</code> if this server has any clients in any of the given states.
	 */
	public final boolean hasClient(final BleServerState ... states)
	{
		return getClientCount(states) > 0;
	}

	final P_ServerServiceManager serviceMngr_server()
	{
		return getServiceManager();
	}

	/**
	 * Pretty-prints the list of connecting or connected clients.
	 */
	public final String toString()
	{
		return this.getClass().getSimpleName() + " with " + m_clientMngr.getClientCount(BleServerState.toBits(CONNECTING, CONNECTED)) + " connected/ing clients.";
	}

	/**
	 * Returns the local mac address provided by {@link BluetoothAdapter#getAddress()}.
	 */
	@Override public final @Nullable(Nullable.Prevalence.NEVER) String getMacAddress()
	{
		return getManager().managerLayer().getAddress();
	}
}
