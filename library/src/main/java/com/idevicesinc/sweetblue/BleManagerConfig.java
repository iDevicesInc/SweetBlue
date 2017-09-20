package com.idevicesinc.sweetblue;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Extendable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;
import com.idevicesinc.sweetblue.impl.DefaultLogger;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.ReflectionUuidNameMap;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.UuidNameMap;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.json.JSONObject;

/**
 * Provides a number of options to pass to the {@link BleManager#get(Context, BleManagerConfig)}
 * singleton getter or {@link BleManager#setConfig(BleManagerConfig)}.
 * Use {@link Interval#DISABLED} or <code>null</code> to disable any time-based options.
 */
@Extendable
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
	 * Default value for {@link #idleUpdateRate}.
	 */
	public static final double DEFAULT_IDLE_UPDATE_RATE					= 0.5;

	/**
	 * Default value for {@link #minTimeToIdle}.
	 */
	public static final double DEFAULT_DELAY_BEFORE_IDLE				= 20.0;

	/**
	 * Default value for {@link #uhOhCallbackThrottle}.
	 */
	public static final double DEFAULT_UH_OH_CALLBACK_THROTTLE			= 30.0;

	/**
	 * Default value for {@link #scanClassicBoostLength}.
	 */
	public static final double DEFAULT_CLASSIC_SCAN_BOOST_TIME			= 0.5;

	/**
	 * Default value for {@link #scanReportDelay}.
	 */
	public static final double DEFAULT_SCAN_REPORT_DELAY				= .5;

	/**
	 * Default value for {@link #infiniteScanInterval}
	 */
	public static final double DEFAULT_SCAN_INFINITE_INTERVAL_TIME 		= 10;

	/**
	 * Default value for {@link #infinitePauseInterval}
	 */
	public static final double DEFAULT_SCAN_INFINITE_PAUSE_TIME 		= 0.5;

	/**
	 * Default value for {@link #defaultStatePollRate}
	 */
	public static final double DEFAULT_MANAGER_STATE_POLL_RATE			= .1;
	
	static final BleManagerConfig NULL = new BleManagerConfigNull();

	static class BleManagerConfigNull extends BleManagerConfig {
		{
			reconnectFilter	= new ReconnectFilter.DefaultNullReconnectFilter();
		}
	}

	
	/**
	 * Maximum amount of time for a classic scan to run. This was determined based on experimentation.
	 * Documentation says that classic scan goes on for about 12 seconds. I forget what the reasoning
	 * was for setting a time lower than this, so here's a TODO to try to remember that.
	 */
	static final double MAX_CLASSIC_SCAN_TIME							= 7.0;

	/**
	 * Default is {@link LogOptions#OFF} - There are 3 static instances you can use for convenience - {@link LogOptions#OFF},
	 * {@link LogOptions#ON}, and {@link LogOptions#ALL_ON}. If you want more fine-grained logs, then instantiate {@link LogOptions} with the levels of logging
	 * you would like for SweetBlue logs, and native logs (logs printed when receiving any native callbacks).
	 */
	public LogOptions loggingOptions						= LogOptions.OFF;


	/**
	 * Default is {@link DefaultLogger} - which prints the log statements to Android's logcat. If you want to
	 * pipe the log statements elsewhere, create a class which implements {@link SweetLogger}, and set this field
	 * with an instance of it. If {@link #loggingOptions} is not set -- or it's set to {@link LogOptions#OFF} --, then this option will not affect anything.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public SweetLogger logger						= new DefaultLogger();
	
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
	 * Default is {@link Interval#DISABLED} - This will run a short classic scan before a regular BLE scan. Sometimes, for whatever reason, the native
	 * BLE stack will refuse to see a particular device. However, if you go into Bluetooth settings on the phone, it sees it (they do a classic scan
	 * here), and suddenly it shows up in a BLE scan after that. So the idea with this feature is to do the same thing without having to go into
	 * Bluetooth settings.
	 *
	 * This seems to affect the number of devices that are returned in total, and how quickly they are discovered, so this is disabled by default now.
	 * If you decide that you need to use it, we recommend going with a time length of {@link #DEFAULT_CLASSIC_SCAN_BOOST_TIME}.
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval scanClassicBoostLength						= Interval.DISABLED;

	/**
	 * Default is {@link #DEFAULT_SCAN_INFINITE_INTERVAL_TIME} - When running an infinite scan, SweetBlue will pause the scan, and restart it again a short
	 * time later, defined by {@link #infinitePauseInterval}. The android stack tends to find less and less devices the longer a scan runs. This helps to keep
	 * scan results coming in when performing an infinite scan, leading to better results.
	 */
	public Interval infiniteScanInterval 						= Interval.secs(DEFAULT_SCAN_INFINITE_INTERVAL_TIME);

	/**
	 * Default is {@link #DEFAULT_SCAN_INFINITE_PAUSE_TIME} - This is the amount of time SweetBlue will wait before resuming a scan.
	 *
	 * See {@link #infiniteScanInterval}
	 */
	public Interval infinitePauseInterval 						= Interval.secs(DEFAULT_SCAN_INFINITE_PAUSE_TIME);

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
	 * a {@link ScanFilter.ScanEvent#scanRecord} or {@link ScanFilter.ScanEvent#advertisedServices}
	 * to {@link ScanFilter#onEvent(ScanFilter.ScanEvent)}.
	 * Most likely you will be forced to filter on name only for your implementation of
	 * {@link ScanFilter#onEvent(ScanFilter.ScanEvent)}.
	 * As such this is meant as a better-than-nothing back-up solution for BLE scanning.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean revertToClassicDiscoveryIfNeeded			= true;
	
	/**
	 * Default is <code>true</code> - SweetBlue originally ran most operations on its own internal thread based on
	 * some theories that since proved invalid. While the library can do so, it's now
	 * recommended to run on the main thread in order to avoid any possible multithreading issues.
	 * Note that if this is set to false, then allowCallsFromAllThreads will automatically be set to <code>true</code>.
	 * Setting this to false will result in a smoother UI experience, at the cost of the possibility of multithreading
	 * issues, as stated above. While we have put a lot of effort to safe-guard against multithreading issues, they may
	 * still happen, which is why this is true by default.
	 */
	public boolean runOnMainThread									= true;
	
	/**
	 * Default is <code>true</code> - whether all callbacks are posted to the main thread or from SweetBlue's internal
	 * thread. If {@link #runOnMainThread}==true then this setting is meaningless because SweetBlue's
	 * internal thread is already the main thread to begin with.
	 */
	public boolean postCallbacksToMainThread						= true;
	
	/**
	 * Default is <code>true</code> - requires the {@link android.Manifest.permission#WAKE_LOCK} permission in your app's manifest file.
	 * It should look like this: {@code <uses-permission android:name="android.permission.WAKE_LOCK" />}
	 * Sets whether the library will attempt to obtain a wake lock in certain situations.
	 * For now the only situation is when there are no remote bluetooth devices
	 * {@link BleDeviceState#CONNECTED} but one or more devices are {@link BleDeviceState#RECONNECTING_LONG_TERM}.
	 * The wake lock will be released when devices are reconnected (e.g. from coming back
	 * into range) or when reconnection is stopped either through {@link BleDevice#disconnect()} or returning
	 * {@link ReconnectFilter.ConnectionLostPlease#stopRetrying()} from
	 * {@link ReconnectFilter#onEvent(ReconnectFilter.ConnectionLostEvent)}.
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
	 * @deprecated Use {@link #scanApi} with {@link BleScanApi#PRE_LOLLIPOP} instead.
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
	 * Default is {@value #DEFAULT_UH_OH_CALLBACK_THROTTLE} seconds - {@link UhOhListener.UhOh} callbacks from {@link UhOhListener}
	 * can be a little spammy at times so this is an option to throttle them back on a per-{@link UhOhListener.UhOh} basis.
	 * Set this to {@link Interval#DISABLED} to receive all every {@link UhOhListener.UhOh} and manage them yourself.
	 * 
	 * @see UhOhListener
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Interval	uhOhCallbackThrottle					= Interval.secs(DEFAULT_UH_OH_CALLBACK_THROTTLE);

	/**
	 * Default is <code>true</code> - This allows SweetBlue to store the last Uh Oh time to disk. This makes it so that even if you shutdown, and restart
	 * {@link BleManager}, SweetBlue will still respect the {@link #uhOhCallbackThrottle}. Otherwise, if you restart the manager, and get an {@link com.idevicesinc.sweetblue.UhOhListener.UhOh}
	 * it will be dispatched immediately.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean manageLastUhOhOnDisk						= true;

	/**
	 * Default is {@value #DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME} seconds - Unless {@link Interval#DISABLED},
	 * this option will kick off a scan for {@link #autoScanActiveTime} seconds
	 * {@link #autoScanDelayAfterResume} seconds after {@link BleManager#onResume()} is called.
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval autoScanDelayAfterResume				= Interval.secs(DEFAULT_AUTO_SCAN_DELAY_AFTER_RESUME);

	/**
	 * Default is {@link Interval#DISABLED}. If set, this will automatically start scanning after the specified {@link Interval}. This also
	 * depends on {@link #autoScanActiveTime}, in that if that value is not set, this value will do nothing.
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval autoScanDelayAfterBleTurnsOn			= Interval.DISABLED;

	/**
	 * Default is {@link Interval#DISABLED} - Length of time in seconds that the library will automatically scan for devices.
	 * Used in conjunction with {@link #autoScanPauseInterval}, {@link #autoScanPauseTimeWhileAppIsBackgrounded}, {@link #autoScanDelayAfterResume},
	 * and {@link #autoScanDelayAfterBleTurnsOn}, this option allows the library to periodically send off scan "pulses" that last
	 * {@link #autoScanActiveTime} seconds. Use {@link BleManager#startPeriodicScan(Interval, Interval)} to adjust this behavior while the library is running.
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
	 * Generally shouldn't need to be changed. You can set this to {@link Interval#DISABLED} and call {@link BleManager#update(double, long)} yourself
	 * if you want to tie the library in to an existing update loop used in your application.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.RARE)
	public Interval autoUpdateRate							= Interval.secs(DEFAULT_AUTO_UPDATE_RATE);

	/**
	 * Default is {@link #DEFAULT_MANAGER_STATE_POLL_RATE} seconds - The rate at which the library will poll the native manager's
	 * state. This only applies to devices running Marshmallow or higher. This call can drain the battery if it's left at the same
	 * rate as {@link #autoUpdateRate}, as it uses reflection to poll the native state. This is needed on some phones where SweetBlue
	 * doesn't receive a state change when it should.
	 * If this is <code>null</code>, then state polling will be disabled.
	 */
	@Advanced
	@Nullable(Prevalence.RARE)
	public Interval defaultStatePollRate					= Interval.secs(DEFAULT_MANAGER_STATE_POLL_RATE);

	/**
	 * Default is {@value #DEFAULT_IDLE_UPDATE_RATE} seconds - The rate at which the library's internal update loop ticks, after
	 * {@link #minTimeToIdle} has elapsed.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public Interval idleUpdateRate							= Interval.secs(DEFAULT_IDLE_UPDATE_RATE);

	/**
	 * Default is {@value #DEFAULT_DELAY_BEFORE_IDLE} seconds - This is the amount of time the library will wait with no tasks before
	 * lowering the update loop tick to {@link #idleUpdateRate}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public Interval minTimeToIdle							= Interval.secs(DEFAULT_DELAY_BEFORE_IDLE);

	/**
	 * Default is {@link Interval#DISABLED} - This sets an amount of time to delay between executing each task in the queue. The delay simply makes sure
	 * that the amount of time requested here has passed since the last task ended.
	 */
	@Advanced
	public Interval delayBetweenTasks						= Interval.DISABLED;

	/**
	 * Default is <code>false</code><br></br>
	 * <br></br>
	 * This specifies if SweetBlue is running in a unit test or not. If set to <code>true</code>, then SweetBlue will create
	 * a thread to act as the UI thread. If this is <code>null</code>, then SweetBlue will look for the class {@link junit.framework.Assert}. If it
	 * is found, then this boolean will be set to <code>true</code>. If <code>false</code>, then SweetBlue runs normally.
	 *
	 */
	Boolean unitTest										= false;

	/**
	 * Default is {@value #DEFAULT_SCAN_REPORT_DELAY} seconds - Only applicable for Lollipop and up (i.e. > 5.0), this is the value given to
	 * {@link android.bluetooth.le.ScanSettings.Builder#setReportDelay(long)} so that scan results are "batched" ¯\_(ツ)_/¯. It's not clear from source
	 * code, API documentation, or internet search what effects this has, when you would want to use it, etc. The reason we use the default
	 * value provided is largely subjective. It seemed to help discover a peripheral faster on the Nexus 7 that was only advertising on channels
	 * 37 and 38 - i.e. not on channel 39 too.
	 * <br><br>
	 * It has been observed that with the default value on the Pixel, it can take over 5 seconds to get the first batch of devices seen. So, SweetBlue will
	 * automatically use {@link Interval#ZERO} for the delay.
	 * <br><br>
	 * NOTE: This option is only relevant if {@link BluetoothAdapter#isOffloadedScanBatchingSupported()} returns <code>true</code> - otherwise
	 * it has no effect because the hardware does not support it.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.RARE)
	public Interval scanReportDelay							= Interval.secs(DEFAULT_SCAN_REPORT_DELAY);

	/**
	 * Default is <code>null</code>, meaning no filtering - all discovered devices will
	 * be piped through your {@link DiscoveryListener} instance
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
	 * @see DiscoveryListener
	 */
	@Nullable(Prevalence.NORMAL)
	public DiscoveryListener defaultDiscoveryListener		= null;


	/**
	 * Default is <code>true</code> - SweetBlue polls the native Bluetooth state to ensure it's always up-to-date with the current Bluetooth state (in case
	 * we don't receive a callback from the native Bluetooth stack for any reason). However, there are rare cases where the state we get from polling isn't wrong
	 * per se, but ahead of the native callback. So for instance, BLE was turned off, then on...polling may say it's ON, then try to connect to a device. Then
	 * we get the native callback, but by this time, the connect has failed. In this case, you should turn this to <code>false</code>. But then you are at the
	 * mercy of Android's Bluetooth callbacks coming in. This polling is only done on devices running Android Marshmallow (6.0+) or above.
	 *
	 * @deprecated - Use {@link #defaultStatePollRate} instead. If you want it disabled, set that option to {@link Interval#DISABLED}.
	 */
	@Deprecated
	public boolean allowManagerStatePolling					= true;

	/**
	 * Default is <code>null</code>
	 *
	 * @deprecated This is deprecated in favor of {@link BleScanApi}. If this is not null, SweetBlue will set
	 * {@link #scanApi} automatically from this setting.
	 */
	@Deprecated
	public BleScanMode scanMode								= null;


	/**
	 * Default is {@link BleScanApi#AUTO} - see {@link BleScanApi} for more details.
	 */
	public BleScanApi scanApi								= BleScanApi.AUTO;

	/**
	 * NOTE: This is ONLY applicable on devices running Lollipop or above.
	 * Default is {@link BleScanPower#AUTO} - see {@link BleScanPower} for more details.
	 */
	public BleScanPower scanPower							= BleScanPower.AUTO;

	/**
	 * Default is <code>null</code> - provide an instance here that will be called at the end of {@link BleManager#update(double, long)}.
	 * This might be useful for extension/wrapper libraries or apps that want to tie into the {@link BleManager} instance's existing update loop.
	 */
	public PI_UpdateLoop.Callback updateLoopCallback			= null;


	P_GattLayer newGattLayer(BleDevice device)
	{
		if (gattLayerFactory == null)
		{
			gattLayerFactory = new P_AndroidGattLayerFactory();
		}
		try
		{
			return gattLayerFactory.newInstance(device);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	P_NativeDeviceLayerFactory nativeDeviceFactory 				= new P_AndroidDeviceLayerFactory();

	P_GattLayerFactory gattLayerFactory					= new P_AndroidGattLayerFactory();

	P_NativeDeviceLayer newDeviceLayer(BleDevice device)
	{
		if (nativeDeviceFactory == null)
		{
			nativeDeviceFactory = new P_AndroidDeviceLayerFactory();
		}
		try
		{
			return nativeDeviceFactory.newInstance(device);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	P_NativeManagerLayer nativeManagerLayer						= new P_AndroidBluetoothManager();

// TODO - Remove this once we figure out a different solution for unit tests
//
//	/**
//	 * This allows the use of custom {@link PI_UpdateLoop} for the internal processing of SweetBlue. This is only exposed
//	 * for unit testing purposes, you should never change this unless you know the internals of SweetBlue intimately.
//	 */
//	@UnitTest
//	public PI_UpdateLoop.IUpdateLoopFactory updateLoopFactory	= new PI_UpdateLoop.DefaultUpdateLoopFactory();


	/**
	 * Used if {@link #loggingOptions} is not {@link LogOptions#OFF}. Gives threads names so they are more easily identifiable.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	final String[] debugThreadNames =
	{
		"ABE", "BOB", "CAM", "DON", "ELI", "FAY", "GUS", "HAL", "IAN", "JAY", "KAY", "LEO",
		"MAX", "NED", "OLA", "PAT", "QUE", "RON", "SAL", "TED", "UVA", "VAL", "WES", "XIB", "YEE", "ZED"
	};
	
	/**
	 * Default is <code>null</code> - optional, only used if {@link #loggingOptions} is not {@link LogOptions#OFF}. Provides a look-up table
	 * so logs can show the name associated with a {@link UUID} along with its numeric string.
	 */
	@Nullable(Prevalence.NORMAL)
	public List<UuidNameMap> uuidNameMaps					= null;

	/**
	 * Default is {@link com.idevicesinc.sweetblue.BleManagerConfig.DeviceNameComparator}. This specifies how to
	 * sort the list of devices in {@link BleManager}.
	 */
	public Comparator<BleDevice> defaultListComparator					= new DeviceNameComparator();
	
	//--- DRK > Not sure if this is useful so keeping it package private for now.
	int	connectionFailUhOhCount								= 0;


	public static final class P_AndroidDeviceLayerFactory implements P_NativeDeviceLayerFactory<P_AndroidBleDevice>
	{
		@Override public P_AndroidBleDevice newInstance(BleDevice device)
		{
			return new P_AndroidBleDevice(device);
		}
	}

	public static final class P_AndroidGattLayerFactory implements P_GattLayerFactory<P_AndroidGatt>
	{
		@Override public P_AndroidGatt newInstance(BleDevice device)
		{
			return new P_AndroidGatt(device);
		}
	}

	/**
	 * Default sorter class for sorting the list of devices in {@link BleManager}. This sorts by
	 * {@link BleDevice#getName_debug()}.
	 */
	public static class DeviceNameComparator implements Comparator<BleDevice> {

		@Override public int compare(BleDevice lhs, BleDevice rhs)
		{
			return lhs.getName_debug().compareTo(rhs.getName_debug());
		}
	}
	
	/**
	 * Creates a {@link BleManagerConfig} with all default options set. See each member of this class
	 * for what the default options are set to.
	 */
	public BleManagerConfig()
	{
		this(LogOptions.OFF);
	}

	/**
	 * Creates a {@link BleManagerConfig} with all default options set. Then, any configuration options
	 * specified in the given JSONObject will be applied over the defaults.  See {@link BleNodeConfig#writeJSON()}
	 * regarding the creation of the JSONObject
	 */
	public BleManagerConfig(JSONObject jo)
	{
		super();
		readJSON(jo);
	}
	
	/**
	 * Returns a new constructor that populates {@link #uuidNameMaps} with {@link Uuids}
	 * using {@link ReflectionUuidNameMap} to help with readable logging. This sets {@link #loggingOptions} to
	 * {@link LogOptions#ON}.
	 */
	public static BleManagerConfig newWithLogging()
	{
		return new BleManagerConfig(LogOptions.ON);
	}
	
	/**
	 * Convenience constructor that populates {@link #uuidNameMaps} with {@link Uuids}
	 * using {@link ReflectionUuidNameMap} if logging is enabled.
	 * 
	 * @param logOptions Sets {@link #loggingOptions}.
	 */
	protected BleManagerConfig(LogOptions logOptions)
	{
		this.loggingOptions = logOptions;
		
		if( logOptions.enabled() )
		{
			uuidNameMaps = new ArrayList<>();
			uuidNameMaps.add(new ReflectionUuidNameMap(Uuids.class));
		}
	}
	
	@Override public BleManagerConfig clone()
	{
		return (BleManagerConfig) super.clone();
	}
}
