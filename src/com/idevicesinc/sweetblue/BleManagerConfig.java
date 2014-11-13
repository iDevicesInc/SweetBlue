package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Please;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener;
import com.idevicesinc.sweetblue.BleManager.UhOhListener;
import com.idevicesinc.sweetblue.utils.UuidNameMap;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.ReflectionUuidNameMap;
import com.idevicesinc.sweetblue.utils.StandardUuids;
import com.idevicesinc.sweetblue.utils.Utils;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;

/**
 * Provides a number of options to pass to the {@link BleManager#BleManager(Context, BleManagerConfig)}
 * constructor. Use {@link Interval#DISABLED} or <code>null</code> to disable any time-based options. 
 * 
 * @author dougkoellmer
 */
public class BleManagerConfig implements Cloneable
{
	public static final double DEFAULT_MINIMUM_SCAN_TIME				= 5.0;
	public static final double DEFAULT_AUTO_SCAN_INTERVAL				= 3.0;
	public static final double DEFAULT_INITIAL_RECONNECT_DELAY			= 2.0;
	public static final double DEFAULT_RECONNECT_ATTEMPT_RATE			= 4.0;
	public static final double DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME 	= 0.5;
	public static final double DEFAULT_TASK_TIMEOUT						= 10.0;
	public static final double DEFAULT_AUTO_UPDATE_RATE					= 1.01/10.0;
	public static final double DEFAULT_UH_OH_CALLBACK_THROTTLE			= 30.0;
	public static final double DEFAULT_SCAN_KEEP_ALIVE					= DEFAULT_MINIMUM_SCAN_TIME*2.5;
	public static final int DEFAULT_RUNNING_AVERAGE_N					= 10;
	
	/**
	 * Maximum amount of time for a classic scan to run. This was determined based on experimentation.
	 * Documentation says that classic scan goes on for about 12 seconds. I forget what the reasoning
	 * was for setting a time lower than this, so here's a TODO to try to remember that.
	 */
	static final double MAX_CLASSIC_SCAN_TIME							= 7.0;
	
	/**
	 * In at least some cases it's not possible to determine beforehand whether a given characteristic requires
	 * bonding, so implementing this interface on {@link BleManagerConfig#bondingFilter} lets the app give
	 * a hint to the library so it can bond before attempting to read or write an encrypted characteristic.
	 * Providing these hints lets the library handle things in a more deterministic and optimized fashion, but is not required.
	 * 
	 * @author dougkoellmer
	 */
	public static interface BondingFilter
	{
		boolean requiresBonding(UUID characteristicUuid);
	}
	
	/**
	 * An optional whitelisting mechanism for scanning. Provide an implementation at
	 * {@link BleManagerConfig#defaultAdvertisingFilter} or {@link BleManager#startScan(BleManagerConfig.AdvertisingFilter)}
	 * or {@link BleManager#startScan(BleManagerConfig.AdvertisingFilter, Interval)}.
	 * 
	 * @author dougkoellmer
	 */
	public static interface AdvertisingFilter
	{
		/**
		 * Return true to acknowledge the discovery, in which case
		 * {@link DiscoveryListener#onDeviceDiscovered(BleDevice)} will be called shortly.
		 * 
		 * @param nativeInstance		Other parameters are probably enough to make a decision but this native instance is provided just in case.
		 * @param advertisedServices	A list of {@link UUID}s parsed from {@code scanRecord} as a convenience. May be empty, notably
		 * 								if {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is invoked.
		 * @param rawDeviceName			The unaltered device name retrieved from the native bluetooth stack.
		 * @param normalizedDeviceName	See {@link BleDevice#getNormalizedName()} for an explanation.
		 * @param scanRecord			The raw scan record received when the device was discovered. May be empty, especially
		 * 								if {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is invoked.
		 * @param rssi					The RSSI received when the device was discovered.
		 * 
		 * @return						Whether to acknowledge the discovery.
		 */
		boolean acknowledgeDiscovery(BluetoothDevice nativeInstance, List<UUID> advertisedServices, String rawDeviceName, String normalizedDeviceName, byte[] scanRecord, int rssi);
	}
	
	/**
	 * Convenience implementation of {@link AdvertisingFilter} which filters using
	 * a whitelist of known primary advertising {@link UUID}s passed in to the constructor.
	 * 
	 * @author dougkoellmer
	 */
	public static class DefaultAdvertisingFilter implements AdvertisingFilter
	{
		private final ArrayList<UUID> m_whitelist;
		
		public DefaultAdvertisingFilter(Collection<UUID> whitelist)
		{
			m_whitelist = new ArrayList<UUID>(whitelist);
		}
		
		/**
		 * Acknowledges the discovery if there's an overlap between the given advertisedServices
		 * and the {@link Collection} passed into {@link #DefaultAdvertisingFilter(Collection)}.
		 */
		@Override public boolean acknowledgeDiscovery(BluetoothDevice nativeInstance, List<UUID> advertisedServices, String rawDeviceName, String normalizedDeviceName, byte[] scanRecord, int rssi)
		{
			return Utils.haveMatchingIds(advertisedServices, m_whitelist);
		}
	}
	
	/**
	 * An optional interface you can implement on {@link BleManagerConfig#reconnectRateLimiter } to control reconnection behavior.
	 * 
	 * @see #reconnectRateLimiter
	 * @see DefaultReconnectRateLimiter
	 * 
	 * @author dougkoellmer
	 */
	public static interface ReconnectRateLimiter
	{
		/**
		 * Return this from {@link #getTimeToNextReconnect(BleDevice, int, Interval, Interval)} to instantly reconnect.
		 */
		public static final Interval INSTANTLY = Interval.seconds(0.0);
		
		/**
		 * Return this from {@link #getTimeToNextReconnect(BleDevice, int, Interval, Interval)} to stop a reconnect attempt loop.
		 */
		public static final Interval CANCEL = Interval.seconds(-1.0);
		
		/**
		 * Called for every connection failure while device is {@link DeviceState#ATTEMPTING_RECONNECT}.
		 * Use the static members of this interface to create return values to stop reconnection or try again
		 * instantly. Use static methods of {@link Interval} to try again after some amount of time. Numeric parameters
		 * are provided in order to give the app a variety of ways to calculate the next delay. Use all, some, or none of them.
		 */
		Interval getTimeToNextReconnect(BleDevice device, int connectFailureCount, Interval totalTimeReconnecting, Interval previousDelay);
	}
	
	/**
	 * Default implementation of {@link ReconnectRateLimiter} that uses {@link #DEFAULT_INITIAL_RECONNECT_DELAY}
	 * and {@link #DEFAULT_RECONNECT_ATTEMPT_RATE} to infinitely try to reconnect.
	 * 
	 * @author dougkoellmer
	 */
	public static class DefaultReconnectRateLimiter implements ReconnectRateLimiter
	{
		@Override public Interval getTimeToNextReconnect(BleDevice device, int connectFailureCount, Interval totalTimeReconnecting, Interval previousDelay)
		{
			if( connectFailureCount == 0 )
			{
				return Interval.seconds(DEFAULT_INITIAL_RECONNECT_DELAY);
			}
			else
			{
				return Interval.seconds(DEFAULT_RECONNECT_ATTEMPT_RATE);
			}
		}
	}
	
	/**
	 * Default is false - basically only useful for developers working on the library itself.
	 * May also be useful for providing context when reporting bugs.
	 */
	public boolean loggingEnabled						= false;
	
	/**
	 * Default is true - whether all callbacks are posted to the main thread or from SweetBlue's internal
	 * thread. If {@link #runOnMainThread}==true then this setting is meaningless because SweetBlue's
	 * internal thread is already the main thread to begin with.
	 */
	public boolean postCallbacksToMainThread			= true;
	
	/**
	 * Default is false - this option may help mitigate crashes with "Unfortunately,
	 * Bluetooth Share has stopped" error messages. See https://github.com/RadiusNetworks/bluetooth-crash-resolver or
	 * http://developer.radiusnetworks.com/2014/04/02/a-solution-for-android-bluetooth-crashes.html or
	 * Google "Bluetooth Crash Resolver".
	 */
	public boolean enableCrashResolver					= false;
	
	/**
	 * Default is true - makes it so {@link BleManager#stopScan()} is called after {@link BleManager#onPause()}.
	 * 
	 * @see BleManager#onPause()
	 */
	public boolean stopScanOnPause						= true;
	
	/**
	 * Default is false - use this option to globally force bonding after a
	 * {@link BleDevice} is {@link DeviceState#CONNECTED} if it is not {@link DeviceState#BONDED} already.
	 */
	public boolean autoBondAfterConnect					= false;
	
	/**
	 * Default is true - whether to automatically get services immediately after a {@link BleDevice} is
	 * {@link DeviceState#CONNECTED}. Currently this is the only way to get a device's services.
	 */
	public boolean autoGetServices						= true;
	
	/**
	 * Some android devices have known issues when it comes to bonding. So far the worst culprits
	 * are Xperias. To be safe this is set to true by default if we're running on a Sony device.
	 * The problem seems to be associated with mismanagement of pairing keys by the OS and
	 * this brute force solution seems to be the only way to smooth things out.
	 */
	public boolean removeBondOnDisconnect				= Utils.isSony();
	
	/**
	 * Default is same as {@link #removeBondOnDisconnect} - see {@link #removeBondOnDisconnect} for explanation.
	 */
	public boolean removeBondOnDiscovery				= removeBondOnDisconnect;
	
	/**
	 * Default is false - set this to allow or disallow autoscanning while any
	 * {@link BleDevice} is {@link DeviceState#UPDATING_FIRMWARE}. If false,
	 * then firmware updates may complete faster if you're periodically scanning
	 * through {@link #autoScanTime} or {@link BleManager#startPeriodicScan(Interval, Interval)}.
	 * {@link BleManager#startScan()} will still start a scan regardless.
	 */
	public boolean autoScanDuringFirmwareUpdates		= false;
	
	/**
	 * Default is false - if true and you call {@link BleDevice#startPoll(UUID, Interval, BleDevice.ReadWriteListener)}
	 * or {@link BleDevice#startChangeTrackingPoll(UUID, Interval, BleDevice.ReadWriteListener)()} with identical
	 * parameters then two identical polls would run which would probably be wasteful and unintentional.
	 * This option provides a defense against that situation.
	 */
	public boolean allowDuplicatePollEntries			= false;
	
	/**
	 * Default is true - SweetBlue uses {@link BluetoothAdapter#startLeScan()} by default but for unknown
	 * reasons this can fail sometimes. In this case SweetBlue can revert to using classic bluetooth
	 * discovery through {@link BluetoothAdapter#startDiscovery()}. Be aware that classic
	 * discovery may not discover some or any advertising BLE devices, nor will it provide
	 * a scanRecord or advertisedServices to {@link AdvertisingFilter#acknowledgeDiscovery}.
	 * As such this is meant as a back-up solution for BLE scanning, not something to be relied on.
	 */
	public boolean revertToClassicDiscoveryIfNeeded		= true;
	
	/**
	 * Default is true - SweetBlue originally ran most operations on its own internal thread based on
	 * some theories that since proved invalid. While the library can still sort of do so, it's now 
	 * recommended to run on the main thread in order to avoid any possible multithreading issues.
	 */
	public boolean runOnMainThread						= true;
	
	/**
	 * Default is true - requires the {@link Manifest.permission#WAKE_LOCK} permission in your app's manifest file.
	 * It should look like this: {@code<uses-permission android:name="android.permission.WAKE_LOCK" />}
	 * Sets whether the library will attempt to obtain a wake lock in certain situations.
	 * For now the only situation is when there are no remote bluetooth devices
	 * connected but one or more devices are {@link DeviceState#ATTEMPTING_RECONNECT}.
	 * The wake lock will be released when devices are reconnected (e.g. from coming back
	 * into range) or when reconnection is stopped either through {@link BleDevice#disconnect()} or returning
	 * {@link ReconnectRateLimiter#CANCEL} from {@link ReconnectRateLimiter#getTimeToNextReconnect(BleDevice, int, double, double)}.
	 * Wake locks will also be released if Bluetooth is turned off either from the App or OS settings.
	 * Note that Android itself uses some kind of implicit wake lock when you are connected to
	 * one or more devices and requires no explicit wake lock nor any extra permissions to do so.  
	 */
	public boolean manageCpuWakeLock					= true;
	
	/**
	 * Default is false - {@link BleDevice#getAverageReadTime()} and {@link BleDevice#getAverageWriteTime()} can be 
	 * skewed if the peripheral you are connecting to adjusts its maximum throughput for OTA firmware updates.
	 * Use this option to let the library know whether you want firmware update read/writes to factor in.
	 * 
	 * @see BleDevice#getAverageReadTime()
	 * @see BleDevice#getAverageWriteTime() 
	 */
	public boolean includeFirmwareUpdateReadWriteTimesInAverage = false;
	
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
	 * HOWEVER, it's important to note that the library WILL automatically revert to autoConnect==true after a first failed
	 * connection if you do a retry by returning {@link BleDevice.ConnectionFailListener.Please#RETRY} from
	 * {@link ConnectionFailListener#onConnectionFail(BleDevice, BleDevice.ConnectionFailListener.E_Reason, int)}.
	 * <br><br>
	 * So really this option mainly exists for those situations where you KNOW that you have a device that only works
	 * with autoConnect==true and you want connection time to be faster (i.e. you don't want to wait for that first
	 * failed connection for the library to internally start using autoConnect==true).
	 */
	public boolean alwaysUseAutoConnect = false;
	
	/**
	 * Default is {@value #DEFAULT_UH_OH_CALLBACK_THROTTLE} seconds - {@link UhOh} callbacks from {@link UhOhListener}
	 * can be a little spammy at times so this is an option to throttle them back on a per-{@link UhOh} basis.
	 * Set this to {@link Interval#DISABLED} to receive all every {@link UhOh} and manage them yourself.
	 * 
	 * @see BleManager.UhOhListener
	 */
	public Interval	uhOhCallbackThrottle				= Interval.seconds(DEFAULT_UH_OH_CALLBACK_THROTTLE);
	
	/**
	 * Default is {@value #DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME} seconds - Unless {@link Interval#DISABLED},
	 * this option will kick off a scan for {@link #autoScanTime} seconds
	 * {@link #autoScanDelayAfterResume} seconds after {@link BleManager#onResume()} is called.
	 */
	public Interval autoScanDelayAfterResume			= Interval.seconds(DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME);
	
	/**
	 * Default is {@value #DEFAULT_AUTO_UPDATE_RATE} seconds - The rate at which the library's internal update loop ticks.
	 * Generally shouldn't need to be changed. You can set this to {@link Interval#DISABLED} and call {@link BleManager#update(double)} yourself
	 * if you want to tie the library in to an existing update loop used in your application.
	 */
	public Interval autoUpdateRate						= Interval.seconds(DEFAULT_AUTO_UPDATE_RATE);
	
	/**
	 * Default is {@link Interval#DISABLED} - Length of time in seconds that the library will automatically scan for devices. Used in conjunction with {@link #autoScanInterval},
	 * this option allows the library to periodically send off scan "pulses" that last {@link #autoScanTime} seconds.
	 * Use {@link BleManager#startPeriodicScan(Interval, Interval)} to adjust this behavior while the library is running.
	 * If either {@link #autoScanTime} or {@link #autoScanInterval} is {@link Interval#DISABLED} then auto scanning is disabled.
	 * It can also be turned off with {@link BleManager#stopPeriodicScan()}.
	 * 
	 * @see #autoScanInterval
	 * @see BleManager#startPeriodicScan(Interval, Interval)
	 * @see BleManager#stopPeriodicScan()
	 */
	public Interval autoScanTime						= Interval.DISABLED; //Interval.seconds(DEFAULT_MINIMUM_SCAN_TIME);
	
	/**
	 * Default is {@value #DEFAULT_AUTO_SCAN_INTERVAL} seconds - Length of time in seconds between automatic scan pulses defined by {@link #autoScanTime}.
	 * 
	 * @see #autoScanTime
	 */
	public Interval autoScanInterval					= Interval.seconds(DEFAULT_AUTO_SCAN_INTERVAL);
	
	/**
	 * Default is {@link Interval#DISABLED} - Same as {@link #autoScanInterval} except this value is used while the app is paused.
	 * 
	 * @see #autoScanInterval
	 * @see BleManager#onPause()
	 */
	public Interval autoScanIntervalWhileAppIsPaused	= Interval.DISABLED;
	
	/**
	 * Default is {@link Interval#DEFAULT_MINIMUM_SCAN_TIME} seconds - Minimum amount of time in seconds that the library strives to give to a scanning operation.  
	 */
	public Interval	idealMinScanTime					= Interval.seconds(DEFAULT_MINIMUM_SCAN_TIME);
	
	/**
	 * Default is {@link Interval#DEFAULT_MINIMUM_SCAN_TIME} seconds - Undiscovery of devices must be
	 * approximated by checking when the last time was that we discovered a device,
	 * and if this time is greater than {@link #scanKeepAlive} then the device is undiscovered. However a scan
	 * operation must be allowed a certain amount of time to make sure it discovers all nearby devices that are
	 * still advertising. This is that time in seconds.
	 * 
	 * @see BleManager.DiscoveryListener#onDeviceUndiscovered(BleDevice)
	 * @see #scanKeepAlive
	 */
	public Interval	minScanTimeToInvokeUndiscovery		= Interval.seconds(DEFAULT_MINIMUM_SCAN_TIME);
	
	/**
	 * Default is {@link Interval#DEFAULT_SCAN_KEEP_ALIVE} seconds - If a device exceeds this amount of time since its
	 * last discovery then it is a candidate for being undiscovered.
	 * The default for this option attempts to accommodate the worst Android phones (BLE-wise), which may make it seem
	 * like it takes a long time to undiscover a device. You may want to configure this number based on the phone or
	 * manufacturer. For example, based on testing, in order to make undiscovery snappier the Galaxy S5 could use lower times.
	 * 
	 * @see BleManager.DiscoveryListener#onDeviceUndiscovered(BleDevice)
	 * @see #minScanTimeToInvokeUndiscovery
	 */
	public Interval	scanKeepAlive						= Interval.seconds(DEFAULT_SCAN_KEEP_ALIVE);
	
	/**
	 * Default is {@link Interval#DEFAULT_RUNNING_AVERAGE_N} - The number of historical write times that the library should keep track of when calculating average time.
	 * 
	 * @see BleDevice#getAverageWriteTime()
	 * @see #nForAverageRunningReadTime
	 */
	public int		nForAverageRunningWriteTime			= DEFAULT_RUNNING_AVERAGE_N;
	
	/**
	 * Default is {@link Interval#DEFAULT_RUNNING_AVERAGE_N} - Same thing as {@link #nForAverageRunningWriteTime} but for reads.
	 * 
	 * @see BleDevice#getAverageWriteTime()
	 * @see #nForAverageRunningWriteTime
	 */
	public int		nForAverageRunningReadTime			= DEFAULT_RUNNING_AVERAGE_N;
	
	/**
	 * Default is null, meaning no filtering - all discovered devices will
	 * be piped through your {@link BleManager.DiscoveryListener} instance.
	 * 
	 * @see AdvertisingFilter
	 */
	public AdvertisingFilter defaultAdvertisingFilter;
	
	/**
	 * Default is null, meaning the library won't preemptively attempt to bond for any characteristic operations.
	 * @see BondingFilter
	 */
	public BondingFilter bondingFilter;
	
	/**
	 * Default is null - Can also be set post-construction with {@link BleManager#setListener_Discovery(DiscoveryListener)},
	 * which will override the implementation provided here.
	 * 
	 * @see BleManager.DiscoveryListener
	 */
	public BleManager.DiscoveryListener defaultDiscoveryListener = null;
	
	/**
	 * Default is an instance of {@link DefaultReconnectRateLimiter} - set an implementation here to
	 * have fine control over reconnect behavior. This is basically how often and how long
	 * the library attempts to reconnect to a device that for example may have gone out of range. Set this variable to
	 * <code>null</code> if reconnect behavior isn't desired. If not <code>null</code>, your app may find
	 * {@link #manageCpuWakeLock} useful in order to force the app/device to stay awake while attempting a reconnect.
	 * 
	 * @see #manageCpuWakeLock
	 * @see ReconnectRateLimiter
	 * @see DefaultReconnectRateLimiter
	 */
	public ReconnectRateLimiter reconnectRateLimiter = new DefaultReconnectRateLimiter();
	
	/**
	 * Used if {@link #loggingEnabled} is true. Gives threads names so they are more easily identifiable.
	 */
	final String[] debugThreadNames =
	{
		"AMY", "BEN", "CAM", "DON", "ELI", "FAY", "GUS", "HAL", "IAN", "JAY", "LEO",
		"MAX", "NED", "OLA", "PAT", "RON", "SAL", "TED", "VAL", "WES", "YEE", "ZED"
	};
	
	/**
	 * Default is null - optional, only used if {@link #loggingEnabled} is true. Provides a look-up table
	 * so logs can show the name associated with a {@link UUID} along with its numeric string.
	 */
	public List<UuidNameMap> uuidNameMaps = null;
	
	//--- DRK > Not sure if this is useful so keeping it package private for now.
	int		connectionFailUhOhCount						= 0;
	
	public BleManagerConfig()
	{
		this(false);
	}
	
	/**
	 * Convenience constructor that populates {@link #uuidNameMaps} with {@link StandardUuids}
	 * using {@link ReflectionUuidNameMap} if logging is enabled.
	 * 
	 * @param loggingEnabled_in Sets {@link #loggingEnabled}.
	 */
	public BleManagerConfig(boolean loggingEnabled_in)
	{
		this.loggingEnabled = loggingEnabled_in;
		
		if( this.loggingEnabled )
		{
			uuidNameMaps = new ArrayList<UuidNameMap>();
			uuidNameMaps.add(new ReflectionUuidNameMap(StandardUuids.class));
		}
	}
	
	@Override protected BleManagerConfig clone()
	{
		try
		{
			return (BleManagerConfig) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
		}
		
		return null;
	}
}
