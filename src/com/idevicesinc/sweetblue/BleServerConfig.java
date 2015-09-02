package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.UUID;

/**
 * Provides a number of options to (optionally) pass to {@link BleServer#setConfig(BleServerConfig)}.
 * This class is also a super class of {@link BleManagerConfig}, which you can pass
 * to {@link BleManager#get(Context, BleManagerConfig)} or {@link BleManager#setConfig(BleManagerConfig)} to set default base options for all servers at once.
 * For all options in this class, you may set the value to <code>null</code> when passed to {@link BleServer#setConfig(BleServerConfig)}
 * and the value will then be inherited from the {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}.
 * Otherwise, if the value is not <code>null</code> it will override any option in the {@link BleManagerConfig}.
 * If an option is ultimately <code>null</code> (<code>null</code> when passed to {@link BleServer#setConfig(BleServerConfig)}
 * *and* {@link BleManager#get(Context, BleManagerConfig)}) then it is interpreted as <code>false</code> or {@link Interval#DISABLED}.
 * <br><br>
 * TIP: You can use {@link Interval#DISABLED} instead of <code>null</code> to disable any time-based options, for code readability's sake.
 * <br><br>
 * TIP: You can use {@link #newNulled()} (or {@link #nullOut()}) then only set the few options you want for {@link BleServer#setConfig(BleServerConfig)}.
 */
public class BleServerConfig
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
	 * Creates a {@link BleServerConfig} with all default options set. See each member of this class
	 * for what the default options are set to. Consider using {@link #newNulled()} also.
	 */
	public BleServerConfig()
	{
	}

	/**
	 * Sets all {@link Nullable} options in {@link BleServerConfig}, {@link BleDeviceConfig}, {@link BleManagerConfig} to <code>null</code>
	 * so for example it's easier to cherry-pick just a few options to override from {@link BleManagerConfig} when using {@link BleDevice#setConfig(BleDeviceConfig)}.
	 * <br><br>
	 * NOTE: This doesn't affect any non-nullable subclass members of {@link BleManagerConfig} like {@link BleManagerConfig#stopScanOnPause}.
	 */
	public void nullOut()
	{
		Utils_Reflection.nullOut(this, BleDeviceConfig.class);
		Utils_Reflection.nullOut(this, BleManagerConfig.class);
		Utils_Reflection.nullOut(this, BleServerConfig.class);
	}

	/**
	 * Convenience method that returns a nulled out {@link BleServerConfig}, which is useful
	 * when using {@link BleServer#setConfig(BleServerConfig)} to only override a few options
	 * from {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}
	 * or {@link BleManager#setConfig(BleManagerConfig)}.
	 */
	public static BleServerConfig newNulled()
	{
		BleServerConfig config = new BleServerConfig();
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

	@Override protected BleServerConfig clone()
	{
		try
		{
			return (BleServerConfig) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
		}

		return null;
	}
}
