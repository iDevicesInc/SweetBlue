package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.database.Cursor;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.EmptyCursor;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataColumn;
import com.idevicesinc.sweetblue.utils.HistoricalDataQuery;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;


/**
 * Abstract base class for {@link BleDevice} and {@link BleServer}, mostly just to statically tie their APIs together
 * wherever possible. That is, not much actual shared implementation exists in this class as of this writing.
 */
public abstract class BleNode implements UsesCustomNull
{
	/**
	 * Base interface for {@link BleDevice.ConnectionFailListener} and {@link BleServer.ConnectionFailListener}.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ConnectionFailListener
	{
		/**
		 * Describes usage of the <code>autoConnect</code> parameter for either {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}
		 * or {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}.
		 */
		@com.idevicesinc.sweetblue.annotations.Advanced
		public static enum AutoConnectUsage
		{
			/**
			 * Used when we didn't start the connection process, i.e. it came out of nowhere. Rare case but can happen, for example after
			 * SweetBlue considers a connect timed out based on {@link BleNodeConfig#taskTimeoutRequestFilter} but then it somehow
			 * does come in (shouldn't happen but who knows).
			 */
			UNKNOWN,

			/**
			 * Usage is not applicable.
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
		 * Abstract base class for structures passed to {@link BleServer.ConnectionFailListener#onEvent(BleServer.ConnectionFailListener.ConnectionFailEvent)}
		 * and {@link BleDevice.ConnectionFailListener#onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent)} to provide more info about how/why a connection failed.
		 */
		@Immutable
		public abstract static class ConnectionFailEvent extends Event implements UsesCustomNull
		{
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
			 * If not applicable, for example if {@link BleDevice.ConnectionFailListener.ConnectionFailEvent#status()} is {@link BleDevice.ConnectionFailListener.Status#EXPLICIT_DISCONNECT},
			 * then this is set to {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
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

			ConnectionFailEvent(int failureCountSoFar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, AutoConnectUsage autoConnectUsage)
			{
				this.m_failureCountSoFar = failureCountSoFar;
				this.m_latestAttemptTime = latestAttemptTime;
				this.m_totalAttemptTime = totalAttemptTime;
				this.m_gattStatus = gattStatus;
				this.m_autoConnectUsage = autoConnectUsage;
			}
		}

		/**
		 * Return value for {@link BleDevice.ConnectionFailListener#onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent)}
		 * and {@link BleServer.ConnectionFailListener#onEvent(BleServer.ConnectionFailListener.ConnectionFailEvent)}.
		 * Generally you will only return {@link #retry()} or {@link #doNotRetry()}, but there are more advanced options as well.
		 */
		@Immutable
		public static class Please
		{
			/*package*/ static final int PE_Please_NULL								= -1;
			/*package*/ static final int PE_Please_RETRY							=  0;
			/*package*/ static final int PE_Please_RETRY_WITH_AUTOCONNECT_TRUE		=  1;
			/*package*/ static final int PE_Please_RETRY_WITH_AUTOCONNECT_FALSE		=  2;
			/*package*/ static final int PE_Please_DO_NOT_RETRY						=  3;

			/*package*/ static final boolean isRetry(final int please__PE_Please)
			{
				return please__PE_Please != PE_Please_DO_NOT_RETRY && please__PE_Please != PE_Please_NULL;
			}

			private final int m_please__PE_Please;

			private Please(final int please__PE_Please)
			{
				m_please__PE_Please = please__PE_Please;
			}

			/*package*/ int/*__PE_Please*/ please()
			{
				return m_please__PE_Please;
			}

			/**
			 * Return this to retry the connection, continuing the connection fail retry loop. <code>autoConnect</code> passed to
			 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}
			 * will be false or true based on what has worked in the past, or on {@link BleDeviceConfig#alwaysUseAutoConnect}.
			 */
			public static Please retry()
			{
				return new Please(PE_Please_RETRY);
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
				return new Please(PE_Please_DO_NOT_RETRY);
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
			 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}.
			 * See more discussion at {@link BleDeviceConfig#alwaysUseAutoConnect}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public static Please retryWithAutoConnectTrue()
			{
				return new Please(PE_Please_RETRY_WITH_AUTOCONNECT_TRUE);
			}

			/**
			 * Opposite of{@link #retryWithAutoConnectTrue()}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public static Please retryWithAutoConnectFalse()
			{
				return new Please(PE_Please_RETRY_WITH_AUTOCONNECT_FALSE);
			}

			/**
			 * Returns <code>true</code> for everything except {@link #doNotRetry()}.
			 */
			public boolean isRetry()
			{
				return isRetry(m_please__PE_Please);
			}
		}
	}

	/**
	 * A callback that is used by various overloads of {@link BleDevice#loadHistoricalData()} that accept instances hereof.
	 * You can also set default listeners on {@link BleDevice#setListener_HistoricalDataLoad(BleNode.HistoricalDataLoadListener)}
	 * and {@link BleManager#setListener_HistoricalDataLoad(BleNode.HistoricalDataLoadListener)}.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface HistoricalDataLoadListener extends GenericListener_Void<HistoricalDataLoadListener.HistoricalDataLoadEvent>
	{
		/**
		 * Enumerates the status codes for operations kicked off from {@link BleDevice#loadHistoricalData()} (or overloads).
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Fulfills soft contract of {@link UsesCustomNull}.
			 */
			NULL,

			/**
			 * Historical data is fully loaded to memory and ready to access synchronously (without blocking current thread)
			 * through {@link BleDevice#getHistoricalData_iterator(UUID)} (or overloads).
			 */
			LOADED,

			/**
			 * {@link BleDevice#loadHistoricalData()} (or overloads) was called but the data was already loaded to memory.
			 */
			ALREADY_LOADED,

			/**
			 * {@link BleDevice#loadHistoricalData()} (or overloads) was called but there was no data available to load to memory.
			 */
			NOTHING_TO_LOAD,

			/**
			 * {@link BleDevice#loadHistoricalData()} (or overloads) was called and the operation was successfully started -
			 * expect another {@link HistoricalDataLoadEvent} with {@link HistoricalDataLoadEvent#status()} being {@link #LOADED} shortly.
			 */
			STARTED_LOADING,

			/**
			 * Same idea as {@link #STARTED_LOADING}, not an error status, but letting you know that the load was already in progress
			 * when {@link BleDevice#loadHistoricalData()} (or overloads) was called a second time. This doesn't
			 * affect the actual loading process at all, and {@link #LOADED} will eventually be returned for both callbacks.
			 */
			ALREADY_LOADING;

			/**
			 * Returns true if <code>this==</code> {@link #NULL}.
			 */
			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Event struct passed to {@link HistoricalDataLoadListener#onEvent(HistoricalDataLoadEvent)} that provides
		 * further information about the status of a historical data load to memory using {@link BleDevice#loadHistoricalData()}
		 * (or overloads).
		 */
		@com.idevicesinc.sweetblue.annotations.Immutable
		public static class HistoricalDataLoadEvent extends Event
		{
			/**
			 * The mac address that the data is being queried for.
			 */
			public String macAddress() {  return m_macAddress; }
			private final String m_macAddress;

			/**
			 * The {@link UUID} that the data is being loaded for.
			 */
			public UUID uuid() {  return m_uuid;  }
			private final UUID m_uuid;

			/**
			 * The resulting time range spanning all of the data loaded to memory, or {@link EpochTimeRange#NULL} if not applicable.
			 */
			public EpochTimeRange range() {  return m_range; }
			private final EpochTimeRange m_range;

			/**
			 * The general status of the load operation.
			 */
			public Status status() {  return m_status; }
			private final Status m_status;

			private final BleNode m_endpoint;

			HistoricalDataLoadEvent(final BleNode endpoint, final String macAddress, final UUID uuid, final EpochTimeRange range, final Status status)
			{
				m_endpoint = endpoint;
				m_macAddress = macAddress;
				m_uuid = uuid;
				m_range = range;
				m_status = status;
			}

			/**
			 * Returns <code>true</code> if {@link #status()} is either {@link HistoricalDataLoadListener.Status#LOADED} or
			 *  {@link HistoricalDataLoadListener.Status#ALREADY_LOADED}.
			 */
			public boolean wasSuccess()
			{
				return status() == Status.LOADED || status() == Status.ALREADY_LOADED;
			}

			@Override public String toString()
			{
				return Utils_String.toString
				(
					this.getClass(),
					"macAddress", macAddress(),
					"uuid", m_endpoint.getManager().getLogger().uuidName(uuid()),
					"status", status()
				);
			}
		}

		/**
		 * Called when the historical data for a given characteristic {@link UUID} is done loading from disk.
		 */
		void onEvent(final HistoricalDataLoadEvent e);
	}

	/**
	 * A callback that is used by {@link BleDevice#select()} to listen for when a database query is done processing.
	 */
	@com.idevicesinc.sweetblue.annotations.Alpha
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface HistoricalDataQueryListener extends GenericListener_Void<HistoricalDataQueryListener.HistoricalDataQueryEvent>
	{
		/**
		 * Enumerates the status codes for operations kicked off from {@link BleDevice#select()}.
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Fulfills soft contract of {@link UsesCustomNull}.
			 */
			NULL,

			/**
			 * Tried to query historical data on {@link BleDevice#NULL} or {@link BleServer#NULL}.
			 */
			NULL_ENDPOINT,

			/**
			 * Query completed successfully - {@link HistoricalDataQueryEvent#cursor()} may be empty but there were no exceptions or anything.
			 */
			SUCCESS,

			/**
			 * There is no backing table for the given {@link UUID}.
			 */
			NO_TABLE,

			/**
			 * General failure - this feature is still in {@link com.idevicesinc.sweetblue.annotations.Alpha} so expect more detailed error statuses in the future.
			 */
			ERROR;

			/**
			 * Returns true if <code>this==</code> {@link #NULL}.
			 */
			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Event struct passed to {@link HistoricalDataQueryListener#onEvent(HistoricalDataQueryEvent)} that provides
		 * further information about the status of a historical data load to memory using {@link BleDevice#loadHistoricalData()}
		 * (or overloads).
		 */
		@com.idevicesinc.sweetblue.annotations.Immutable
		public static class HistoricalDataQueryEvent extends Event
		{
			/**
			 * The {@link UUID} that the data is being queried for.
			 */
			public UUID uuid() {  return m_uuid;  }
			private final UUID m_uuid;

			/**
			 * The general status of the query operation.
			 */
			public Status status() {  return m_status; }
			private final Status m_status;

			/**
			 * The resulting {@link Cursor} from the database query. This will never be null, just an empty cursor if anything goes wrong.
			 */
			public @Nullable(Nullable.Prevalence.NEVER) Cursor cursor() {  return m_cursor; }
			private final Cursor m_cursor;

			/**
			 * The raw query given to the database.
			 */
			public @Nullable(Nullable.Prevalence.NEVER) String rawQuery() {  return m_rawQuery; }
			private final String m_rawQuery;

			private final BleNode m_endpoint;

			public HistoricalDataQueryEvent(final BleNode endpoint, final UUID uuid, final Cursor cursor, final Status status, final String rawQuery)
			{
				m_endpoint = endpoint;
				m_uuid = uuid;
				m_cursor = cursor;
				m_status = status;
				m_rawQuery = rawQuery;
			}

			/**
			 * Returns <code>true</code> if {@link #status()} is {@link HistoricalDataQueryListener.Status#SUCCESS}.
			 */
			public boolean wasSuccess()
			{
				return status() == Status.SUCCESS;
			}

			@Override public String toString()
			{
				return Utils_String.toString
				(
					this.getClass(),
					"uuid",				m_endpoint.getManager().getLogger().uuidName(uuid()),
					"status",			status()
				);
			}
		}

		/**
		 * Called when the historical data for a given characteristic {@link UUID} is done querying.
		 */
		void onEvent(final HistoricalDataQueryEvent e);
	}


	/**
	 * Field for app to associate any data it wants with instances of this class
	 * instead of having to subclass or manage associative hash maps or something.
	 * The library does not touch or interact with this data in any way.
	 *
	 * @see BleManager#appData
	 * @see BleServer#appData
	 */
	public Object appData;

	private final BleManager m_manager;

	//--- DRK > Can't be final cause can't reference subclass 'this' while calling super() constructor.
	private final PA_ServiceManager m_serviceMngr;

	protected BleNode(final BleManager manager)
	{
		m_manager = manager;
		m_serviceMngr = newServiceManager();
	}

	protected abstract PA_ServiceManager newServiceManager();
	
	protected <T extends PA_ServiceManager> T getServiceManager()
	{
		return (T) m_serviceMngr;
	}

	/**
	 * Overload of {@link #getNativeDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
	 * matching the given {@link UUID}.
	 *
	 * @deprecated Use {@link #getNativeBleDescriptor(UUID)} instead.
	 */
	@Deprecated
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor getNativeDescriptor(final UUID descUuid)
	{
		return getNativeBleDescriptor(descUuid).getDescriptor();
	}

	/**
	 * Overload of {@link #getNativeDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
	 * matching the given {@link UUID}.
	 *
	 * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptorWrapper#isNull()} to check if the {@link BluetoothGattDescriptor}
	 * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
	 */
	public @Nullable(Nullable.Prevalence.NEVER)
	BleDescriptorWrapper getNativeBleDescriptor(final UUID descUuid)
	{
		return getNativeBleDescriptor(null, null, descUuid);
	}

	/**
	 * Overload of {@link #getNativeDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
	 * inside the given characteristic matching the given {@link UUID}.
	 *
	 * @deprecated Use {@link #getNativeBleDescriptor_inChar(UUID, UUID)} instead.
	 */
	@Deprecated
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor getNativeDescriptor_inChar(final UUID charUuid, final UUID descUuid)
	{
		return getNativeBleDescriptor_inChar(charUuid, descUuid).getDescriptor();
	}

	/**
	 * Overload of {@link #getNativeDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
	 * inside the given characteristic matching the given {@link UUID}.
	 *
	 * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptorWrapper#isNull()} to check if the {@link BluetoothGattDescriptor}
	 * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
	 */
	public @Nullable(Nullable.Prevalence.NEVER)
	BleDescriptorWrapper getNativeBleDescriptor_inChar(final UUID charUuid, final UUID descUuid)
	{
		return getNativeBleDescriptor(null, charUuid, descUuid);
	}

	/**
	 * Overload of {@link #getNativeDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
	 * inside the given service matching the given {@link UUID}.
	 *
	 * @deprecated Use {@link #getNativeBleDescriptor_inService(UUID, UUID)} instead.
	 */
	@Deprecated
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor getNativeDescriptor_inService(final UUID serviceUuid, final UUID descUuid)
	{
		return getNativeBleDescriptor_inService(serviceUuid, descUuid).getDescriptor();
	}

	/**
	 * Overload of {@link #getNativeDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
	 * inside the given service matching the given {@link UUID}.
	 *
	 * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptorWrapper#isNull()} to check if the {@link BluetoothGattDescriptor}
	 * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
	 */
	public @Nullable(Nullable.Prevalence.NEVER)
	BleDescriptorWrapper getNativeBleDescriptor_inService(final UUID serviceUuid, final UUID descUuid)
	{
		return getNativeBleDescriptor(serviceUuid, null, descUuid);
	}

	/**
	 * Returns the native descriptor for the given UUID in case you need lower-level access.
	 *
	 * @deprecated - Use {@link #getNativeBleDescriptor(UUID, UUID, UUID)} instead.
	 */
	@Deprecated
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor getNativeDescriptor(final UUID serviceUuid, final UUID charUuid, final UUID descUuid)
	{
		return getNativeBleDescriptor(serviceUuid, charUuid, descUuid).getDescriptor();
	}

	/**
	 * Returns the {@link BleDescriptorWrapper} for the given UUID in case you need lower-level access.
	 *
	 * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptorWrapper#isNull()} to check if the {@link BluetoothGattDescriptor}
	 * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
	 */
	public @Nullable(Nullable.Prevalence.NEVER)
	BleDescriptorWrapper getNativeBleDescriptor(final UUID serviceUuid, final UUID charUuid, final UUID descUuid)
	{
		return m_serviceMngr.getDescriptor(serviceUuid, charUuid, descUuid);
	}

	/**
	 * Returns the native characteristic for the given UUID in case you need lower-level access.
	 *
	 * @deprecated Use {@link #getNativeBleCharacteristic(UUID)} instead.
	 */
	@Deprecated
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattCharacteristic getNativeCharacteristic(final UUID charUuid)
	{
		return getNativeBleCharacteristic(charUuid).getCharacteristic();
	}

	/**
	 * Returns the native characteristic for the given UUID in case you need lower-level access.
	 *
	 * Note that this will never return a <code>null</code> instance. You need to call {@link BleCharacteristicWrapper#isNull()} to check if the {@link BluetoothGattCharacteristic}
	 * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested characteristic).
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NEVER)
	BleCharacteristicWrapper getNativeBleCharacteristic(final UUID charUuid)
	{
		return getNativeBleCharacteristic(null, charUuid);
	}

	/**
	 * Overload of {@link #getNativeCharacteristic(UUID)} for when you have characteristics with identical uuids under different services.
	 *
	 * @deprecated Use {@link #getNativeBleCharacteristic(UUID, UUID)} instead.
	 */
	@Deprecated
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattCharacteristic getNativeCharacteristic(final UUID serviceUuid, final UUID charUuid)
	{
		return getNativeBleCharacteristic(serviceUuid, charUuid).getCharacteristic();
	}

	/**
	 * Overload of {@link #getNativeCharacteristic(UUID)} for when you have characteristics with identical uuids under different services.
	 *
	 * Note that this will never return a <code>null</code> instance. You need to call {@link BleCharacteristicWrapper#isNull()} to check if the {@link BluetoothGattCharacteristic}
	 * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested characteristic).
	 */
	public @Nullable(Nullable.Prevalence.NEVER)
	BleCharacteristicWrapper getNativeBleCharacteristic(final UUID serviceUuid, final UUID charUuid)
	{
		return m_serviceMngr.getCharacteristic(serviceUuid, charUuid);
	}

	/**
	 * Overload of {@link #getNativeCharacteristic(UUID, UUID)} for when you have characteristics with identical uuids within the same service.
	 */
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattCharacteristic getNativeCharacteristic(final UUID serviceUuid, final UUID charUuid, final DescriptorFilter descriptorFilter)
	{
		return m_serviceMngr.getCharacteristic(serviceUuid, charUuid, descriptorFilter).getCharacteristic();
	}

	/**
	 * Returns the native service for the given UUID in case you need lower-level access.
	 *
	 * @deprecated Use {@link #getNativeBleService(UUID)} instead.
	 */
	@Deprecated
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattService getNativeService(final UUID serviceUuid)
	{
		return getNativeBleService(serviceUuid).getService();
	}

	/**
	 * Returns the native service for the given UUID in case you need lower-level access.
	 *
	 * Note that this will never return a <code>null</code> instance. You need to call {@link BleServiceWrapper#isNull()} to check if the {@link BluetoothGattService}
	 * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested service).
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NEVER)
	BleServiceWrapper getNativeBleService(final UUID serviceUuid)
	{
		return m_serviceMngr.getServiceDirectlyFromNativeNode(serviceUuid);
	}

	/**
	 * Returns all {@link BluetoothGattService} instances.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattService> getNativeServices()
	{
		return m_serviceMngr.getServices();
	}

	/**
	 * Convenience overload of {@link #getNativeServices()} that returns a {@link List}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattService> getNativeServices_List()
	{
		return m_serviceMngr.getServices_List();
	}

	/**
	 * Overload of {@link #getNativeServices()} that uses a for each construct instead of returning an iterator.
	 */
	public void getNativeServices(final ForEach_Void<BluetoothGattService> forEach)
	{
		m_serviceMngr.getServices(forEach);
	}

	/**
	 * Overload of {@link #getNativeServices()} that uses a for each construct instead of returning an iterator.
	 */
	public void getNativeServices(final ForEach_Breakable<BluetoothGattService> forEach)
	{
		m_serviceMngr.getServices(forEach);
	}

	/**
	 * Overload of {@link #getNativeCharacteristics()} that uses a for each construct instead of returning an iterator.
	 */
	public void getNativeCharacteristics(final ForEach_Void<BluetoothGattCharacteristic> forEach)
	{
		m_serviceMngr.getCharacteristics(null, forEach);
	}

	/**
	 * Overload of {@link #getNativeCharacteristics()} that uses a for each construct instead of returning an iterator.
	 */
	public void getNativeCharacteristics(final ForEach_Breakable<BluetoothGattCharacteristic> forEach)
	{
		m_serviceMngr.getCharacteristics(null, forEach);
	}

	/**
	 * Overload of {@link #getNativeCharacteristics(UUID)} that uses a for each construct instead of returning an iterator.
	 */
	public void getNativeCharacteristics(final UUID serviceUuid, final ForEach_Void<BluetoothGattCharacteristic> forEach)
	{
		m_serviceMngr.getCharacteristics(serviceUuid, forEach);
	}

	/**
	 * Overload of {@link #getNativeCharacteristics(UUID)} that uses a for each construct instead of returning an iterator.
	 */
	public void getNativeCharacteristics(final UUID serviceUuid, final ForEach_Breakable<BluetoothGattCharacteristic> forEach)
	{
		m_serviceMngr.getCharacteristics(serviceUuid, forEach);
	}

	/**
	 * Returns all {@link BluetoothGattCharacteristic} instances.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattCharacteristic> getNativeCharacteristics()
	{
		return m_serviceMngr.getCharacteristics(null);
	}

	/**
	 * Convenience overload of {@link #getNativeCharacteristics()} that returns a {@link List}.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattCharacteristic> getNativeCharacteristics_List()
	{
		return m_serviceMngr.getCharacteristics_List(null);
	}

	/**
	 * Same as {@link #getNativeCharacteristics()} but you can filter on the service {@link UUID}.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattCharacteristic> getNativeCharacteristics(UUID serviceUuid)
	{
		return m_serviceMngr.getCharacteristics(serviceUuid);
	}

	/**
	 * Convenience overload of {@link #getNativeCharacteristics(UUID)} that returns a {@link List}.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattCharacteristic> getNativeCharacteristics_List(UUID serviceUuid)
	{
		return m_serviceMngr.getCharacteristics_List(serviceUuid);
	}

	/**
	 * Returns all descriptors on this node.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattDescriptor> getNativeDescriptors()
	{
		return m_serviceMngr.getDescriptors(null, null);
	}

	/**
	 * Returns all descriptors on this node as a list.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattDescriptor> getNativeDescriptors_List()
	{
		return m_serviceMngr.getDescriptors_List(null, null);
	}

	/**
	 * Returns all descriptors on this node in the given service.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattDescriptor> getNativeDescriptors_inService(final UUID serviceUuid)
	{
		return m_serviceMngr.getDescriptors(serviceUuid, null);
	}

	/**
	 * Returns all descriptors on this node in the given service as a list.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattDescriptor> getNativeDescriptors_inService_List(final UUID serviceUuid)
	{
		return m_serviceMngr.getDescriptors_List(serviceUuid, null);
	}

	/**
	 * Returns all descriptors on this node in the given characteristic.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattDescriptor> getNativeDescriptors_inChar(final UUID charUuid)
	{
		return m_serviceMngr.getDescriptors(null, charUuid);
	}

	/**
	 * Returns all descriptors on this node in the given characteristic as a list.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattDescriptor> getNativeDescriptors_inChar_List(final UUID charUuid)
	{
		return m_serviceMngr.getDescriptors_List(null, charUuid);
	}

	/**
	 * Returns all descriptors on this node in the given characteristic.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) Iterator<BluetoothGattDescriptor> getNativeDescriptors(final UUID serviceUuid, final UUID charUuid)
	{
		return m_serviceMngr.getDescriptors(serviceUuid, charUuid);
	}

	/**
	 * Returns all descriptors on this node in the given characteristic as a list.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) List<BluetoothGattDescriptor> getNativeDescriptors_List(final UUID serviceUuid, final UUID charUuid)
	{
		return m_serviceMngr.getDescriptors_List(serviceUuid, charUuid);
	}

	/**
	 * Overload of {@link BleNode#getNativeDescriptors()} using a for each construct.
	 */
	public void getNativeDescriptors(final ForEach_Void<BluetoothGattDescriptor> forEach)
	{
		m_serviceMngr.getDescriptors(null, null, forEach);
	}

	/**
	 * Overload of {@link BleNode#getNativeDescriptors()} using a for each construct.
	 */
	public void getNativeDescriptors(final ForEach_Breakable<BluetoothGattDescriptor> forEach)
	{
		m_serviceMngr.getDescriptors(null, null, forEach);
	}

	/**
	 * Overload of {@link BleNode#getNativeDescriptors_inService(UUID)} using a for each construct.
	 */
	public void getNativeDescriptors_inService(final UUID serviceUuid, final ForEach_Void<BluetoothGattDescriptor> forEach)
	{
		m_serviceMngr.getDescriptors(serviceUuid, null, forEach);
	}

	/**
	 * Overload of {@link BleNode#getNativeDescriptors_inService(UUID)} using a for each construct.
	 */
	public void getNativeDescriptors_inService(final UUID serviceUuid, final ForEach_Breakable<BluetoothGattDescriptor> forEach)
	{
		m_serviceMngr.getDescriptors(serviceUuid, null, forEach);
	}

	/**
	 * Overload of {@link BleNode#getNativeDescriptors_inChar(UUID)} using a for each construct.
	 */
	public void getNativeDescriptors_inChar(final UUID charUuid, final ForEach_Void<BluetoothGattDescriptor> forEach)
	{
		m_serviceMngr.getDescriptors(null, charUuid, forEach);
	}

	/**
	 * Overload of {@link BleNode#getNativeDescriptors_inChar(UUID)} using a for each construct.
	 */
	public void getNativeDescriptors_inChar(final UUID charUuid, final ForEach_Breakable<BluetoothGattDescriptor> forEach)
	{
		m_serviceMngr.getDescriptors(null, charUuid, forEach);
	}

	/**
	 * Overload of {@link BleNode#getNativeDescriptors(UUID, UUID)} using a for each construct.
	 */
	public void getNativeDescriptors(final UUID serviceUuid, final UUID charUuid, final ForEach_Void<BluetoothGattDescriptor> forEach)
	{
		m_serviceMngr.getDescriptors(serviceUuid, charUuid, forEach);
	}

	/**
	 * Overload of {@link BleNode#getNativeDescriptors(UUID, UUID)} using a for each construct.
	 */
	public void getNativeDescriptors(final UUID serviceUuid, final UUID charUuid, final ForEach_Breakable<BluetoothGattDescriptor> forEach)
	{
		m_serviceMngr.getDescriptors(serviceUuid, charUuid, forEach);
	}

	/**
	 * Returns a new {@link com.idevicesinc.sweetblue.utils.HistoricalData} instance using
	 * {@link com.idevicesinc.sweetblue.BleDeviceConfig#historicalDataFactory} if available.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
	{
		final BleDeviceConfig.HistoricalDataFactory factory_device = conf_node().historicalDataFactory;
		final BleDeviceConfig.HistoricalDataFactory factory_mngr = conf_mngr().historicalDataFactory;
		final BleDeviceConfig.HistoricalDataFactory factory = factory_device != null ? factory_device : factory_mngr;

		if( factory != null )
		{
			return factory.newHistoricalData(data, epochTime);
		}
		else
		{
			return new HistoricalData(data, epochTime);
		}
	}

	/**
	 * Returns this endpoint's manager.
	 */
	public BleManager getManager()
	{
		if (isNull())
		{
			return BleManager.s_instance;
		}
		else
		{
			return m_manager;
		}
	}

	/**
	 * Convenience method that casts {@link #appData} for you.
	 */
	public <T> T appData()
	{
		return (T) appData;
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

	abstract BleNodeConfig conf_node();

	P_TaskQueue queue()
	{
		return getManager().getTaskQueue();
	}

	P_Logger logger()
	{
		return getManager().getLogger();
	}

	/**
	 * Provides a means to perform a raw SQL query on the database storing the historical data for this node. Use {@link BleDevice#getHistoricalDataTableName(UUID)}
	 * to generate table names and {@link HistoricalDataColumn} to get column names.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) HistoricalDataQueryListener.HistoricalDataQueryEvent queryHistoricalData(final String query)
	{
		if( this.isNull() )
		{
			return new HistoricalDataQueryListener.HistoricalDataQueryEvent(this, Uuids.INVALID, new EmptyCursor(), HistoricalDataQueryListener.Status.NULL_ENDPOINT, query);
		}
		else
		{
			final Cursor cursor = this.getManager().m_historicalDatabase.query(query);

			return new BleDevice.HistoricalDataQueryListener.HistoricalDataQueryEvent(this, Uuids.INVALID, cursor, BleDevice.HistoricalDataQueryListener.Status.SUCCESS, query);
		}
	}

	/**
	 * Same as {@link #queryHistoricalData(String)} but performs the query on a background thread and returns the result back on the main thread
	 * through the provided {@link BleNode.HistoricalDataQueryListener}.
	 */
	public void queryHistoricalData(final String query, final HistoricalDataQueryListener listener)
	{
		if( this.isNull() )
		{
			listener.onEvent(new HistoricalDataQueryListener.HistoricalDataQueryEvent(this, Uuids.INVALID, new EmptyCursor(), HistoricalDataQueryListener.Status.NULL_ENDPOINT, query));
		}
		else
		{
			P_HistoricalDataManager.post(new Runnable()
			{
				@Override public void run()
				{
					final BleDevice.HistoricalDataQueryListener.HistoricalDataQueryEvent e = queryHistoricalData(query);

					getManager().postEvent(listener, e);
				}
			});
		}
	}

	/**
	 * Provides a way to perform a statically checked SQL query by chaining method calls.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@com.idevicesinc.sweetblue.annotations.Alpha
	public @Nullable(Nullable.Prevalence.NEVER) HistoricalDataQuery.Part_Select select()
	{
		final HistoricalDataQuery.Part_Select select = HistoricalDataQuery.select(this, getManager().m_historicalDatabase);

		return select;
	}

	/**
	 * Just some sugar for casting to subclasses.
	 */
	public <T extends BleNode> T cast()
	{
		return (T) this;
	}

	/**
	 * Safer version of {@link #cast()} that will return {@link BleDevice#NULL} or {@link BleServer#NULL}
	 * if the cast cannot be made.
	 */
	public @Nullable(Nullable.Prevalence.NEVER) <T extends BleNode> T cast(final Class<T> type)
	{
		if( this instanceof BleDevice && type == BleServer.class )
		{
			return (T) BleServer.NULL;
		}
		else if( this instanceof BleServer && type == BleDevice.class )
		{
			return (T) BleDevice.NULL;
		}
		else
		{
			return cast();
		}
	}

	/**
	 * Returns the MAC address of the remote {@link BleDevice} or local {@link BleServer}.
	 */
	public abstract @Nullable(Nullable.Prevalence.NEVER) String getMacAddress();
}
