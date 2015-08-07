/**
 * 
 */
package com.idevicesinc.sweetblue;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Lambda;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;


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
	 * {@link BleServer.RequestListener} and {@link BleServer.ResponseListener} with common enums/structures.
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
		 * {@link BleServer.RequestListener.RequestEvent} and {@link BleServer.ResponseListener.ResponseEvent} with a common API.
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
			 * The {@link BleServer} this {@link ExchangeEvent} is for.
			 */
			public BleServer server() {  return m_server;  }
			private final BleServer m_server;

			/**
			 * Returns the mac address of the client peripheral that we are exchanging data with.
			 */
			public String macAddress()  {  return m_nativeDevice.getAddress(); }
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
			 * Either the data sent from the client if {@link #type()} {@link Type#isWrite()} is true and we're handling a request,
			 * or the data provided through {@link BleServer.RequestListener.Please#respondWithSuccess(byte[])} if
			 * {@link #type()} {@link Type#isRead()}, or the data provided through {@link BleServer#notify(String, UUID, byte[])}
			 * (or overloads). This will never be null, at worst it will be an empty array.
			 */
			public @Nullable(Nullable.Prevalence.NEVER) byte[] data() {  return m_data; }
			private final byte[] m_data;

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

			ExchangeEvent(BleServer server, BluetoothDevice nativeDevice, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_in, int requestId, int offset, final boolean responseNeeded)
			{
				m_server = server;
				m_nativeDevice = nativeDevice;
				m_charUuid = charUuid_in != null ? charUuid_in : NON_APPLICABLE_UUID;;
				m_descUuid = descUuid_in != null ? descUuid_in : NON_APPLICABLE_UUID;
				m_type = type_in;
				m_target = target_in;
				m_requestId = requestId;
				m_offset = offset;
				m_responseNeeded = responseNeeded;

				m_data = data_in != null ? data_in : EMPTY_BYTE_ARRAY;
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
						"data",				data(),
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
			RequestEvent(BleServer server, BluetoothDevice nativeDevice, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_in, int requestId, int offset, final boolean responseNeeded)
			{
				super(server, nativeDevice, charUuid_in, descUuid_in, type_in, target_in, data_in, requestId, offset, responseNeeded);
			}
		}

		/**
		 * Struct returned from {@link BleServer.RequestListener#onEvent(RequestEvent)}.
		 * Use the static constructor methods to create instances.
		 */
		@Immutable
		public static class Please
		{
			final int m_status;
			final int m_offset;
			final byte[] m_data;
			final ResponseListener m_responseListener;

			final boolean m_respond;

			private Please(final byte[] data, final int status, final int offset, final ResponseListener responseListener)
			{
				m_respond = true;

				m_data = data;
				m_status = status;
				m_offset = offset;
				m_responseListener = responseListener;
			}

			private Please(final ResponseListener responseListener)
			{
				m_respond = false;

				m_status = 0;
				m_offset = 0;
				m_data = null;
				m_responseListener = responseListener;
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
			 * Same as {@link #doNotRespond()} but allows you to provide a listener specific to this response.
			 *
			 * @see BleServer#setListener_Response(ResponseListener)
			 */
			public static Please doNotRespond(final ResponseListener listener)
			{
				return new Please(listener);
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
			 * @see BleServer#setListener_Response(ResponseListener)
			 */
			public static Please respondWithSuccess(final byte[] data, final ResponseListener listener)
			{
				return new Please(data, BleStatuses.GATT_SUCCESS, /*offset=*/0, listener);
			}

			/**
			 * Use this as the return value of {@link BleServer.RequestListener#onEvent(RequestEvent)}
			 * when {@link RequestEvent#responseNeeded()} is <code>true</code> and {@link RequestEvent#type()}
			 * {@link Type#isWrite()} is <code>true</code> and you consider the write successful.
			 */
			public static Please respondWithSuccess()
			{
				return respondWithSuccess((ResponseListener)null);
			}

			/**
			 * Same as {@link #respondWithSuccess()} but allows you to provide a listener specific to this response.
			 *
			 * @see BleServer#setListener_Response(ResponseListener)
			 */
			public static Please respondWithSuccess(final ResponseListener listener)
			{
				return new Please(EMPTY_BYTE_ARRAY, BleStatuses.GATT_SUCCESS, /*offset=*/0, listener);
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
			 * @see BleServer#setListener_Response(ResponseListener)
			 */
			public static Please respondWithError(final int gattStatus, final ResponseListener listener)
			{
				return new Please(EMPTY_BYTE_ARRAY, gattStatus, /*offset=*/0, listener);
			}
		}
		
		/**
		 * Called when a read or write from the client is requested.
		 */
		Please onEvent(final RequestEvent event);
	}

	/**
	 * Provide an instance to various static methods of {@link BleServer.RequestListener.Please} such as
	 * {@link BleServer.RequestListener.Please#respondWithSuccess(ResponseListener)}, or {@link BleServer#setListener_Response(ResponseListener)},
	 * or {@link BleManager#setListener_Response(ResponseListener)}.
	 */
	public static interface ResponseListener extends ExchangeListener
	{
		/**
		 * Struct passed to {@link {@link BleServer.ResponseListener#onEvent(ResponseEvent)}} that provides details
		 * about the original request that prompted the response, along with information on its success or failure.
		 */
		@Immutable
		public static class ResponseEvent extends ExchangeEvent implements UsesCustomNull
		{
			/**
			 * Returns the result of the response, or {@link BleServer.ResponseListener.Status#NO_RESPONSE_ATTEMPTED} if
			 * for example {@link BleServer.RequestListener.Please#doNotRespond(ResponseListener)} was used.
			 */
			public Status status()  {  return m_status;  }
			private final Status m_status;

			ResponseEvent(BleServer server, BluetoothDevice nativeDevice, UUID charUuid_in, UUID descUuid_in, Type type_in, Target target_in, byte[] data_in, int requestId, int offset, final boolean responseNeeded, final Status status)
			{
				super(server, nativeDevice, charUuid_in, descUuid_in, type_in, target_in, data_in, requestId, offset, responseNeeded);

				m_status = status;
			}

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
			 * {@link BleServer.RequestListener.Please#doNotRespond(ResponseListener)} (or overloads)
			 * were called or {@link RequestListener.RequestEvent#responseNeeded()} was <code>false</code>.
			 */
			NO_RESPONSE_ATTEMPTED,

			/**
			 * The underlying call to {@link BluetoothGattServer#sendResponse(BluetoothDevice, int, int, int, byte[])}
			 * failed for reasons unknown.
			 */
			FAILED_TO_SEND_OUT,

			NOT_CONNECTED;

			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Called when a response to a request is fulfilled or failed.
		 */
		void onEvent(final ResponseEvent event);
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
		 * Subclass that adds the server field.
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
			public String macAddress()  {  return m_nativeDevice.getAddress();  }
			private final BluetoothDevice m_nativeDevice;

			/**
			 * The change in gattStatus that may have precipitated the state change, or {@link BleDeviceConfig#GATT_STATUS_NOT_APPLICABLE}.
			 * For example if {@link #didEnter(State)} with {@link BleServerState#DISCONNECTED} is <code>true</code> and
			 * {@link #didExit(State)} with {@link BleServerState#CONNECTING} is also <code>true</code> then {@link #gattStatus()} may be greater
			 * than zero and give some further hint as to why the connection failed.
			 */
			public int gattStatus() {  return m_gattStatus;  }
			private final int m_gattStatus;

			StateEvent(BleServer server, BluetoothDevice nativeDevice, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
			{
				super(oldStateBits, newStateBits, intentMask);

				m_server = server;
				m_gattStatus = gattStatus;
				m_nativeDevice = nativeDevice;
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

	static final byte[]				EMPTY_BYTE_ARRAY	= new byte[0];

	final Object m_threadLock = new Object();
	private final P_ServerStateTracker m_stateTracker;
	private final BleManager m_mngr;
	private final P_TaskQueue m_queue;
	final P_BleServer_Listeners m_listeners;
	final P_NativeServerWrapper m_nativeWrapper;
	private RequestListener m_requestListener;
	private ResponseListener m_responseListener_default;
	private final P_Logger m_logger;
	private final boolean m_isNull;

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
			m_stateTracker.set(PA_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleServerState.NULL, true);
			m_listeners = null;
			m_nativeWrapper = null;
		}
		else
		{
			m_queue = m_mngr.getTaskQueue();
			m_logger = m_mngr.getLogger();
			m_stateTracker = new P_ServerStateTracker(this);
			m_stateTracker.set(PA_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleServerState.DISCONNECTED, true);
			m_listeners = new P_BleServer_Listeners(this);
			m_nativeWrapper = new P_NativeServerWrapper(this);
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

	/**
	 * This is a default catch-all convenience listener that will be called after any listener provided through
	 * the static methods of {@link BleServer.RequestListener.Please} such as {@link BleServer.RequestListener.Please#respondWithSuccess(ResponseListener)}.
	 *
	 * @see BleManager#setListener_Response(ResponseListener)
	 */
	public void setListener_Response(final ResponseListener listener)
	{
		m_responseListener_default = listener;
	}
	
	public BluetoothGattServer openGattServer( final Context context, final List<BluetoothGattService> gattServices, RequestListener listener )
	{
		m_requestListener = listener;
		m_serverNative = m_btMngr.openGattServer( context, m_listeners );

		if ( null != gattServices )
		{
    		for ( BluetoothGattService service : gattServices ) {
    			m_serverNative.addService( service );
    		}
		}
		m_nativeWrapper.setNative(m_serverNative);
		return m_serverNative;
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseListener.ResponseEvent notify( final String macAddress, UUID charUuid, byte[] data )
	{
		return notify(macAddress, null, charUuid, data, (ResponseListener) null);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseListener.ResponseEvent notify( final String macAddress, UUID charUuid, byte[] data, ResponseListener listener)
	{
		return notify(macAddress, (UUID) null, charUuid, data, listener);
	}

	public @Nullable(Nullable.Prevalence.NEVER) ResponseListener.ResponseEvent notify( final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data )
	{
		return notify(macAddress, serviceUuid, charUuid, data, (ResponseListener) null);
	}

	/**
	 * Use this method to send a notification to the client device with the given mac address to the given characteristic {@link UUID}.
	 * If there is any kind of "early-out" issue then this method will return a {@link BleServer.ResponseListener.ResponseEvent} in addition
	 * to passing it through the listener. Otherwise this method will return an instance with {@link ResponseListener.ResponseEvent#isNull()} being
	 * <code>true</code>.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) ResponseListener.ResponseEvent notify( final String macAddress, UUID serviceUuid, UUID charUuid, byte [] data, ResponseListener listener )
	{
//		m_queue.add(new P_Task_SendNotification(this, device, m_serverNative.getService(serviceUuid).getCharacteristic(charaUuid), data, listener, m_taskStateListener, true));

		return null;
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
	 * Returns the bitwise state mask representation of {@link BleServerState} for this device.
	 * 
	 * @see BleServerState
	 */
	@Advanced
	public int getStateMask()
	{
		return m_stateTracker.getState();
	}

	/**
	 * Returns this server's manager.
	 */
	public BleManager getManager()
	{
		return m_mngr;
	}

	public void connect(final String macAddress)
	{

	}

	public void disconnect(final String macAddress)
	{

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

		m_queue.add( new P_Task_DisconnectServer( this, m_taskStateListener ) );
	}

	@Override public boolean isNull()
	{
		return m_isNull;
	}



	void onNativeConnect() {
		
	}

	void onNativeDisconnect( boolean explicit )
	{
		m_serverNative.close();
	}

	void setNativeDevice( BluetoothDevice device )
	{
		m_device = m_mngr.newDevice(device.getAddress());
	}
	
}
