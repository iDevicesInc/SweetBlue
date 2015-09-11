package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;

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
public class BleNodeConfig
{
	/**
	 * @deprecated Use {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
	 */
	@Deprecated
	public static final int GATT_STATUS_NOT_APPLICABLE 					= BleStatuses.GATT_STATUS_NOT_APPLICABLE;

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
	 * HOWEVER, it's important to note that you can have fine-grained control over its usage through the {@link BleDevice.ConnectionFailListener.Please}
	 * returned from {@link BleDevice.ConnectionFailListener#onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent)} (or the equivalent
	 * structures that are inner structures of {@link BleServer}).
	 * <br><br>
	 * So really this option mainly exists for those situations where you KNOW that you have a device or server that only works
	 * with autoConnect==true and you want connection time to be faster (i.e. you don't want to wait for that first
	 * failed connection for the library to internally start using autoConnect==true).
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Nullable.Prevalence.NORMAL)
	public Boolean alwaysUseAutoConnect							= false;

	/**
	 * Default is <code>true</code> - controls whether the library is allowed to optimize fast disconnect/reconnect cycles
	 * by actually not disconnecting in the native stack at all. For example, if this option is <code>true</code> and your
	 * {@link BleDevice} is {@link BleDeviceState#CONNECTED}, calling {@link BleDevice#disconnect()} then {@link BleDevice#connect()}
	 * again won't result in a native disconnect/reconnect - your actual physical ble device firmware won't know that a disconnect was requested.
	 */
	@Nullable(Nullable.Prevalence.NORMAL)
	public Boolean disconnectIsCancellable						= true;

	/**
	 * Default is an instance of {@link DefaultTimeoutRequestFilter} - set an implementation here to
	 * have fine control over how long individual {@link BleTask} instances can take before they
	 * are considered "timed out" and failed.
	 * <br><br>
	 * NOTE: Setting this to <code>null</code> will disable timeouts for all {@link BleTask} instances,
	 * which would probably be very dangerous to do - a task could just sit there spinning forever.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Nullable.Prevalence.RARE)
	public TimeoutRequestFilter timeoutRequestFilter						= new DefaultTimeoutRequestFilter();

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
		public static HistoricalDataLogFilter DISABLED = null;

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
		 * Event passed to {@link BleDeviceConfig.HistoricalDataLogFilter#onEvent(HistoricalDataLogEvent)} that provides
		 * information you can use to determine whether or not {@link HistoricalDataLogEvent#data()} should be logged.
		 */
		public static class HistoricalDataLogEvent
		{
			/**
			 * The device in question.
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

			private final BleNode m_endpoint;

			HistoricalDataLogEvent(final BleNode endpoint, final String macAddress, final UUID charUuid, final byte[] data, final EpochTime epochTime, final Source source)
			{
				m_endpoint = endpoint;
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
								"charUuid", m_endpoint.getManager().getLogger().charName(charUuid()),
								"source", source(),
								"data", data()
						);
			}
		}

		static enum PersistenceLevel
		{
			NONE, MEMORY, DISK, BOTH;

			public boolean includesMemory()
			{
				return this == MEMORY || this == BOTH;
			}

			public boolean includesDisk()
			{
				return this == DISK || this == BOTH;
			}
		}

		/**
		 * Special value returned from {@link BleDeviceConfig.HistoricalDataLogFilter#onEvent(HistoricalDataLogEvent)}
		 * that determines if/how {@link HistoricalDataLogFilter.HistoricalDataLogEvent#data()} will get logged.
		 */
		public static class Please
		{
			final PersistenceLevel m_logChoice;

			private byte[] m_amendedData = null;
			private EpochTime m_amendedEpochTime = null;
			private Long m_logLimit = null;

			private Please(final PersistenceLevel logChoice)
			{
				m_logChoice = logChoice;
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
				return new Please(PersistenceLevel.DISK);
			}

			/**
			 * Will log the data to current app memory only. When the app is destroyed the data will be lost.
			 */
			public static Please logToMemory()
			{
				return new Please(PersistenceLevel.MEMORY);
			}

			/**
			 * Will log the data to both memory and disk - combination of {@link #logToMemory()} and {@link #logToDisk()}.
			 */
			public static Please logToMemoryAndDisk()
			{
				return new Please(PersistenceLevel.BOTH);
			}

			/**
			 * Will not log the data.
			 */
			public static Please doNotLog()
			{
				return new Please(PersistenceLevel.NONE);
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
	 * Provides a way to control timeout behavior for various {@link BleTask} instances. Assign an instance to {@link BleDeviceConfig#timeoutRequestFilter}.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	@com.idevicesinc.sweetblue.annotations.Advanced
	public static interface TimeoutRequestFilter
	{
		/**
		 * Event passed to {@link TimeoutRequestFilter#onEvent(TimeoutRequestEvent)} that provides
		 * information about the {@link BleTask} that will soon be executed.
		 */
		@Immutable
		public static class TimeoutRequestEvent
		{
			/**
			 * The {@link BleDevice} associated with the {@link #task()}, or {@link BleDevice#NULL} if
			 * {@link #task()} {@link BleTask#isDeviceSpecific()} does not return <code>true</code>.
			 */
			public BleDevice device(){  return m_device;  }
			private BleDevice m_device;

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
		}

		/**
		 * Use static constructor methods to create instances to return from {@link TimeoutRequestFilter#onEvent(TimeoutRequestEvent)}.
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
		Please onEvent(TimeoutRequestEvent e);
	}

	/**
	 * Default implementation of {@link TimeoutRequestFilter} that simply sets the timeout
	 * for all {@link BleTask} instances to {@link #DEFAULT_TASK_TIMEOUT} seconds.
	 */
	public static class DefaultTimeoutRequestFilter implements TimeoutRequestFilter
	{
		public static final double DEFAULT_TASK_TIMEOUT						= 12.5;

		private static final Please DEFAULT_RETURN_VALUE = Please.setTimeoutFor(Interval.secs(DEFAULT_TASK_TIMEOUT));

		@Override public Please onEvent(TimeoutRequestEvent e)
		{
			if( e.task() == BleTask.BOND )
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
	 * Creates a {@link BleNodeConfig} with all default options set. See each member of this class
	 * for what the default options are set to. Consider using {@link #newNulled()} also.
	 */
	public BleNodeConfig()
	{
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

	static double getTimeout(final TimeoutRequestFilter.TimeoutRequestEvent event)
	{
		final BleManager manager = event.manager();
		final BleDevice device_nullable = !event.device().isNull() ? event.device() : null;
		final BleServer server_nullable = !event.server().isNull() ? event.server() : null;

		final TimeoutRequestFilter filter_specific;

		if( device_nullable != null )
		{
			filter_specific = device_nullable.conf_device().timeoutRequestFilter;
		}
		else if( server_nullable != null )
		{
			filter_specific = server_nullable.conf_server().timeoutRequestFilter;
		}
		else
		{
			filter_specific = null;
		}

		final TimeoutRequestFilter filter_mngr = manager.m_config.timeoutRequestFilter;
		final TimeoutRequestFilter filter = filter_specific != null ? filter_specific : filter_mngr;
		final TimeoutRequestFilter.Please please = filter != null ? filter.onEvent(event) : null;
		final Interval timeout = please != null ? please.m_interval : Interval.DISABLED;
		final double toReturn = timeout != null ? timeout.secs() : Interval.DISABLED.secs();

		event.device().getManager().getLogger().checkPlease(please, TimeoutRequestFilter.Please.class);

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

	@Override protected BleNodeConfig clone()
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
}
