package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Extendable;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_JSONUtil;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.utils.WrongThreadError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Provides a number of options to (optionally) pass to {@link BleServer#setConfig(BleNodeConfig)}.
 * This class is also a super class of {@link BleManagerConfig}, which you can pass
 * to {@link BleManager#get(Context, BleManagerConfig)} or {@link BleManager#setConfig(BleManagerConfig)} to set default base options for all servers at once.
 * For all options in this class, you may set the value to <code>null</code> when passed to {@link BleServer#setConfig(BleNodeConfig)}
 * and the value will then be inherited from the {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}.
 * Otherwise, if the value is not <code>null</code> it will override any option in the {@link BleManagerConfig}.
 * If an option is ultimately <code>null</code> (<code>null</code> when passed to {@link BleServer#setConfig(BleNodeConfig)}
 * *and* {@link BleManager#get(Context, BleManagerConfig)}) then it is interpreted as <code>false</code> or {@link Interval#DISABLED}.
 * <br><br>
 * TIP: You can use {@link Interval#DISABLED} instead of <code>null</code> to disable any time-based options, for code readability's sake.
 * <br><br>
 * TIP: You can use {@link #newNulled()} (or {@link #nullOut()}) then only set the few options you want for {@link BleServer#setConfig(BleNodeConfig)}.
 */
@Extendable
public class BleNodeConfig
{
	/**
	 * The default MTU size in bytes for gatt reads/writes/notifies/etc.
	 */
	public static final int DEFAULT_MTU_SIZE							= 23;

	/**
	 * The overhead in bytes that is subtracted from the total mtu size (e.g. {@link #DEFAULT_MTU_SIZE}) to
	 * give you the effective payload size that your application can send. For Android this
	 * payload size is almost always 23-3=20 bytes.
	 */
	public static final int GATT_WRITE_MTU_OVERHEAD = 3;

	/**
	 * The overhead in bytes that is subtracted from the total mtu size (e.g. {@link #DEFAULT_MTU_SIZE}) to
	 * give you the effective payload size that your application can receive. For Android this
	 * payload size is almost always 23-1=22 bytes.
	 */
	public static final int GATT_READ_MTU_OVERHEAD = 1;

	/**
	 * Constant for an invalid or unknown transmission power.
	 *
	 * @see BleDevice#getTxPower()
	 * @see BleDeviceConfig#defaultTxPower
	 */
	public static final int INVALID_TX_POWER							= Integer.MIN_VALUE;

	/**
	 * The default size of the list that keeps track of a {@link BleNode}'s connection failure history. This is to prevent
	 * the list from growing too large, if the device is unable to connect, and you have a large long term reconnect time set
	 * with {@link #reconnectFilter}.
	 */
	public static final int DEFAULT_MAX_CONNECTION_FAIL_HISTORY_SIZE	= 25;

	/**
	 * Default is <code>false</code> - see the <code>boolean autoConnect</code> parameters of
	 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}
	 * and {@link android.bluetooth.BluetoothGattServer#connect(BluetoothDevice, boolean)}.
	 * <br><br>
	 * This parameter is one of Android's deepest mysteries. By default we keep it false, but it has been observed that a
	 * connection can fail or time out, but then if you try again with autoConnect set to true it works! One would think,
	 * why not always set it to true? Well, while true is anecdotally more stable, it also (anecdotally) makes for longer
	 * connection times, which becomes a UX problem. Would you rather have a 5-10 second connection process that is successful
	 * with 99% of devices, or a 1-2 second connection process that is successful with 95% of devices? By default we've chosen the latter.
	 * <br><br>
	 * HOWEVER, it's important to note that you can have fine-grained control over its usage through the {@link BleNode.ConnectionFailListener.Please}
	 * returned from {@link BleDevice.ConnectionFailListener#onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent)} (or the equivalent
	 * structures that are inner structures of {@link BleServer}).
	 * <br><br>
	 * So really this option mainly exists for those situations where you KNOW that you have a device or server that only works
	 * with autoConnect==true and you want connection time to be faster (i.e. you don't want to wait for that first
	 * failed connection for the library to internally start using autoConnect==true).
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Nullable.Prevalence.NORMAL)
	public Boolean alwaysUseAutoConnect										= false;

	/**
	 * Default is <code>false</code> - SweetBlue, for the sake of performance, stability, and simplicity, requires all back and forth to take place on the main thread,
	 * with {@link WrongThreadError} thrown if not.
	 * Versions less than v2 did not enforce this, but feedback indicated that the threading model was unclear and some people would kick
	 * off SweetBlue operations on alternate threads, which could definitely lead to problems. This remains as an option to help older code bases transitioning to >= v2
	 *
	 *
	 * @deprecated - This value is not used anymore. Just left here so we don't cause build errors.
	 */
	@Nullable(Nullable.Prevalence.RARE)
	@Deprecated
	public Boolean allowCallsFromAllThreads									= false;

	/**
	 * Default is <code>true</code> - controls whether the library is allowed to optimize fast disconnect/reconnect cycles
	 * by actually not disconnecting in the native stack at all. For example, if this option is <code>true</code> and your
	 * {@link BleDevice} is {@link BleDeviceState#CONNECTED}, calling {@link BleDevice#disconnect()} then {@link BleDevice#connect()}
	 * again won't result in a native disconnect/reconnect - your actual physical ble device firmware won't know that a disconnect was requested.
	 */
	@Nullable(Nullable.Prevalence.NORMAL)
	public Boolean disconnectIsCancellable									= true;

	/**
	 * Default is <code>true</code> - this will automatically stripe writes that are larger than the MTU size into multiple WRITE requests for you.
	 * If you are using {@link BleDevice#setMtu(int)}, this may make things unstable.
	 */
	public boolean autoStripeWrites											= true;

	/**
	 * Default is an instance of {@link DefaultTaskTimeoutRequestFilter} - set an implementation here to
	 * have fine control over how long individual {@link BleTask} instances can take before they
	 * are considered "timed out" and failed.
	 * <br><br>
	 * NOTE: Setting this to <code>null</code> will disable timeouts for all {@link BleTask} instances,
	 * which would probably be very dangerous to do - a task could just sit there spinning forever.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Nullable.Prevalence.RARE)
	public TaskTimeoutRequestFilter taskTimeoutRequestFilter				= new DefaultTaskTimeoutRequestFilter();

	/**
	 * Default is an instance of {@link BleNodeConfig.DefaultHistoricalDataLogFilter} -
	 * set an implementation here to control how/if data is logged.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Nullable.Prevalence.NORMAL)
	public HistoricalDataLogFilter historicalDataLogFilter					= new DefaultHistoricalDataLogFilter();

	/**
	 * Implement this to override the default behavior, which is simply to return an instance created with
	 * the constructor {@link HistoricalData#HistoricalData(byte[], com.idevicesinc.sweetblue.utils.EpochTime)}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Nullable.Prevalence.NORMAL)
	public HistoricalDataFactory historicalDataFactory						= new HistoricalDataFactory()
	{
		@Override public HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
		{
			return new HistoricalData(data, epochTime);
		}
	};

	/**
	 * Default is an instance of {@link DefaultReconnectFilter} using the timings that are <code>public static final</code> members thereof - set your own implementation here to
	 * have fine-grain control over reconnect behavior while a device is {@link BleDeviceState#RECONNECTING_LONG_TERM} or {@link BleDeviceState#RECONNECTING_SHORT_TERM}.
	 * This is basically how often and how long the library attempts to reconnect to a device that for example may have gone out of range. Set this variable to
	 * <code>null</code> if reconnect behavior isn't desired. If not <code>null</code>, your app may find
	 * {@link BleManagerConfig#manageCpuWakeLock} useful in order to force the app/phone to stay awake while attempting a reconnect.
	 *
	 * @see BleManagerConfig#manageCpuWakeLock
	 * @see ReconnectFilter
	 * @see DefaultReconnectFilter
	 */
	@Nullable(Nullable.Prevalence.NORMAL)
	public ReconnectFilter reconnectFilter									= new DefaultReconnectFilter();

	/**
	 * Provide an instance of this class to {@link com.idevicesinc.sweetblue.BleDeviceConfig#historicalDataLogFilter} to control
	 * how/if historical data from BLE operations is logged.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface HistoricalDataLogFilter
	{
		/**
		 * Special value you can use in place of Java's built-in <code>null</code>, just for code readability.
		 */
		public static final HistoricalDataLogFilter DISABLED = null;

		/**
		 * Signifies where the data came from, usually from a BLE read or notification.
		 */
		public static enum Source implements UsesCustomNull
		{
			/**
			 * Satisfies soft contract of {@link com.idevicesinc.sweetblue.utils.UsesCustomNull}
			 */
			NULL,

			/**
			 * Originates from {@link BleDevice#read(java.util.UUID, BleDevice.ReadWriteListener)}.
			 *
			 * @see com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type#READ
			 */
			READ,

			/**
			 * Originates from {@link BleDevice#startPoll(java.util.UUID, com.idevicesinc.sweetblue.utils.Interval, BleDevice.ReadWriteListener)}.
			 *
			 * @see com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type#POLL
			 */
			POLL,

			/**
			 * Originates from {@link BleDevice#enableNotify(java.util.UUID, com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
			 *
			 * @see com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type#NOTIFICATION
			 */
			NOTIFICATION,

			/**
			 * Originates from {@link com.idevicesinc.sweetblue.BleDevice#enableNotify(java.util.UUID, com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
			 *
			 * @see com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type#INDICATION
			 */
			INDICATION,

			/**
			 * Originates from {@link com.idevicesinc.sweetblue.BleDevice#enableNotify(java.util.UUID, Interval, com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)},
			 * where a force-read timeout is invoked, or from {@link BleDevice#startChangeTrackingPoll(java.util.UUID, com.idevicesinc.sweetblue.utils.Interval, BleDevice.ReadWriteListener)}.
			 *
			 * @see com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type#PSUEDO_NOTIFICATION
			 */
			PSUEDO_NOTIFICATION,

			/**
			 * Originates from manual addition of data through {@link BleDevice#addHistoricalData(UUID, byte[], EpochTime)}
			 * or various overloads that add one piece of historical data.
			 */
			SINGLE_MANUAL_ADDITION,

			/**
			 * Originates from manual addition of data through {@link BleDevice#addHistoricalData(UUID, java.util.Iterator)}
			 * or various overloads that add multiple pieces of data all at once.
			 */
			MULTIPLE_MANUAL_ADDITIONS;

			/**
			 * Returns <code>true</code> if <code>this</code> == {@link #NULL}.
			 */
			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Event passed to {@link BleNodeConfig.HistoricalDataLogFilter#onEvent(HistoricalDataLogEvent)} that provides
		 * information you can use to determine whether or not {@link HistoricalDataLogEvent#data()} should be logged.
		 */
		public static class HistoricalDataLogEvent extends Event
		{
			/**
			 * The node that is currently trying to reconnect.
			 */
			public BleNode node(){  return m_node;  }
			private final BleNode m_node;

			/**
			 * Tries to cast {@link #node()} to a {@link BleDevice}, otherwise returns {@link BleDevice#NULL}.
			 */
			public BleDevice device(){  return node().cast(BleDevice.class);  }

			/**
			 * Tries to cast {@link #node()} to a {@link BleServer}, otherwise returns {@link BleServer#NULL}.
			 */
			public BleServer server(){  return node().cast(BleServer.class);  }

			/**
			 * The device or server client in question.
			 */
			public String macAddress()  {  return m_macAddress;  }
			private final String m_macAddress;

			/**
			 * The data to be written.
			 */
			public byte[] data()  {  return m_data;  }
			private final byte[] m_data;

			/**
			 * Timestamp of when the data was obtained.
			 */
			public EpochTime epochTime()  {  return m_epochTime;  }
			private final EpochTime m_epochTime;

			/**
			 * The source of the data - read, notify, etc.
			 */
			public Source source()  {  return m_source;  }
			private final Source m_source;

			/**
			 * The characteristic {@link java.util.UUID} associated with {@link #data()}.
			 */
			public UUID charUuid()  {  return m_charUuid;  }
			private final UUID m_charUuid;

			HistoricalDataLogEvent(final BleNode node, final String macAddress, final UUID charUuid, final byte[] data, final EpochTime epochTime, final Source source)
			{
				m_node = node;
				m_macAddress = macAddress;
				m_charUuid = charUuid;
				m_data = data;
				m_epochTime = epochTime;
				m_source = source;
			}

			/**
			 * Returns true if this event is associated with the given uuid.
			 */
			public boolean isFor(final UUID uuid)
			{
				return uuid.equals(charUuid());
			}

			/**
			 * Returns true if this event is associated with any of the given uuids.
			 */
			public boolean isFor(final UUID[] uuids)
			{
				return Utils.contains(uuids, charUuid());
			}

			@Override public String toString()
			{
				return Utils_String.toString
				(
					this.getClass(),
					"macAddress", macAddress(),
					"charUuid", m_node.getManager().getLogger().charName(charUuid()),
					"source", source(),
					"data", data()
				);
			}

			public static boolean includesMemory(final int enum_PersistenceLevel)
			{
				return enum_PersistenceLevel == PersistenceLevel_MEMORY || enum_PersistenceLevel == PersistenceLevel_BOTH;
			}

			public static boolean includesDisk(final int enum_PersistenceLevel)
			{
				return enum_PersistenceLevel == PersistenceLevel_DISK || enum_PersistenceLevel == PersistenceLevel_BOTH;
			}
		}

		/*package*/ static int PersistenceLevel_NONE	= 0;
		/*package*/ static int PersistenceLevel_MEMORY	= 1;
		/*package*/ static int PersistenceLevel_DISK	= 2;
		/*package*/ static int PersistenceLevel_BOTH	= 3;

		/**
		 * Special value returned from {@link BleNodeConfig.HistoricalDataLogFilter#onEvent(HistoricalDataLogEvent)}
		 * that determines if/how {@link HistoricalDataLogFilter.HistoricalDataLogEvent#data()} will get logged.
		 */
		public static class Please
		{
			final int m_persistenceLevel;

			private byte[] m_amendedData = null;
			private EpochTime m_amendedEpochTime = null;
			private Long m_logLimit = null;

			private Please(final int persistenceLevel)
			{
				m_persistenceLevel = persistenceLevel;
			}

			/**
			 * Returns the limit provided through {@link #andLimitLogTo(long)}, or {@link Long#MAX_VALUE} if not applicable.
			 */
			public long getLimit()
			{
				return m_logLimit != null ? m_logLimit : Long.MAX_VALUE;
			}

			/**
			 * Returns the amended data provided through {@link #andAmendData(byte[])}, or <code>null</code> if not applicable.
			 */
			@Nullable(Nullable.Prevalence.NORMAL) public byte[] getAmendedData()
			{
				return m_amendedData;
			}

			/**
			 * Returns the amended epoch time provided through {@link #andAmendEpochTime(EpochTime)}, or {@link com.idevicesinc.sweetblue.utils.EpochTime#NULL} if not applicable.
			 */
			@Nullable(Nullable.Prevalence.NEVER) public EpochTime getAmendedEpochTime()
			{
				return m_amendedEpochTime != null ? m_amendedEpochTime : EpochTime.NULL;
			}

			/**
			 * Last chance to amend or replace {@link HistoricalDataLogEvent#data()} before it's written to the log.
			 *
			 * @return <code>this</code> so you can chain calls together.
			 */
			public Please andAmendData(final byte[] data)
			{
				m_amendedData = data;

				return this;
			}

			/**
			 * Last chance to amend or replace {@link HistoricalDataLogEvent#epochTime()} before it's written to the log.
			 * @return <code>this</code> so you can chain calls together.
			 */
			public Please andAmendEpochTime(final EpochTime epochTime)
			{
				m_amendedEpochTime = epochTime;

				return this;
			}

			/**
			 * Calling this will crop the log to the given limit <i>before></i> {@link HistoricalDataLogEvent#data()} is written.
			 * So if you call this with <code>0</code> {@link BleDevice#getHistoricalDataCount(java.util.UUID)} will return <code>1</code>
			 * after this.
			 *
			 * @return <code>this</code> so you can chain calls together.
			 */
			public Please andLimitLogTo(final long logLimit)
			{
				m_logLimit = logLimit;

				return this;
			}

			/**
			 * Will log the data to disk only, currently through SQLite. Data is preserved across app sessions
			 * until (a) the user uninstalls the app, (b) the user clears the app's data, or (c) you call
			 * one of the {@link com.idevicesinc.sweetblue.BleDevice#clearHistoricalData()} overloads.
			 */
			public static Please logToDisk()
			{
				return new Please(PersistenceLevel_DISK);
			}

			/**
			 * Will log the data to current app memory only. When the app is destroyed the data will be lost.
			 */
			public static Please logToMemory()
			{
				return new Please(PersistenceLevel_MEMORY);
			}

			/**
			 * Will log the data to both memory and disk - combination of {@link #logToMemory()} and {@link #logToDisk()}.
			 */
			public static Please logToMemoryAndDisk()
			{
				return new Please(PersistenceLevel_BOTH);
			}

			/**
			 * Will not log the data.
			 */
			public static Please doNotLog()
			{
				return new Please(PersistenceLevel_NONE);
			}
		}

		/**
		 * Implement this method to be notified of when the library requests whether historical data should be written to a log,
		 * and to respond with your preference of if/how this data should be written.
		 */
		Please onEvent(final HistoricalDataLogEvent e);
	}

	/**
	 * Default implementation of {@link HistoricalDataLogFilter} set on {@link #historicalDataLogFilter}
	 * that logs the most recent data reading to memory only, flushing the previous one.
	 */
	public static class DefaultHistoricalDataLogFilter implements HistoricalDataLogFilter
	{
		private static final Please DEFAULT = Please.logToMemory().andLimitLogTo(1);

		@Override public Please onEvent(final HistoricalDataLogEvent e)
		{
			return DEFAULT;
		}
	}

	/**
	 * Provide an instance to {@link #historicalDataFactory} to return custom subclasses
	 * of {@link com.idevicesinc.sweetblue.utils.HistoricalData} if you would like. For example
	 * you might have a graphing library which requires a "Point" interface with methods <code>getX()</code>
	 * and <code>getY()</code>. You could then create a factory that returns subclasses of
	 * {@link com.idevicesinc.sweetblue.utils.HistoricalData} that implement this interface, so you don't
	 * need to duplicate the data and waste memory.
	 */
	public static interface HistoricalDataFactory
	{
		/**
		 * Return a new subclass of {@link HistoricalData} that for example implements a custom interface
		 * for another library that handles graphing or analytics.
		 */
		HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime);
	}

	/**
	 * Provides a way to control timeout behavior for various {@link BleTask} instances. Assign an instance to {@link BleDeviceConfig#taskTimeoutRequestFilter}.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	@com.idevicesinc.sweetblue.annotations.Advanced
	public static interface TaskTimeoutRequestFilter
	{
		/**
		 * Event passed to {@link TaskTimeoutRequestFilter#onEvent(TaskTimeoutRequestEvent)} that provides
		 * information about the {@link BleTask} that will soon be executed.
		 */
		@Immutable
		public static class TaskTimeoutRequestEvent extends Event
		{
			/**
			 * The {@link BleDevice} associated with the {@link #task()}, or {@link BleDevice#NULL} if
			 * {@link #task()} {@link BleTask#isDeviceSpecific()} does not return <code>true</code>.
			 */
			public BleDevice device(){  return m_device;  }
			private BleDevice m_device;

			/**
			 * Convience to return the mac address of {@link #device()}.
			 */
			public String macAddress()  {  return m_device.getMacAddress();  }

			/**
			 * The {@link BleServer} associated with the {@link #task()}, or {@link BleServer#NULL} if
			 * {@link #task()} {@link BleTask#isServerSpecific()} does not return <code>true</code>.
			 */
			public BleServer server(){  return m_server;  }
			private BleServer m_server;

			/**
			 * Returns the manager.
			 */
			public BleManager manager(){  return m_manager;  }
			private BleManager m_manager;

			/**
			 * The type of task for which we are requesting a timeout.
			 */
			public BleTask task(){  return m_task;  }
			private BleTask m_task;

			/**
			 * The ble characteristic {@link UUID} associated with the task if {@link BleTask#usesCharUuid()}
			 * returns <code>true</code>, or {@link Uuids#INVALID} otherwise.
			 */
			public UUID charUuid(){  return m_charUuid;  }
			private UUID m_charUuid;

			/**
			 * The ble descriptor {@link UUID} associated with the task, or {@link Uuids#INVALID} otherwise.
			 * For now only associated with {@link BleTask#TOGGLE_NOTIFY}.
			 */
			public UUID descUuid(){  return m_descUuid;  }
			private UUID m_descUuid;

			void init(BleManager manager, BleDevice device, BleServer server, BleTask task, UUID charUuid, UUID descUuid)
			{
				m_manager = manager;
				m_device = device;
				m_server = server;
				m_task = task;
				m_charUuid = charUuid;
				m_descUuid = descUuid;
			}

			@Override public String toString()
			{
				if( device() != BleDevice.NULL )
				{
					return Utils_String.toString
					(
						this.getClass(),
						"device",		device(),
						"task",			task(),
						"charUuid",		charUuid()
					);
				}
				else
				{
					return Utils_String.toString
					(
						this.getClass(),
						"server",		server(),
						"task",			task(),
						"charUuid",		charUuid()
					);
				}
			}
		}

		/**
		 * Use static constructor methods to create instances to return from {@link TaskTimeoutRequestFilter#onEvent(TaskTimeoutRequestEvent)}.
		 */
		@Immutable
		public static class Please
		{
			private final Interval m_interval;

			Please(Interval interval)
			{
				m_interval = interval;
			}

			/**
			 * Tells SweetBlue to wait for the given interval before timing out the task.
			 */
			public static Please setTimeoutFor(final Interval interval)
			{
				return new Please(interval);
			}

			/**
			 * Tells SweetBlue to not timeout the task at all.
			 * <br><br>
			 * WARNING: This can be dangerous to use because if a task never finishes it will block all other operations indefinitely.
			 */
			public static Please doNotUseTimeout()
			{
				return new Please(Interval.DISABLED);
			}
		}

		/**
		 * Implement this to have fine-grained control over {@link BleTask} timeout behavior.
		 */
		Please onEvent(TaskTimeoutRequestEvent e);
	}

	/**
	 * Default implementation of {@link TaskTimeoutRequestFilter} that simply sets the timeout
	 * for all {@link BleTask} instances to {@link #DEFAULT_TASK_TIMEOUT} seconds.
	 */
	public static class DefaultTaskTimeoutRequestFilter implements TaskTimeoutRequestFilter
	{
		/**
		 * Default value for all tasks.
		 */
		public static final double DEFAULT_TASK_TIMEOUT					= 12.5;

		/**
		 * Value used for crash resolver process because this can take a bit longer.
		 */
		public static final double DEFAULT_CRASH_RESOLVER_TIMEOUT		= 50.0;

		private static final Please DEFAULT_RETURN_VALUE = Please.setTimeoutFor(Interval.secs(DEFAULT_TASK_TIMEOUT));

		@Override public Please onEvent(TaskTimeoutRequestEvent e)
		{
			if( e.task() == BleTask.RESOLVE_CRASHES )
			{
				return Please.setTimeoutFor(Interval.secs(DEFAULT_CRASH_RESOLVER_TIMEOUT));
			}
			else if( e.task() == BleTask.BOND )
			{
				return DEFAULT_RETURN_VALUE;
			}
			else
			{
				return DEFAULT_RETURN_VALUE;
			}
		}
	}

	/**
	 * An optional interface you can implement on {@link BleNodeConfig#reconnectFilter} to control reconnection behavior.
	 *
	 * @see #reconnectFilter
	 * @see DefaultReconnectFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ReconnectFilter
	{
		/**
		 * An enum provided through {@link ReconnectEvent#type()} that describes what reconnect stage we're at.
		 */
		public static enum Type
		{
			/**
			 * A small period of time has passed since we last asked about {@link #SHORT_TERM__SHOULD_TRY_AGAIN}, so just making sure you want to keep going.
			 */
			SHORT_TERM__SHOULD_CONTINUE,

			/**
			 * An attempt to reconnect in the short term failed, should we try again?.
			 */
			SHORT_TERM__SHOULD_TRY_AGAIN,

			/**
			 * A small period of time has passed since we last asked about {@link #LONG_TERM__SHOULD_TRY_AGAIN}, so just making sure you want to keep going.
			 */
			LONG_TERM__SHOULD_CONTINUE,

			/**
			 * An attempt to reconnect in the long term failed, should we try again?.
			 */
			LONG_TERM__SHOULD_TRY_AGAIN;

			/**
			 * Is this either {@link #SHORT_TERM__SHOULD_CONTINUE} or {@link #LONG_TERM__SHOULD_TRY_AGAIN}.
			 */
			public boolean isShouldTryAgain()
			{
				return this == SHORT_TERM__SHOULD_TRY_AGAIN || this == LONG_TERM__SHOULD_TRY_AGAIN;
			}

			/**
			 * Is this either {@link #SHORT_TERM__SHOULD_CONTINUE} or {@link #LONG_TERM__SHOULD_CONTINUE}.
			 */
			public boolean isShouldContinue()
			{
				return this == SHORT_TERM__SHOULD_CONTINUE || this == LONG_TERM__SHOULD_CONTINUE;
			}

			/**
			 * Is this either {@link #SHORT_TERM__SHOULD_TRY_AGAIN} or {@link #SHORT_TERM__SHOULD_CONTINUE}.
			 */
			public boolean isShortTerm()
			{
				return this == SHORT_TERM__SHOULD_TRY_AGAIN || this == SHORT_TERM__SHOULD_CONTINUE;
			}

			/**
			 * Is this either {@link #LONG_TERM__SHOULD_TRY_AGAIN} or {@link #LONG_TERM__SHOULD_CONTINUE}.
			 */
			public boolean isLongTerm()
			{
				return this == LONG_TERM__SHOULD_TRY_AGAIN || this == LONG_TERM__SHOULD_CONTINUE;
			}
		}

		/**
		 * Struct passed to {@link BleNodeConfig.ReconnectFilter#onEvent(BleNodeConfig.ReconnectFilter.ReconnectEvent)} to aid in making a decision.
		 */
		@Immutable
		public static class ReconnectEvent extends Event
		{
			/**
			 * The node that is currently trying to reconnect.
			 */
			public BleNode node(){  return m_node;  }
			private BleNode m_node;

			/**
			 * Tries to cast {@link #node()} to a {@link BleDevice}, otherwise returns {@link BleDevice#NULL}.
			 */
			public BleDevice device(){  return node().cast(BleDevice.class);  }

			/**
			 * Tries to cast {@link #node()} to a {@link BleServer}, otherwise returns {@link BleServer#NULL}.
			 */
			public BleServer server(){  return node().cast(BleServer.class);  }

			/**
			 * Convience to return the mac address of {@link #device()} or the client being reconnected to the {@link #server()}.
			 */
			public String macAddress()  {  return m_macAddress;  }
			private String m_macAddress;

			/**
			 * The number of times a reconnect attempt has failed so far.
			 */
			public int failureCount(){  return m_failureCount;  }
			private int m_failureCount;

			/**
			 * The total amount of time since the device disconnected and we started the reconnect process.
			 */
			public Interval totalTimeReconnecting(){  return m_totalTimeReconnecting;  }
			private Interval m_totalTimeReconnecting;

			/**
			 * The previous {@link Interval} returned through {@link BleNodeConfig.ReconnectFilter.Please#retryIn(Interval)},
			 * or {@link Interval#ZERO} for the first invocation.
			 */
			public Interval previousDelay(){  return m_previousDelay;  }
			private Interval m_previousDelay;

			/**
			 * Returns the more detailed information about why the connection failed. This is passed to {@link BleDevice.ConnectionFailListener#onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent)}
			 * before the call is made to {@link BleNodeConfig.ReconnectFilter#onEvent(ReconnectEvent)}. For the first call to {@link ReconnectFilter#onEvent(ReconnectEvent)},
			 * right after a spontaneous disconnect occurred, the connection didn't fail, so {@link BleNode.ConnectionFailListener.ConnectionFailEvent#isNull()} will return <code>true</code>.
			 */
			public BleNode.ConnectionFailListener.ConnectionFailEvent connectionFailEvent(){  return m_connectionFailEvent;  }
			private BleNode.ConnectionFailListener.ConnectionFailEvent m_connectionFailEvent;

			/**
			 * See {@link BleNodeConfig.ReconnectFilter.Type} for more info.
			 */
			public Type type(){  return m_type;  }
			private Type m_type;

			/*package*/ ReconnectEvent(BleNode node, final String macAddress, int failureCount, Interval totalTimeReconnecting, Interval previousDelay, BleNode.ConnectionFailListener.ConnectionFailEvent connectionFailEvent, final Type type)
			{
				this.init(node, macAddress, failureCount, totalTimeReconnecting, previousDelay, connectionFailEvent, type);
			}

			/*package*/ ReconnectEvent()
			{
			}

			/*package*/ void init(BleNode node, final String macAddress, int failureCount, Interval totalTimeReconnecting, Interval previousDelay, BleNode.ConnectionFailListener.ConnectionFailEvent connectionFailEvent, final Type type)
			{
				this.m_node						= node;
				this.m_macAddress				= macAddress;
				this.m_failureCount				= failureCount;
				this.m_totalTimeReconnecting	= totalTimeReconnecting;
				this.m_previousDelay			= previousDelay;
				this.m_connectionFailEvent		= connectionFailEvent;
				this.m_type						= type;
			}

			@Override public String toString()
			{
				return Utils_String.toString
				(
					this.getClass(),
					"node",						node(),
					"type",						type(),
					"failureCount",				failureCount(),
					"totalTimeReconnecting",	totalTimeReconnecting(),
					"previousDelay",			previousDelay()
				);
			}
		}

		/**
		 * Return value for {@link ReconnectFilter#onEvent(ReconnectEvent)}. Use static constructor methods to create instances.
		 */
		@Immutable
		public static class Please
		{
			private static final Interval SHOULD_TRY_AGAIN__INSTANTLY	= Interval.ZERO;

			private static final Please SHOULD_CONTINUE__PERSIST		= new Please(true);
			private static final Please SHOULD_CONTINUE__STOP			= new Please(false);

			private final Interval m_interval__SHOULD_TRY_AGAIN;
			private final boolean m_persist;

			private Please(final Interval interval__SHOULD_TRY_AGAIN)
			{
				m_interval__SHOULD_TRY_AGAIN = interval__SHOULD_TRY_AGAIN;
				m_persist = true;
			}

			private Please(boolean persist)
			{
				m_persist = persist;
				m_interval__SHOULD_TRY_AGAIN = null;
			}

			/*package*/ Interval interval()
			{
				return m_interval__SHOULD_TRY_AGAIN;
			}

			/*package*/ boolean shouldPersist()
			{
				return m_persist;
			}

			/**
			 * When {@link BleNodeConfig.ReconnectFilter.ReconnectEvent#type()} is either {@link Type#SHORT_TERM__SHOULD_TRY_AGAIN} or {@link Type#LONG_TERM__SHOULD_TRY_AGAIN},
			 * return this from {@link BleNodeConfig.ReconnectFilter#onEvent(BleNodeConfig.ReconnectFilter.ReconnectEvent)} to instantly reconnect.
			 */
			public static Please retryInstantly()
			{
				return new Please(SHOULD_TRY_AGAIN__INSTANTLY);
			}

			/**
			 * Return this from {@link BleNodeConfig.ReconnectFilter#onEvent(BleNodeConfig.ReconnectFilter.ReconnectEvent)} to stop a reconnect attempt loop.
			 * Note that {@link BleDevice#disconnect()} {@link BleServer#disconnect(String)} will also stop any ongoing reconnect loops.
			 */
			public static Please stopRetrying()
			{
				return SHOULD_CONTINUE__STOP;
			}

			/**
			 * Return this from {@link BleNodeConfig.ReconnectFilter#onEvent(BleNodeConfig.ReconnectFilter.ReconnectEvent)} to retry after the given amount of time.
			 */
			public static Please retryIn(Interval interval)
			{
				return new Please(interval != null ? interval : SHOULD_TRY_AGAIN__INSTANTLY);
			}

			/**
			 * Indicates that the {@link BleDevice} should keep {@link BleDeviceState#RECONNECTING_LONG_TERM} or
			 * {@link BleDeviceState#RECONNECTING_SHORT_TERM}.
			 */
			public static Please persist()
			{
				return SHOULD_CONTINUE__PERSIST;
			}

			/**
			 * Returns {@link #persist()} if the condition holds, {@link #stopRetrying()} otherwise.
			 */
			public static Please persistIf(final boolean condition)
			{
				return condition ? persist() : stopRetrying();
			}

			/**
			 * Returns {@link #stopRetrying()} if the condition holds, {@link #persist()} otherwise.
			 */
			public static Please stopRetryingIf(final boolean condition)
			{
				return condition ? stopRetrying() : persist();
			}
		}

		/**
		 * Called for every connection failure while device is {@link BleDeviceState#RECONNECTING_LONG_TERM}.
		 * Use the static methods of {@link Please} as return values to stop reconnection ({@link Please#stopRetrying()}), try again
		 * instantly ({@link Please#retryInstantly()}), or after some amount of time {@link Please#retryIn(Interval)}.
		 */
		Please onEvent(final ReconnectEvent e);
	}

	static class DefaultNullReconnectFilter implements ReconnectFilter
	{
		public static final Please DEFAULT_INITIAL_RECONNECT_DELAY	= Please.retryInstantly();

		public static final Interval SHORT_TERM_ATTEMPT_RATE		= Interval.secs(1.0);

		public static final Interval SHORT_TERM_TIMEOUT				= Interval.FIVE_SECS;

		private final Please m_please__SHORT_TERM__SHOULD_TRY_AGAIN;
		private final Interval m_timeout__SHORT_TERM__SHOULD_CONTINUE;

		public DefaultNullReconnectFilter()
		{
			this
					(
							DefaultReconnectFilter.SHORT_TERM_ATTEMPT_RATE,
							DefaultReconnectFilter.SHORT_TERM_TIMEOUT
					);
		}

		public DefaultNullReconnectFilter(final Interval reconnectRate__SHORT_TERM, final Interval timeout__SHORT_TERM)
		{
			m_please__SHORT_TERM__SHOULD_TRY_AGAIN = Please.retryIn(reconnectRate__SHORT_TERM);

			m_timeout__SHORT_TERM__SHOULD_CONTINUE = timeout__SHORT_TERM;
		}

		@Override public Please onEvent(final ReconnectEvent e)
		{
			if( e.type().isShouldTryAgain() )
			{
				if( e.failureCount() == 0 )
				{
					return DEFAULT_INITIAL_RECONNECT_DELAY;
				}
				else
				{
					if( e.type().isShortTerm() )
					{
						return m_please__SHORT_TERM__SHOULD_TRY_AGAIN;
					}
					else
					{
						return Please.stopRetrying();
					}
				}
			}
			else if( e.type().isShouldContinue() )
			{
				if( e.node() instanceof BleDevice )
				{
					final boolean definitelyPersist = BleDeviceState.CONNECTING_OVERALL.overlaps(e.device().getNativeStateMask()) &&
							BleDeviceState.CONNECTED.overlaps(e.device().getNativeStateMask());

					//--- DRK > We don't interrupt if we're in the middle of connecting
					//---		but this will be the last attempt if it fails.
					if( definitelyPersist )
					{
						return Please.persist();
					}
					else
					{
						return shouldContinue(e);
					}
				}
				else
				{
					return shouldContinue(e);
				}
			}
			else
			{
				return Please.stopRetrying();
			}
		}

		private Please shouldContinue(final ReconnectEvent e)
		{
			if( e.type().isShortTerm() )
			{
				return Please.persistIf(e.totalTimeReconnecting().lt(m_timeout__SHORT_TERM__SHOULD_CONTINUE));
			}
			else
			{
				return Please.stopRetrying();
			}
		}
	}

	/**
	 * Default implementation of {@link ReconnectFilter} that uses {@link ReconnectFilter.Please#retryInstantly()} for the
	 * first reconnect attempt, and from then on uses the {@link Interval} rate passed to the constructor
	 *
	 */
	public static class DefaultReconnectFilter implements ReconnectFilter
	{
		public static final Please DEFAULT_INITIAL_RECONNECT_DELAY	= Please.retryInstantly();

		public static final Interval LONG_TERM_ATTEMPT_RATE			= Interval.secs(3.0);
		public static final Interval SHORT_TERM_ATTEMPT_RATE		= Interval.secs(1.0);

		public static final Interval SHORT_TERM_TIMEOUT				= Interval.FIVE_SECS;
		public static final Interval LONG_TERM_TIMEOUT				= Interval.mins(5);

		private final Please m_please__SHORT_TERM__SHOULD_TRY_AGAIN;
		private final Please m_please__LONG_TERM__SHOULD_TRY_AGAIN;

		private final Interval m_timeout__SHORT_TERM__SHOULD_CONTINUE;
		private final Interval m_timeout__LONG_TERM__SHOULD_CONTINUE;

		public DefaultReconnectFilter()
		{
			this
			(
				DefaultReconnectFilter.SHORT_TERM_ATTEMPT_RATE,
				DefaultReconnectFilter.LONG_TERM_ATTEMPT_RATE,
				DefaultReconnectFilter.SHORT_TERM_TIMEOUT,
				DefaultReconnectFilter.LONG_TERM_TIMEOUT
			);
		}

		public DefaultReconnectFilter(final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
		{
			m_please__SHORT_TERM__SHOULD_TRY_AGAIN = Please.retryIn(reconnectRate__SHORT_TERM);
			m_please__LONG_TERM__SHOULD_TRY_AGAIN = Please.retryIn(reconnectRate__LONG_TERM);

			m_timeout__SHORT_TERM__SHOULD_CONTINUE = timeout__SHORT_TERM;
			m_timeout__LONG_TERM__SHOULD_CONTINUE = timeout__LONG_TERM;
		}

		@Override public Please onEvent(final ReconnectEvent e)
		{
			if( e.type().isShouldTryAgain() )
			{
				if( e.failureCount() == 0 )
				{
					return DEFAULT_INITIAL_RECONNECT_DELAY;
				}
				else
				{
					if( e.type().isShortTerm() )
					{
						return m_please__SHORT_TERM__SHOULD_TRY_AGAIN;
					}
					else
					{
						return m_please__LONG_TERM__SHOULD_TRY_AGAIN;
					}
				}
			}
			else if( e.type().isShouldContinue() )
			{
				if( e.node() instanceof BleDevice )
				{
					final boolean definitelyPersist = BleDeviceState.CONNECTING_OVERALL.overlaps(e.device().getNativeStateMask()) &&
							BleDeviceState.CONNECTED.overlaps(e.device().getNativeStateMask());

					//--- DRK > We don't interrupt if we're in the middle of connecting
					//---		but this will be the last attempt if it fails.
					if( definitelyPersist )
					{
						return Please.persist();
					}
					else
					{
						return shouldContinue(e);
					}
				}
				else
				{
					return shouldContinue(e);
				}
			}
			else
			{
				return Please.stopRetrying();
			}
		}

		private Please shouldContinue(final ReconnectEvent e)
		{
			if( e.type().isShortTerm() )
			{
				return Please.persistIf(e.totalTimeReconnecting().lt(m_timeout__SHORT_TERM__SHOULD_CONTINUE));
			}
			else
			{
				return Please.persistIf(e.totalTimeReconnecting().lt(m_timeout__LONG_TERM__SHOULD_CONTINUE));
			}
		}
	}

	static final String WRONG_THREAD_MESSAGE =

			"As of v2.0.0 this API must be called on the main thread. " +
			"To temporarily disable this enforcement for migrations from v1.*.* set BleNodeConfig.allowCallsFromAllThreads=true. " +
			"HOWEVER, this should only be treated as a temporary solution for your app.";

	/**
	 * Creates a {@link BleNodeConfig} with all default options set. See each member of this class
	 * for what the default options are set to. Consider using {@link #newNulled()} also.
	 */
	public BleNodeConfig()
	{
	}

	/**
	 * Creates a {@link BleNodeConfig} with all default options set. Then, any configuration options
	 * specified in the given JSONObject will be applied over the defaults.  See {@link BleNodeConfig.writeJSON}
	 * regarding the creation of the JSONObject
	 */
	public BleNodeConfig(JSONObject jo)
	{
		readJSON(jo);
	}

	/**
	 * Sets all {@link Nullable} options in {@link BleNodeConfig}, {@link BleDeviceConfig}, {@link BleManagerConfig} to <code>null</code>
	 * so for example it's easier to cherry-pick just a few options to override from {@link BleManagerConfig} when using {@link BleDevice#setConfig(BleDeviceConfig)}.
	 * <br><br>
	 * NOTE: This doesn't affect any non-nullable subclass members of {@link BleManagerConfig} like {@link BleManagerConfig#stopScanOnPause}.
	 */
	public void nullOut()
	{
		Utils_Reflection.nullOut(this, BleDeviceConfig.class);
		Utils_Reflection.nullOut(this, BleManagerConfig.class);
		Utils_Reflection.nullOut(this, BleNodeConfig.class);
	}

	/**
	 * Convenience method that returns a nulled out {@link BleNodeConfig}, which is useful
	 * when using {@link BleServer#setConfig(BleNodeConfig)} to only override a few options
	 * from {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}
	 * or {@link BleManager#setConfig(BleManagerConfig)}.
	 */
	public static BleNodeConfig newNulled()
	{
		BleNodeConfig config = new BleNodeConfig();
		config.nullOut();

		return config;
	}

	static double getTimeout(final TaskTimeoutRequestFilter.TaskTimeoutRequestEvent event)
	{
		final BleManager manager = event.manager();
		final BleDevice device_nullable = !event.device().isNull() ? event.device() : null;
		final BleServer server_nullable = !event.server().isNull() ? event.server() : null;

		final TaskTimeoutRequestFilter filter_specific;

		if( device_nullable != null )
		{
			filter_specific = device_nullable.conf_device().taskTimeoutRequestFilter;
		}
		else if( server_nullable != null )
		{
			filter_specific = server_nullable.conf_node().taskTimeoutRequestFilter;
		}
		else
		{
			filter_specific = null;
		}

		final TaskTimeoutRequestFilter filter_mngr = manager.m_config.taskTimeoutRequestFilter;
		final TaskTimeoutRequestFilter filter = filter_specific != null ? filter_specific : filter_mngr;
		final TaskTimeoutRequestFilter.Please please = filter != null ? filter.onEvent(event) : null;
		final Interval timeout = please != null ? please.m_interval : Interval.DISABLED;
		final double toReturn = timeout != null ? timeout.secs() : Interval.DISABLED.secs();

		event.device().getManager().getLogger().checkPlease(please, TaskTimeoutRequestFilter.Please.class);

		return toReturn;
	}


	static boolean boolOrDefault(Boolean bool_nullable)
	{
		return bool_nullable == null ? false : bool_nullable;
	}

	static Interval intervalOrDefault(Interval value_nullable)
	{
		return value_nullable == null ? Interval.DISABLED : value_nullable;
	}

	static boolean bool(Boolean bool_device_nullable, Boolean bool_mngr_nullable)
	{
		return bool_device_nullable != null ? bool_device_nullable : boolOrDefault(bool_mngr_nullable);
	}

	static Interval interval(Interval interval_device_nullable, Interval interval_mngr_nullable)
	{
		return interval_device_nullable != null ? interval_device_nullable : intervalOrDefault(interval_mngr_nullable);
	}

	static Integer integer(Integer int_device_nullable, Integer int_mngr_nullable)
	{
		return int_device_nullable != null ? int_device_nullable : int_mngr_nullable;
	}

	static Integer integer(Integer int_device_nullable, Integer int_mngr_nullable, int defaultValue)
	{
		return integerOrDefault(integer(int_device_nullable, int_mngr_nullable), defaultValue);
	}

	static int integerOrZero(Integer value_nullable)
	{
		return integerOrDefault(value_nullable, 0);
	}

	static int integerOrDefault(Integer value_nullable, int defaultValue)
	{
		return value_nullable != null ? value_nullable : defaultValue;
	}

	static <T> T filter(T filter_device, T filter_mngr)
	{
		return filter_device != null ? filter_device : filter_mngr;
	}

	@Override public BleNodeConfig clone()
	{
		try
		{
			return (BleNodeConfig) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
		}

		return null;
	}

	/**
	 * Creates and returns a JSONObject that represents all of the mutable settings of this object.
	 * Keys are variable names and objects are values represented in JSON form.  Only types that we
	 * know how to convert to JSON will be included.
	 */
	public JSONObject writeJSON()
	{
		try
		{
			JSONObject jo = P_JSONUtil.objectToJSON(this);
			return jo;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Acceps a JSON object that represents a set of configuration options.  These options will be
	 * applied to this object, overwriting any existing options.  Options not defined in the JSON
	 * object will not be effected at all.
	 */
	public void readJSON(JSONObject jo)
	{
		try
		{
			P_JSONUtil.applyJSONToObject(this, jo);
		}
		catch (Exception e)
		{
		}
	}
}
