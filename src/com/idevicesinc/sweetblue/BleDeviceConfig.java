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
	public static final double DEFAULT_TASK_TIMEOUT						= 10.0;
	
	/**
	 * Status code used for {@link BleDevice.ReadWriteListener.Result#gattStatus} when the operation failed at a point where a
	 * gatt status from the underlying stack isn't provided or applicable.
	 * <br><br>
	 * Also used for {@link BleDevice.ConnectionFailListener.Info#gattStatus} for when the failure didn't involve the gatt layer.
	 */
	public static final int GATT_STATUS_NOT_APPLICABLE 					= -1;
	
	/**
	 * In at least some cases it's not possible to determine beforehand whether a given characteristic requires
	 * bonding, so implementing this interface on {@link BleManagerConfig#bondingFilter} lets the app give
	 * a hint to the library so it can bond before attempting to read or write an encrypted characteristic.
	 * Providing these hints lets the library handle things in a more deterministic and optimized fashion, but is not required.
	 */
	public static interface BondingFilter
	{
		/**
		 * Return true if the characteristic requires bonding, false otherwise.
		 */
		boolean requiresBonding(UUID characteristicUuid);
	}
	
	/**
	 * An optional interface you can implement on {@link BleManagerConfig#reconnectRateLimiter } to control reconnection behavior.
	 * 
	 * @see #reconnectRateLimiter
	 * @see DefaultReconnectRateLimiter
	 */
	public static interface ReconnectRateLimiter
	{
		/**
		 * Return this from {@link ReconnectRateLimiter#getTimeToNextReconnect(ReconnectRateLimiter.Info)} to instantly reconnect.
		 */
		public static final Interval INSTANTLY = Interval.ZERO;
		
		/**
		 * Return this from {@link ReconnectRateLimiter#getTimeToNextReconnect(ReconnectRateLimiter.Info)} to stop a reconnect attempt loop.
		 * Note that {@link BleDevice#disconnect()} will also cancel any ongoing reconnect loop.
		 */
		public static final Interval CANCEL = Interval.DISABLED;
		
		/**
		 * Struct passed to {@link ReconnectRateLimiter#getTimeToNextReconnect(ReconnectRateLimiter.Info)} to aid in making a decision.
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
			 * The previous {@link Interval} returned from {@link ReconnectRateLimiter#getTimeToNextReconnect(Info)}, or {@link Interval#ZERO}
			 * for the first invocation.
			 */
			public Interval previousDelay(){  return m_previousDelay;  }
			private final Interval m_previousDelay;
			
			Info(BleDevice device, int failureCount, Interval totalTimeReconnecting, Interval previousDelay)
			{
				this.m_device = device;
				this.m_failureCount = failureCount;
				this.m_totalTimeReconnecting = totalTimeReconnecting;
				this.m_previousDelay = previousDelay;
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
		 * Called for every connection failure while device is {@link BleDeviceState#ATTEMPTING_RECONNECT}.
		 * Use the static {@link Interval} members of this interface as return values to stop reconnection ({@link #CANCEL}) or try again
		 * instantly ({@link #INSTANTLY}). Use static construction methods of {@link Interval} to try again after some amount of time.
		 */
		Interval getTimeToNextReconnect(Info info);
	}
	
	/**
	 * Default implementation of {@link ReconnectRateLimiter} that uses {@link #DEFAULT_INITIAL_RECONNECT_DELAY}
	 * and {@link #DEFAULT_RECONNECT_ATTEMPT_RATE} to infinitely try to reconnect.
	 */
	public static class DefaultReconnectRateLimiter implements ReconnectRateLimiter
	{
		public static final Interval DEFAULT_INITIAL_RECONNECT_DELAY = INSTANTLY;
		public static final Interval DEFAULT_RECONNECT_ATTEMPT_RATE = Interval.secs(3.0);
		
		@Override public Interval getTimeToNextReconnect(Info info)
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
	 * Default is true - whether to automatically get services immediately after a {@link BleDevice} is
	 * {@link BleDeviceState#CONNECTED}. Currently this is the only way to get a device's services.
	 */
	Boolean autoGetServices								= true;
	
	/**
	 * Default is false - if true and you call {@link BleDevice#startPoll(UUID, Interval, BleDevice.ReadWriteListener)}
	 * or {@link BleDevice#startChangeTrackingPoll(UUID, Interval, BleDevice.ReadWriteListener)()} with identical
	 * parameters then two identical polls would run which would probably be wasteful and unintentional.
	 * This option provides a defense against that situation.
	 */
	public Boolean allowDuplicatePollEntries			= false;
	
	/**
	 * Default is false - {@link BleDevice#getAverageReadTime()} and {@link BleDevice#getAverageWriteTime()} can be 
	 * skewed if the peripheral you are connecting to adjusts its maximum throughput for OTA firmware updates.
	 * Use this option to let the library know whether you want firmware update read/writes to factor in.
	 * 
	 * @see BleDevice#getAverageReadTime()
	 * @see BleDevice#getAverageWriteTime() 
	 */
	public Boolean includeFirmwareUpdateReadWriteTimesInAverage = false;
	
	/**
	 * Default is false - see the <code>boolean autoConnect</code> parameter of
	 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}. 
	 * 
	 * This parameter is one of Android's deepest mysteries. By default we keep it false, but it has been observed that a
	 * connection can fail or time out, but then if you try again with autoConnect set to true it works! One would think,
	 * why not always set it to true? Well, while true is anecdotally more stable, it also (anecdotally) makes for longer
	 * connection times, which becomes a UX problem. Would you rather have a 5-10 second connection process that is successful
	 * with 99% of devices, or a 1-2 second connection process that is successful with 95% of devices? By default we've chosen the latter.
	 * <br><br>
	 * HOWEVER, it's important to note that you can have fine-grained control over its usage through the {@link ConnectionFailListener.Please}
	 * returned from {@link ConnectionFailListener#onConnectionFail(com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Info)}.
	 * <br><br>
	 * So really this option mainly exists for those situations where you KNOW that you have a device that only works
	 * with autoConnect==true and you want connection time to be faster (i.e. you don't want to wait for that first
	 * failed connection for the library to internally start using autoConnect==true).
	 */
	public Boolean alwaysUseAutoConnect = false;
	
	/**
	 * Default is <code>true</code> - controls whether {@link BleManager} will keep a device in active memory when it goes {@link BleState#OFF}.
	 * If <code>false</code> then a device will be purged and you'll have to do {@link BleManager#startScan()} again to discover devices
	 * if/when {@link BleManager} goes back {@link BleState#ON}.
	 * <br><br>
	 * NOTE: if this flag is true for {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)} then this
	 * applies to all devices.
	 */
	public Boolean retainDeviceWhenBleTurnsOff = true;
	
	/**
	 * Default is <code>true</code> - only applicable if {@link #retainDeviceWhenBleTurnsOff} is also true. If {@link #retainDeviceWhenBleTurnsOff}
	 * is false then devices will be undiscovered when {@link BleManager} goes {@link BleState#OFF} regardless.
	 * <br><br>
	 * NOTE: See NOTE for {@link #retainDeviceWhenBleTurnsOff} for how this applies to {@link BleManagerConfig}. 
	 * 
	 * @see #retainDeviceWhenBleTurnsOff
	 * @see #autoReconnectDeviceWhenBleTurnsBackOn
	 */
	public Boolean undiscoverDeviceWhenBleTurnsOff = true;
	
	/**
	 * Default is <code>true</code> - if devices are kept in memory for a {@link BleManager#turnOff()}/{@link BleManager#turnOn()} cycle
	 * (or a {@link BleManager#dropTacticalNuke()}) because {@link #retainDeviceWhenBleTurnsOff} is <code>true</code>, then a {@link BleDevice#connect()}
	 * will be attempted for any devices that were previously {@link BleDeviceState#CONNECTED}.
	 * <br><br>
	 * NOTE: See NOTE for {@link #retainDeviceWhenBleTurnsOff} for how this applies to {@link BleManagerConfig}.
	 * 
	 * @see #retainDeviceWhenBleTurnsOff
	 */
	public Boolean autoReconnectDeviceWhenBleTurnsBackOn = true;
	
	/**
	 * Default is <code>true</code> - controls whether the {@link State.ChangeIntent} behind a device going {@link BleDeviceState#DISCONNECTED}
	 * is saved to and loaded from disk so that it can be restored across app sessions, undiscoveries, and BLE
	 * {@link BleState#OFF}->{@link BleState#ON} cycles. This uses Android's {@link SharedPreferences} so does not require
	 * any extra permissions. The main advantage of this is the following scenario: User connects to a device through your app,
	 * does what they want, kills the app, then opens the app sometime later. {@link BleDevice#getLastDisconnectIntent()} returns
	 * {@link State.ChangeIntent#UNINTENTIONAL}, which lets you know that you can probably automatically connect to this device without user confirmation.
	 */
	public Boolean manageLastDisconnectOnDisk = true;
	
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
	public Boolean cacheDeviceOnUndiscovery = true;
	
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
	 * Default is an array of {@link Interval} instances created using {@link Interval#secs()} with {@link #DEFAULT_TASK_TIMEOUT}.
	 * This is an array of timeouts whose indices are meant to map to {@link BleTask} ordinals and provide a
	 * way to control how long a given task is allowed to run before being "cut loose". If no option is provided for a given {@link BleTask},
	 * either by setting this array null, or by providing <code>null</code> or {@link Interval#DISABLED} for a given {@link BleTask} then
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
	 * Default is <code>0x0</code> - controls which {@link BleDeviceState} enter events will invoke an automatic attempt at {@link BleDevice#bond()}.
	 */
	public Integer		autoBond_stateEnter					= 0x0;
	
	/**
	 * Default is <code>0x0</code> - controls which {@link BleDeviceState} exit events will invoke an automatic attempt at {@link BleDevice#bond()}.
	 */
	public Integer		autoBond_stateExit					= 0x0;
	
	/**
	 * Default is set to bitwise OR of {@link BleDeviceState#DISCONNECTED} and {@link BleDeviceState#UNDISCOVERED}
	 * based on info from {@link android.os.Build}.
	 * Background: some android devices have issues when it comes to bonding. So far the worst culprits
	 * are certain Sony and Motorola phones, so if it looks like {@link Build#MANUFACTURER}
	 * is either one of those, this is set to true. Please look at the source for this member for the most
	 * up-to-date values. The problem seems to be associated with mismanagement of pairing keys by the OS and
	 * this brute force solution seems to be the only way to smooth things out.
	 */
	public Integer		autoUnbond_stateEnter				= phoneHasBondingIssues() ? BleDeviceState.DISCONNECTED.or(BleDeviceState.UNDISCOVERED) : 0x0;
	
	/**
	 * Default is <code>0x0</code> - controls which {@link BleDeviceState} exit events will invoke an automatic attempt at {@link BleDevice#unbond()}.
	 */
	public Integer		autoUnbond_stateExit				= 0x0;
	
	/**
	 * Default is null, meaning the library won't preemptively attempt to bond for any characteristic operations.
	 * 
	 * @see BondingFilter
	 */
	public BondingFilter bondingFilter						= null;
	
	/**
	 * Default is an instance of {@link DefaultReconnectRateLimiter} - set an implementation here to
	 * have fine control over reconnect behavior. This is basically how often and how long
	 * the library attempts to reconnect to a device that for example may have gone out of range. Set this variable to
	 * <code>null</code> if reconnect behavior isn't desired. If not <code>null</code>, your app may find
	 * {@link BleManagerConfig#manageCpuWakeLock} useful in order to force the app/device to stay awake while attempting a reconnect.
	 * 
	 * @see BleManagerConfig#manageCpuWakeLock
	 * @see ReconnectRateLimiter
	 * @see DefaultReconnectRateLimiter
	 */
	public ReconnectRateLimiter reconnectRateLimiter = new DefaultReconnectRateLimiter();
	
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
	
	static int integer(Integer value_nullable)
	{
		return value_nullable != null ? value_nullable : 0x0;
	}
	
	public BleDeviceConfig()
	{
	}
	
	private static Interval getTaskInterval(final BleTask task, final Interval[] intervals_device_nullable, final Interval[] intervals_mngr)
	{
		final int ordinal = task.ordinal();
		final Interval interval_device = intervals_device_nullable != null && intervals_device_nullable.length > ordinal ? intervals_device_nullable[ordinal] : null;
		final Interval interval_mngr = intervals_mngr != null && intervals_mngr.length > ordinal ? intervals_mngr[ordinal] : null;
		
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
	
	/**
	 * Returns true for certain Sony and Motorola products, which may have problems managing bonding state
	 * and so this method is used to set {@link #autoUnbond_stateEnter}.
	 */
	public boolean phoneHasBondingIssues()
	{
		return Utils.isManufacturer("sony") || Utils.isManufacturer("motorola") && Utils.isProduct("ghost");
	}
	
	/**
	 * Convenience method that does a bitwise OR of the given states to {@link #autoBond_stateEnter}.
	 */
	public void autoBondWhenEntering(BleDeviceState ... states)
	{
		autoBond_stateEnter = autoBond_stateEnter != null ? autoBond_stateEnter : 0x0;
		
		for( int i = 0; i < states.length; i++ )
		{
			autoBond_stateEnter |= states[i].bit();
		}
	}
	
	/**
	 * Convenience method that does a bitwise OR of the given states to {@link #autoBond_stateExit}.
	 */
	public void autoBondWhenExiting(BleDeviceState ... states)
	{
		autoBond_stateExit = autoBond_stateExit != null ? autoBond_stateExit : 0x0;
		
		for( int i = 0; i < states.length; i++ )
		{
			autoBond_stateExit |= states[i].bit();
		}
	}
	
	/**
	 * Convenience method that does a bitwise OR of the given states to {@link #autoUnbond_stateEnter}.
	 */
	public void autoUnbondWhenEntering(BleDeviceState ... states)
	{
		autoUnbond_stateEnter = autoUnbond_stateEnter != null ? autoUnbond_stateEnter : 0x0;
		
		for( int i = 0; i < states.length; i++ )
		{
			autoUnbond_stateEnter |= states[i].bit();
		}
	}
	
	/**
	 * Convenience method that does a bitwise OR of the given states to {@link #autoUnbond_stateExit}.
	 */
	public void autoUnbondWhenExiting(BleDeviceState ... states)
	{
		autoUnbond_stateExit = autoUnbond_stateExit != null ? autoUnbond_stateExit : 0x0;
		
		for( int i = 0; i < states.length; i++ )
		{
			autoUnbond_stateExit |= states[i].bit();
		}
	}
	
	private static boolean autoBondOrUnbond(final int oldStateBits, final int newStateBits, final int autoEnterMask, final int autoExitMask)
	{
		final int enterMask = newStateBits & ~oldStateBits;
		
		if( (enterMask & autoEnterMask) != 0x0 )  return true;
		
		final int exitMask = oldStateBits & ~newStateBits;
		
		if( (exitMask & autoExitMask) != 0x0 )  return true;
		
		return false;
	}
	
	boolean autoBond(final int oldStateBits, final int newStateBits)
	{
		return autoBondOrUnbond(oldStateBits, newStateBits, integer(autoBond_stateEnter), integer(autoBond_stateExit));
	}
	
	boolean autoUnbond(final int oldStateBits, final int newStateBits)
	{
		return autoBondOrUnbond(oldStateBits, newStateBits, integer(autoUnbond_stateEnter), integer(autoUnbond_stateExit));
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
