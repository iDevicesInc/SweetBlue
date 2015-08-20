/**
 * 
 */
package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Lambda;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;

import static com.idevicesinc.sweetblue.BleServerState.*;


/**
 * Wrapper for functionality exposed by {@link BluetoothGattServer}. For OS levels less than 5.0, this
 * is only useful by piggybacking on an existing {@link BleDevice} that is currently {@link BleDeviceState#CONNECTED}.
 * For OS levels 5.0 and up a {@link BleServer} is capable of acting as an independent, advertising peripheral.
 */
public class BleServer implements UsesCustomNull
{
	/**
	 * Special value that is used in place of Java's built-in <code>null</code>.
	 */
	@Immutable
	public static final BleServer NULL = new BleServer(null, /*isNull=*/true);

	/**
	 * Tagging interface, not to be implemented directly as this is just the base interface to statically tie together
	 * {@link BleServer.RequestListener} and {@link ResponseCompletionListener} with common enums/structures.
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
			 * Only for {@link BleServer#notify(String, UUID, byte[])} or overloads.
			 */
			NOTIFICATION;

			/**
			 * Shorthand for checking if this equals {@link #READ}.
			 */
			public boolean isRead()
			{
				return this == READ;
			}

			/**
			 * Shorthand for checking if this equals {@link #NOTIFICATION}.
			 */
			public boolean isNotification()
			{
				return this == NOTIFICATION;
			}

			/**
			 * Shorthand for checking if this equals {@link #WRITE} or {@link #PREPARED_WRITE}.
			 */
			public boolean isWrite()
			{
				return this == WRITE || this == PREPARED_WRITE;
			}
		}

		/**
		 * Like {@link BleServer.ExchangeListener}, this class should not be used directly as this is just a base class to statically tie together
		 * {@link BleServer.RequestListener.RequestEvent} and {@link ResponseCompletionListener.ResponseCompletionEvent} with a common API.
		 */
		@Immutable
		public abstract static class ExchangeEvent
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
			public BleServer server() {  return m_server;  }
			private final BleServer m_server;

			/**
			 * Returns the mac address of the client peripheral that we are exchanging data with.
			 */
			public String macAddress()  {  return m_nativeDevice.getAddress(); }

			/**
			 * Returns the native bluetooth device object representing the client making the request.
			 */
			public BluetoothDevice nativeDevice()  {  return m_nativeDevice;  };
			private final BluetoothDevice m_nativeDevice;

			/**
			 * The type of operation, read or write.
			 */
			public Type type() {  return m_type;  }
			private final Type m_type;

			/**
			 * The type of GATT object this {@link ExchangeEvent} is for, characteristic or descriptor.
			 */
			public Target target() {  return m_target; }
			private final Target m_target;

			/**
			 * The {@link UUID} of the service associated with this {@link ExchangeEvent}.
			 */
			public UUID serviceUuid() {  return m_serviceUuid; }
			private final UUID m_serviceUuid;

			/**
			 * The {@link UUID} of the characteristic associated with this {@link ExchangeEvent}. This will always be
			 * a valid {@link UUID}, even if {@link #target()} is {@link Target#DESCRIPTOR}.
			 */
			public UUID charUuid() {  return m_charUuid; }
			private final UUID m_charUuid;

			/**
			 * The {@link UUID} of the descriptor associated with this {@link ExchangeEvent}. If {@link #target} is
			 * {@link Target#CHARACTERISTIC} then this will be referentially equal (i.e. you can use == to compare)
			 * to {@link #NON_APPLICABLE_UUID}.
			 */
			public UUID descUuid() {  return m_descUuid; }
			private final UUID m_descUuid;

			/**
			 * The data received from the client if {@link #type()} is {@link Type#isWrite()}, otherwise an empty byte array.
			 * This is in contrast to {@link BleServer.ResponseCompletionListener.ResponseCompletionEvent#data_sent()} if
			 * {@link #type()} is {@link Type#isRead()}.
			 *
			 */
			public @Nullable(Nullable.Prevalence.NEVER) byte[] data_received() {  return m_data_received; }
			private final byte[] m_data_received;

			/**
			 * The request id forwarded from the native stack. See various methods of {@link android.bluetooth.BluetoothGattServerCallback} for explanation.
			 */
			public int requestId()  {  return m_requestId;  }
			private final int m_requestId;

			/**
			 * The offset forwarded from the native stack. See various methods of {@link android.bluetooth.BluetoothGattServerCallback} for explanation.
			 */
			public int offset()  {  return m_offset;  }
			private final int m_offset;

			/**
			 * Dictates whether a response is needed.
			 */
			public boolean responseNeeded()  {  return m_responseNeeded;  }
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

				m_data_received = data_in != null ? data_in : EMPTY_BYTE_ARRAY;
			}

			@Override public String toString()
			{
				if( type().isRead() )
				{
					return Utils.toString
					(
						this.getClass(),
						"type",				type(),
						"target",			target(),
						"macAddress",		macAddress(),
						"charUuid",			server().getManager().getLogger().uuidName(charUuid()),
						"requestId",		requestId()
					);
				}
				else
				{
					return Utils.toString
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
	}

	/**
	 * Provide an instance through {@link BleManager#newServer(RequestListener)} or {@link BleServer#setListener_Request(RequestListener)}.
	 * The return value of {@link BleServer.RequestListener#onEvent(RequestEvent)} is used to decide if/how to respond to a given {@link BleServer.RequestListener.RequestEvent}.
	 */
	@Lambda
	public static interface RequestListener extends ExchangeListener
	{
		/**
		 * Struct passed to {@link {@link BleServer.RequestListener#onEvent(RequestEvent)}} that provides details about the client and what it wants from us, the server.
		 */
		@Immutable
		public static class RequestEvent extends ExchangeEvent
		{
			RequestEvent(BleServer server, BluetoothDevice nativeDevice, UUID serviceUuid_in, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_in, int requestId, int offset, final boolean responseNeeded)
			{
				super(server, nativeDevice, serviceUuid_in, charUuid_in, descUuid_in, type_in, target_in, data_in, requestId, offset, responseNeeded);
			}
		}

		/**
		 * Struct returned from {@link BleServer.RequestListener#onEvent(RequestEvent)}.
		 * Use the static constructor methods to create instances.
		 */
		@Immutable
		public static class Please
		{
			final int m_gattStatus;
			final int m_offset;
			final FutureData m_futureData;
			final ResponseCompletionListener m_responseListener;

			final boolean m_respond;

			private Please(final FutureData futureData, final int gattStatus, final int offset, final ResponseCompletionListener responseListener)
			{
				m_respond = true;

				m_futureData = futureData != null ? futureData : BleDevice.EMPTY_FUTURE_DATA;
				m_gattStatus = gattStatus;
				m_offset = offset;
				m_responseListener = responseListener != null ? responseListener : NULL_RESPONSE_LISTENER;
			}

			private Please(final ResponseCompletionListener responseListener)
			{
				m_respond = false;

				m_gattStatus = 0;
				m_offset = 0;
				m_futureData = BleDevice.EMPTY_FUTURE_DATA;
				m_responseListener = responseListener != null ? responseListener : NULL_RESPONSE_LISTENER;
			}

			/**
			 * Use this as the return value of {@link BleServer.RequestListener#onEvent(RequestEvent)}
			 * when {@link RequestEvent#responseNeeded()} is <code>true</code>.
			 */
			public static Please doNotRespond()
			{
				return doNotRespond(null);
			}

			/**
			 * Same as {@link #doNotRespond()} but allows you to provide a listener specific to this (non-)response.
			 * Your {@link ResponseCompletionListener#onEvent(ResponseCompletionListener.ResponseCompletionEvent)} will simply be called
			 * with {@link ResponseCompletionListener.Status#NO_RESPONSE_ATTEMPTED}.
			 *
			 * @see BleServer#setListener_ResponseCompletion(ResponseCompletionListener)
			 */
			public static Please doNotRespond(final ResponseCompletionListener listener)
			{
				return new Please(listener);
			}

			/**
			 * Overload of {@link #respondWithSuccess(byte[]) - see {@link FutureData} for why/when you would want to use this.
			 */
			public static Please respondWithSuccess(final FutureData futureData)
			{
				return respondWithSuccess(futureData, null);
			}

			/**
			 * Overload of {@link #respondWithSuccess(byte[], com.idevicesinc.sweetblue.BleServer.ResponseCompletionListener) - see {@link FutureData} for why/when you would want to use this.
			 */
			public static Please respondWithSuccess(final FutureData futureData, final ResponseCompletionListener listener)
			{
				return new Please(futureData, BleStatuses.GATT_SUCCESS, /*offset=*/0, listener);
			}

			/**
			 * Use this as the return value of {@link BleServer.RequestListener#onEvent(RequestEvent)} when
			 * {@link RequestEvent#type()} {@link Type#isRead()} is <code>true</code> and you can respect
			 * the read request and respond with data.
			 */
			public static Please respondWithSuccess(final byte[] data)
			{
				return respondWithSuccess(data, null);
			}

			/**
			 * Same as {@link #respondWithSuccess(byte[])} but allows you to provide a listener specific to this response.
			 *
			 * @see BleServer#setListener_ResponseCompletion(ResponseCompletionListener)
			 */
			public static Please respondWithSuccess(final byte[] data, final ResponseCompletionListener listener)
			{
				return respondWithSuccess(new PresentData(data), listener);
			}

			/**
			 * Use this as the return value of {@link BleServer.RequestListener#onEvent(RequestEvent)}
			 * when {@link RequestEvent#responseNeeded()} is <code>true</code> and {@link RequestEvent#type()}
			 * {@link Type#isWrite()} is <code>true</code> and you consider the write successful.
			 */
			public static Please respondWithSuccess()
			{
				return respondWithSuccess((ResponseCompletionListener)null);
			}

			/**
			 * Same as {@link #respondWithSuccess()} but allows you to provide a listener specific to this response.
			 *
			 * @see BleServer#setListener_ResponseCompletion(ResponseCompletionListener)
			 */
			public static Please respondWithSuccess(final ResponseCompletionListener listener)
			{
				return new Please(BleDevice.EMPTY_FUTURE_DATA, BleStatuses.GATT_SUCCESS, /*offset=*/0, listener);
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
			 * @see BleServer#setListener_ResponseCompletion(ResponseCompletionListener)
			 */
			public static Please respondWithError(final int gattStatus, final ResponseCompletionListener listener)
			{
				return new Please(BleDevice.EMPTY_FUTURE_DATA, gattStatus, /*offset=*/0, listener);
			}
		}
		
		/**
		 * Called when a read or write from the client is requested.
		 */
		Please onEvent(final RequestEvent event);
	}

	/**
	 * Provide an instance to various static methods of {@link BleServer.RequestListener.Please} such as
	 * {@link BleServer.RequestListener.Please#respondWithSuccess(ResponseCompletionListener)}, or {@link BleServer#setListener_ResponseCompletion(ResponseCompletionListener)},
	 * or {@link BleManager#setListener_ResponseCompletion(ResponseCompletionListener)}.
	 */
	public static interface ResponseCompletionListener extends ExchangeListener
	{
		/**
		 * Struct passed to {@link {@link ResponseCompletionListener#onEvent(ResponseCompletionEvent)}} that provides details
		 * about the original request that prompted the response, along with information on its success or failure.
		 */
		@Immutable
		public static class ResponseCompletionEvent extends ExchangeEvent implements UsesCustomNull
		{
			/**
			 * Returns the result of the response, or {@link ResponseCompletionListener.Status#NO_RESPONSE_ATTEMPTED} if
			 * for example {@link BleServer.RequestListener.Please#doNotRespond(ResponseCompletionListener)} was used.
			 */
			public Status status()  {  return m_status;  }
			private final Status m_status;

			/**
			 * The data that was attempted to be sent back to the client if {@link #type()} {@link Type#isRead()} is <code>true</code>.
			 */
			public byte[] data_sent()  {  return m_data_sent;  }
			private final byte[] m_data_sent;

			ResponseCompletionEvent(BleServer server, BluetoothDevice nativeDevice, UUID serviceUuid_in, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_received, byte[] data_sent, int requestId, int offset, final boolean responseNeeded, final Status status)
			{
				super(server, nativeDevice, serviceUuid_in, charUuid_in, descUuid_in, type_in, target_in, data_received, requestId, offset, responseNeeded);

				m_status = status;
				m_data_sent = data_sent;
			}

			ResponseCompletionEvent(final RequestListener.RequestEvent e, final byte[] data_sent, final Status status)
			{
				super(e.server(), e.nativeDevice(), e.serviceUuid(), e.charUuid(), e.descUuid(), e.type(), e.target(), e.data_received(), e.requestId(), e.offset(), e.responseNeeded());

				m_status = status;
				m_data_sent = data_sent;
			}

			static ResponseCompletionEvent EARLY_OUT__NOTIFICATION(final BleServer server, final BluetoothDevice nativeDevice, final UUID serviceUuid, final UUID charUuid, final FutureData data, final Status status)
			{
				return new ResponseCompletionEvent
				(
					server, nativeDevice, serviceUuid, charUuid, NON_APPLICABLE_UUID, Type.NOTIFICATION,
					Target.CHARACTERISTIC, EMPTY_BYTE_ARRAY, data.getData(), NON_APPLICABLE_REQUEST_ID, 0, false, status
				);
			}

			static ResponseCompletionEvent NULL__NOTIFICATION(final BleServer server, final BluetoothDevice nativeDevice, final UUID serviceUuid, final UUID charUuid)
			{
				return EARLY_OUT__NOTIFICATION(server, nativeDevice, serviceUuid, charUuid, BleServer.EMPTY_FUTURE_DATA, Status.NULL);
			}

			/**
			 * Will return true in certain early-out cases when there is no issue and the response can continue.
			 * See {@link BleServer#notify(String, UUID, UUID, byte[], ResponseCompletionListener)} for more information.
			 */
			@Override public boolean isNull()
			{
				return status().isNull();
			}
		}

		/**
		 * Enumeration of the various success and error statuses possible for a response.
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Fulfills the soft contract of {@link UsesCustomNull}.
			 */
			NULL,

			/**
			 * The response to the client was successfully sent.
			 */
			SUCCESS,

			/**
			 * {@link BleServer.RequestListener.Please#doNotRespond(ResponseCompletionListener)} (or overloads)
			 * were called or {@link RequestListener.RequestEvent#responseNeeded()} was <code>false</code>.
			 */
			NO_RESPONSE_ATTEMPTED,

			/**
			 * The server does not have a {@link com.idevicesinc.sweetblue.BleServer.RequestListener} set so no valid response
			 * could be sent. Please set a listener through {@link BleServer#setListener_Request(RequestListener)}.
			 */
			NO_REQUEST_LISTENER_SET,

			/**
			 * Couldn't find a matching {@link ResponseCompletionEvent#target()} for {@link ResponseCompletionEvent#charUuid()}.
			 */
			NO_MATCHING_TARGET,

			/**
			 * For now only relevant if {@link ResponseCompletionEvent#type()} is {@link Type#NOTIFICATION} -
			 * {@link BluetoothGattCharacteristic#setValue(byte[])} (or one of its overloads) returned <code>false</code>.
			 */
			FAILED_TO_SET_VALUE_ON_TARGET,

			/**
			 * The underlying call to {@link BluetoothGattServer#sendResponse(BluetoothDevice, int, int, int, byte[])}
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
			 * {@link ResponseCompletionEvent#status()} could potentially be {@link #CANCELLED_FROM_DISCONNECT} because SweetBlue might get
			 * the disconnect callback before the turning off callback. Basic testing has revealed that this is *not* the case, but you never know.
			 * <br><br>
			 * Either way, the device was or will be disconnected.
			 */
			CANCELLED_FROM_BLE_TURNING_OFF,

			/**
			 * Could not communicate with the client device because the server is not currently {@link BleServerState#CONNECTED}.
			 */
			NOT_CONNECTED;

			/**
			 * Returns true if <code>this==</code> {@link #NULL}.
			 */
			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Called when a response to a request is fulfilled or failed.
		 */
		void onEvent(final ResponseCompletionEvent event);
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
			public BleServer server() {  return m_server;  }
			private final BleServer m_server;

			/**
			 * Returns the mac address of the client that's undergoing the state change with this {@link #server()}.
			 */
			public String macAddress()  {  return m_macAddress;  }
			private final String m_macAddress;

			/**
			 * The change in gattStatus that may have precipitated the state change, or {@link BleDeviceConfig#GATT_STATUS_NOT_APPLICABLE}.
			 * For example if {@link #didEnter(State)} with {@link BleServerState#DISCONNECTED} is <code>true</code> and
			 * {@link #didExit(State)} with {@link BleServerState#CONNECTING} is also <code>true</code> then {@link #gattStatus()} may be greater
			 * than zero and give some further hint as to why the connection failed.
			 */
			public int gattStatus() {  return m_gattStatus;  }
			private final int m_gattStatus;

			StateEvent(BleServer server, String macAddress, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
			{
				super(oldStateBits, newStateBits, intentMask);

				m_server = server;
				m_gattStatus = gattStatus;
				m_macAddress = macAddress;
			}

			@Override public String toString()
			{
				return Utils.toString
				(
					this.getClass(),
					"server",			server().getName_debug(),
					"entered",			Utils.toString(enterMask(),						BleServerState.VALUES()),
					"exited", 			Utils.toString(exitMask(),						BleServerState.VALUES()),
					"current",			Utils.toString(newStateBits(),					BleServerState.VALUES()),
					"gattStatus",		server().m_logger.gattStatus(gattStatus())
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
	public static interface ConnectionFailListener
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
			 * because the operation took longer than the time dictated by {@link BleDeviceConfig#timeoutRequestFilter}.
			 */
			NATIVE_CONNECTION_TIMED_OUT,

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
			 * process.
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
			 * Whether this reason honors a {@link Please#isRetry()}. Returns <code>false</code> if {@link #wasCancelled()} or
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
						this == NATIVE_CONNECTION_TIMED_OUT				 ;
			}
		}

		/**
		 * Describes usage of the <code>autoConnect</code> parameter for
		 * {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}.
		 */
		@com.idevicesinc.sweetblue.annotations.Advanced
		public static enum AutoConnectUsage
		{
			/**
			 * Used when we didn't start the connection process, i.e. it came out of nowhere. Rare case but can happen, for example after
			 * SweetBlue considers a connect timed out based on {@link BleManagerConfig#timeoutRequestFilter} but then it somehow
			 * does come in (shouldn't happen but who knows).
			 */
			UNKNOWN,

			/**
			 * Usage is not applicable to the{@link ConnectionFailEvent#status()} given.
			 */
			NOT_APPLICABLE,

			/**
			 * <code>autoConnect</code> was used.
			 */
			USED,

			/**
			 * <code>autoConnect</code> was not used.
			 */
			NOT_USED;
		}

		/**
		 * Structure passed to {@link ConnectionFailListener#onEvent(ConnectionFailEvent)} to provide more info about how/why the connection failed.
		 */
		@Immutable
		public static class ConnectionFailEvent implements UsesCustomNull
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
			 * The failure count so far. This will start at 1 and keep incrementing for more failures.
			 */
			public int failureCountSoFar() {  return m_failureCountSoFar;  }
			private final int m_failureCountSoFar;

			/**
			 * How long the last connection attempt took before failing.
			 */
			public Interval attemptTime_latest() {  return m_latestAttemptTime;  }
			private final Interval m_latestAttemptTime;

			/**
			 * How long it's been since {@link BleDevice#connect()} (or overloads) were initially called.
			 */
			public Interval attemptTime_total() {  return m_totalAttemptTime;  }
			private final Interval m_totalAttemptTime;

			/**
			 * The gattStatus returned, if applicable, from native callbacks like {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)}
			 * or {@link BluetoothGattCallback#onServicesDiscovered(BluetoothGatt, int)}.
			 * If not applicable, for example if {@link ConnectionFailEvent#status()} is {@link Status#EXPLICIT_DISCONNECT}, then this is set to
			 * {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
			 * <br><br>
			 * See {@link BleDevice.ReadWriteListener.ReadWriteEvent#gattStatus()} for more information about gatt status codes in general.
			 *
			 * @see BleDevice.ReadWriteListener.ReadWriteEvent#gattStatus()
			 */
			public int gattStatus() {  return m_gattStatus;  }
			private final int m_gattStatus;

			/**
			 * Whether <code>autoConnect=true</code> was passed to {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}.
			 * See more discussion at {@link BleDeviceConfig#alwaysUseAutoConnect}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public AutoConnectUsage autoConnectUsage() {  return m_autoConnectUsage;  }
			private final AutoConnectUsage m_autoConnectUsage;

			/**
			 * Returns a chronologically-ordered list of all {@link ConnectionFailEvent} instances returned through
			 * {@link ConnectionFailListener#onEvent(ConnectionFailEvent)} since the first call to {@link BleDevice#connect()},
			 * including the current instance. Thus this list will always have at least a length of one (except if {@link #isNull()} is <code>true</code>).
			 * The list length is "reset" back to one whenever a {@link BleDeviceState#CONNECTING_OVERALL} operation completes, either
			 * through becoming {@link BleDeviceState#INITIALIZED}, or {@link BleDeviceState#DISCONNECTED} for good.
			 */
			public ConnectionFailEvent[] history()  {  return m_history;  }
			private final ConnectionFailEvent[] m_history;

			ConnectionFailEvent(BleServer server, final BluetoothDevice nativeDevice, Status status, int failureCountSoFar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, AutoConnectUsage autoConnectUsage, ArrayList<ConnectionFailEvent> history)
			{
				this.m_server = server;
				this.m_nativeDevice = nativeDevice;
				this.m_status = status;
				this.m_failureCountSoFar = failureCountSoFar;
				this.m_latestAttemptTime = latestAttemptTime;
				this.m_totalAttemptTime = totalAttemptTime;
				this.m_gattStatus = gattStatus;
				this.m_autoConnectUsage = autoConnectUsage;

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
			static ConnectionFailEvent[] EMPTY_HISTORY()
			{
				s_emptyHistory = s_emptyHistory != null ? s_emptyHistory : new ConnectionFailEvent[0];

				return s_emptyHistory;
			}

			static ConnectionFailEvent NULL(BleServer server, BluetoothDevice nativeDevice)
			{
				return new ConnectionFailEvent(server, nativeDevice, Status.NULL, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, AutoConnectUsage.NOT_APPLICABLE, null);
			}

			static ConnectionFailEvent EARLY_OUT(BleServer server, BluetoothDevice nativeDevice, Status status)
			{
				return new ConnectionFailListener.ConnectionFailEvent(server, nativeDevice, status, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, AutoConnectUsage.NOT_APPLICABLE, null);
			}

			/**
			 * Returns whether this {@link ConnectionFailEvent} instance is a "dummy" value. For now used for
			 * {@link BleDeviceConfig.ReconnectRequestFilter.ReconnectRequestEvent#connectionFailInfo()} in certain situations.
			 */
			@Override public boolean isNull()
			{
				return status().isNull();
			}

			/**
			 * Forwards {@link com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Status#shouldBeReportedToUser()}
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
					return Utils.toString
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

		/**
		 * Return value for {@link ConnectionFailListener#onEvent(ConnectionFailEvent)}.
		 * Generally you will only return {@link #retry()} or {@link #doNotRetry()}, but there are more advanced options as well.
		 */
		@Immutable
		public static class Please
		{
			static enum PE_Please
			{
				RETRY, RETRY_WITH_AUTOCONNECT_TRUE, RETRY_WITH_AUTOCONNECT_FALSE, DO_NOT_RETRY;

				boolean isRetry()
				{
					return this != DO_NOT_RETRY;
				}
			}

			private final PE_Please m_please;

			private Please(PE_Please please)
			{
				m_please = please;
			}

			PE_Please please()
			{
				return m_please;
			}

			/**
			 * Return this to retry the connection, continuing the connection fail retry loop. <code>autoConnect</code> passed to
			 * {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}
			 * will be false or true based on what has worked in the past, or on {@link BleServerConfig#alwaysUseAutoConnect}.
			 */
			public static Please retry()
			{
				return new Please(PE_Please.RETRY);
			}

			/**
			 * Returns {@link #retry()} if the given condition holds <code>true</code>, {@link #doNotRetry()} otherwise.
			 */
			public static Please retryIf(boolean condition)
			{
				return condition ? retry() : doNotRetry();
			}

			/**
			 * Return this to stop the connection fail retry loop.
			 */
			public static Please doNotRetry()
			{
				return new Please(PE_Please.DO_NOT_RETRY);
			}

			/**
			 * Returns {@link #doNotRetry()} if the given condition holds <code>true</code>, {@link #retry()} otherwise.
			 */
			public static Please doNotRetryIf(boolean condition)
			{
				return condition ? doNotRetry() : retry();
			}

			/**
			 * Same as {@link #retry()}, but <code>autoConnect=true</code> will be passed to
			 * {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}.
			 * See more discussion at {@link BleServerConfig#alwaysUseAutoConnect}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public static Please retryWithAutoConnectTrue()
			{
				return new Please(PE_Please.RETRY_WITH_AUTOCONNECT_TRUE);
			}

			/**
			 * Opposite of{@link #retryWithAutoConnectTrue()}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public static Please retryWithAutoConnectFalse()
			{
				return new Please(PE_Please.RETRY_WITH_AUTOCONNECT_FALSE);
			}

			/**
			 * Returns <code>true</code> for everything except {@link #doNotRetry()}.
			 */
			public boolean isRetry()
			{
				return m_please != null && m_please.isRetry();
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
		 * The default connection fail limit past which {@link DefaultConnectionFailListener} will start returning {@link Please#retryWithAutoConnectTrue()}.
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

		public int getRetryCount()
		{
			return m_retryCount;
		}

		@Override public Please onEvent(ConnectionFailEvent e)
		{
			//--- DRK > Not necessary to check this ourselves, just being explicit.
			if (!e.status().allowsRetry() )
			{
				return Please.doNotRetry();
			}
			if (e.failureCountSoFar() <= m_retryCount)
			{
				return Please.retry();
			}
			else
			{
				return Please.doNotRetry();
			}
		}
	}

	private static final ResponseCompletionListener NULL_RESPONSE_LISTENER = new ResponseCompletionListener()
	{
		@Override public void onEvent(ResponseCompletionEvent event)
		{
		}
	};

	static final byte[]				EMPTY_BYTE_ARRAY	= new byte[0];
	static final FutureData			EMPTY_FUTURE_DATA 	= new PresentData(EMPTY_BYTE_ARRAY);

	private final P_ServerStateTracker m_stateTracker;
	private final BleManager m_mngr;
	private final P_TaskQueue m_queue;
	final P_BleServer_Listeners m_listeners;
	final P_NativeServerWrapper m_nativeWrapper;
	private RequestListener m_requestListener;
	private ResponseCompletionListener m_responseListener_default;
	private final P_Logger m_logger;
	private final boolean m_isNull;
	private BleServerConfig m_config = null;
	private final P_ServerConnectionFailManager m_connectionFailMngr;

	/**
	 * Field for app to associate any data it wants with instances of this class
	 * instead of having to subclass or manage associative hash maps or something.
	 * The library does not touch or interact with this data in any way.
	 *
	 * @see BleManager#appData
	 * @see BleDevice#appData
	 */
	public Object appData;

	BleServer(final BleManager mngr, final boolean isNull)
	{
		m_mngr = mngr;
		m_isNull = isNull;

		if( isNull )
		{
			m_queue = null;
			m_logger = null;
			m_stateTracker = new P_ServerStateTracker(this);
			m_listeners = null;
			m_nativeWrapper = new P_NativeServerWrapper(this);
			m_connectionFailMngr = new P_ServerConnectionFailManager(this);
		}
		else
		{
			m_queue = m_mngr.getTaskQueue();
			m_logger = m_mngr.getLogger();
			m_stateTracker = new P_ServerStateTracker(this);
			m_listeners = new P_BleServer_Listeners(this);
			m_nativeWrapper = new P_NativeServerWrapper(this);
			m_connectionFailMngr = new P_ServerConnectionFailManager(this);
		}
	}

	public void setConfig(final BleServerConfig config)
	{
		m_config = config;
	}

	BleServerConfig conf_server()
	{
		return m_config != null ? m_config : conf_mngr();
	}

	BleManagerConfig conf_mngr()
	{
		if (getManager() != null)
		{
			return getManager().m_config;
		}
		else
		{
			return BleManagerConfig.NULL;
		}
	}
	
	/**
	 * Set a listener here to be notified whenever this server's state changes in relation to a specific client.
	 */
	public void setListener_State(final BleServer.StateListener listener)
	{
		m_stateTracker.setListener(listener);
	}

	/**
	 * Set a listener here to override any listener provided previously either through this method or through {@link BleManager#newServer(RequestListener)} or otherwise.
	 */
	public void setListener_Request(final RequestListener listener)
	{
		m_requestListener = listener;
	}

	public @Nullable(Nullable.Prevalence.RARE) RequestListener getListener_Request()
	{
		return m_requestListener;
	}

	/**
	 * This is a default catch-all convenience listener that will be called after any listener provided through
	 * the static methods of {@link BleServer.RequestListener.Please} such as {@link BleServer.RequestListener.Please#respondWithSuccess(ResponseCompletionListener)}.
	 *
	 * @see BleManager#setListener_ResponseCompletion(ResponseCompletionListener)
	 */
	public void setListener_ResponseCompletion(final ResponseCompletionListener listener)
	{
		m_responseListener_default = listener;
	}

	public void setListener_ConnectionFail(final ConnectionFailListener listener)
	{
		m_connectionFailMngr.setListener(listener);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseCompletionListener.ResponseCompletionEvent notify( final String macAddress, UUID charUuid, byte[] data )
	{
		return notify(macAddress, null, charUuid, data, (ResponseCompletionListener) null);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseCompletionListener.ResponseCompletionEvent notify( final String macAddress, UUID charUuid, byte[] data, ResponseCompletionListener listener)
	{
		return notify(macAddress, (UUID) null, charUuid, data, listener);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseCompletionListener.ResponseCompletionEvent notify( final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data )
	{
		return notify(macAddress, serviceUuid, charUuid, data, (ResponseCompletionListener) null);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseCompletionListener.ResponseCompletionEvent notify( final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data, ResponseCompletionListener listener )
	{
		return notify(macAddress, serviceUuid, charUuid, new PresentData(data), listener);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseCompletionListener.ResponseCompletionEvent notify( final String macAddress, final UUID charUuid, final FutureData futureData)
	{
		return notify(macAddress, null, charUuid, futureData, (ResponseCompletionListener) null);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseCompletionListener.ResponseCompletionEvent notify( final String macAddress, final UUID charUuid, final FutureData futureData, ResponseCompletionListener listener)
	{
		return notify(macAddress, (UUID) null, charUuid, futureData, listener);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseCompletionListener.ResponseCompletionEvent notify( final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData )
	{
		return notify(macAddress, serviceUuid, charUuid, futureData, (ResponseCompletionListener) null);
	}

	/**
	 * Use this method to send a notification to the client device with the given mac address to the given characteristic {@link UUID}.
	 * If there is any kind of "early-out" issue then this method will return a {@link ResponseCompletionListener.ResponseCompletionEvent} in addition
	 * to passing it through the listener. Otherwise this method will return an instance with {@link ResponseCompletionListener.ResponseCompletionEvent#isNull()} being
	 * <code>true</code>.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) ResponseCompletionListener.ResponseCompletionEvent notify( final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, ResponseCompletionListener listener )
	{
		final BluetoothDevice nativeDevice = newNativeDevice(macAddress);

		if( !is(macAddress, CONNECTED ) )
		{
			final ResponseCompletionListener.ResponseCompletionEvent e = ResponseCompletionListener.ResponseCompletionEvent.EARLY_OUT__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid, futureData, ResponseCompletionListener.Status.NOT_CONNECTED);

			invokeResponseListeners(e, listener);

			return e;
		}

		final BluetoothGattCharacteristic char_native = getNativeCharacteristic(serviceUuid, charUuid);

		if( char_native == null )
		{
			final ResponseCompletionListener.ResponseCompletionEvent e = ResponseCompletionListener.ResponseCompletionEvent.EARLY_OUT__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid, futureData, ResponseCompletionListener.Status.NO_MATCHING_TARGET);

			invokeResponseListeners(e, listener);

			return e;
		}

		final boolean confirm;
		final P_Task_SendNotification task = P_Task_SendNotification(this, nativeDevice, serviceUuid, charUuid, futureData, confirm, listener);
		m_queue.add(task);

		return ResponseCompletionListener.ResponseCompletionEvent.NULL__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid);
	}

	/**
	 * Provides just-in-case lower-level access to the native server instance.
	 * See similar warning for {@link BleDevice#getNative()}.
	 */
	@Advanced
	public @Nullable(Nullable.Prevalence.RARE) BluetoothGattServer getNative()
	{
		return m_nativeWrapper.getNative();
	}

	/**
	 * Returns the bitwise state mask representation of {@link BleServerState} for the given client mac address.
	 *
	 * @see BleServerState
	 */
	@Advanced
	public int getStateMask(final String macAddress)
	{
		return m_stateTracker.getStateMask(macAddress);
	}

	public boolean is(final String macAddress, final BleServerState state)
	{
		return state.overlaps(getStateMask(macAddress));
	}

	public boolean isAny(final String macAddress, final BleServerState ... states )
	{
		final int stateMask = getStateMask(macAddress);

		for( int i = 0; i < states.length; i++ )
		{
			if( states[i].overlaps(stateMask) )  return true;
		}

		return false;
	}

	/**
	 * Returns this server's manager.
	 */
	public BleManager getManager()
	{
		return m_mngr;
	}

	public ConnectionFailListener.ConnectionFailEvent connect(final String macAddress)
	{
		return connect(macAddress, null, null);
	}

	public ConnectionFailListener.ConnectionFailEvent connect(final String macAddress, final StateListener stateListener)
	{
		return connect(macAddress, stateListener, null);
	}

	public ConnectionFailListener.ConnectionFailEvent connect(final String macAddress, final ConnectionFailListener connectionFailListener)
	{
		return connect(macAddress, null, connectionFailListener);
	}

	public ConnectionFailListener.ConnectionFailEvent connect(final String macAddress, final StateListener stateListener, final ConnectionFailListener connectionFailListener)
	{
		return connect_internal(newNativeDevice(macAddress), stateListener, connectionFailListener);
	}

	ConnectionFailListener.ConnectionFailEvent connect_internal(final BluetoothDevice nativeDevice)
	{
		return connect_internal(nativeDevice, null, null);
	}

	ConnectionFailListener.ConnectionFailEvent connect_internal(final BluetoothDevice nativeDevice, final StateListener stateListener, final ConnectionFailListener connectionFailListener)
	{
		if( isNull() )
		{
			final ConnectionFailListener.ConnectionFailEvent e = ConnectionFailListener.ConnectionFailEvent.EARLY_OUT(this, nativeDevice, ConnectionFailListener.Status.NULL_SERVER);

			m_connectionFailMngr.invokeCallback(e);

			return e;
		}

		if( stateListener != null )
		{
			setListener_State(stateListener);
		}

		if( connectionFailListener != null )
		{
			setListener_ConnectionFail(connectionFailListener);
		}

		m_connectionFailMngr.onExplicitConnectionStarted();

		if( isAny(nativeDevice.getAddress(), CONNECTING, CONNECTED) )
		{
			final ConnectionFailListener.ConnectionFailEvent e = ConnectionFailListener.ConnectionFailEvent.EARLY_OUT(this, nativeDevice, ConnectionFailListener.Status.ALREADY_CONNECTING_OR_CONNECTED);

			m_connectionFailMngr.invokeCallback(e);

			return e;
		}

		final P_Task_ConnectServer task = new P_Task_ConnectServer(this, nativeDevice, m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
		m_queue.add(task);

		m_stateTracker.doStateTransition(nativeDevice.getAddress(), BleServerState.DISCONNECTED /* ==> */, BleServerState.CONNECTING, ChangeIntent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		return ConnectionFailListener.ConnectionFailEvent.NULL(this, nativeDevice);
	}

	private BluetoothDevice newNativeDevice(final String macAddress)
	{
		final BleManager mngr = getManager();

		return mngr == null ? null : mngr.newNativeDevice(macAddress);
	}

	public boolean disconnect(final String macAddress)
	{
		m_connectionFailMngr.onExplicitDisconnect();

		if( is(macAddress, DISCONNECTED) )  return false;

		final int stateMask = getStateMask(macAddress);
		final BleServerState oldConnectionState;

		if( BleServerState.CONNECTING.overlaps(stateMask) )
		{
			oldConnectionState = CONNECTING;
		}
		else if( BleServerState.CONNECTED.overlaps(stateMask) )
		{
			oldConnectionState = CONNECTED;
		}
		else
		{
			getManager().ASSERT(false, "Expected to be connecting or connected for an explicit disconnect.");

			return false;
		}

		final BluetoothDevice nativeDevice = newNativeDevice(macAddress);
		final P_Task_DisconnectServer task = new P_Task_DisconnectServer(this, nativeDevice, m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
		m_queue.add(task);

		m_stateTracker.doStateTransition(macAddress, oldConnectionState /* ==> */, BleServerState.DISCONNECTED, ChangeIntent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		if( oldConnectionState == CONNECTING )
		{
			m_connectionFailMngr.onNativeConnectFail(nativeDevice, ConnectionFailListener.Status.EXPLICIT_DISCONNECT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}

		return true;
	}

	/**
	 * Disconnects this server completely, disconnecting all connected clients and shutting things down.
	 * To disconnect individual clients use {@link #disconnect(String)}.
	 */
	public void disconnect()
	{
		if( stateListener != null )
		{
			setListener_State(stateListener);
		}

		m_queue.add(new P_Task_DisconnectServer(this, m_taskStateListener));
	}

	@Override public boolean isNull()
	{
		return m_isNull;
	}

	void onNativeConnecting_implicit(final String macAddress)
	{
		m_stateTracker.doStateTransition(macAddress, BleServerState.DISCONNECTED /* ==> */, BleServerState.CONNECTING, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	void onNativeConnect(final String macAddress, final boolean explicit)
	{
		final ChangeIntent intent = explicit ? ChangeIntent.INTENTIONAL : ChangeIntent.UNINTENTIONAL;

		m_stateTracker.doStateTransition(macAddress, BleServerState.CONNECTING /* ==> */, BleServerState.CONNECTED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	void onNativeConnectFail(final BluetoothDevice nativeDevice, final ConnectionFailListener.Status status, final int gattStatus)
	{
		if( status == ConnectionFailListener.Status.NATIVE_CONNECTION_TIMED_OUT )
		{
			final P_Task_DisconnectServer task = new P_Task_DisconnectServer(this, nativeDevice, m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
			m_queue.add(task);
		}

		m_stateTracker.doStateTransition(nativeDevice.getAddress(), BleServerState.CONNECTING /* ==> */, BleServerState.DISCONNECTED, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		m_connectionFailMngr.onNativeConnectFail(nativeDevice, status, gattStatus);
	}

	void onNativeDisconnect( final String macAddress, final boolean explicit, final int gattStatus)
	{
		if( !explicit )
		{
			m_stateTracker.doStateTransition(macAddress, BleServerState.CONNECTED /* ==> */, BleServerState.DISCONNECTED, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else
		{
			// explicit case gets handled immediately by the disconnect method.
		}
	}

	/**
	 * Does a referential equality check on the two servers.
	 */
	public boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final BleServer server_nullable)
	{
		if (server_nullable == null)												return false;
		if (server_nullable == this)												return true;
		if (server_nullable.getNative() == null || this.getNative() == null)		return false;
		if( this.isNull() && server_nullable.isNull() )								return true;

		return server_nullable.getNative().equals(this.getNative());
	}

	/**
	 * Returns {@link #equals(BleServer)} if object is an instance of {@link BleServer}. Otherwise calls super.
	 *
	 * @see BleServer#equals(BleServer)
	 */
	@Override public boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable)
	{
		if( object_nullable == null )  return false;

		if (object_nullable instanceof BleServer)
		{
			BleServer object_cast = (BleServer) object_nullable;

			return this.equals(object_cast);
		}

		return false;
	}

	void invokeResponseListeners(final ResponseCompletionListener.ResponseCompletionEvent e, final ResponseCompletionListener listener_specific_nullable)
	{
		if( listener_specific_nullable != null )
		{
			listener_specific_nullable.onEvent(e);
		}

		if( m_responseListener_default != null )
		{
			m_responseListener_default.onEvent(e);
		}

		if( m_mngr.m_defaultServerResponseListener != null )
		{
			m_mngr.m_defaultServerResponseListener.onEvent(e);
		}
	}

	/**
	 * Returns the native descriptor for the given UUID in case you need lower-level access.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor getNativeDescriptor(final UUID charUuid, final UUID descUUID)
	{
		final UUID serviceUuid = null;

		final BluetoothGattCharacteristic char_native = getNativeCharacteristic(serviceUuid, charUuid);

		if( char_native == null )  return null;

		final BluetoothGattDescriptor desc_native = char_native.getDescriptor(descUUID);

		return desc_native;
	}

	/**
	 * Returns the native characteristic for the given UUID in case you need lower-level access.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattCharacteristic getNativeCharacteristic(final UUID characteristicUuid)
	{
		final UUID serviceUuid = null;

		final P_Characteristic characteristic = m_serviceMngr.getCharacteristic(serviceUuid, characteristicUuid);

		if (characteristic == null)  return null;

		return characteristic.getGuaranteedNative();
	}

	/**
	 * Overload of {@link #getNativeCharacteristic(UUID)} for when you have characteristics with identical uuids under different services.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattCharacteristic getNativeCharacteristic(final UUID serviceUuid, final UUID characteristicUuid)
	{
		final P_Characteristic characteristic = m_serviceMngr.getCharacteristic(serviceUuid, characteristicUuid);

		if (characteristic == null)  return null;

		return characteristic.getGuaranteedNative();
	}

	/**
	 * Returns the native service for the given UUID.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NORMAL)
	BluetoothGattService getNativeService(final UUID uuid)
	{
		final P_Service service = m_serviceMngr.get(uuid);

		if (service == null)  return null;

		return service.getNative();
	}

	/**
	 * Returns all {@link BluetoothGattService} instances once {@link BleDevice#is(BleDeviceState)} with
	 * {@link BleDeviceState#SERVICES_DISCOVERED} returns <code>true</code>.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NEVER)
	Iterator<BluetoothGattService> getNativeServices()
	{
		return m_serviceMngr.getNativeServices();
	}

	/**
	 * Convenience overload of {@link #getNativeServices()} that returns a {@link List}.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattService> getNativeServices_List()
	{
		return m_serviceMngr.getNativeServices_List();
	}

	/**
	 * Returns all {@link BluetoothGattService} instances once {@link BleDevice#is(BleDeviceState)} with
	 * {@link BleDeviceState#SERVICES_DISCOVERED} returns <code>true</code>.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattCharacteristic> getNativeCharacteristics()
	{
		return m_serviceMngr.getNativeCharacteristics();
	}

	/**
	 * Convenience overload of {@link #getNativeCharacteristics()} that returns a {@link List}.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattCharacteristic> getNativeCharacteristics_List()
	{
		return m_serviceMngr.getNativeCharacteristics_List();
	}

	/**
	 * Same as {@link #getNativeCharacteristics()} but you can filter on the service {@link UUID}.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattCharacteristic> getNativeCharacteristics(UUID service)
	{
		return m_serviceMngr.getNativeCharacteristics(service);
	}

	/**
	 * Convenience overload of {@link #getNativeCharacteristics(UUID)} that returns a {@link List}.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattCharacteristic> getNativeCharacteristics_List(UUID service)
	{
		return m_serviceMngr.getNativeCharacteristics_List(service);
	}
}
