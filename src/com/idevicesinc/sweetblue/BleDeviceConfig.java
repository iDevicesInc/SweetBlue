package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.utils.*;

/**
 * Provides a number of options to (optionally) pass to {@link BleDevice#setConfig(BleDeviceConfig)}.
 * This class is also the super class of {@link BleManagerConfig}, which you can pass
 * to {@link BleManager#get(Context, BleManagerConfig)} to set default base options for all devices at once.
 * For all options in this class, you may set the value to <code>null</code> when passed to {@link BleDevice#setConfig(BleDeviceConfig)}
 * and the value will then be inherited from the {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}.
 * Otherwise, if the value is not <code>null</code> it will override any option in the {@link BleManagerConfig}.
 * If an option is ultimately <code>null</code> (<code>null</code> when passed to {@link BleDevice#setConfig(BleDeviceConfig)}
 * *and* {@link BleManager#get(Context, BleManagerConfig)}) then it is interpreted as <code>false</code> or {@link Interval#DISABLED}.
 * <br><br>
 * TIP: You can use {@link Interval#DISABLED} instead of <code>null</code> to disable any time-based options, for code readability's sake.
 */
public class BleDeviceConfig implements Cloneable
{
	public static final double DEFAULT_MINIMUM_SCAN_TIME				= 5.0;
	public static final int DEFAULT_RUNNING_AVERAGE_N					= 10;
	public static final double DEFAULT_SCAN_KEEP_ALIVE					= DEFAULT_MINIMUM_SCAN_TIME*2.5;
	public static final double DEFAULT_TASK_TIMEOUT						= 12.5;

	/**
	 * Default value for {@link #defaultTxPower}.
	 */
	public static final int DEFAULT_TX_POWER							= 4;
	
	/**
	 * Status code used for {@link BleDevice.ReadWriteListener.Result#gattStatus} when the operation failed at a point where a
	 * gatt status from the underlying stack isn't provided or applicable.
	 * <br><br>
	 * Also used for {@link BleDevice.ConnectionFailListener.Info#gattStatus} for when the failure didn't involve the gatt layer.
	 */
	public static final int GATT_STATUS_NOT_APPLICABLE 					= -1;
	
	/**
	 * Used on {@link BleDevice.BondListener.BondEvent#failReason()} when {@link BleDevice.BondListener.BondEvent#status()}
	 * isn't applicable, for example {@link BleDevice.BondListener.Status#SUCCESS}.
	 */
	public static final int BOND_FAIL_REASON_NOT_APPLICABLE				= GATT_STATUS_NOT_APPLICABLE;
	
	/**
	 * As of now there are two main default uses for this class...
	 * <br><br>
	 * The first is that in at least some cases it's not possible to determine beforehand whether a given characteristic requires
	 * bonding, so implementing this interface on {@link BleManagerConfig#bondFilter} lets the app give
	 * a hint to the library so it can bond before attempting to read or write an encrypted characteristic.
	 * Providing these hints lets the library handle things in a more deterministic and optimized fashion, but is not required.
	 * <br><br>
	 * The second is that some android devices have issues when it comes to bonding. So far the worst culprits
	 * are certain Sony and Motorola phones, so if it looks like {@link Build#MANUFACTURER}
	 * is either one of those, {@link DefaultBondFilter} is set to unbond upon discoveries and disconnects.
	 * Please look at the source of {@link DefaultBondFilter} for the most up-to-date spec.
	 * The problem seems to be associated with mismanagement of pairing keys by the OS and
	 * this brute force solution seems to be the only way to smooth things out.
	 */
	public static interface BondFilter
	{
		/**
		 * Just a dummy subclass of {@link BleDevice.StateListener.ChangeEvent} so that this gets auto-imported for implementations of {@link BondFilter}. 
		 */
		public static class StateChangeEvent extends BleDevice.StateListener.ChangeEvent
		{
			StateChangeEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask)
			{
				super(device, oldStateBits, newStateBits, intentMask);
			}
		}
		
		/**
		 * An enumeration of the type of characteristic operation for a {@link CharacteristicEvent}.
		 */
		public static enum CharacteristicEventType
		{
			/**
			 * Started from {@link BleDevice#read(UUID, ReadWriteListener)}, {@link BleDevice#startPoll(UUID, Interval, ReadWriteListener)}, etc.
			 */
			READ,
			
			/**
			 * Started from {@link BleDevice#write(UUID, byte[], ReadWriteListener)} or overloads.
			 */
			WRITE,
			
			/**
			 * Started from {@link BleDevice#enableNotify(UUID, ReadWriteListener)} or overloads.
			 */
			ENABLE_NOTIFY;
		}
		
		/**
		 * Struct passed to {@link BondFilter#onCharacteristicEvent(CharacteristicEvent)}.
		 */
		public static class CharacteristicEvent
		{
			/**
			 * Returns the {@link BleDevice} in question.
			 */
			public BleDevice device(){  return m_device;  }
			private final BleDevice m_device;
			
			/**
			 * Returns the {@link UUID} of the characteristic in question.
			 */
			public UUID charUuid(){  return m_uuid;  }
			private final UUID m_uuid;
			
			/**
			 * Returns the type of characteristic operation, read, write, etc.
			 */
			public CharacteristicEventType type(){  return m_type;  }
			private final CharacteristicEventType m_type;
			
			CharacteristicEvent(BleDevice device, UUID uuid, CharacteristicEventType type)
			{
				m_device = device;
				m_uuid = uuid;
				m_type = type;
			}
			
			@Override public String toString()
			{
				return Utils.toString
				(
					"device",		device().getName_debug(),
					"charUuid",		device().getManager().getLogger().charName(charUuid()),
					"type",			type()
				);
			}
		}
		
		/**
		 * Return value for the various interface methods of {@link BondFilter}.
		 * Use static constructor methods to create instances.
		 */
		public static class Please
		{
			private final Boolean m_bond;
			
			Please(Boolean bond)
			{
				m_bond = bond;
			}
			
			Boolean bond_private()
			{
				return m_bond;
			}
			
			/**
			 * Device should be bonded if it isn't already.
			 */
			public static Please bond()
			{
				return new Please(true);
			}
			
			/**
			 * Device should be unbonded if it isn't already.
			 */
			public static Please unbond()
			{
				return new Please(false);
			}
			
			/**
			 * Device's bond state should not be affected.
			 */
			public static Please doNothing()
			{
				return new Please(null);
			}
		}
		
		/**
		 * Called after a device undergoes a change in its {@link BleDeviceState}.
		 */
		Please onStateChange(StateChangeEvent event);
		
		/**
		 * Called immediately before reading, writing, or enabling notification on a characteristic.
		 */
		Please onCharacteristicEvent(CharacteristicEvent event);
	}
	
	/**
	 * Default implementation of {@link BondFilter} that unbonds for certain phone models upon discovery and disconnects.
	 * See further explanation in documentation for {@link BondFilter}.
	 */
	public static class DefaultBondFilter implements BondFilter
	{
		/**
		 * Forwards {@link Utils#phoneHasBondingIssues()}. Override to make this <code>true</code> for more (or fewer) phones.
		 */
		public boolean phoneHasBondingIssues()
		{
			return Utils.phoneHasBondingIssues();
		}

		@Override public Please onStateChange(StateChangeEvent event)
		{
			if( phoneHasBondingIssues() )
			{
				if( event.didEnterAny(BleDeviceState.DISCOVERED, BleDeviceState.DISCONNECTED) )
				{
					return Please.unbond();
				}
			}
			
			return Please.doNothing();
		}

		@Override public Please onCharacteristicEvent(CharacteristicEvent event)
		{
			return Please.doNothing();
		}
	}
	
	/**
	 * An optional interface you can implement on {@link BleManagerConfig#reconnectLoop } to control reconnection behavior.
	 * 
	 * @see #reconnectLoop
	 * @see DefaultReconnectLoop
	 */
	public static interface ReconnectLoop
	{		
		/**
		 * Struct passed to {@link ReconnectLoop#onReconnectRequest(ReconnectLoop.Info)} to aid in making a decision.
		 */
		public static class Info
		{
			/**
			 * The device that is currently {@link BleDeviceState#ATTEMPTING_RECONNECT}.
			 */
			public BleDevice device(){  return m_device;  }
			private final BleDevice m_device;
			
			/**
			 * The number of times a reconnect attempt has failed so far.
			 */
			public int failureCount(){  return m_failureCount;  }
			private final int m_failureCount;
			
			/**
			 * The total amount of time since the device went {@link BleDeviceState#DISCONNECTED} and we started the reconnect loop.
			 */
			public Interval totalTimeReconnecting(){  return m_totalTimeReconnecting;  }
			private final Interval m_totalTimeReconnecting;
			
			/**
			 * The previous {@link Interval} returned from {@link ReconnectLoop#onReconnectRequest(Info)}, or {@link Interval#ZERO}
			 * for the first invocation.
			 */
			public Interval previousDelay(){  return m_previousDelay;  }
			private final Interval m_previousDelay;
			
			/**
			 * Returns the more detailed information about why the connection failed. This is passed to {@link BleDevice.ConnectionFailListener#onConnectionFail(com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Info)}
			 * before the call is made to {@link ReconnectLoop#onReconnectRequest(Info)}. For the first call to {@link ReconnectLoop#onReconnectRequest(Info)},
			 * right after a spontaneous disconnect occurred, the connection didn't fail, so {@link ConnectionFailListener.Info#isNull()} will return <code>true</code>.
			 */
			public ConnectionFailListener.Info connectionFailInfo(){  return m_connectionFailInfo;  }
			private final ConnectionFailListener.Info m_connectionFailInfo;
			
			Info(BleDevice device, int failureCount, Interval totalTimeReconnecting, Interval previousDelay, ConnectionFailListener.Info connectionFailInfo)
			{
				this.m_device = device;
				this.m_failureCount = failureCount;
				this.m_totalTimeReconnecting = totalTimeReconnecting;
				this.m_previousDelay = previousDelay;
				this.m_connectionFailInfo = connectionFailInfo;
			}
			
			@Override public String toString()
			{
				return Utils.toString
				(
					"device",					device().getName_debug(),
					"failureCount",				failureCount(),
					"totalTimeReconnecting",	totalTimeReconnecting(),
					"previousDelay",			previousDelay()
				);
			}
		}
		
		/**
		 * Return value for {@link ReconnectLoop#onReconnectRequest(Info)}. Use static constructor methods to create instances.
		 */
		public static class Please
		{
			static final Interval INSTANTLY = Interval.ZERO;
			static final Interval STOP = Interval.DISABLED;
			
			private final Interval m_interval;
			
			private Please(Interval interval)
			{
				m_interval = interval;
			}
			
			Interval getInterval()
			{
				return m_interval;
			}
			
			/**
			 * Return this from {@link ReconnectLoop#onReconnectRequest(ReconnectLoop.Info)} to instantly reconnect.
			 */
			public static Please retryInstantly()
			{
				return new Please(INSTANTLY);
			}
			
			/**
			 * Return this from {@link ReconnectLoop#onReconnectRequest(ReconnectLoop.Info)} to stop a reconnect attempt loop.
			 * Note that {@link BleDevice#disconnect()} will also stop any ongoing reconnect loop.
			 */
			public static Please stopRetrying()
			{
				return new Please(STOP);
			}
			
			/**
			 * Return this from {@link ReconnectLoop#onReconnectRequest(ReconnectLoop.Info)} to retry after the given amount of time.
			 */
			public static Please retryIn(Interval interval)
			{
				return new Please(interval != null ? interval : INSTANTLY);
			}
		}
		
		/**
		 * Called for every connection failure while device is {@link BleDeviceState#ATTEMPTING_RECONNECT}.
		 * Use the static methods of {@link Please} as return values to stop reconnection ({@link Please#stopRetrying()}), try again
		 * instantly ({@link Please#retryInstantly()}), or after some amount of time {@link Please#retryIn(Interval)}.
		 */
		Please onReconnectRequest(Info info);
	}
	
	/**
	 * Default implementation of {@link ReconnectLoop} that uses {@link #DEFAULT_INITIAL_RECONNECT_DELAY}
	 * and {@link #DEFAULT_RECONNECT_ATTEMPT_RATE} to infinitely try to reconnect.
	 */
	public static class DefaultReconnectLoop implements ReconnectLoop
	{
		public static final Please DEFAULT_INITIAL_RECONNECT_DELAY = Please.retryInstantly();
		public static final Please DEFAULT_RECONNECT_ATTEMPT_RATE = Please.retryIn(Interval.secs(3.0));
		
		@Override public Please onReconnectRequest(Info info)
		{
			if( info.failureCount() == 0 )
			{
				return DEFAULT_INITIAL_RECONNECT_DELAY;
			}
			else
			{
				return DEFAULT_RECONNECT_ATTEMPT_RATE;
			}
		}
	}
	
	/**
	 * Default is <code>true</code> - whether to automatically get services immediately after a {@link BleDevice} is
	 * {@link BleDeviceState#CONNECTED}. Currently this is the only way to get a device's services.
	 */
	Boolean autoGetServices								= true;
	
	/**
	 * Default is <code>false</code>se - if true and you call {@link BleDevice#startPoll(UUID, Interval, BleDevice.ReadWriteListener)}
	 * or {@link BleDevice#startChangeTrackingPoll(UUID, Interval, BleDevice.ReadWriteListener)()} with identical
	 * parameters then two identical polls would run which would probably be wasteful and unintentional.
	 * This option provides a defense against that situation.
	 */
	public Boolean allowDuplicatePollEntries			= false;
	
	/**
	 * Default is <code>false</code>se - {@link BleDevice#getAverageReadTime()} and {@link BleDevice#getAverageWriteTime()} can be 
	 * skewed if the peripheral you are connecting to adjusts its maximum throughput for OTA firmware updates and the like.
	 * Use this option to let the library know whether you want read/writes to factor in while {@link BleDeviceState#UPDATING_FIRMWARE}.
	 * 
	 * @see BleDevice#getAverageReadTime()
	 * @see BleDevice#getAverageWriteTime() 
	 */
	public Boolean includeOtaReadWriteTimesInAverage		= false;
	
	/**
	 * Default is <code>false</code> - see the <code>boolean autoConnect</code> parameter of
	 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}. 
	 * 
	 * This parameter is one of Android's deepest mysteries. By default we keep it false, but it has been observed that a
	 * connection can fail or time out, but then if you try again with autoConnect set to true it works! One would think,
	 * why not always set it to true? Well, while true is anecdotally more stable, it also (anecdotally) makes for longer
	 * connection times, which becomes a UX problem. Would you rather have a 5-10 second connection process that is successful
	 * with 99% of devices, or a 1-2 second connection process that is successful with 95% of devices? By default we've chosen the latter.
	 * <br><br>
	 * HOWEVER, it's important to note that you can have fine-grained control over its usage through the {@link ConnectionFailListener.PE_Please}
	 * returned from {@link ConnectionFailListener#onConnectionFail(com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Info)}.
	 * <br><br>
	 * So really this option mainly exists for those situations where you KNOW that you have a device that only works
	 * with autoConnect==true and you want connection time to be faster (i.e. you don't want to wait for that first
	 * failed connection for the library to internally start using autoConnect==true).
	 */
	public Boolean alwaysUseAutoConnect						= false;
	
	/**
	 * Default is <code>true</code> - controls whether {@link BleManager} will keep a device in active memory when it goes {@link BleManagerState#OFF}.
	 * If <code>false</code> then a device will be purged and you'll have to do {@link BleManager#startScan()} again to discover devices
	 * if/when {@link BleManager} goes back {@link BleManagerState#ON}.
	 * <br><br>
	 * NOTE: if this flag is true for {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)} then this
	 * applies to all devices.
	 */
	public Boolean retainDeviceWhenBleTurnsOff				= true;
	
	/**
	 * Default is <code>true</code> - only applicable if {@link #retainDeviceWhenBleTurnsOff} is also true. If {@link #retainDeviceWhenBleTurnsOff}
	 * is false then devices will be undiscovered when {@link BleManager} goes {@link BleManagerState#OFF} regardless.
	 * <br><br>
	 * NOTE: See NOTE for {@link #retainDeviceWhenBleTurnsOff} for how this applies to {@link BleManagerConfig}. 
	 * 
	 * @see #retainDeviceWhenBleTurnsOff
	 * @see #autoReconnectDeviceWhenBleTurnsBackOn
	 */
	public Boolean undiscoverDeviceWhenBleTurnsOff			= true;
	
	/**
	 * Default is <code>true</code> - if devices are kept in memory for a {@link BleManager#turnOff()}/{@link BleManager#turnOn()} cycle
	 * (or a {@link BleManager#dropTacticalNuke()}) because {@link #retainDeviceWhenBleTurnsOff} is <code>true</code>, then a {@link BleDevice#connect()}
	 * will be attempted for any devices that were previously {@link BleDeviceState#CONNECTED}.
	 * <br><br>
	 * NOTE: See NOTE for {@link #retainDeviceWhenBleTurnsOff} for how this applies to {@link BleManagerConfig}.
	 * 
	 * @see #retainDeviceWhenBleTurnsOff
	 */
	public Boolean autoReconnectDeviceWhenBleTurnsBackOn 	= true;
	
	/**
	 * Default is <code>true</code> - controls whether the {@link State.ChangeIntent} behind a device going {@link BleDeviceState#DISCONNECTED}
	 * is saved to and loaded from disk so that it can be restored across app sessions, undiscoveries, and BLE
	 * {@link BleManagerState#OFF}->{@link BleManagerState#ON} cycles. This uses Android's {@link SharedPreferences} so does not require
	 * any extra permissions. The main advantage of this is the following scenario: User connects to a device through your app,
	 * does what they want, kills the app, then opens the app sometime later. {@link BleDevice#getLastDisconnectIntent()} returns
	 * {@link State.ChangeIntent#UNINTENTIONAL}, which lets you know that you can probably automatically connect to this device without user confirmation.
	 */
	public Boolean manageLastDisconnectOnDisk				= true;
	
	/**
	 * Default is <code>true</code> - controls whether a {@link BleDevice} is placed into an in-memory cache when it becomes {@link BleDeviceState#UNDISCOVERED}.
	 * If <code>true</code>, subsequent calls to {@link BleManager.DiscoveryListener#onDiscoveryEvent(BleManager.DiscoveryListener.DiscoveryEvent)} with
	 * {@link LifeCycle#DISCOVERED} (or calls to {@link BleManager#newDevice(String)}) will return the cached {@link BleDevice} instead of creating a new one.
	 * <br><br>
	 * The advantages of caching are:<br>
	 * <ul>
	 * <li>Slightly better performance at the cost of some retained memory, especially in situations where you're frequently discovering and undiscovering devices.
	 * <li>Resistance to future stack failures that would otherwise mean missing data like {@link BleDevice#getAdvertisedServices()} for future discovery events.
	 * <li>More resistant to potential "user error" of retaining devices in app-land after BleManager undiscovery.
	 * <ul><br>
	 * This is kept as an option in case there's some unforeseen problem with devices being cached for a certain application.
	 * 
	 * See also {@link #minScanTimeToInvokeUndiscovery}.
	 */
	public Boolean cacheDeviceOnUndiscovery					= true;
	
	/**
	 * Default is {@link #DEFAULT_MINIMUM_SCAN_TIME} seconds - Undiscovery of devices must be
	 * approximated by checking when the last time was that we discovered a device,
	 * and if this time is greater than {@link #scanKeepAlive} then the device is undiscovered. However a scan
	 * operation must be allowed a certain amount of time to make sure it discovers all nearby devices that are
	 * still advertising. This is that time in seconds.
	 * <br><br>
	 * Use {@link Interval#DISABLED} to disable undiscovery altogether.
	 * 
	 * @see BleManager.DiscoveryListener_Full#onDeviceUndiscovered(BleDevice)
	 * @see #scanKeepAlive
	 */
	public Interval	minScanTimeToInvokeUndiscovery		= Interval.secs(DEFAULT_MINIMUM_SCAN_TIME);
	
	/**
	 * Default is {@link #DEFAULT_SCAN_KEEP_ALIVE} seconds - If a device exceeds this amount of time since its
	 * last discovery then it is a candidate for being undiscovered.
	 * The default for this option attempts to accommodate the worst Android phones (BLE-wise), which may make it seem
	 * like it takes a long time to undiscover a device. You may want to configure this number based on the phone or
	 * manufacturer. For example, based on testing, in order to make undiscovery snappier the Galaxy S5 could use lower times.
	 * <br><br>
	 * Use {@link Interval#DISABLED} to disable undiscovery altogether.
	 * 
	 * @see BleManager.DiscoveryListener_Full#onDeviceUndiscovered(BleDevice)
	 * @see #minScanTimeToInvokeUndiscovery
	 */
	public Interval	scanKeepAlive						= Interval.secs(DEFAULT_SCAN_KEEP_ALIVE);
	
	/**
	 * Default is an array of {@link Interval} instances populated using {@link Interval#secs(double)} with {@link #DEFAULT_TASK_TIMEOUT}.
	 * This is an array of timeouts whose indices are meant to map to {@link BleTask} ordinals and provide a
	 * way to control how long a given task is allowed to run before being "cut loose". If no option is provided for a given {@link BleTask},
	 * either by setting this array null, or by providing <code>null</code> or {@link Interval#DISABLED} for a given {@link BleTask}, then
	 * no timeout is observed.
	 * <br><br>
	 * TIP: Use {@link #setTimeout(Interval, BleTask...)} to modify this option more easily.
	 */
	public Interval[] timeouts							= newTaskTimeArray();
	{
		final Interval defaultTimeout = Interval.secs(DEFAULT_TASK_TIMEOUT);
		for( int i = 0; i < timeouts.length; i++ )
		{
			timeouts[i] = defaultTimeout;
		}
	}
	
	/**
	 * Default is {@link #DEFAULT_RUNNING_AVERAGE_N} - The number of historical write times that the library should keep track of when calculating average time.
	 * 
	 * @see BleDevice#getAverageWriteTime()
	 * @see #nForAverageRunningReadTime
	 */
	public Integer		nForAverageRunningWriteTime			= DEFAULT_RUNNING_AVERAGE_N;
	
	/**
	 * Default is {@link #DEFAULT_RUNNING_AVERAGE_N} - Same thing as {@link #nForAverageRunningWriteTime} but for reads.
	 * 
	 * @see BleDevice#getAverageWriteTime()
	 * @see #nForAverageRunningWriteTime
	 */
	public Integer		nForAverageRunningReadTime			= DEFAULT_RUNNING_AVERAGE_N;
	
	/**
	 * Default is {@link #DEFAULT_TX_POWER} - this value is used if we can't establish a device's TxPower from the device itself,
	 * either through its scan record or by reading the standard characteristic.
	 * 
	 * @see BleDevice#getTxPower()
	 */
	public Integer		defaultTxPower						= DEFAULT_TX_POWER;
	
	/**
	 * Default is instance of {@link DefaultBondFilter}.
	 * 
	 * @see BondFilter
	 */
	public BondFilter bondFilter							= new DefaultBondFilter();
	
	/**
	 * Default is an instance of {@link DefaultReconnectLoop} - set an implementation here to
	 * have fine control over reconnect behavior. This is basically how often and how long
	 * the library attempts to reconnect to a device that for example may have gone out of range. Set this variable to
	 * <code>null</code> if reconnect behavior isn't desired. If not <code>null</code>, your app may find
	 * {@link BleManagerConfig#manageCpuWakeLock} useful in order to force the app/device to stay awake while attempting a reconnect.
	 * 
	 * @see BleManagerConfig#manageCpuWakeLock
	 * @see ReconnectLoop
	 * @see DefaultReconnectLoop
	 */
	public ReconnectLoop reconnectLoop = new DefaultReconnectLoop();
	
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
	
	static Integer integer(Integer int_device_nullable, Integer int_mngr)
	{
		return int_device_nullable != null ? int_device_nullable : int_mngr;
	}
	
	static int integerOrZero(Integer value_nullable)
	{
		return value_nullable != null ? value_nullable : 0x0;
	}
	
	public BleDeviceConfig()
	{
	}
	
	private static Interval getTaskInterval(final BleTask task, final Interval[] intervals_device_nullable, final Interval[] intervals_mngr_nullable)
	{
		final int ordinal = task.ordinal();
		final Interval interval_device = intervals_device_nullable != null && intervals_device_nullable.length > ordinal ? intervals_device_nullable[ordinal] : null;
		final Interval interval_mngr = intervals_mngr_nullable != null && intervals_mngr_nullable.length > ordinal ? intervals_mngr_nullable[ordinal] : null;
		
		return interval(interval_device, interval_mngr);
	}
	
	static Interval getTimeout(final BleTask task, final BleDeviceConfig conf_device_nullable, final BleManagerConfig conf_mngr)
	{
		final Interval[] timeouts_device = conf_device_nullable != null ? conf_device_nullable.timeouts : null;
		final Interval[] timeouts_mngr = conf_mngr.timeouts;
		
		return getTaskInterval(task, timeouts_device, timeouts_mngr);
	}
	
	/**
	 * Convenience member to add entries to {@link #timeouts} for you.
	 */
	public void setTimeout(final Interval interval_nullable, final BleTask ... tasks)
	{
		this.timeouts = this.timeouts != null ? this.timeouts : newTaskTimeArray();
		
		if( this.timeouts.length < BleTask.values().length )
		{
			Interval[] timeouts_new = newTaskTimeArray();
			
			for( int i = 0; i < this.timeouts.length; i++ )
			{
				timeouts_new[i] = this.timeouts[i];
			}
			
			this.timeouts = timeouts_new;
		}
		
		for( int i = 0; i < tasks.length; i++ )
		{
			this.timeouts[tasks[i].ordinal()] = interval_nullable;
		}
	}
	
	private static Interval[] newTaskTimeArray()
	{
		return new Interval[BleTask.values().length];
	}
	
	@Override protected BleDeviceConfig clone()
	{
		try
		{
			return (BleDeviceConfig) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
		}
		
		return null;
	}
}
