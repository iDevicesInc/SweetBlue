package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.app.Application;

import com.idevicesinc.sweetblue.BleManager.DiscoveryListener;
import com.idevicesinc.sweetblue.BleManager.UhOhListener;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.ReflectionUuidNameMap;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.UuidNameMap;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * Provides a number of options to pass to the {@link BleManager#BleManager(Application, BleManagerConfig)}
 * constructor. Use {@link Interval#DISABLED} or <code>null</code> to disable any time-based options.
 */
public class BleManagerConfig extends BleDeviceConfig
{
	public static final double DEFAULT_AUTO_SCAN_INTERVAL				= 3.0;
	public static final double DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME 	= 0.5;
	public static final double DEFAULT_TASK_TIMEOUT						= 10.0;
	public static final double DEFAULT_AUTO_UPDATE_RATE					= 1.01/30.0;
	public static final double DEFAULT_UH_OH_CALLBACK_THROTTLE			= 30.0;
	
	/**
	 * Maximum amount of time for a classic scan to run. This was determined based on experimentation.
	 * Documentation says that classic scan goes on for about 12 seconds. I forget what the reasoning
	 * was for setting a time lower than this, so here's a TODO to try to remember that.
	 */
	static final double MAX_CLASSIC_SCAN_TIME							= 7.0;
	
	/**
	 * An optional whitelisting mechanism for scanning. Provide an implementation at
	 * {@link BleManagerConfig#defaultAdvertisingFilter} or one of the various {@link BleManager#startScan()}
	 * overloads, i.e. {@link BleManager#startScan(BleManagerConfig.AdvertisingFilter)},
	 * {@link BleManager#startScan(Interval, BleManagerConfig.AdvertisingFilter)}, etc.
	 */
	public static interface AdvertisingFilter
	{
		/**
		 * This enum gives you an indication of the last interaction with a device between app sessions or
		 * in-app BLE {@link BleState#OFF}->{@link BleState#ON} cycles, which basically means how it was last {@link BleDeviceState#DISCONNECTED}.
		 * Passed to {@link BleManagerConfig.AdvertisingFilter#acknowledgeDiscovery(BleManagerConfig.AdvertisingFilter.Packet)}
		 * and {@link BleManager.DiscoveryListener#onDeviceDiscovered(BleDevice, LastDisconnect)}.
		 * <br><br>
		 * See further explanation at {@link BleDeviceConfig#manageLastDisconnectOnDisk}.
		 * <br><br>
		 * TIP: If {@link Packet#lastDisconnect} isn't {@link LastDisconnect#UNKNOWN} then most likely you can early-out
		 * and return <code>true</code> from {@link AdvertisingFilter#acknowledgeDiscovery(Packet)} without having to check
		 * uuids or names matching.
		 */
		public static enum LastDisconnect
		{
			/**
			 * The last disconnect is unknown because (a) device has never been seen before, (b)
			 * reason for disconnect was app being killed and {@link BleDeviceConfig#saveDisconnectReasonToDisk}
			 * was <code>false</code>, (c) app user cleared app data between app sessions, (d) etc., etc.
			 */
			UNKNOWN,
			
			/**
			 * From a user experience perspective, the user may not have wanted the
			 * disconnect to happen, and thus *probably* would want to be automatically connected again
			 * as soon as the device is discovered.
			 */
			UNINTENTIONAL,
			
			/**
			 * The last reason the device was {@link BleDeviceState#DISCONNECTED} was because {@link BleDevice#disconnect()}
			 * was called, which most-likely means the user doesn't want to automatically connect to this device again.
			 */
			INTENTIONAL;
			
			private static final int DISK_VALUE__UNKNOWN		= -1;
			private static final int DISK_VALUE__UNINTENTIONAL	= 0;
			private static final int DISK_VALUE__INTENTIONAL	= 1;
			
			int toDiskValue()
			{
				switch(this)
				{
					case INTENTIONAL:		return DISK_VALUE__INTENTIONAL;
					case UNINTENTIONAL:		return DISK_VALUE__UNINTENTIONAL;
					case UNKNOWN:
					default:				return DISK_VALUE__UNKNOWN;
				}
			}
			
			static LastDisconnect fromDiskValue(int diskValue)
			{
				switch(diskValue)
				{
					case DISK_VALUE__INTENTIONAL:		return INTENTIONAL;
					case DISK_VALUE__UNINTENTIONAL:		return UNINTENTIONAL;
					case DISK_VALUE__UNKNOWN:
					default:							return UNKNOWN;
				}
			}
		}
		
		/**
		 * Instances of this class are passed to {@link AdvertisingFilter#acknowledgeDiscovery(Packet)} to aid in making a decision.
		 */
		public static class Packet
		{
			/**
			 * Other parameters are probably enough to make a decision but this native instance is provided just in case.
			 */
			public final BluetoothDevice nativeInstance;
			
			/**
			 * A list of {@link UUID}s parsed from {@code scanRecord} as a convenience. May be empty, notably
			 * if {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is invoked.
			 */
			public final List<UUID> advertisedServices;
			
			/**
			 * The unaltered device name retrieved from the native bluetooth stack.
			 */
			public final String rawDeviceName;
			
			/**
			 * See {@link BleDevice#getName_normalized()} for an explanation.
			 */
			public final String normalizedDeviceName;
			
			/**
			 * The raw scan record received when the device was discovered. May be empty, especially
			 * if {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is invoked.
			 */
			public final byte[] scanRecord;
			
			/**
			 * The RSSI received when the device was discovered.
			 */
			public final int rssi;
			
			/**
			 * The {@link LastDisconnect} of {@link #nativeInstance}.
			 */
			public final LastDisconnect lastDisconnect;
			
			Packet(BluetoothDevice nativeInstance_in, List<UUID> advertisedServices_in, String rawDeviceName_in, String normalizedDeviceName_in, byte[] scanRecord_in, int rssi_in, LastDisconnect lastDisconnect_in)
			{
				this.nativeInstance = nativeInstance_in;
				this.advertisedServices = advertisedServices_in;
				this.rawDeviceName = rawDeviceName_in;
				this.normalizedDeviceName = normalizedDeviceName_in;
				this.scanRecord = scanRecord_in;
				this.rssi = rssi_in;
				this.lastDisconnect = lastDisconnect_in;
			}
		}
		
		/**
		 * Return true to acknowledge the discovery, in which case {@link DiscoveryListener#onDeviceDiscovered(BleDevice, LastDisconnect)} will be called shortly.
		 * 
		 * @return Whether to acknowledge the discovery.
		 */
		boolean acknowledgeDiscovery(Packet packet);
	}
	
	/**
	 * Convenience implementation of {@link AdvertisingFilter} which filters using
	 * a whitelist of known primary advertising {@link UUID}s passed in to the constructor.
	 */
	public static class DefaultAdvertisingFilter implements AdvertisingFilter
	{
		private final ArrayList<UUID> m_whitelist;
		
		public DefaultAdvertisingFilter(Collection<UUID> whitelist)
		{
			m_whitelist = new ArrayList<UUID>(whitelist);
		}
		
		public DefaultAdvertisingFilter(UUID whitelist)
		{
			m_whitelist = new ArrayList<UUID>();
			m_whitelist.add(whitelist);
		}
		
		/**
		 * Acknowledges the discovery if there's an overlap between the given advertisedServices
		 * and the {@link Collection} passed into {@link BleManagerConfig.DefaultAdvertisingFilter#DefaultAdvertisingFilter(Collection)}.
		 */
		@Override public boolean acknowledgeDiscovery(Packet packet)
		{
			return Utils.haveMatchingIds(packet.advertisedServices, m_whitelist);
		}
	}
	
	/**
	 * Default is false - basically only useful for developers working on the library itself.
	 * May also be useful for providing context when reporting bugs.
	 */
	public boolean loggingEnabled						= false;
	
	/**
	 * Default is false - this option may help mitigate crashes with "Unfortunately,
	 * Bluetooth Share has stopped" error messages. See https://github.com/RadiusNetworks/bluetooth-crash-resolver or
	 * http://developer.radiusnetworks.com/2014/04/02/a-solution-for-android-bluetooth-crashes.html or
	 * Google "Bluetooth Crash Resolver" for more information.
	 */
	public boolean enableCrashResolver					= false;
	
	/**
	 * Default is true - makes it so {@link BleManager#stopScan()} is called automatically after {@link BleManager#onPause()}.
	 * If you're doing an infinite scan (e.g. by calling {@link BleManager#startScan()}, i.e. no timeout), then the scan
	 * will automatically continue after {@link BleManager#onResume()} is called.
	 * 
	 * @see BleManager#onPause()
	 */
	public boolean stopScanOnPause						= true;
	
	/**
	 * Default is false - set this to allow or disallow autoscanning while any
	 * {@link BleDevice} is {@link BleDeviceState#UPDATING_FIRMWARE}. If false,
	 * then firmware updates may complete faster if you're periodically scanning
	 * through {@link #autoScanTime} or {@link BleManager#startPeriodicScan(Interval, Interval)}.
	 * {@link BleManager#startScan()} will still start a scan regardless.
	 */
	public boolean autoScanDuringFirmwareUpdates		= false;
	
	/**
	 * Default is true - SweetBlue uses {@link BluetoothAdapter#startLeScan()} by default but for unknown
	 * reasons this can fail sometimes. In this case SweetBlue can revert to using classic bluetooth
	 * discovery through {@link BluetoothAdapter#startDiscovery()}. Be aware that classic
	 * discovery may not discover some or any advertising BLE devices, nor will it provide
	 * a scanRecord or advertisedServices to {@link AdvertisingFilter#acknowledgeDiscovery}.
	 * Most likely you will be forced to filter on name only for your implementation of
	 * {@link AdvertisingFilter#acknowledgeDiscovery(Packet)}.
	 * As such this is meant as a better-than-nothing back-up solution for BLE scanning.
	 */
	public boolean revertToClassicDiscoveryIfNeeded		= true;
	
	/**
	 * Default is true - SweetBlue originally ran most operations on its own internal thread based on
	 * some theories that since proved invalid. While the library can still sort of do so, it's now 
	 * recommended to run on the main thread in order to avoid any possible multithreading issues.
	 */
	boolean runOnMainThread						= true;
	
	/**
	 * Default is true - whether all callbacks are posted to the main thread or from SweetBlue's internal
	 * thread. If {@link #runOnMainThread}==true then this setting is meaningless because SweetBlue's
	 * internal thread is already the main thread to begin with.
	 */
	boolean postCallbacksToMainThread			= true;
	
	/**
	 * Default is true - requires the {@link Manifest.permission#WAKE_LOCK} permission in your app's manifest file.
	 * It should look like this: {@code <uses-permission android:name="android.permission.WAKE_LOCK" />}
	 * Sets whether the library will attempt to obtain a wake lock in certain situations.
	 * For now the only situation is when there are no remote bluetooth devices
	 * connected but one or more devices are {@link BleDeviceState#ATTEMPTING_RECONNECT}.
	 * The wake lock will be released when devices are reconnected (e.g. from coming back
	 * into range) or when reconnection is stopped either through {@link BleDevice#disconnect()} or returning
	 * {@link ReconnectRateLimiter#CANCEL} from {@link ReconnectRateLimiter#getTimeToNextReconnect(BleDevice, int, Interval, Interval)}.
	 * Wake locks will also be released if Bluetooth is turned off either from the App or OS settings.
	 * Note that Android itself uses some kind of implicit wake lock when you are connected to
	 * one or more devices and requires no explicit wake lock nor any extra permissions to do so.  
	 */
	public boolean manageCpuWakeLock					= true;
	
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
	 * Default is {@link #DEFAULT_MINIMUM_SCAN_TIME} seconds - Minimum amount of time in seconds that the library strives to give to a scanning operation.  
	 */
	public Interval	idealMinScanTime					= Interval.seconds(DEFAULT_MINIMUM_SCAN_TIME);
	
	/**
	 * Default is null, meaning no filtering - all discovered devices will
	 * be piped through your {@link BleManager.DiscoveryListener} instance.
	 * 
	 * @see AdvertisingFilter
	 */
	public AdvertisingFilter defaultAdvertisingFilter				= null;
	
	/**
	 * Default is null - Can also be set post-construction with {@link BleManager#setListener_Discovery(DiscoveryListener)},
	 * which will override the implementation provided here.
	 * 
	 * @see BleManager.DiscoveryListener
	 */
	public BleManager.DiscoveryListener defaultDiscoveryListener	= null;
	
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
	
	/**
	 * Creates a {@link BleManagerConfig} with all default options set. See each member of this class
	 * for what the default options are set to.
	 */
	public BleManagerConfig()
	{
		this(false);
	}
	
	/**
	 * Convenience constructor that populates {@link #uuidNameMaps} with {@link Uuids}
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
			uuidNameMaps.add(new ReflectionUuidNameMap(Uuids.class));
		}
	}
	
	@Override protected BleManagerConfig clone()
	{
		return (BleManagerConfig) super.clone();
	}
}
