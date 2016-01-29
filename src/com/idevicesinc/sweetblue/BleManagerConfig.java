package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.SparseArray;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.ReflectionUuidNameMap;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.UpdateLoop;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.UuidNameMap;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * Provides a number of options to pass to the {@link BleManager#get(Context, BleManagerConfig)}
 * singleton getter or {@link BleManager#setConfig(BleManagerConfig)}.
 * Use {@link Interval#DISABLED} or <code>null</code> to disable any time-based options.
 */
public class BleManagerConfig extends BleDeviceConfig
{
	/**
	 * Default value for {@link #autoScanPauseInterval}.
	 */
	public static final double DEFAULT_AUTO_SCAN_PAUSE_TIME				= 3.0;

	/**
	 * Default value for {@link #autoScanDelayAfterResume}.
	 */
	public static final double DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME 	= 0.5;

	/**
	 * Default value for {@link #autoUpdateRate}.
	 */
	public static final double DEFAULT_AUTO_UPDATE_RATE					= 1.01/50.0;

	/**
	 * Default value for {@link #uhOhCallbackThrottle}.
	 */
	public static final double DEFAULT_UH_OH_CALLBACK_THROTTLE			= 30.0;

	/**
	 * Default value for {@link #scanReportDelay}.
	 */
	public static final double DEFAULT_SCAN_REPORT_DELAY				= .5;
	
	static final BleManagerConfig NULL = new BleManagerConfig();
	
	/**
	 * Maximum amount of time for a classic scan to run. This was determined based on experimentation.
	 * Documentation says that classic scan goes on for about 12 seconds. I forget what the reasoning
	 * was for setting a time lower than this, so here's a TODO to try to remember that.
	 */
	static final double MAX_CLASSIC_SCAN_TIME							= 7.0;
	
	/**
	 * Default is <code>false</code> - basically only useful for developers working on the library itself.
	 * May also be useful for providing context when reporting bugs.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean loggingEnabled							= false;
	
	/**
	 * Default is <code>false</code> - this option may help mitigate crashes with "Unfortunately,
	 * Bluetooth Share has stopped" error messages. See https://github.com/RadiusNetworks/bluetooth-crash-resolver or
	 * http://developer.radiusnetworks.com/2014/04/02/a-solution-for-android-bluetooth-crashes.html or
	 * Google "Bluetooth Crash Resolver" for more information.
	 * <br><br>
	 * NOTE:" This option gates a "proactive" approach towards mitigating the above-described crash.
	 * 
	 * @see #enableCrashResolverForReset
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean enableCrashResolver						= false;
	
	/**
	 * Default is <code>true</code> - this option gates whether the "crash resolver" described in {@link #enableCrashResolver}
	 * is invoked during a {@link BleManager#reset()} operation to forcefully clear the memory that causes the crash.
	 * 
	 * @see #enableCrashResolver
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean enableCrashResolverForReset				= true;
	
	/**
	 * Default is <code>true</code> - makes it so {@link BleManager#stopScan()} is called automatically after {@link BleManager#onPause()}.
	 * If you're doing an infinite scan (e.g. by calling {@link BleManager#startScan()}, i.e. no timeout), then the scan
	 * will automatically continue after {@link BleManager#onResume()} is called.
	 * 
	 * @see BleManager#onPause()
	 */
	public boolean stopScanOnPause							= true;
	
	/**
	 * Default is <code>false</code> - set this to allow or disallow autoscanning while any
	 * {@link BleDevice} is {@link BleDeviceState#PERFORMING_OTA}. If false,
	 * then OTAs may complete faster if you're periodically scanning
	 * through {@link #autoScanActiveTime} or {@link BleManager#startPeriodicScan(Interval, Interval)}.
	 * {@link BleManager#startScan()} will still start a scan regardless.
	 */
	public boolean autoScanDuringOta						= false;
	
	/**
	 * Default is <code>true</code> - SweetBlue uses {@link BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)} by default but for unknown
	 * reasons this can fail sometimes. In this case SweetBlue can revert to using classic bluetooth
	 * discovery through {@link BluetoothAdapter#startDiscovery()}. Be aware that classic
	 * discovery may not discover some or any advertising BLE devices, nor will it provide
	 * a {@link BleManagerConfig.ScanFilter.ScanEvent#scanRecord} or {@link BleManagerConfig.ScanFilter.ScanEvent#advertisedServices}
	 * to {@link BleManagerConfig.ScanFilter#onEvent(BleManagerConfig.ScanFilter.ScanEvent)}.
	 * Most likely you will be forced to filter on name only for your implementation of
	 * {@link BleManagerConfig.ScanFilter#onEvent(BleManagerConfig.ScanFilter.ScanEvent)}.
	 * As such this is meant as a better-than-nothing back-up solution for BLE scanning.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean revertToClassicDiscoveryIfNeeded			= true;
	
	/**
	 * Default is <code>true</code> - SweetBlue originally ran most operations on its own internal thread based on
	 * some theories that since proved invalid. While the library can still sort of do so, it's now 
	 * recommended to run on the main thread in order to avoid any possible multithreading issues.
	 */
	boolean runOnMainThread									= true;
	
	/**
	 * Default is <code>true</code> - whether all callbacks are posted to the main thread or from SweetBlue's internal
	 * thread. If {@link #runOnMainThread}==true then this setting is meaningless because SweetBlue's
	 * internal thread is already the main thread to begin with.
	 */
	boolean postCallbacksToMainThread						= true;
	
	/**
	 * Default is <code>true</code> - requires the {@link android.Manifest.permission#WAKE_LOCK} permission in your app's manifest file.
	 * It should look like this: {@code <uses-permission android:name="android.permission.WAKE_LOCK" />}
	 * Sets whether the library will attempt to obtain a wake lock in certain situations.
	 * For now the only situation is when there are no remote bluetooth devices
	 * {@link BleDeviceState#CONNECTED} but one or more devices are {@link BleDeviceState#RECONNECTING_LONG_TERM}.
	 * The wake lock will be released when devices are reconnected (e.g. from coming back
	 * into range) or when reconnection is stopped either through {@link BleDevice#disconnect()} or returning
	 * {@link BleNodeConfig.ReconnectFilter.Please#stopRetrying()} from
	 * {@link BleNodeConfig.ReconnectFilter#onEvent(BleNodeConfig.ReconnectFilter.ReconnectEvent)}.
	 * Wake locks will also be released if Bluetooth is turned off either from the App or OS settings.
	 * Note that Android itself uses some kind of implicit wake lock when you are connected to
	 * one or more devices and requires no explicit wake lock nor any extra permissions to do so.  
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean manageCpuWakeLock						= true;

	/**
	 * Default is <code>false</code> - Only applicable for Lollipop and up (i.e. > 5.0), this will force the use of the deprecated "pre-lollipop"
	 * scanning API, that is {@link BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)}. There is no known reason you would
	 * want this, but including it just in case.
	 *
	 * @deprecated Use {@link #scanMode} with {@link BleScanMode#PRE_LOLLIPOP} instead.
	 */
	@Deprecated
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean forcePreLollipopScan						= false;

	/**
	 * Default is <code>true</code> - Controls whether {@link BleManager#onResume()} and {@link BleManager#onPause()} will get called
	 * automatically by using {@link android.app.Application.ActivityLifecycleCallbacks}.
	 */
	public boolean autoPauseResumeDetection				= true;
	
	/**
	 * Default is {@value #DEFAULT_UH_OH_CALLBACK_THROTTLE} seconds - {@link BleManager.UhOhListener.UhOh} callbacks from {@link BleManager.UhOhListener}
	 * can be a little spammy at times so this is an option to throttle them back on a per-{@link BleManager.UhOhListener.UhOh} basis.
	 * Set this to {@link Interval#DISABLED} to receive all every {@link BleManager.UhOhListener.UhOh} and manage them yourself.
	 * 
	 * @see BleManager.UhOhListener
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Interval	uhOhCallbackThrottle					= Interval.secs(DEFAULT_UH_OH_CALLBACK_THROTTLE);
	
	/**
	 * Default is {@value #DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME} seconds - Unless {@link Interval#DISABLED},
	 * this option will kick off a scan for {@link #autoScanActiveTime} seconds
	 * {@link #autoScanDelayAfterResume} seconds after {@link BleManager#onResume()} is called.
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval autoScanDelayAfterResume				= Interval.secs(DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME);

	/**
	 * Default is {@link Interval#DISABLED}. If set, this will automatically start scanning after the specified {@link Interval}.
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval autoScanDelayAfterBleTurnsOn			= Interval.DISABLED;

	/**
	 * Default is {@link Interval#DISABLED} - Length of time in seconds that the library will automatically scan for devices.
	 * Used in conjunction with {@link #autoScanPauseInterval}, {@link #autoScanPauseTimeWhileAppIsBackgrounded}, and {@link #autoScanDelayAfterResume},
	 * this option allows the library to periodically send off scan "pulses" that last {@link #autoScanActiveTime} seconds.
	 * Use {@link BleManager#startPeriodicScan(Interval, Interval)} to adjust this behavior while the library is running.
	 * If either {@link #autoScanActiveTime} or {@link #autoScanPauseInterval} is {@link Interval#DISABLED} then auto scanning is disabled.
	 * It can also be turned off with {@link BleManager#stopPeriodicScan()}.
	 *
	 * @see #autoScanPauseInterval
	 * @see BleManager#startPeriodicScan(Interval, Interval)
	 * @see BleManager#stopPeriodicScan()
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval autoScanActiveTime						= Interval.DISABLED;

	/**
	 * Default is {@value #DEFAULT_AUTO_SCAN_PAUSE_TIME} seconds - Length of time in seconds between automatic scan pulses defined by {@link #autoScanActiveTime}.
	 *
	 * @see #autoScanActiveTime
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval autoScanPauseInterval					= Interval.secs(DEFAULT_AUTO_SCAN_PAUSE_TIME);

	/**
	 * Default is {@link Interval#DISABLED} - Same as {@link #autoScanPauseInterval} except this value is used while the app is paused.
	 *
	 * @see #autoScanPauseInterval
	 * @see BleManager#onPause()
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval autoScanPauseTimeWhileAppIsBackgrounded = Interval.DISABLED;

	/**
	 * Default is {@link #DEFAULT_MINIMUM_SCAN_TIME} seconds - Minimum amount of time in seconds that the library strives to give to a scanning operation.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Interval	idealMinScanTime						= Interval.secs(DEFAULT_MINIMUM_SCAN_TIME);
	
	/**
	 * Default is {@value #DEFAULT_AUTO_UPDATE_RATE} seconds - The rate at which the library's internal update loop ticks.
	 * Generally shouldn't need to be changed. You can set this to {@link Interval#DISABLED} and call {@link BleManager#update(double)} yourself
	 * if you want to tie the library in to an existing update loop used in your application.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.RARE)
	public Interval autoUpdateRate							= Interval.secs(DEFAULT_AUTO_UPDATE_RATE);

	/**
	 * Default is {@value #DEFAULT_SCAN_REPORT_DELAY} seconds - Only applicable for Lollipop and up (i.e. > 5.0), this is the value given to
	 * {@link android.bluetooth.le.ScanSettings.Builder#setReportDelay(long)} so that scan results are "batched" ¯\_(ツ)_/¯. It's not clear from source
	 * code, API documentation, or internet search what effects this has, when you would want to use it, etc. The reason we use the default
	 * value provided is largely subjective. It seemed to help discover a peripheral faster on the Nexus 7 that was only advertising on channels
	 * 37 and 38 - i.e. not on channel 39 too. It is also the default option in the nRF Master Control panel diagnostic app.
	 * <br><br>
	 * NOTE: This option is only relevant if {@link BluetoothAdapter#isOffloadedScanBatchingSupported()} returns <code>true</code> - otherwise
	 * it has no effect because the hardware does not support it.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.RARE)
	public Interval scanReportDelay							= Interval.secs(DEFAULT_SCAN_REPORT_DELAY);

	/**
	 * Default is <code>null</code>, meaning no filtering - all discovered devices will
	 * be piped through your {@link BleManager.DiscoveryListener} instance
	 * and added to the internal list of {@link BleManager}.
	 * 
	 * @see ScanFilter
	 */
	@Nullable(Prevalence.NORMAL)
	public ScanFilter defaultScanFilter						= null;
	
	/**
	 * Default is <code>null</code> - can also be set post-construction with {@link BleManager#setListener_Discovery(DiscoveryListener)},
	 * which will override the implementation provided here.
	 * 
	 * @see BleManager.DiscoveryListener
	 */
	@Nullable(Prevalence.NORMAL)
	public DiscoveryListener defaultDiscoveryListener		= null;

	/**
	 * Default is {@link BleScanMode#AUTO} - see {@link BleScanMode} for more details.
	 */
	public BleScanMode scanMode								= BleScanMode.AUTO;

	/**
	 * Default is <code>null</code> - provide an instance here that will be called at the end of {@link BleManager#update(double)}.
	 * This might be useful for extension/wrapper libraries or apps that want to tie into the {@link BleManager} instance's existing update loop.
	 */
	public UpdateLoop.Callback updateLoopCallback			= null;
	
	/**
	 * Used if {@link #loggingEnabled} is <code>true</code>. Gives threads names so they are more easily identifiable.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	final String[] debugThreadNames =
	{
		"AMY", "BEN", "CAM", "DON", "ELI", "FAY", "GUS", "HAL", "IAN", "JAY", "LEO",
		"MAX", "NED", "OLA", "PAT", "RON", "SAL", "TED", "VAL", "WES", "YEE", "ZED"
	};
	
	/**
	 * Default is <code>null</code> - optional, only used if {@link #loggingEnabled} is true. Provides a look-up table
	 * so logs can show the name associated with a {@link UUID} along with its numeric string.
	 */
	@Nullable(Prevalence.NORMAL)
	public List<UuidNameMap> uuidNameMaps					= null;
	
	//--- DRK > Not sure if this is useful so keeping it package private for now.
	int	connectionFailUhOhCount								= 0;

	/**
	 * An optional whitelisting mechanism for scanning. Provide an implementation at
	 * {@link BleManagerConfig#defaultScanFilter} or one of the various {@link BleManager#startScan()}
	 * overloads, i.e. {@link BleManager#startScan(BleManagerConfig.ScanFilter)},
	 * {@link BleManager#startScan(Interval, BleManagerConfig.ScanFilter)}, etc.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ScanFilter
	{
		/**
		 * Instances of this class are passed to {@link ScanFilter#onEvent(ScanEvent)} to aid in making a decision.
		 */
		@Immutable
		public static class ScanEvent extends Event
		{
			/**
			 * Other parameters are probably enough to make a decision but this native instance is provided just in case.
			 */
			public BluetoothDevice nativeInstance(){  return m_nativeInstance;  }
			private final BluetoothDevice m_nativeInstance;

			/**
			 * A list of {@link UUID}s parsed from {@link #scanRecord()} as a convenience. May be empty, notably
			 * if {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is invoked.
			 */
			public List<UUID> advertisedServices(){  return m_advertisedServices;  }
			private final List<UUID> m_advertisedServices;

			/**
			 * The unaltered device name retrieved from the native bluetooth stack.
			 */
			public String name_native(){  return m_rawDeviceName;  }
			private final String m_rawDeviceName;

			/**
			 * See {@link BleDevice#getName_normalized()} for an explanation.
			 */
			public String name_normalized(){  return m_normalizedDeviceName;  }
			private final String m_normalizedDeviceName;

			/**
			 * The raw scan record received when the device was discovered. May be empty, especially
			 * if {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is invoked.
			 */
			public byte[] scanRecord(){  return m_scanRecord;  }
			private final byte[] m_scanRecord;

			/**
			 * The RSSI received when the device was discovered.
			 */
			public int rssi(){  return m_rssi;  }
			private final int m_rssi;

			/**
			 * Returns the transmission power of the device in decibels, or {@link BleNodeConfig#INVALID_TX_POWER} if device is not advertising its transmission power.
			 */
			public int txPower(){  return m_txPower;  }
			private final int m_txPower;

			/**
			 * Returns the mac address of the discovered device.
			 */
			public String macAddress(){  return m_nativeInstance.getAddress();  }

			/**
			 * See explanation at {@link BleDevice#getLastDisconnectIntent()}.
			 * <br><br>
			 * TIP: If {@link ScanEvent#lastDisconnectIntent} isn't {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#NULL} then most likely you can early-out
			 * and return <code>true</code> from {@link ScanFilter#onEvent(ScanEvent)} without having to check
			 * uuids or names matching, because obviously you've seen and connected to this device before.
			 */
			public State.ChangeIntent lastDisconnectIntent(){  return m_lastDisconnectIntent;  }
			private final State.ChangeIntent m_lastDisconnectIntent;

			/**
			 * Returns the advertising flags, if any, parsed from {@link #scanRecord()}.
			 */
			public int advertisingFlags()  {  return m_advertisingFlags;  }
			private final int m_advertisingFlags;

			/**
			 * Returns the manufacturer-specific data, if any, parsed from {@link #scanRecord()}.
			 */
			public SparseArray<byte[]> manufacturerData(){  return m_manufacturerData;  }
			private final SparseArray<byte[]> m_manufacturerData;

			/**
			 * Returns the service data, if any, parsed from {@link #scanRecord()}.
			 */
			public Map<UUID, byte[]> serviceData()  {  return m_serviceData;  }
			private final Map<UUID, byte[]> m_serviceData;

			ScanEvent
			(
				BluetoothDevice nativeInstance, List<UUID> advertisedServices, String rawDeviceName,
				String normalizedDeviceName, byte[] scanRecord, int rssi, State.ChangeIntent lastDisconnectIntent,
				final int txPower, final SparseArray<byte[]> manufacturerData, final Map<UUID, byte[]> serviceData, final int advFlags
			)
			{
				this.m_nativeInstance = nativeInstance;
				this.m_advertisedServices = advertisedServices;
				this.m_rawDeviceName = rawDeviceName != null ? rawDeviceName : "";
				this.m_normalizedDeviceName = normalizedDeviceName;
				this.m_scanRecord = scanRecord != null ? scanRecord : BleDevice.EMPTY_BYTE_ARRAY;
				this.m_rssi = rssi;
				this.m_lastDisconnectIntent = lastDisconnectIntent;
				this.m_txPower = txPower;
				this.m_advertisingFlags = advFlags;
				this.m_manufacturerData = manufacturerData;
				this.m_serviceData = serviceData;
			}

			/*package*/ static ScanEvent fromScanRecord(final BluetoothDevice device_native, final String rawDeviceName, final String normalizedDeviceName, final int rssi, final State.ChangeIntent lastDisconnectIntent, final byte[] scanRecord)
			{
				final Pointer<Integer> advFlags = new Pointer<Integer>();
				final Pointer<Integer> txPower = new Pointer<Integer>();
				final List<UUID> serviceUuids = new ArrayList<UUID>();
				final SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();
				final Map<UUID, byte[]> serviceData = new HashMap<UUID, byte[]>();
				final String name = rawDeviceName != null ? rawDeviceName : Utils_ScanRecord.parseName(scanRecord);

				Utils_ScanRecord.parseScanRecord(scanRecord, advFlags, txPower, serviceUuids, manufacturerData, serviceData);

				final ScanEvent e = new ScanEvent(device_native, serviceUuids, name, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent, txPower.value, manufacturerData, serviceData, advFlags.value);

				return e;
			}

			@Override public String toString()
			{
				return Utils_String.toString
				(
					this.getClass(),
					"macAddress", macAddress(),
					"name", name_normalized(),
					"services", advertisedServices()
				);
			}
		}

		/**
		 * Small struct passed back from {@link ScanFilter#onEvent(ScanEvent)}.
		 * Use static constructor methods to create an instance.
		 */
		public static class Please
		{
			static int STOP_SCAN			= 0x1;
			static int STOP_PERIODIC_SCAN	= 0x2;

			private final boolean m_ack;
			private final BleDeviceConfig m_config;
			int m_stopScanOptions;

			private Please(boolean ack, BleDeviceConfig config_nullable)
			{
				m_ack = ack;
				m_config = config_nullable;
			}

			boolean ack()
			{
				return m_ack;
			}

			BleDeviceConfig getConfig()
			{
				return m_config;
			}

			/**
			 * Shorthand for calling {@link BleManager#stopScan(BleManagerConfig.ScanFilter)}.
			 */
			public Please thenStopScan()
			{
				m_stopScanOptions |= STOP_SCAN;

				return this;
			}

			/**
			 * Shorthand for calling {@link BleManager#stopPeriodicScan(BleManagerConfig.ScanFilter)}.
			 */
			public Please thenStopPeriodicScan()
			{
				m_stopScanOptions |= STOP_PERIODIC_SCAN;

				return this;
			}

			/**
			 * Shorthand for calling both {@link BleManager#stopScan(BleManagerConfig.ScanFilter)} and {@link BleManager#stopPeriodicScan(BleManagerConfig.ScanFilter)}.
			 */
			public Please thenStopAllScanning()
			{
				thenStopScan();
				thenStopPeriodicScan();

				return this;
			}

			/**
			 * Return this from {@link ScanFilter#onEvent(ScanEvent)} to acknowledge the discovery.
			 * {@link BleManager.DiscoveryListener#onEvent(com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent)}
			 * will be called presently with a newly created {@link BleDevice}.
			 */
			public static Please acknowledge()
			{
				return new Please(true, null);
			}

			/**
			 * Returns {@link #acknowledge()} if the given condition holds <code>true</code>, {@link #ignore()} otherwise.
			 */
			public static Please acknowledgeIf(boolean condition)
			{
				return condition ? acknowledge() : ignore();
			}

			/**
			 * Same as {@link #acknowledgeIf(boolean)} but lets you pass a {@link BleDeviceConfig} as well.
			 */
			public static Please acknowledgeIf(boolean condition, BleDeviceConfig config)
			{
				return condition ? acknowledge(config) : ignore();
			}

			/**
			 * Same as {@link #acknowledge()} but allows you to pass a {@link BleDeviceConfig}
			 * instance to the {@link BleDevice} that's about to be created.
			 */
			public static Please acknowledge(BleDeviceConfig config)
			{
				return new Please(true, config);
			}

			/**
			 * Return this from {@link ScanFilter#onEvent(ScanEvent)} to say no to the discovery.
			 */
			public static Please ignore()
			{
				return new Please(false, null);
			}

			/**
			 * Returns {@link #ignore()} if the given condition holds <code>true</code>, {@link #acknowledge()} otherwise.
			 */
			public static Please ignoreIf(final boolean condition)
			{
				return condition ? ignore() : acknowledge();
			}
		}

		/**
		 * Return {@link Please#acknowledge()} to acknowledge the discovery, in which case {@link BleManager.DiscoveryListener#onEvent(BleManager.DiscoveryListener.DiscoveryEvent)}
		 * will be called shortly. Otherwise return {@link Please#ignore()} to ignore the discovered device.
		 *
		 * @return {@link Please#acknowledge()}, {@link Please#ignore()}, or {@link Please#acknowledge(BleDeviceConfig)} (or other static constructor methods that may be added in the future).
		 */
		Please onEvent(final ScanEvent e);
	}

	/**
	 * Convenience implementation of {@link ScanFilter} which filters using
	 * a whitelist of known primary advertising {@link UUID}s passed in to the constructor.
	 */
	public static class DefaultScanFilter implements ScanFilter
	{
		private final ArrayList<UUID> m_whitelist;

		public DefaultScanFilter(Collection<UUID> whitelist)
		{
			m_whitelist = new ArrayList<UUID>(whitelist);
		}

		public DefaultScanFilter(UUID whitelist)
		{
			m_whitelist = new ArrayList<UUID>();
			m_whitelist.add(whitelist);
		}

		/**
		 * Acknowledges the discovery if there's an overlap between the given advertisedServices
		 * and the {@link Collection} passed into the constructor of {@link BleManagerConfig.DefaultScanFilter}.
		 */
		@Override public Please onEvent(final ScanEvent e)
		{
			return Please.acknowledgeIf( Utils.haveMatchingIds(e.advertisedServices(), m_whitelist) );
		}
	}
	
	/**
	 * Creates a {@link BleManagerConfig} with all default options set. See each member of this class
	 * for what the default options are set to.
	 */
	public BleManagerConfig()
	{
		this(false);
	}
	
	/**
	 * Returns a new constructor that populates {@link #uuidNameMaps} with {@link Uuids}
	 * using {@link ReflectionUuidNameMap} to help with readable logging.
	 */
	public static BleManagerConfig newWithLogging()
	{
		return new BleManagerConfig(true);
	}
	
	/**
	 * Convenience constructor that populates {@link #uuidNameMaps} with {@link Uuids}
	 * using {@link ReflectionUuidNameMap} if logging is enabled.
	 * 
	 * @param loggingEnabled_in Sets {@link #loggingEnabled}.
	 */
	protected BleManagerConfig(boolean loggingEnabled_in)
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
