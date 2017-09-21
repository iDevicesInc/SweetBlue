package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleManagerState.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.idevicesinc.sweetblue.BondListener.BondEvent;
import com.idevicesinc.sweetblue.BondListener.Status;
import com.idevicesinc.sweetblue.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.ScanFilter.Please;
import com.idevicesinc.sweetblue.P_ScanManager.DiscoveryEntry;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * The entry point to the library. Get a singleton instance using {@link #get(android.content.Context, BleManagerConfig)} or its overloads. Make sure
 * to hook up this manager to lifecycle events for your app as a whole: {@link #onPause()} and {@link #onResume()}.
 * <br><br>
 * Also put the following entries (or something similar) in the root of your AndroidManifest.xml:
 * <br><br>
 * {@code <uses-sdk android:minSdkVersion="18" android:targetSdkVersion="23" />}<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH" /> }<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" /> }<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED" /> }<br>
 * {@code <uses-permission android:name="android.permission.WAKE_LOCK" /> } <br>
 * {@code <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /> } <br>
 * {@code <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" /> }<br>
 * <br><br>
 * {@link android.Manifest.permission#WAKE_LOCK} is recommended but optional, needed if {@link BleManagerConfig#manageCpuWakeLock} is enabled to aid with reconnect loops.
 * As of now it's enabled by default.
 * <br><br>
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} (or {@link android.Manifest.permission#ACCESS_FINE_LOCATION})
 * is also strongly recommended but optional. Without it, {@link BleManager#startScan()} and overloads will not properly return results in {@link android.os.Build.VERSION_CODES#M} and above.
 * See {@link #startScan(Interval, ScanFilter, DiscoveryListener)} for more information.
 * <br><br>
 * Now here is a simple example usage:<pre><code>
 * public class MyActivity extends Activity
 * {
 *     {@literal @}Override protected void onCreate(Bundle savedInstanceState)
 *      {
 *          // A ScanFilter decides whether a BleDevice instance will be created
 *          // and passed to the DiscoveryListener implementation below.
 *         final ScanFilter scanFilter = new ScanFilter()
 *         {
 *            {@literal @}Override public Please onEvent(ScanEvent e)
 *             {
 *                 return Please.acknowledgeIf(e.name_normalized().contains("my_device"))
 *                              .thenStopScan();
 *             }
 *         };
 *
 *         // New BleDevice instances are provided through this listener.
 *         // Nested listeners then listen for connection and read results.
 *         final DiscoveryListener discoveryListener = new DiscoveryListener()
 *         {
 *            {@literal @}Override public void onEvent(DiscoveryEvent e)
 *             {
 *                 if( e.was(LifeCycle.DISCOVERED) )
 *                 {
 *                     e.device().connect(new StateListener()
 *                     {
 *                        {@literal @}Override public void onEvent(StateEvent e)
 *                         {
 *                             if( e.didEnter(BleDeviceState.INITIALIZED) )
 *                             {
 *                                 e.device().read(Uuids.BATTERY_LEVEL, new ReadWriteListener()
 *                                 {
 *                                    {@literal @}Override public void onEvent(ReadWriteEvent e)
 *                                     {
 *                                         if( e.wasSuccess() )
 *                                         {
 *                                             Log.i("", "Battery level is " + e.data_byte() + "%");
 *                                         }
 *                                     }
 *                                 });
 *                             }
 *                         }
 *                     });
 *                 }
 *             }
 *         };
 *
 *         // Helps you navigate the treacherous waters of Android M Location requirements for scanning.
 *         BluetoothEnabler.start(this, new DefaultBluetoothEnablerFilter()
 *         {
 *            {@literal @}Override public Please onEvent(BluetoothEnablerEvent e)
 *             {
 *                 if( e.isDone() )
 *                 {
 *                     e.bleManager().startScan(scanFilter, discoveryListener);
 *                 }
 *
 *                 return super.onEvent(e);
 *             }
 *         });
 *    }
 * </code>
 * </pre>
 */
public final class BleManager
{
	/**
	 * Create the singleton instance or retrieve the already-created singleton instance with default configuration options set.
	 * If you call this after you call {@link #get(android.content.Context, BleManagerConfig)} (for example in another
	 * {@link android.app.Activity}), the {@link BleManagerConfig} originally passed in will be used.
	 * Otherwise, if a new instance is to be created, this calls {@link #get(android.content.Context, BleManagerConfig)} with a {@link BleManagerConfig}
	 * instance created using the default constructor {@link BleManagerConfig#BleManagerConfig()}.
	 */
	public static BleManager get(Context context)
	{
		if( s_instance == null )
		{
			return get(context, new BleManagerConfig());
		}
		else
		{
			verifySingleton(context);

			return s_instance;
		}
	}

	/**
	 * Create the singleton instance or retrieve the already-created singleton instance with custom configuration options set.
	 * If you call this more than once (for example from a different {@link android.app.Activity}
	 * with different {@link BleManagerConfig} options set then the newer options overwrite the older options.
	 */
	public static BleManager get(Context context, BleManagerConfig config)
	{
		if( s_instance == null )
		{
			s_instance = new BleManager(context, config);

			return s_instance;
		}
		else
		{
			verifySingleton(context);

			s_instance.setConfig(config);

			return s_instance;
		}
	}

	private static void verifySingleton(Context context)
	{
		//--- DRK > Not confident how this method behaves with complex applications, multiple activities, services, widgets, etc.
		//---		Don't want to throw Errors needlessly, so commenting out for now.
//		if( s_instance != null && s_instance.getApplicationContext() != context.getApplicationContext() )
//		{
//			//--- DRK > Not sure how/if this could happen, but I never underestimate Android.
//			throw new InstantiationError("There can only be one instance of "+BleManager.class.getSimpleName() + " created per application.");
//		}
	}

	private final static long UPDATE_LOOP_WARNING_DELAY = 10000;

	private final Context m_context;
	private UpdateRunnable m_updateRunnable;
	private final P_ScanFilterManager m_filterMngr;
	final P_BluetoothCrashResolver m_crashResolver;
	private			P_Logger m_logger;
			  BleManagerConfig m_config;
		final P_DeviceManager m_deviceMngr;
		final P_DeviceManager m_deviceMngr_cache;
	final P_BleManager_Listeners m_listeners;
	final P_BleStateTracker m_stateTracker;
	final P_NativeBleStateTracker m_nativeStateTracker;
	private P_PostManager m_postManager;
	private P_ScanManager m_scanManager;
	private final P_TaskManager m_taskManager;
	private 	P_UhOhThrottler m_uhOhThrottler;
				P_WakeLockManager m_wakeLockMngr;

			BleDevice.HistoricalDataLoadListener m_historicalDataLoadListener;
			DiscoveryListener m_discoveryListener;
	private P_WrappingResetListener m_resetListeners;
	private AssertListener m_assertionListener;
			DeviceStateListener m_defaultDeviceStateListener;
			DeviceReconnectFilter m_defaultDeviceReconnectFilter;
			ServerReconnectFilter m_defaultConnectionFailListener_server;
			DeviceConnectListener m_defaultDeviceConnectListener;
			BondListener m_defaultBondListener;
			ReadWriteListener m_defaultReadWriteListener;
			NotificationListener m_defaultNotificationListener;
	final P_DiskOptionsManager m_diskOptionsMngr;

	private double m_timeForegrounded = 0.0;
	private long m_timeTurnedOn = 0;
	private long m_lastTaskExecution;
	private long m_currentTick;
	private boolean m_isForegrounded = false;
	private boolean m_ready = false;
	private boolean m_unitTestCheckDone = false;
    private long m_lastUpdateLoopWarning = 0;


    ServerStateListener m_defaultServerStateListener;
	OutgoingListener m_defaultServerOutgoingListener;
	IncomingListener m_defaultServerIncomingListener;
	AddServiceListener m_serviceAddListener;
	AdvertisingListener m_advertisingListener;
//    final P_ServerManager m_serverMngr;

	final Backend_HistoricalDatabase m_historicalDatabase;

	BleServer m_server = null;

	static BleManager s_instance = null;

	/**
	 * Field for app to associate any data it wants with the singleton instance of this class
	 * instead of having to subclass or manage associative hash maps or something.
	 * The library does not touch or interact with this data in any way.
	 *
	 * @see BleDevice#appData
	 * @see BleServer#appData
	 */
	public Object appData;


	private BleManager(Context context, BleManagerConfig config)
	{
		m_context = context.getApplicationContext();

		m_currentTick = System.currentTimeMillis();

		addLifecycleCallbacks();
		m_config = config.clone();
		m_logger = new P_Logger(this, m_config.debugThreadNames, m_config.uuidNameMaps, m_config.loggingOptions, m_config.logger);
		m_scanManager = new P_ScanManager(this);
		m_historicalDatabase = PU_HistoricalData.newDatabase(context, this);
		m_diskOptionsMngr = new P_DiskOptionsManager(m_context);
		m_filterMngr = new P_ScanFilterManager(this, m_config.defaultScanFilter);
		if (m_config.nativeManagerLayer.isManagerNull())
		{
			m_config.nativeManagerLayer.resetManager(m_context);
		}
		BleManagerState nativeState = BleManagerState.get(m_config.nativeManagerLayer.getState());

		if (m_timeTurnedOn == 0 && nativeState.overlaps(BluetoothAdapter.STATE_ON)) {
			m_timeTurnedOn = System.currentTimeMillis();
		}

		m_stateTracker = new P_BleStateTracker(this);
		m_stateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		m_nativeStateTracker = new P_NativeBleStateTracker(this);
		m_nativeStateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		m_taskManager = new P_TaskManager(this);
		m_crashResolver = new P_BluetoothCrashResolver(m_context);
		m_deviceMngr = new P_DeviceManager(this);
//		m_serverMngr = new P_ServerManager(this);
		m_deviceMngr_cache = new P_DeviceManager(this);
		m_listeners = new P_BleManager_Listeners(this);

		m_lastTaskExecution = System.currentTimeMillis();

		initConfigDependentMembers();

		updateLogger();

		m_postManager.postToMain(new Runnable()
		{
			@Override public void run()
			{
				m_postManager.postToUpdateThread(new Runnable()
				{
					@Override public void run()
					{
						if (!m_config.unitTest)
						{
							m_logger.printBuildInfo();
						}
					}
				});
			}
		});
	}

	/**
	 * Updates the config options for this instance after calling {@link #get(android.content.Context)} or {@link #get(android.content.Context, BleManagerConfig)}.
	 * Providing a <code>null</code> value will set everything back to default values.
	 */
	public final void setConfig(@Nullable(Prevalence.RARE) BleManagerConfig config_nullable)
	{
		m_config = config_nullable != null ? config_nullable.clone() : new BleManagerConfig();
		updateLogger();
		initConfigDependentMembers();
	}

	public final BleManagerConfig getConfigClone()
	{
		return m_config.clone();
	}

	private void updateLogger()
	{
		m_logger.updateInstance(m_config.debugThreadNames, m_config.loggingOptions, m_config.logger);
	}

	private void initConfigDependentMembers()
	{
		// Check if one of the classes from the unit test module can be loaded. If so, we're running in a
		// unit test. Otherwise, we aren't
		try
		{
			Class.forName("com.idevicesinc.sweetblue.UnitTestLogger");
			m_config.unitTest = true;
		}
		catch (ClassNotFoundException e)
		{
			m_config.unitTest = false;
		}

		m_listeners.updatePollRate(m_config.defaultStatePollRate);

		m_filterMngr.setDefaultFilter(m_config.defaultScanFilter);

		if (m_config.nativeManagerLayer instanceof P_AndroidBluetoothManager)
		{
			((P_AndroidBluetoothManager) m_config.nativeManagerLayer).setBleManager(this);
		}
		if (m_config.nativeManagerLayer.isManagerNull())
		{
			m_config.nativeManagerLayer.resetManager(m_context);
		}

		boolean startUpdate = true;

		if (m_updateRunnable != null)
		{
			m_updateRunnable.m_shutdown = false;
			m_postManager.removeUpdateCallbacks(m_updateRunnable);
		}
		else
		{
			if (Interval.isEnabled(m_config.autoUpdateRate))
			{
				m_updateRunnable = new UpdateRunnable(m_config.autoUpdateRate.millis());
			}
			else
			{
				startUpdate = false;
				m_updateRunnable = new UpdateRunnable();
			}
		}

		m_uhOhThrottler = new P_UhOhThrottler(this, Interval.secs(m_config.uhOhCallbackThrottle));

		if( m_wakeLockMngr == null )
		{
			m_wakeLockMngr = new P_WakeLockManager(this, m_config.manageCpuWakeLock);
		}
		else if( m_wakeLockMngr != null && m_config.manageCpuWakeLock == false )
		{
			m_wakeLockMngr.clear();
			m_wakeLockMngr = new P_WakeLockManager(this, m_config.manageCpuWakeLock);
		}

		if( m_config.defaultDiscoveryListener != null )
		{
			this.setListener_Discovery(m_config.defaultDiscoveryListener);
		}

		initPostManager();

		if( startUpdate )
		{
			m_postManager.postToUpdateThreadDelayed(m_updateRunnable, m_config.autoUpdateRate.millis());
		}
	}

	private void initPostManager()
	{
		final P_SweetHandler update;
		P_SweetHandler ui;
		if (m_config.runOnMainThread)
		{
			update = new P_SweetUIHandler(this);
			ui = update;
		}
		else
		{
			ui = new P_SweetUIHandler(this);
			update = new P_SweetBlueThread();
			update.post(() -> m_logger.setUpdateThread(android.os.Process.myTid()));
		}
		ui.post(() -> m_logger.setMainThread(android.os.Process.myTid()));
		m_postManager = new P_PostManager(this, ui, update);
	}

	/**
	 * Returns whether the manager is in any of the provided states.
	 */
	public final boolean isAny(BleManagerState ... states)
	{
		for( int i = 0; i < states.length; i++ )
		{
			if( is(states[i]) )  return true;
		}

		return false;
	}

	/**
	 * Returns whether the manager is in all of the provided states.
	 *
	 * @see #isAny(BleManagerState...)
	 */
	public final boolean isAll(BleManagerState... states)
	{
		for (int i = 0; i < states.length; i++)
		{
			if( !is(states[i]) )  return false;
		}

		return true;
	}

	/**
	 * Returns whether the manager is in the provided state.
	 *
	 * @see #isAny(BleManagerState...)
	 */
	public final boolean is(final BleManagerState state)
	{
		return state.overlaps(getStateMask());
	}

	/**
	 * Returns <code>true</code> if there is partial bitwise overlap between the provided value and {@link #getStateMask()}.
	 *
	 * @see #isAll(int)
	 */
	public final boolean isAny(final int mask_BleManagerState)
	{
		return (getStateMask() & mask_BleManagerState) != 0x0;
	}

	/**
	 * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask()}.
	 *
	 * @see #isAny(int)
	 */
	public final boolean isAll(final int mask_BleManagerState)
	{
		return (getStateMask() & mask_BleManagerState) == mask_BleManagerState;
	}

	/**
	 * See similar comment for {@link BleDevice#getTimeInState(BleDeviceState)}.
	 *
	 * @see BleDevice#getTimeInState(BleDeviceState)
	 */
	public final Interval getTimeInState(BleManagerState state)
	{
		return Interval.millis(m_stateTracker.getTimeInState(state.ordinal()));
	}

	/**
	 * See similar comment for {@link BleDevice#getTimeInState(BleDeviceState)}.
	 *
	 * @see BleDevice#getTimeInState(BleDeviceState)
	 */
	public final Interval getTimeInNativeState(BleManagerState state)
	{
		return Interval.millis(m_nativeStateTracker.getTimeInState(state.ordinal()));
	}

	/**
	 * Checks the underlying stack to see if BLE is supported on the phone.
	 */
	public final boolean isBleSupported()
	{
		PackageManager pm = m_context.getPackageManager();
		boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

		return hasBLE;
	}

	/**
	 * Checks to see if the device is running an Android OS which supports
	 * advertising.
	 */
	public final boolean isAdvertisingSupportedByAndroidVersion()
	{
		return Utils.isLollipop();
	}

	/**
	 * Checks to see if the device supports advertising.
	 */
	public final boolean isAdvertisingSupportedByChipset()
	{
		if( isAdvertisingSupportedByAndroidVersion() )
		{
			return managerLayer().isMultipleAdvertisementSupported();
		}
		else
		{
			return false;
		}
	}

	/**
	 * Checks to see if the device supports advertising BLE services.
	 */
	public final boolean isAdvertisingSupported()
	{
		return isAdvertisingSupportedByAndroidVersion() && isAdvertisingSupportedByChipset();
	}

	/**
	 * Disables BLE if manager is {@link BleManagerState#ON}. This disconnects all current
	 * connections, stops scanning, and forgets all discovered devices.
	 */
	public final void turnOff()
	{
		turnOff_private(false);
	}

	/**
	 * Returns the native manager.
	 */
	@Advanced
	public final BluetoothManager getNative()
	{
		return managerLayer().getNativeManager();
	}

	/**
	 * Returns the native bluetooth adapter.
	 */
	@Advanced
	public final BluetoothAdapter getNativeAdapter()
	{
		return managerLayer().getNativeAdaptor();
	}

	final P_NativeManagerLayer managerLayer()
	{
		return m_config.nativeManagerLayer;
	}

	/**
	 * Sets a default backup {@link BleNode.HistoricalDataLoadListener} that will be invoked
	 * for all historical data loads to memory for all uuids for all devices.
	 */
	public final void setListener_HistoricalDataLoad(@Nullable(Prevalence.NORMAL) final BleNode.HistoricalDataLoadListener listener_nullable)
	{
		m_historicalDataLoadListener = listener_nullable;
	}

	/**
	 * Set a listener here to be notified whenever we encounter an {@link UhOhListener.UhOh}.
	 */
	public final void setListener_UhOh(@Nullable(Prevalence.NORMAL) UhOhListener listener_nullable)
	{
		m_uhOhThrottler.setListener(listener_nullable);
	}

	/**
	 * Set a listener here to be notified whenever {@link #ASSERT(boolean)} fails.
	 * Mostly for use by internal library developers.
	 */
	public final void setListener_Assert(@Nullable(Prevalence.NORMAL) AssertListener listener_nullable)
	{
		m_assertionListener = listener_nullable;
	}

	/**
	 * Set a listener here to be notified whenever a {@link BleDevice} is discovered, rediscovered, or undiscovered.
	 */
	public final void setListener_Discovery(@Nullable(Prevalence.NORMAL) DiscoveryListener listener_nullable)
	{
		m_discoveryListener = listener_nullable;
	}

	/**
	 * Returns the discovery listener set with {@link #setListener_Discovery(DiscoveryListener)} or
	 * {@link BleManagerConfig#defaultDiscoveryListener}, or <code>null</code> if not set.
	 */
	public final DiscoveryListener getListener_Discovery()
	{
		return m_discoveryListener;
	}

	/**
	 * Set a listener here to be notified whenever this manager's {@link BleManagerState} changes.
	 */
	public final void setListener_State(@Nullable(Prevalence.NORMAL) ManagerStateListener listener_nullable)
	{
		m_stateTracker.setListener(listener_nullable);
	}

	/**
	 * Convenience method to listen for all changes in {@link BleDeviceState} for all devices.
	 * The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleDevice#setListener_State(DeviceStateListener)}.
	 *
	 * @see BleDevice#setListener_State(DeviceStateListener)
	 */
	public final void setListener_DeviceState(@Nullable(Prevalence.NORMAL) DeviceStateListener listener_nullable)
	{
		m_defaultDeviceStateListener = listener_nullable;
	}

	/**
	 * Convenience method to handle server connection fail events at the manager level. The listener provided
	 * will only get called if the server whose connection failed doesn't have a listener provided to
	 * {@link BleServer#setListener_ReconnectFilter(ServerReconnectFilter)}. This is unlike the behavior
	 * behind (for example) {@link #setListener_ServerState(ServerStateListener)} because
	 * {@link ServerReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)} requires a return value.
	 *
	 * @see BleServer#setListener_ReconnectFilter(ServerReconnectFilter)
	 */
	public final void setListener_ConnectionFail_Server(@Nullable(Prevalence.NORMAL) ServerReconnectFilter listener_nullable)
	{
		m_defaultConnectionFailListener_server = listener_nullable;
	}

	/**
	 * Convenience method to handle server request events at the manager level. The listener provided
	 * will only get called if the server receiving a request doesn't have a listener provided to
	 * {@link BleServer#setListener_Incoming(IncomingListener)} . This is unlike the behavior (for example)
	 * behind {@link #setListener_Outgoing(OutgoingListener)} because
	 * {@link IncomingListener#onEvent(IncomingListener.IncomingEvent)} requires a return value.
	 *
	 * @see BleServer#setListener_Incoming(IncomingListener)
	 */
	public final void setListener_Incoming(@Nullable(Prevalence.NORMAL) IncomingListener listener_nullable)
	{
		m_defaultServerIncomingListener = listener_nullable;
	}

	/**
	 * Convenience method to listen for all service addition events for all servers.
	 * The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleServer#setListener_ServiceAdd(AddServiceListener)}.
	 *
	 * @see BleServer#setListener_ServiceAdd(AddServiceListener)
	 */
	public final void setListener_ServiceAdd(@Nullable(Prevalence.NORMAL) AddServiceListener listener_nullable)
	{
		m_serviceAddListener = listener_nullable;
	}

	/**
	 * Convenience method to listen for all changes in {@link BleServerState} for all servers.
	 * The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleServer#setListener_State(ServerStateListener)}.
	 *
	 * @see BleServer#setListener_State(ServerStateListener)
	 */
	public final void setListener_ServerState(@Nullable(Prevalence.NORMAL) ServerStateListener listener_nullable)
	{
		m_defaultServerStateListener = listener_nullable;
	}

	/**
	 * Convenience method to listen for completion of all outgoing messages from
	 * {@link BleServer} instances. The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleServer#setListener_Outgoing(OutgoingListener)}.
	 *
	 * @see BleServer#setListener_Outgoing(OutgoingListener)
	 */
	public final void setListener_Outgoing(@Nullable(Prevalence.NORMAL) OutgoingListener listener_nullable)
	{
		m_defaultServerOutgoingListener = listener_nullable;
	}

	/**
	 * Convenience method to handle connection fail events at the manager level. The listener provided
	 * will only get called if the device whose connection failed doesn't have a listener provided to
	 * {@link BleDevice#setListener_Reconnect(DeviceReconnectFilter)}. This is unlike the behavior
	 * behind {@link #setListener_DeviceState(DeviceStateListener)} because
	 * {@link DeviceReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)} requires a return value.
	 *
	 * @see BleDevice#setListener_Reconnect(DeviceReconnectFilter)
	 */
	public final void setListener_DeviceReconnect(@Nullable(Prevalence.NORMAL) DeviceReconnectFilter listener_nullable)
	{
		m_defaultDeviceReconnectFilter = listener_nullable;
	}

	/**
	 * Convenience method to handle {@link BleDevice} connect events at a manager level. This listener simply reports when
	 * a device is connected, or has failed to connect -- {@link DeviceConnectListener#onEvent(Event)} may fire multiple times
	 * when {@link DeviceConnectListener.ConnectEvent#wasSuccess()} is <code>false</code>, as SweetBlue may be retrying the connection
	 * in the background, remember to check {@link DeviceConnectListener.ConnectEvent#isRetrying()}.
	 */
	public final void setListener_DeviceConnect(@Nullable(Prevalence.NORMAL) DeviceConnectListener listener_nullable)
	{
		m_defaultDeviceConnectListener = listener_nullable;
	}

	/**
	 * Convenience method to set a default back up listener for all {@link BondEvent}s across all {@link BleDevice} instances.
	 */
	public final void setListener_Bond(@Nullable(Prevalence.NORMAL) BondListener listener_nullable)
	{
		m_defaultBondListener = listener_nullable;
	}

	/**
	 * Sets a default backup {@link ReadWriteListener} that will be called for all {@link BleDevice} instances.
	 * <br><br>
	 * TIP: Place some analytics code in the listener here.
	 */
	public final void setListener_Read_Write(@Nullable(Prevalence.NORMAL) ReadWriteListener listener_nullable)
	{
		m_defaultReadWriteListener = listener_nullable;
	}

	public final void setListener_Notification(@Nullable(Prevalence.NORMAL) NotificationListener listener_nullable)
	{
		m_defaultNotificationListener = listener_nullable;
	}

	/**
	 * Set a listener here to be notified whenever this manager's native {@link BleManagerState} changes.
	 */
	public final void setListener_NativeState(NativeManagerStateListener listener)
	{
		m_nativeStateTracker.setListener(listener);
	}

	/**
	 * Set a listener here to be notified of the result of starting to advertise.
	 */
	public final void setListener_Advertising(AdvertisingListener listener)
	{
		m_advertisingListener = listener;
	}

	/**
	 * Manually starts a periodic scan. This is the post-constructor runtime equivalent to setting
	 * {@link BleManagerConfig#autoScanActiveTime} and {@link BleManagerConfig#autoScanPauseInterval}, so see
	 * their comments for more detail. Calling this forever-after overrides the options you set
	 * in {@link BleManagerConfig}.
	 *
	 * @see BleManagerConfig#autoScanActiveTime
	 * @see BleManagerConfig#autoScanPauseInterval
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, (ScanFilter) null, (DiscoveryListener) null);
	}

	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(DiscoveryListener)} for you too.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, DiscoveryListener discoveryListener)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, (ScanFilter) null, discoveryListener);
	}

	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but adds a filter too.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, ScanFilter filter)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, filter, (DiscoveryListener) null);
	}

	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(DiscoveryListener)} for you too and adds a filter.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, ScanFilter filter, DiscoveryListener discoveryListener)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, filter, ScanFilter.ApplyMode.CombineEither, discoveryListener);
	}

	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(DiscoveryListener)} for you too and adds a filter and a filter mode.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, ScanFilter filter, ScanFilter.ApplyMode filterApplyMode, DiscoveryListener discoveryListener)
	{
		showScanWarningIfNeeded();

		if( discoveryListener != null )
		{
			setListener_Discovery(discoveryListener);
		}

		m_filterMngr.setEphemeralFilter(filter, filterApplyMode);

		m_config.autoScanActiveTime = scanActiveTime;
		m_config.autoScanPauseInterval = scanPauseTime;

		if( doAutoScan() )
		{
			startScan_private(new ScanOptions().scanPeriodically(m_config.autoScanActiveTime, m_config.autoScanPauseInterval));
		}
	}

	/**
	 * Same as {@link #stopPeriodicScan()} but will also unregister any {@link ScanFilter} provided
	 * through {@link #startPeriodicScan(Interval, Interval, ScanFilter)} or other overloads.
	 */
	public final void stopPeriodicScan(final ScanFilter filter)
	{
		m_filterMngr.clearEphemeralFilter();

		stopPeriodicScan();
	}

	/**
	 * Stops a periodic scan previously started either explicitly with {@link #startPeriodicScan(Interval, Interval)} or through
	 * the {@link BleManagerConfig#autoScanActiveTime} and {@link BleManagerConfig#autoScanPauseInterval} config options.
	 */
	public final void stopPeriodicScan()
	{
		m_config.autoScanActiveTime = Interval.DISABLED;

		if( false == m_scanManager.isInfiniteScan() )
		{
			this.stopScan();
		}
	}

	/**
	 * Starts a scan that will continue indefinitely until {@link #stopScan()} is called.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 *
	 * @return <code>true</code> if scan started, <code>false></code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan()
	{
		return startScan(Interval.INFINITE);
	}

	/**
	 * Calls {@link #startScan(Interval, ScanFilter)} with {@link Interval#INFINITE}.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 *
	 * @return <code>true</code> if scan started, <code>false></code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(ScanFilter filter)
	{
		return startScan(Interval.INFINITE, filter, (DiscoveryListener) null);
	}

	/**
	 * Same as {@link #startScan()} but also calls {@link #setListener_Discovery(DiscoveryListener)} for you.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 *
	 * @return <code>true</code> if scan started, <code>false></code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(DiscoveryListener discoveryListener)
	{
		return startScan(Interval.INFINITE, (ScanFilter) null, discoveryListener);
	}

	/**
	 * Overload of {@link #startScan(Interval, ScanFilter, DiscoveryListener)}
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 *
	 * @return <code>true</code> if scan started, <code>false></code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(Interval scanTime, ScanFilter filter)
	{
		return startScan(scanTime, filter, (DiscoveryListener) null);
	}

	/**
	 * Overload of {@link #startScan(Interval, ScanFilter, DiscoveryListener)}
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 *
	 * @return <code>true</code> if scan started, <code>false></code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(Interval scanTime, DiscoveryListener discoveryListener)
	{
		return startScan(scanTime, (ScanFilter) null, discoveryListener);
	}

	/**
	 * Same as {@link #startScan()} but also calls {@link #setListener_Discovery(DiscoveryListener)} for you.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 *
	 * @return <code>true</code> if scan started, <code>false></code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(ScanFilter filter, DiscoveryListener discoveryListener)
	{
		return startScan(Interval.INFINITE, filter, discoveryListener);
	}

	/**
	 * Starts a scan that will generally last for the given time (roughly).
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 *
	 * @return <code>true</code> if scan started, <code>false></code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(Interval scanTime)
	{
		return startScan(scanTime, (ScanFilter) null, (DiscoveryListener) null);
	}

	/**
	 * Same as {@link #startScan(Interval)} but also calls {@link #setListener_Discovery(DiscoveryListener)} for you.
	 * <br><br>
	 * WARNING: For {@link android.os.Build.VERSION_CODES#M} and up, in order for this method to return scan events
	 * through {@link ScanFilter} you must have {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
	 * in your AndroidManifest.xml, AND enabled at runtime (see {@link #isLocationEnabledForScanning_byRuntimePermissions()} and {@link #turnOnLocationWithIntent_forPermissions(Activity, int)}),
	 * AND location services should be enabled (see {@link #isLocationEnabledForScanning_byOsServices()} and {@link #isLocationEnabledForScanning_byOsServices()}).
	 * <br><br>
	 * The assumed reason why location must be enabled is that an app might scan for bluetooth devices like iBeacons with known physical locations and unique advertisement packets.
	 * Knowing the physical locations, the app could report back that you're definitely within ~50 ft. of a given longitude and latitude. With multiple beacons involved and/or fine-tuned RSSI-based
	 * distance calculations the location could get pretty accurate. For example a department store app could sprinkle a few dozen beacons throughout its store and
	 * if you had their app running they would know exactly where you are. Not an everyday concern, and it makes BLE even more annoying to implement on Android,
	 * but Google is understandably erring on the side of privacy and security for its users.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 *
	 * @return <code>true</code> if scan started, <code>false></code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(Interval scanTime, ScanFilter filter, DiscoveryListener discoveryListener)
	{
		showScanWarningIfNeeded();

		return startScan_private(new ScanOptions().scanFor(scanTime).withScanFilter(filter).withDiscoveryListener(discoveryListener));
	}

	public final boolean startScan(ScanOptions options)
	{
		showScanWarningIfNeeded();

		return startScan_private(options);
	}

	private void showScanWarningIfNeeded()
	{
		if( false == isLocationEnabledForScanning() )
		{
			final String ENABLED = "enabled";
			final String DISABLED = "disabled";

			final boolean reasonA = isLocationEnabledForScanning_byManifestPermissions();
			final boolean reasonB = isLocationEnabledForScanning_byRuntimePermissions();
			final boolean reasonC = isLocationEnabledForScanning_byOsServices();
			final String enabledA = reasonA ? ENABLED : DISABLED;
			final String enabledB = reasonB ? ENABLED : DISABLED;
			final String enabledC = reasonC ? ENABLED : DISABLED;

			Log.w
					(
							BleManager.class.getSimpleName(),

							"As of Android M, in order for low energy scan results to return you must have the following:\n" +
									"(A) " + Manifest.permission.ACCESS_COARSE_LOCATION + " or " + Manifest.permission.ACCESS_FINE_LOCATION + " in your AndroidManifest.xml.\n" +
									"(B) Runtime permissions for aforementioned location permissions as described at https://developer.android.com/training/permissions/requesting.html.\n" +
									"(C) Location services enabled, the same as if you go to OS settings App and enable Location.\n" +
									"It looks like (A) is " + enabledA + ", (B) is " + enabledB + ", and (C) is " + enabledC + ".\n" +
									"Various methods like BleManager.isLocationEnabledForScanning*() overloads and BleManager.turnOnLocationWithIntent*() overloads can help with this painful process.\n" +
									"Good luck!"
					);
		}
	}

//
//    final boolean startScan_private(Interval scanTime, ScanFilter filter, DiscoveryListener discoveryListener, final boolean isPoll)
//    {
	final boolean startScan_private(ScanOptions options)
	{
		if (m_taskManager.isInQueue(P_Task_Scan.class, this))
		{
			getLogger().w("A startScan method was called when there's already a scan task in the queue!");
			return false;
		}

		if( false == isBluetoothEnabled() )
		{
			m_logger.e(BleManager.class.getSimpleName() + " is not " + ON + "! Please use the turnOn() method first.");

			return false;
		}

		final P_Task_Scan scanTask = m_taskManager.get(P_Task_Scan.class, this);

		if( scanTask != null )
		{
			scanTask.resetTimeout(options.m_scanTime.secs());
		}
		else
		{
			ASSERT(!m_taskManager.isCurrentOrInQueue(P_Task_Scan.class, this));

			m_stateTracker.append(BleManagerState.STARTING_SCAN, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

			m_scanManager.resetTimeNotScanning();
			options.m_scanTime = options.m_scanTime.secs() < 0.0 ? Interval.INFINITE : options.m_scanTime;
			m_scanManager.setInfiniteScan(options.m_scanTime.equals(Interval.INFINITE), options.m_forceIndefinite);

			if( options.m_discoveryListener != null )
			{
				setListener_Discovery(options.m_discoveryListener);
			}


			if (options.m_scanFilter != null)
			{
				m_filterMngr.setEphemeralFilter(options.m_scanFilter, options.m_scanFilterApplyMode);
			}

			if (options.m_isPeriodic)
			{
				m_config.autoScanActiveTime = options.m_scanTime;
				m_config.autoScanPauseInterval = options.m_pauseTime;
			}

            PE_TaskPriority pri = options.m_isPriorityScan ? PE_TaskPriority.CRITICAL : null;

			boolean startScan = true;

			if (options.m_isPeriodic)
			{
				if (!doAutoScan() )
				{
					startScan = false;
				}
			}

			if (startScan)
			{
				m_taskManager.add(new P_Task_Scan(this, m_listeners.getScanTaskListener(), options.m_scanTime.secs(), options.m_isPeriodic, pri));
			}
		}

		return true;
	}

	/**
	 * Requires the {@link android.Manifest.permission#WAKE_LOCK} permission. Gives you access to the internal
	 * wake lock as a convenience and eventually calls {@link android.os.PowerManager.WakeLock#acquire()}.
	 *
	 * @see BleManagerConfig#manageCpuWakeLock
	 */
	@Advanced
	public final void pushWakeLock()
	{
		m_wakeLockMngr.push();
	}

	/**
	 * Opposite of {@link #pushWakeLock()}, eventually calls {@link android.os.PowerManager.WakeLock#release()}.
	 */
	@Advanced
	public final void popWakeLock()
	{
		m_wakeLockMngr.pop();
	}

	/**
	 * Fires a callback to {@link AssertListener} if condition is false. Will post a {@link android.util.Log#ERROR}-level
	 * message with a stack trace to the console as well if {@link BleManagerConfig#loggingOptions} is not {@link LogOptions#OFF}.
	 */
	@Advanced
	public final boolean ASSERT(boolean condition)
	{
		return ASSERT(condition, "");
	}

	/**
	 * Same as {@link #ASSERT(boolean)} but with an added message.
	 */
	@Advanced
	public final boolean ASSERT(boolean condition, String message)
	{
		if( !condition )
		{
			Exception dummyException = null;
			message = message != null ? message : "";

			if( m_logger.isEnabled() || m_assertionListener != null )
			{
				dummyException = new Exception();
			}

			if( m_logger.isEnabled() )
			{
				Log.e(BleManager.class.getSimpleName(), "ASSERTION FAILED " + message, dummyException);
			}

			if( m_assertionListener != null )
			{
				final AssertListener.AssertEvent event = new AssertListener.AssertEvent(this, message, dummyException.getStackTrace());

				m_assertionListener.onEvent(event);
			}

			return false;
		}

		return true;
	}

	/**
	 * Returns the abstracted bitwise state mask representation of {@link BleManagerState} for this device.
	 *
	 * @see BleManagerState
	 */
	public final int getStateMask()
	{
		return m_stateTracker.getState();
	}

	/**
	 * Returns the native bitwise state mask representation of {@link BleManagerState} for this device.
	 * Similar to calling {@link android.bluetooth.BluetoothAdapter#getState()}
	 *
	 * @see BleManagerState
	 */
	@Advanced
	public final int getNativeStateMask()
	{
		return m_nativeStateTracker.getState();
	}

	/**
	 * Enables BLE if manager is currently {@link BleManagerState#OFF} or {@link BleManagerState#TURNING_OFF}, otherwise does nothing.
	 * For a convenient way to ask your user first see {@link #turnOnWithIntent(android.app.Activity, int)}.
	 */
	public final void turnOn()
	{
		if( isAny(TURNING_ON, ON) )  return;

		if( is(OFF) )
		{
			m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_ON, true, OFF, false);
		}

		m_taskManager.add(new P_Task_TurnBleOn(this, /*implicit=*/false));
		if (m_timeTurnedOn == 0) {
			m_timeTurnedOn = System.currentTimeMillis();
		}
	}

	final void forceOn()
	{
		m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ON, true, OFF, false);
	}

	final void forceOff()
	{
		m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ON, false, OFF, true);
	}

	/**
	 * This is essentially a big red reset button for the Bluetooth stack. Use it ruthlessly
	 * when the stack seems to be acting up, like when you can't connect to a device that you should be
	 * able to connect to. It's similar to calling {@link #turnOff()} then {@link #turnOn()},
	 * but also does other things like removing all bonds (similar to {@link #unbondAll()}) and
	 * other "special sauce" such that you should use this method instead of trying to reset the
	 * stack manually with component calls.
	 * <br><br>
	 * It's good app etiquette to first prompt the user to get permission to reset because
	 * it will affect Bluetooth system-wide and in other apps.
	 *
	 *  @see BleManagerState#RESETTING
	 */
	public final void reset()
	{
		reset(null);
	}

	/**
	 * Same as {@link #reset()} but with a convenience callback for when the reset is
	 * completed and the native BLE stack is (should be) back to normal.
	 *
	 * @see BleManagerState#RESETTING
	 */
	public final void reset(ResetListener listener)
	{
		if( listener != null )
		{
			if( m_resetListeners != null )
			{
				m_resetListeners.addListener(listener);
			}
			else
			{
				m_resetListeners = new P_WrappingResetListener(listener, m_postManager.getUIHandler(), m_config.postCallbacksToMainThread);
			}
		}

		if( is(BleManagerState.RESETTING) )
		{
			return;
		}

		m_stateTracker.append(RESETTING, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		if( m_config.enableCrashResolverForReset )
		{
			m_taskManager.add(new P_Task_CrashResolver(BleManager.this, m_crashResolver, /*partOfReset=*/true));
		}

		turnOff_private(/*removeAllBonds=*/true);

		m_taskManager.add(new P_Task_TurnBleOn(this, /*implicit=*/false, new PA_Task.I_StateListener()
		{
			@Override
			public void onStateChange(PA_Task taskClass, PE_TaskState state)
			{
				if (state.isEndingState())
				{
					ResetListener nukeListeners = m_resetListeners;
					m_resetListeners = null;
					m_nativeStateTracker.remove(RESETTING, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
					m_stateTracker.remove(RESETTING, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

					if (nukeListeners != null)
					{
						ResetListener.ResetEvent event = new ResetListener.ResetEvent(BleManager.this, ResetListener.Progress.COMPLETED);
						nukeListeners.onEvent(event);
					}
				}
			}
		}));
	}

	/**
	 * Removes bonds for all devices that are {@link BleDeviceState#BONDED}.
	 * Essentially a convenience method for calling {@link BleDevice#unbond()},
	 * on each device individually.
	 */
	public final void unbondAll()
	{
		m_deviceMngr.unbondAll(null, Status.CANCELLED_FROM_UNBOND);
	}

	/**
	 * Disconnects all devices that are {@link BleDeviceState#CONNECTED}.
	 * Essentially a convenience method for calling {@link BleDevice#disconnect()},
	 * on each device individually.
	 */
	public final void disconnectAll()
	{
		m_deviceMngr.disconnectAll();
	}

	/**
	 * Same as {@link #disconnectAll()} but drills down to {@link BleDevice#disconnect_remote()} instead.
	 */
	public final void disconnectAll_remote()
	{
		m_deviceMngr.disconnectAll_remote();
	}

	/**
	 * Undiscovers all devices that are {@link BleDeviceState#DISCOVERED}.
	 * Essentially a convenience method for calling {@link BleDevice#undiscover()},
	 * on each device individually.
	 */
	public final void undiscoverAll()
	{
		m_deviceMngr.undiscoverAll();
	}

	/**
	 * If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code>, you can use this method to allow the user to enable location services.
	 * <br><br>
	 * NOTE: If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code> but all other overloads of {@link #isLocationEnabledForScanning()} return <code>true</code> then
	 * SweetBlue will fall back to classic discovery through {@link BluetoothAdapter#startDiscovery()} when you call {@link #startScan()} or overloads, so you may not have to use this.
	 *
	 * @see #isLocationEnabledForScanning_byOsServices()
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void turnOnLocationWithIntent_forOsServices(final Activity callingActivity, int requestCode)
	{
		final Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

		callingActivity.startActivityForResult(enableLocationIntent, requestCode);
	}

	/**
	 * Overload of {@link #turnOnLocationWithIntent_forOsServices(Activity, int)} if you don't care about result.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void turnOnLocationWithIntent_forOsServices(final Activity callingActivity)
	{
		final Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

		callingActivity.startActivity(enableLocationIntent);

		if( false == Utils.isMarshmallow() )
		{
			m_logger.w("You may use this method but since the phone is at " + Build.VERSION.SDK_INT + " and the requirement is "+Build.VERSION_CODES.M+", it is not necessary for scanning.");
		}
	}

	private static final String LOCATION_PERMISSION_NAMESPACE = "location_permission_namespace";
	private static final String LOCATION_PERMISSION_KEY = "location_permission_key";

	/**
	 * Returns <code>true</code> if {@link #turnOnLocationWithIntent_forPermissions(Activity, int)} will pop a system dialog, <code>false</code> if it will bring
	 * you to the OS's Application Settings. The <code>true</code> case happens if the app has never shown a request Location Permissions dialog or has shown a request Location Permission dialog and the user has yet to select "Never ask again". This method is used to weed out the false
	 * negative from {@link Activity#shouldShowRequestPermissionRationale(String)} when the Location Permission has never been requested. Make sure to use this in conjunction with {@link #isLocationEnabledForScanning_byRuntimePermissions()}
	 * which will tell you if permissions are already enabled.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final boolean willLocationPermissionSystemDialogBeShown(Activity callingActivity)
	{
		if( Utils.isMarshmallow() )
		{
			SharedPreferences preferences = callingActivity.getSharedPreferences(LOCATION_PERMISSION_NAMESPACE, Context.MODE_PRIVATE);
			boolean hasNeverAskAgainBeenSelected = !M_Util.shouldShowRequestPermissionRationale(callingActivity);//Call only returns true if Location permission has been previously denied. Returns false if "Never ask again" has been selected
			boolean hasLocationPermissionSystemDialogShownOnce = preferences.getBoolean(LOCATION_PERMISSION_KEY, false);

			return (!hasLocationPermissionSystemDialogShownOnce) || (hasLocationPermissionSystemDialogShownOnce && !hasNeverAskAgainBeenSelected);
		}
		else
		{
			return false;
		}
	}

	/**
	 * If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code>, you can use this method to allow the user to enable location
	 * through an OS intent. The result of the request (i.e. what the user chose) is passed back through {@link Activity#onRequestPermissionsResult(int, String[], int[])}
	 * with the requestCode provided as the second parameter to this method. If the user selected "Never ask again" the function will open up the app settings screen where the
	 * user can navigate to enable the permissions.
	 *
	 * @see #isLocationEnabledForScanning_byRuntimePermissions()
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void turnOnLocationWithIntent_forPermissions(final Activity callingActivity, int requestCode)
	{
		if( Utils.isMarshmallow() )
		{
			if( false == isLocationEnabledForScanning_byRuntimePermissions() && false == willLocationPermissionSystemDialogBeShown(callingActivity))
			{
				final Intent intent = new Intent();
				intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				final Uri uri = Uri.fromParts("package", callingActivity.getPackageName(), null);
				intent.setData(uri);
				callingActivity.startActivityForResult(intent, requestCode);
			}
			else
			{
				final SharedPreferences.Editor editor = callingActivity.getSharedPreferences(LOCATION_PERMISSION_NAMESPACE, Context.MODE_PRIVATE).edit();
				editor.putBoolean(LOCATION_PERMISSION_KEY, true).commit();
				M_Util.requestPermissions(callingActivity, requestCode);
			}
		}
		else
		{
			m_logger.w("BleManager.turnOnLocationWithIntent_forPermissions() is only applicable for API levels 23 and above so this method does nothing.");
		}
	}

	/**
	 * Tells you whether a call to {@link #startScan()} (or overloads), will succeed or not. Basically a convenience for checking if both
	 * {@link #isLocationEnabledForScanning()} and {@link #is(BleManagerState)} with {@link BleManagerState#SCANNING} return <code>true</code>.
	 */
	public final boolean isScanningReady()
	{
		return isLocationEnabledForScanning() && is(ON);
	}

	/**
	 * Convenience method which reports <code>true</code> if the {@link BleManager} is in any of the following states: <br></br>
	 * {@link BleManagerState#SCANNING}, {@link BleManagerState#SCANNING_PAUSED}, {@link BleManagerState#BOOST_SCANNING}, or {@link BleManagerState#STARTING_SCAN}
	 */
	public final boolean isScanning()
	{
		return isAny(SCANNING, SCANNING_PAUSED, BOOST_SCANNING, STARTING_SCAN);
	}

	/**
	 * Returns <code>true</code> if location is enabled to a degree that allows scanning on {@link android.os.Build.VERSION_CODES#M} and above.
	 * If this returns <code>false</code> it means you're on Android M and you either (A) do not have {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
	 * (or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} in your AndroidManifest.xml, see {@link #isLocationEnabledForScanning_byManifestPermissions()}), or (B)
	 * runtime permissions for aformentioned location permissions are off (see {@link #isLocationEnabledForScanning_byRuntimePermissions()} and
	 * https://developer.android.com/training/permissions/index.html), or (C) location services on the phone are disabled (see {@link #isLocationEnabledForScanning_byOsServices()}).
	 * <br><br>
	 * If this returns <code>true</code> then you are good to go for calling {@link #startScan()}.
	 *
	 * @see #startScan(Interval, ScanFilter, DiscoveryListener)
	 *
	 * @see #turnOnLocationWithIntent_forPermissions(Activity, int)
	 * @see #turnOnLocationWithIntent_forOsServices(Activity)
	 * @see #turnOnLocationWithIntent_forOsServices(Activity, int)
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final boolean isLocationEnabledForScanning()
	{
		return managerLayer().isLocationEnabledForScanning();
	}

	/**
	 * Returns <code>true</code> if you're either pre-Android-M, or app has permission for either {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
	 * or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} in your AndroidManifest.xml, <code>false</code> otherwise.
	 *
	 * @see #startScan(Interval, ScanFilter, DiscoveryListener)
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final boolean isLocationEnabledForScanning_byManifestPermissions()
	{
		return Utils.isLocationEnabledForScanning_byManifestPermissions(getApplicationContext());
	}

	/**
	 * Returns <code>true</code> if you're either pre-Android-M, or app has runtime permissions enabled by checking
	 * <a href="https://developer.android.com/reference/android/support/v4/content/ContextCompat.html#checkSelfPermission(android.content.Context, java.lang.String)"</a>	 *
	 * See more information at https://developer.android.com/training/permissions/index.html.
	 *
	 * @see #startScan(Interval, ScanFilter, DiscoveryListener)
	 *
	 * @see #turnOnLocationWithIntent_forPermissions(Activity, int)
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final boolean isLocationEnabledForScanning_byRuntimePermissions()
	{
		return managerLayer().isLocationEnabledForScanning_byRuntimePermissions();
	}

	/**
	 * Returns <code>true</code> if you're either pre-Android-M, or location services are enabled, the same is if you go to the Android Settings app
	 * and manually toggle Location ON/OFF.
	 * <br><br>
	 * NOTE: If this returns <code>false</code> but all other overloads of {@link #isLocationEnabledForScanning()} return <code>true</code> then
	 * SweetBlue will fall back to classic discovery through {@link BluetoothAdapter#startDiscovery()} when you call {@link #startScan()} or overloads.
	 *
	 * @see #startScan(Interval, ScanFilter, DiscoveryListener)
	 *
	 * @see #turnOnLocationWithIntent_forOsServices(Activity)
	 * @see #turnOnLocationWithIntent_forOsServices(Activity, int)
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final boolean isLocationEnabledForScanning_byOsServices()
	{
		return managerLayer().isLocationEnabledForScanning_byOsServices();
	}

	/**
	 * Convenience method to request your user to enable ble in a "standard" way
	 * with an {@link android.content.Intent} instead of using {@link #turnOn()} directly.
	 * Result will be posted as normal to {@link android.app.Activity#onActivityResult(int, int, Intent)}.
	 * If current state is {@link BleManagerState#ON} or {@link BleManagerState#TURNING_ON}
	 * this method early outs and does nothing.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
	 */
	public final void turnOnWithIntent(Activity callingActivity, int requestCode)
	{
		if( isAny(ON, TURNING_ON) )  return;

		final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		callingActivity.startActivityForResult(enableBtIntent, requestCode);
	}

	/**
	 * Opposite of {@link #onPause()}, to be called from your override of {@link android.app.Activity#onResume()} for each {@link android.app.Activity}
	 * in your application. See comment for {@link #onPause()} for a similar explanation for why you should call this method.
	 */
	public final void onResume()
	{
		m_isForegrounded = true;
		m_timeForegrounded = 0.0;

		m_scanManager.onResume();
	}

	/**
	 * It's generally recommended to call this in your override of {@link android.app.Activity#onPause()} for each {@link android.app.Activity}
	 * in your application. This doesn't do much for now, just a little bookkeeping and stops scan automatically if
	 * {@link BleManagerConfig#stopScanOnPause} is <code>true</code>. Strictly speaking you don't *have* to call this method,
	 * but another good reason is for future-proofing. Later releases of this library may do other more important things
	 * in this method so it's good to have it being called just in case.
	 */
	public final void onPause()
	{
		m_isForegrounded = false;
		m_timeForegrounded = 0.0;
		m_scanManager.onPause();
	}

	/**
	 * Call this from your app's {@link android.app.Activity#onDestroy()} method.
	 * NOTE: Apparently no good way to know when app as a whole is being destroyed
	 * and not individual Activities, so keeping this package-private for now.
	 */
	final void onDestroy()
	{
		m_wakeLockMngr.clear();
		m_listeners.onDestroy();
	}

	/**
	 * Disconnects all devices, shuts down the BleManager, and it's backing thread, and unregisters any receivers that may be in use.
	 * This also clears out it's static instance. This is meant to be called upon application exit. However, to use it again,
	 * just call {@link BleManager#get(Context)}, or {@link BleManager#get(Context, BleManagerConfig)} again.
	 */
	public final void shutdown()
	{
		disconnectAll();
		m_uhOhThrottler.shutdown();
		m_updateRunnable.m_shutdown = true;
		m_postManager.removeUpdateCallbacks(m_updateRunnable);
		m_postManager.quit();
		m_wakeLockMngr.clear();
		m_listeners.onDestroy();
		s_instance = null;
	}

	/**
	 * Returns the {@link android.app.Application} provided to the constructor.
	 */
	public final Context getApplicationContext()
	{
		return (Application) m_context;
	}

	/**
	 * Convenience that will call both {@link #stopPeriodicScan()} and {@link #stopScan()} for you.
	 */
	public final void stopAllScanning()
	{
		this.stopPeriodicScan();
		this.stopScan();
	}

	/**
	 * Stops a scan previously started by {@link #startScan()} or its various overloads.
	 * This will also stop the actual scan operation itself that may be ongoing due to
	 * {@link #startPeriodicScan(Interval, Interval)} or defined by {@link BleManagerConfig#autoScanActiveTime},
	 * but scanning in general will still continue periodically until you call {@link #stopPeriodicScan()}.
	 */
	public final void stopScan()
	{
		m_scanManager.setInfiniteScan(false, false);

		stopScan_private(E_Intent.INTENTIONAL);
	}

	/**
	 * Same as {@link #stopScan()} but also unregisters any filter supplied to various overloads of
	 * {@link #startScan()} or {@link #startPeriodicScan(Interval, Interval)} that take an {@link ScanFilter}.
	 * Calling {@link #stopScan()} alone will keep any previously registered filters active.
	 */
	public final void stopScan(ScanFilter filter)
	{
		m_filterMngr.clearEphemeralFilter();

		stopScan();
	}

	final void stopScan_private(E_Intent intent)
	{
		m_scanManager.resetTimeNotScanning();

		if( !m_taskManager.succeed(P_Task_Scan.class, this) )
		{
			// I don't think this is needed, if we're clearing it from the queue below.
//			P_Task_Scan scanTask = m_taskQueue.get(P_Task_Scan.class, BleManager.this);
//			if (scanTask != null)
//				scanTask.succeed();
			m_logger.i("Clearing queue of any scan tasks...");
			m_taskManager.clearQueueOf(P_Task_Scan.class, BleManager.this);
		}

		m_stateTracker.remove(BleManagerState.STARTING_SCAN, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		m_stateTracker.remove(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		m_stateTracker.remove(BleManagerState.SCANNING_PAUSED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		m_stateTracker.remove(BleManagerState.BOOST_SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	/**
	 * Gets a known {@link BleDeviceState#DISCOVERED} device by MAC address, or {@link BleDevice#NULL} if there is no such device.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice(final String macAddress)
	{
		final String macAddress_normalized = normalizeMacAddress(macAddress);

		final BleDevice device = m_deviceMngr.get(macAddress_normalized);

		if( device != null )  return device;

		return BleDevice.NULL;
	}

	/**
	 * Shortcut for checking if {@link #getDevice(String)} returns {@link BleDevice#NULL}.
	 */
	public final boolean hasDevice(final String macAddress)
	{
		return !getDevice(macAddress).isNull();
	}

	/**
	 * Calls {@link #hasDevice(String)}.
	 */
	public final boolean hasDevice(final BleDevice device)
	{
		return hasDevice(device.getMacAddress());
	}

	/**
	 * Might not be useful to outside world. Used for sanity/early-out checks internally. Keeping private for now.
	 * Does referential equality check.
	 */
	private final boolean hasDevice_private(BleDevice device)
	{
		return m_deviceMngr.has(device);
	}

	/**
	 * Returns the first device that is in the given state, or {@link BleDevice#NULL} if no match is found.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice(BleDeviceState state)
	{
		for( int i = 0; i < m_deviceMngr.getCount(); i++ )
		{
			BleDevice device = m_deviceMngr.get(i);

			if( device.is(state) )
			{
				return device;
			}
		}

		return BleDevice.NULL;
	}

	/**
	 * Returns true if we have a device in the given state.
	 */
	public final boolean hasDevice(BleDeviceState state)
	{
		return !getDevice(state).isNull();
	}

	/**
	 * Forwards {@link #getDeviceAt(int)} with index of 0.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice()
	{
		return hasDevices() ? getDeviceAt(0) : BleDevice.NULL;
	}

	/**
	 * Returns the first device that matches the query, or {@link BleDevice#NULL} if no match is found.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice(Object ... query)
	{
		for( int i = 0; i < m_deviceMngr.getCount(); i++ )
		{
			BleDevice device = m_deviceMngr.get(i);

			if( device.is(query) )
			{
				return device;
			}
		}

		return BleDevice.NULL;
	}

	/**
	 * Returns true if we have a device that matches the given query.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final boolean hasDevice(Object ... query)
	{
		return !getDevice(query).isNull();
	}

	/**
	 * Returns the first device which returns <code>true</code> for {@link BleDevice#isAny(int)}, or {@link BleDevice#NULL} if no such device is found.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice(final int mask_BleDeviceState)
	{
		return m_deviceMngr.getDevice(mask_BleDeviceState);
	}

	/**
	 * Returns <code>true</code> if there is any {@link BleDevice} for which {@link BleDevice#isAny(int)} with the given mask returns <code>true</code>.
	 */
	public final boolean hasDevice(final int mask_BleDeviceState)
	{
		return !getDevice(mask_BleDeviceState).isNull();
	}

	/**
	 * Offers a more "functional" means of iterating through the internal list of devices instead of
	 * using {@link #getDevices()} or {@link #getDevices_List()}.
	 */
	public final void getDevices(final ForEach_Void<BleDevice> forEach)
	{
		m_deviceMngr.forEach(forEach);
	}

	/**
	 * Same as {@link #getDevices(ForEach_Void)} but will only return devices
	 * in the given state provided.
	 */
	public final void getDevices(final ForEach_Void<BleDevice> forEach, final BleDeviceState state)
	{
		m_deviceMngr.forEach(forEach, state, true);
	}

	/**
	 * Overload of {@link #getDevices(ForEach_Void)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getDevices(final ForEach_Breakable<BleDevice> forEach)
	{
		m_deviceMngr.forEach(forEach);
	}

	/**
	 * Overload of {@link #getDevices(ForEach_Void, BleDeviceState)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getDevices(final ForEach_Breakable<BleDevice> forEach, final BleDeviceState state)
	{
		m_deviceMngr.forEach(forEach, state, true);
	}

	/**
	 * Returns the mac addresses of all devices that we know about from both current and previous
	 * app sessions.
	 */
	public final @Nullable(Prevalence.NEVER) Iterator<String> getDevices_previouslyConnected()
	{
		return m_diskOptionsMngr.getPreviouslyConnectedDevices();
	}


	/**
	 * Convenience method to return a {@link Set} of currently bonded devices. This simply calls
	 * {@link BluetoothAdapter#getBondedDevices()}, and wraps all bonded devices into separate
	 * {@link BleDevice} classes.
	 *
	 * NOTE: If the Bluetooth radio is turned off, some android devices return <code>null</code>. In this case,
	 * SweetBlue will just return an empty list.
     */
	public final Set<BleDevice> getDevices_bonded()
	{
		Set<BluetoothDevice> native_bonded_devices = managerLayer().getBondedDevices();
		// The native system can return null from the above call if the bluetooth radio is
		// turned off, so if that's the case, just return an empty Set.
		if (native_bonded_devices == null)
			return new HashSet<>(0);

		Set<BleDevice> bonded_devices = new HashSet<>(native_bonded_devices.size());
		BleDevice device;
		for (BluetoothDevice d : native_bonded_devices)
		{
			device = getDevice(d.getAddress());
			if (device.isNull())
			{
				device = newDevice(d.getAddress());
			}
			bonded_devices.add(device);
		}
		return bonded_devices;
	}

	/**
	 * Returns all the devices managed by this class. This generally includes all devices that are either.
	 * {@link BleDeviceState#ADVERTISING} or {@link BleDeviceState#CONNECTED}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices()
	{
		return new BleDeviceIterator(getDevices_List());
	}

	/**
	 * Same as {@link #getDevices()}, but with the devices sorted using {@link BleManagerConfig#defaultListComparator}, which
	 * by default sorts by {@link BleDevice#getName_debug()}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices_sorted()
	{
		return new BleDeviceIterator(getDevices_List_sorted());
	}

	/**
	 * Overload of {@link #getDevices()} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List()
	{
		return (List<BleDevice>) m_deviceMngr.getList().clone();
	}

	/**
	 * Same as {@link #getDevices_List()}, but sorts the list using {@link BleManagerConfig#defaultListComparator}.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List_sorted()
	{
		return (List<BleDevice>) m_deviceMngr.getList_sorted().clone();
	}

	/**
	 * Returns the total number of devices this manager is...managing.
	 * This includes all devices that are {@link BleDeviceState#DISCOVERED}.
	 */
	public final int getDeviceCount()
	{
		return m_deviceMngr.getCount();
	}

	/**
	 * Returns the number of devices that are in the current state.
	 */
	public final int getDeviceCount(BleDeviceState state)
	{
		return m_deviceMngr.getCount(state);
	}

	/**
	 * Returns the number of devices that match the given query.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final int getDeviceCount(Object ... query)
	{
		return m_deviceMngr.getCount(query);
	}

	/**
	 * Accessor into the underlying array used to store {@link BleDevice} instances.
	 * Combine with {@link #getDeviceCount()} to iterate, or you may want to use the
	 * {@link java.util.Iterator} returned from {@link #getDevices()} and its various overloads instead.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDeviceAt(final int index)
	{
		return m_deviceMngr.get(index);
	}

	/**
	 * Returns the index of this device in the internal list, or -1 if it's not found.
	 */
	public final int getDeviceIndex(final BleDevice device)
	{
		for( int i = 0; i < getDeviceCount(); i++ )
		{
			final BleDevice ith = getDeviceAt(i);

			if( ith.equals(device) )
			{
				return i;
			}
		}

		return -1;
	}

	public final @Nullable(Prevalence.NEVER) BleDevice getDevice_previous(final BleDevice device)
	{
		return m_deviceMngr.getDevice_offset(device, -1);
	}

	/**
	 * Same as {@link #getDevice_next(BleDevice, BleDeviceState)} but just returns the next device in the internal list
	 * with no state checking.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice_next(final BleDevice device)
	{
		return m_deviceMngr.getDevice_offset(device, 1);
	}

	/**
	 * Returns the first device previous to the provided one in the internal list that is in the given state. For various fringe cases like
	 * this manager not having any devices, this method returns {@link BleDevice#NULL}. This method wraps
	 * around so that if the provided device is at index 0, the returned device will be the last device this manager holds.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice_previous(final BleDevice device, final BleDeviceState state)
	{
		return m_deviceMngr.getDevice_offset(device, -1, state, true);
	}

	/**
	 * Same as {@link #getDevice_previous(BleDevice, BleDeviceState)} but returns the next device in the internal list
	 * with no state checking.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice_next(final BleDevice device, final BleDeviceState state)
	{
		return m_deviceMngr.getDevice_offset(device, 1, state, true);
	}

	/**
	 * Same as {@link #getDevice_previous(BleDevice, BleDeviceState)} but allows you to pass a query.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice_previous(final BleDevice device, final Object ... query)
	{
		return m_deviceMngr.getDevice_offset(device, -1, query);
	}

	/**
	 * Same as {@link #getDevice_next(BleDevice, BleDeviceState)} but allows you to pass a query.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice_next(final BleDevice device, final Object ... query)
	{
		return m_deviceMngr.getDevice_offset(device, 1, query);
	}

	/**
	 * Returns whether we have any devices. For example if you have never called {@link #startScan()}
	 * or {@link #newDevice(String)} (or overloads) then this will return false.
	 */
	public final boolean hasDevices()
	{
		return m_deviceMngr.getCount() > 0;
	}

	/**
	 * Same as {@link #getDevice(BleDeviceState)} except returns all matching devices.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final BleDeviceState state)
	{
		return new BleDeviceIterator(getDevices_List(), state, true);
	}

	/**
	 * Overload of {@link #getDevices(BleDeviceState)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final BleDeviceState state)
	{
		return m_deviceMngr.getDevices_List(false, state);
	}

	/**
	 * Same as {@link #getDevices_List(BleDeviceState)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List_sorted(final BleDeviceState state)
	{
		return m_deviceMngr.getDevices_List(true, state);
	}

	/**
	 * Same as {@link #getDevice(Object...)} except returns all matching devices.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final Object ... query)
	{
		return new BleDeviceIterator(getDevices_List(), query);
	}

	/**
	 * Overload of {@link #getDevices(Object...)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final Object ... query)
	{
		return m_deviceMngr.getDevices_List(false, query);
	}

	/**
	 * Same as {@link #getDevices_List(Object...)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List_sorted(final Object ... query)
	{
		return m_deviceMngr.getDevices_List(true, query);
	}

	/**
	 * Same as {@link #getDevices()} except filters using {@link BleDevice#isAny(int)}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final int mask_BleDeviceState)
	{
		return new BleDeviceIterator(getDevices_List(), mask_BleDeviceState);
	}

	/**
	 * Overload of {@link #getDevices(int)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final int mask_BleDeviceState)
	{
		return m_deviceMngr.getDevices_List(false, mask_BleDeviceState);
	}

	/**
	 * Same as {@link #getDevices_List(int)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List_sorted(final int mask_BleDeviceState)
	{
		return m_deviceMngr.getDevices_List(true, mask_BleDeviceState);
	}

	/**
	 * Removes the given {@link BleDevice} from SweetBlue's internal device cache list. You should never have to call this
	 * yourself (and probably shouldn't), but it's here for flexibility.
     */
	@Advanced
	public final void removeDeviceFromCache(BleDevice device)
	{
		m_deviceMngr.remove(device, m_deviceMngr_cache);
	}

	/**
	 * Removes all {@link BleDevice}s from SweetBlue's internal device cache list. You should never have to call this
	 * yourself (and probably shouldn't), but it's here for flexibility.
	 */
	@Advanced
	public final void removeAllDevicesFromCache()
	{
		m_deviceMngr.removeAll(m_deviceMngr_cache);
	}

	/**
	 * Returns a new {@link HistoricalData} instance using
	 * {@link BleDeviceConfig#historicalDataFactory} if available.
	 */
	public final HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
	{
		final BleDeviceConfig.HistoricalDataFactory factory = m_config.historicalDataFactory;

		if( m_config.historicalDataFactory != null )
		{
			return m_config.historicalDataFactory.newHistoricalData(data, epochTime);
		}
		else
		{
			return new HistoricalData(data, epochTime);
		}
	}

	/**
	 * Same as {@link #newHistoricalData(byte[], EpochTime)} but tries to use
	 * {@link BleDevice#newHistoricalData(byte[], EpochTime)} if we have a device
	 * matching the given mac address.
	 */
	public final HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime, final String macAddress)
	{
		final BleDevice device = getDevice(macAddress);

		if( device.isNull() )
		{
			return newHistoricalData(data, epochTime);
		}
		else
		{
			return device.newHistoricalData(data, epochTime);
		}
	}

	/**
	 * Overload of {@link #getServer(IncomingListener)} without any initial set-up parameters.
	 */
	public final BleServer getServer()
	{
		return getServer((IncomingListener) null);
	}

	/**
	 * Returns a {@link BleServer} instance. which for now at least is a singleton.
	 */
	public final BleServer getServer(final IncomingListener incomingListener)
	{
		m_server = m_server != null ? m_server : new BleServer(this, /*isNull=*/false);

//		bleServer.setConfig(config);
		m_server.setListener_Incoming(incomingListener);

		return m_server;
	}

	/**
	 * Overload of {@link #getServer(GattDatabase, AddServiceListener)}, with no {@link AddServiceListener} set.
	 */
	public final BleServer getServer(final GattDatabase gattDatabase)
	{
		return getServer(gattDatabase, null);
	}

	/**
	 * Overload of {@link #getServer(IncomingListener, GattDatabase, AddServiceListener)}, with no {@link IncomingListener} set.
	 */
	public final BleServer getServer(final GattDatabase gattDatabase, AddServiceListener addServiceListener)
	{
		return getServer(null, gattDatabase, addServiceListener);
	}

	/**
	 * Returns a {@link BleServer} instance. This is now the preferred method to retrieve the server instance.
	 */
	public final BleServer getServer(final IncomingListener incomingListener, final GattDatabase gattDatabase, final AddServiceListener addServiceListener)
	{
		if (m_server == null)
		{
			m_server = new BleServer(this, /*isNull*/false);
			if (gattDatabase != null)
			{
				for (BluetoothGattService service : gattDatabase.getServiceList())
				{
					m_server.addService(service, addServiceListener);
				}
			}
		}
		m_server.setListener_Incoming(incomingListener);
		return m_server;
	}

	/**
	 * Same as {@link #newDevice(String, String, BleDeviceConfig)} but uses an empty string for the name
	 * and passes a <code>null</code> {@link BleDeviceConfig}, which results in inherited options from {@link BleManagerConfig}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(String macAddress)
	{
		return newDevice(macAddress, null, null);
	}

	/**
	 * Same as {@link #newDevice(String)} but allows a custom name also.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final String name)
	{
		return newDevice(macAddress, name, null);
	}

	/**
	 * Same as {@link #newDevice(String)} but passes a {@link BleDeviceConfig} to be used as well.
	 */

	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final BleDeviceConfig config)
	{
		return newDevice(macAddress, null, config);
	}

	/**
	 * Creates a new {@link BleDevice} or returns an existing one if the macAddress matches.
	 * {@link DiscoveryListener#onEvent(Event)} will be called if a new device
	 * is created.
	 * <br><br>
	 * NOTE: You should always do a {@link BleDevice#isNull()} check on this method's return value just in case. Android
	 * documentation says that underlying stack will always return a valid {@link android.bluetooth.BluetoothDevice}
	 * instance (which is required to create a valid {@link BleDevice} instance), but you really never know.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final String name, final BleDeviceConfig config)
	{
		return newDevice(macAddress, name, null, config);
	}


	final @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final String name, final byte[] scanRecord, final BleDeviceConfig config)
	{
		final String macAddress_normalized = normalizeMacAddress(macAddress);

		final BleDevice existingDevice = this.getDevice(macAddress_normalized);

		if( !existingDevice.isNull() )
		{
			if( config != null )
			{
				existingDevice.setConfig(config);
			}

			if( name != null )
			{
				existingDevice.setName(name);
			}

			return existingDevice;
		}

		final P_NativeDeviceLayer device_native = newNativeDevice(macAddress_normalized);

		if( device_native == null && scanRecord == null) //--- DRK > API says this should never happen...not trusting it! Only returning null instance if scanRecord is null
		{
			return BleDevice.NULL;
		}

		final String name_normalized = Utils_String.normalizeDeviceName(name);

		final BleDevice newDevice = newDevice_private(device_native, name_normalized, name != null ? name : "", BleDeviceOrigin.EXPLICIT, config);

		if( name != null )
		{
			newDevice.setName(name);
		}

		onDiscovered_wrapItUp(newDevice, device_native, /*newlyDiscovered=*/true, /*scanRecord=*/scanRecord, 0, BleDeviceOrigin.EXPLICIT, /*scanEvent=*/null);

		return newDevice;
	}

	final P_NativeDeviceLayer newNativeDevice(final String macAddress)
	{
		BluetoothDevice nativeDevice = managerLayer().getRemoteDevice(macAddress);
		P_NativeDeviceLayer layer = m_config.newDeviceLayer(BleDevice.NULL);
		layer.setNativeDevice(nativeDevice);
		return layer;
	}

	/**
	 * Forcefully undiscovers a device, disconnecting it first if needed and removing it from this manager's internal list.
	 * {@link DiscoveryListener#onEvent(Event)} with {@link LifeCycle#UNDISCOVERED} will be called.
	 * No clear use case has been thought of but the method is here just in case anyway.
	 *
	 * @return	<code>true</code> if the device was undiscovered, <code>false</code> if device is already {@link BleDeviceState#UNDISCOVERED} or manager
	 * 			doesn't contain an instance, checked referentially, not through {@link BleDevice#equals(BleDevice)} (i.e. by mac address).
	 */
	public final boolean undiscover(final BleDevice device)
	{
		if( device == null )							return false;
		if( device.isNull() )							return false;
		if( !hasDevice(device) )						return false;
		if( device.is(BleDeviceState.UNDISCOVERED) )	return false;

		if( device.is(BleDeviceState.CONNECTED) )
			device.disconnectAndUndiscover();
		else
			m_deviceMngr.undiscoverAndRemove(device, m_discoveryListener, m_deviceMngr_cache, E_Intent.INTENTIONAL);

		return true;
	}

	/**
	 * This method will clear the task queue of all tasks.
	 * NOTE: This can really mess things up, especially if you're currently trying to connect to a device. Only use this if you absolutely have to!
	 */
	@Advanced
	public final void clearQueue()
	{
		m_taskManager.clearQueueOfAll();
	}

	/**
	 * Convenience forwarding of {@link #clearSharedPreferences(String)}.
	 *
	 * @see #clearSharedPreferences(String)
	 */
	public final void clearSharedPreferences(final BleDevice device)
	{
		clearSharedPreferences(device.getMacAddress());
	}

	/**
	 * Clears all data currently being held in {@link android.content.SharedPreferences} for a particular device.
	 *
	 * @see BleDeviceConfig#manageLastDisconnectOnDisk
	 * @see BleDeviceConfig#tryBondingWhileDisconnected_manageOnDisk
	 * @see BleDeviceConfig#saveNameChangesToDisk
	 * @see #clearSharedPreferences()
	 */
	public final void clearSharedPreferences(final String macAddress)
	{
		final String macAddress_normalized = normalizeMacAddress(macAddress);

		m_diskOptionsMngr.clear(macAddress_normalized);
	}

	/**
	 * Clears all data currently being held in {@link android.content.SharedPreferences} for all devices.
	 *
	 * @see BleDeviceConfig#manageLastDisconnectOnDisk
	 * @see BleDeviceConfig#tryBondingWhileDisconnected_manageOnDisk
	 * @see BleDeviceConfig#saveNameChangesToDisk
	 * @see #clearSharedPreferences(String)
	 */
	public final void clearSharedPreferences()
	{
		m_diskOptionsMngr.clear();
	}

	//--- DRK > Smooshing together a bunch of package-private accessors here.
	final P_BleStateTracker			getStateTracker(){				return m_stateTracker;									}
	final P_NativeBleStateTracker	getNativeStateTracker(){		return m_nativeStateTracker;							}
	final P_BluetoothCrashResolver	getCrashResolver(){				return m_crashResolver;									}
	final P_TaskManager				getTaskManager(){				return m_taskManager;									}
	final P_Logger					getLogger(){					return m_logger;										}
	final long 						timeTurnedOn(){					return m_timeTurnedOn;									}
	final double 					timeForegrounded(){				return m_timeForegrounded;								}
	final boolean 					isBluetoothEnabled(){			return managerLayer().isBluetoothEnabled();				}
	final P_ScanManager 			getScanManager(){				return m_scanManager;									}
	final long 						getUpdateRate(){				return m_updateRunnable.getUpdateRate();				}
	final P_PostManager 			getPostManager(){				return m_postManager;									}


	private void turnOff_private(final boolean removeAllBonds)
	{
		if( isAny(TURNING_OFF, OFF) )  return;

		if( is(ON) )
		{
			m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_OFF, true, ON, false);
		}

		m_deviceMngr.disconnectAllForTurnOff(PE_TaskPriority.CRITICAL);

		if( removeAllBonds )
		{
			m_deviceMngr.unbondAll(PE_TaskPriority.CRITICAL, BondListener.Status.CANCELLED_FROM_BLE_TURNING_OFF);
		}

		if( m_server != null )
		{
			m_server.disconnect_internal(AddServiceListener.Status.CANCELLED_FROM_BLE_TURNING_OFF, ServerReconnectFilter.Status.CANCELLED_FROM_BLE_TURNING_OFF, State.ChangeIntent.INTENTIONAL);
		}

		final P_Task_TurnBleOff task = new P_Task_TurnBleOff(this, /*implicit=*/false, new PA_Task.I_StateListener()
		{
			@Override public void onStateChange(PA_Task taskClass, PE_TaskState state)
			{
				if( state == PE_TaskState.EXECUTING )
				{
					if( is(RESETTING) )
					{
						m_nativeStateTracker.append(RESETTING, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
					}

					m_deviceMngr.undiscoverAllForTurnOff(m_deviceMngr_cache, E_Intent.INTENTIONAL);
				}
			}
		});

		m_taskManager.add(task);
	}

	String getDeviceName(P_NativeDeviceLayer device, byte[] scanRecord) throws Exception
	{
		final String nameFromDevice;
		final String nameFromRecord;
		nameFromDevice = device.getName();
		nameFromRecord = Utils_ScanRecord.parseName(scanRecord);
		if (isDeviceThatReturnsShortName())
		{
			if (!TextUtils.isEmpty(nameFromRecord))
			{
				return nameFromRecord;
			}
			else
			{
				m_logger.w("Unable to get complete name from scan record! Defaulting to the short name given from BluetoothDevice.");
			}
		}
		return TextUtils.isEmpty(nameFromDevice) ? nameFromRecord : nameFromDevice;
	}

	private boolean isDeviceThatReturnsShortName()
	{
		//--- > RB  Right now, this is the only device we're aware of that returns the short name from BluetoothDevice.getName(). This may grow in the future.
		if (Build.MANUFACTURER.equalsIgnoreCase("amobile") && Build.PRODUCT.equalsIgnoreCase("full_amobile2601_wp_l") && Build.MODEL.equalsIgnoreCase("iot-500"))
		{
			return true;
		}
		return false;
	}

	final synchronized void onDiscoveredFromNativeStack(List<DiscoveryEntry> entries)
	{
		//--- DRK > Protects against fringe case where scan task is executing and app calls turnOff().
		//---		Here the scan task will be interrupted but still potentially has enough time to
		//---		discover another device or two. We're checking the enum state as opposed to the native
		//---		integer state because in this case the "turn off ble" task hasn't started yet and thus
		//---		hasn't called down into native code and thus the native state hasn't changed.
		if( false == is(ON) )  return;

		//--- DRK > Not sure if queued up messages to library's thread can sneak in a device discovery event
		//---		after user called stopScan(), so just a check to prevent unexpected callbacks to the user.
		if( false == is(SCANNING) )  return;

		final List<DiscoveryEntry> list = new ArrayList<>();

		for (DiscoveryEntry entry : entries)
		{

			final String macAddress = entry.device().getAddress();
			BleDevice device_sweetblue = m_deviceMngr.get(macAddress);

			if (device_sweetblue != null)
			{
				if (!device_sweetblue.layerManager().getDeviceLayer().equals(entry.device()))
				{
					ASSERT(false, "Discovered device " + entry.device().getName() + " " + macAddress + " already in list but with new native device instance.");
				}
			}

			final Please please;
			final boolean newlyDiscovered;
			final ScanFilter.ScanEvent scanEvent_nullable;

			if (device_sweetblue == null)
			{
				final String rawDeviceName;

				try
				{
					rawDeviceName = getDeviceName(entry.device(), entry.record());
				}

				//--- DRK > Can occasionally catch a DeadObjectException or NullPointerException here...nothing we can do about it.
				catch (Exception e)
				{
					m_logger.e(e.getStackTrace().toString());

					//--- DRK > Can't actually catch the DeadObjectException itself.
					if (e instanceof DeadObjectException)
					{
						uhOh(UhOhListener.UhOh.DEAD_OBJECT_EXCEPTION);
					}
					else
					{
						uhOh(UhOhListener.UhOh.RANDOM_EXCEPTION);
					}

					continue;
				}

				final String normalizedDeviceName = Utils_String.normalizeDeviceName(rawDeviceName);

				final boolean hitDisk = BleDeviceConfig.boolOrDefault(m_config.manageLastDisconnectOnDisk);
				final State.ChangeIntent lastDisconnectIntent = m_diskOptionsMngr.loadLastDisconnect(macAddress, hitDisk);
				scanEvent_nullable = m_filterMngr.makeEvent() ? ScanFilter.ScanEvent.fromScanRecord(entry.device().getNativeDevice(), rawDeviceName, normalizedDeviceName, entry.rssi(), lastDisconnectIntent, entry.record()) : null;

				please = m_filterMngr.allow(m_logger, scanEvent_nullable);

				if (please != null && false == please.ack()) continue;

				final String name_native = rawDeviceName;

				final BleDeviceConfig config_nullable = please != null ? please.getConfig() : null;
				device_sweetblue = newDevice_private(entry.device(), normalizedDeviceName, name_native, BleDeviceOrigin.FROM_DISCOVERY, config_nullable);
				newlyDiscovered = true;
			}
			else
			{
				scanEvent_nullable = null;
				newlyDiscovered = false;
			}

			entry.m_newlyDiscovered = newlyDiscovered;
			entry.m_bleDevice = device_sweetblue;
			entry.m_origin = BleDeviceOrigin.FROM_DISCOVERY;
			entry.m_scanEvent = scanEvent_nullable;
			list.add(entry);
		}

		onDiscovered_wrapItUp(list);
	}

	private BleDevice newDevice_private(final P_NativeDeviceLayer device_native, final String name_normalized, final String name_native, final BleDeviceOrigin origin, final BleDeviceConfig config_nullable)
	{
		// TODO: for now always true...should these be behind a config option?
		final boolean hitCache = true;

		final BleDevice device_cached;

		if( hitCache )
		{
			device_cached = m_deviceMngr_cache.get(device_native.getAddress());

			if( device_cached != null )
			{
				m_deviceMngr_cache.remove(device_cached, null);
				device_cached.setConfig(config_nullable);
			}
		}
		else
		{
			device_cached = null;
		}

		final BleDevice device = device_cached != null ? device_cached : new BleDevice(BleManager.this, device_native, name_normalized, name_native, origin, config_nullable, /*isNull=*/false);

		m_deviceMngr.add(device);

		return device;
	}

	final void onDiscovered_fromRogueAutoConnect(final BleDevice device, final boolean newlyDiscovered, final List<UUID> services_nullable, final byte[] scanRecord_nullable, final int rssi)
	{
		if( !m_deviceMngr.has(device) ) // DRK > as of now checked upstream also, so just being anal
		{
			m_deviceMngr.add(device);
		}

		onDiscovered_wrapItUp(device, device.layerManager().getDeviceLayer(), newlyDiscovered, scanRecord_nullable, rssi, BleDeviceOrigin.FROM_DISCOVERY, /*scanEvent=*/null);
	}

	final void postEvent(final GenericListener_Void listener, final Event event)
	{
		if (listener != null)
		{
			if (listener instanceof PA_CallbackWrapper)
			{
				m_postManager.runOrPostToUpdateThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (listener != null)
						{
							listener.onEvent(event);
						}
					}
				});
			}
			else
			{
				m_postManager.postCallback(new Runnable()
				{
					@Override
					public void run()
					{
						if (listener != null)
						{
							listener.onEvent(event);
						}
					}
				});
			}
		}
	}

	final <T extends Event> void postEvents(final GenericListener_Void listener, final List<T> events)
	{
		if (listener != null)
		{
			if (listener instanceof PA_CallbackWrapper)
			{
				m_postManager.runOrPostToUpdateThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (listener != null)
						{
							for (Event e : events)
							{
								listener.onEvent(e);
							}
						}
					}
				});
			}
			else
			{
				m_postManager.postCallback(new Runnable()
				{
					@Override
					public void run()
					{
						if (listener != null)
						{
							for (Event e : events)
							{
								listener.onEvent(e);
							}
						}
					}
				});
			}
		}
	}

    private void onDiscovered_wrapItUp(final BleDevice device, final P_NativeDeviceLayer device_native, final boolean newlyDiscovered, final byte[] scanRecord_nullable, final int rssi, final BleDeviceOrigin origin, ScanFilter.ScanEvent scanEvent_nullable)
    {
    	if( newlyDiscovered )
    	{
    		device.onNewlyDiscovered(device_native, scanEvent_nullable, rssi, scanRecord_nullable, origin);

    		if( m_discoveryListener != null )
    		{
				final DiscoveryEvent event = new DiscoveryEvent(device, LifeCycle.DISCOVERED);
				postEvent(m_discoveryListener, event);
    		}
    	}
    	else
    	{
    		device.onRediscovered(device_native, scanEvent_nullable, rssi, scanRecord_nullable, BleDeviceOrigin.FROM_DISCOVERY);

    		if( m_discoveryListener != null )
    		{
				final DiscoveryEvent event = new DiscoveryEvent(device, LifeCycle.REDISCOVERED);
				postEvent(m_discoveryListener, event);
    		}
    	}
    }

	private void onDiscovered_wrapItUp(List<DiscoveryEntry> entries)
	{
		final List<DiscoveryEvent> events = new ArrayList<>(entries.size());

		for (DiscoveryEntry e : entries)
		{
			if (e.m_newlyDiscovered)
			{
				m_logger.i("BleManager", e.device().getAddress(), Utils_String.makeString("Discovered new BleDevice ", e.device().getName()));
				e.m_bleDevice.onNewlyDiscovered(e.device(), e.m_scanEvent, e.rssi(), e.record(), e.m_origin);
				final DiscoveryEvent event = DiscoveryEvent.newEvent(e.m_bleDevice, LifeCycle.DISCOVERED);
				events.add(event);
			}
			else
			{
				m_logger.d("BleManager", e.device().getAddress(), Utils_String.makeString("Re-discovered BleDevice ", e.device().getName()));
				e.m_bleDevice.onRediscovered(e.device(), e.m_scanEvent, e.rssi(), e.record(), e.m_origin);
				final DiscoveryEvent event = DiscoveryEvent.newEvent(e.m_bleDevice, LifeCycle.REDISCOVERED);
				events.add(event);
			}
		}
		if (m_discoveryListener != null)
		{
			postEvents(m_discoveryListener, events);
		}
	}

	final void clearScanningRelatedMembers(final E_Intent intent)
	{
//		m_filterMngr.clear();

		m_scanManager.resetTimeNotScanning();

		m_stateTracker.remove(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	final void tryPurgingStaleDevices(final double scanTime)
	{
		m_deviceMngr.purgeStaleDevices(scanTime, m_deviceMngr_cache, m_discoveryListener);
	}

	final boolean ready() {
		if (!m_ready)
		{
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			{
				m_ready = is(ON);
			}
			else
			{
				m_ready = is(ON) && isLocationEnabledForScanning_byRuntimePermissions() && isLocationEnabledForScanning_byOsServices();
			}
		}
		if (m_ready && !is(BLE_SCAN_READY))
		{
			setBleScanReady();
		}
		return m_ready;
	}

	final void checkIdleStatus()
	{
		if (is(IDLE))
		{
			// To ensure we get back up to speed as soon as possible, we'll remove the callbacks, set the new update rate
			// then post the runnable again. This avoids waiting for the idle time before the speed bumps back up.
			getLogger().i("Update loop is no longer in the IDLE state.");
			getPostManager().removeUpdateCallbacks(m_updateRunnable);
			m_updateRunnable.setUpdateRate(m_config.autoUpdateRate.millis());
			m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, IDLE, false);
			getPostManager().postToUpdateThread(m_updateRunnable);
		}
	}

	/**
	 * This method is made public in case you want to tie the library in to an update loop
	 * from another codebase. Generally you should leave {@link BleManagerConfig#autoUpdateRate}
	 * alone and let the library handle the calling of this method.
	 */
	@Advanced
	public final void update(final double timeStep_seconds, final long currentTime)
	{
		m_currentTick = currentTime;

		m_listeners.update(timeStep_seconds);

		m_uhOhThrottler.update(timeStep_seconds);

		if (m_taskManager.update(timeStep_seconds, currentTime))
		{
			m_lastTaskExecution = currentTime;
			checkIdleStatus();
		}

		if( m_isForegrounded )
		{
			m_timeForegrounded += timeStep_seconds;
		}
		else
		{
			m_timeForegrounded = 0.0;
		}

		m_deviceMngr.update(timeStep_seconds);

		if ( m_timeTurnedOn == 0 && is(ON) )
		{
			m_timeTurnedOn = currentTime;
		}

		if( m_scanManager.update(timeStep_seconds, currentTime) == false )
		{
			if (Interval.isEnabled(m_config.minTimeToIdle))
			{
				if (!is(IDLE) && m_lastTaskExecution + m_config.minTimeToIdle.millis() < currentTime)
				{
					m_updateRunnable.setUpdateRate(m_config.idleUpdateRate.millis());
					m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, IDLE, true);
					getLogger().i("Update loop has entered IDLE state.");
				}
			}
		}

		if( m_config.updateLoopCallback != null )
		{
			m_config.updateLoopCallback.onUpdate(timeStep_seconds);
		}

		if (!is(IDLE) && m_config.autoUpdateRate.millis() < (System.currentTimeMillis() - m_currentTick) && (m_lastUpdateLoopWarning + UPDATE_LOOP_WARNING_DELAY <= m_currentTick))
		{
            m_lastUpdateLoopWarning = m_currentTick;
			getLogger().w("BleManager", String.format("Update loop took longer to run than the current interval of %dms", m_config.autoUpdateRate.millis()));
		}
	}

	/**
	 * Returns this manager's knowledge of the app's foreground state.
	 */
	public final boolean isForegrounded()
	{
		return m_isForegrounded;
	}

	final long currentTime()
	{
		return m_currentTick;
	}

	final boolean doAutoScan()
	{
		return is(ON) && Interval.isEnabled(m_config.autoScanActiveTime) && (m_config.autoScanDuringOta || !m_deviceMngr.hasDevice(BleDeviceState.PERFORMING_OTA));
	}

	final void setBleScanReady()
	{
		m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BLE_SCAN_READY, true);
	}

	final void uhOh(UhOhListener.UhOh reason)
	{
//		if( reason == UhOh.UNKNOWN_CONNECTION_ERROR )
//		{
//			m_connectionFailTracker = 0;
//		}

		m_uhOhThrottler.uhOh(reason);
	}

	@Override public final String toString()
	{
		return m_stateTracker.toString();
	}

	final String normalizeMacAddress(final String macAddress)
	{
		final String macAddress_normalized = Utils_String.normalizeMacAddress(macAddress);

		if( macAddress == macAddress_normalized )
		{
			return macAddress;
		}
		else if( macAddress.equals(macAddress_normalized) )
		{
			return macAddress;
		}
		else
		{
			getLogger().w("Given mac address " + macAddress + " has been auto-normalized to " + macAddress_normalized);

			return macAddress_normalized;
		}
	}

	private Application.ActivityLifecycleCallbacks newLifecycleCallbacks()
	{
		return new Application.ActivityLifecycleCallbacks()
		{
			@Override public void onActivityCreated(Activity activity, Bundle savedInstanceState){}
			@Override public void onActivityStarted(Activity activity){}
			@Override public void onActivityStopped(Activity activity){}
			@Override public void onActivitySaveInstanceState(Activity activity, Bundle outState){}
			@Override public void onActivityDestroyed(Activity activity){}

			@Override public void onActivityPaused(Activity activity)
			{
				if( m_config.autoPauseResumeDetection == true )
				{
					BleManager.this.onPause();
				}
			}

			@Override public void onActivityResumed(Activity activity)
			{
				if( m_config.autoPauseResumeDetection == true )
				{
					BleManager.this.onResume();
				}
			}
		};
	}

	private void addLifecycleCallbacks()
	{
		if( getApplicationContext() instanceof Application )
		{
			final Application application = (Application) getApplicationContext();
			final Application.ActivityLifecycleCallbacks callbacks = newLifecycleCallbacks();

			application.registerActivityLifecycleCallbacks(callbacks);
		}
		else
		{
			//--- DRK > Not sure if this is practically possible but nothing we can do here I suppose.
		}
	}


	private final class UpdateRunnable implements Runnable
	{

		private Long m_lastAutoUpdateTime;
		private long m_autoUpdateRate = -1;
		private boolean m_shutdown = false;


		public UpdateRunnable(long updateRate)
		{
			m_autoUpdateRate = updateRate;
		}

		public UpdateRunnable()
		{
		}

		public void setUpdateRate(long rate)
		{
			m_autoUpdateRate = rate;
		}

		public long getUpdateRate()
		{
			return m_autoUpdateRate;
		}

		@Override public void run()
		{
			long currentTime = System.currentTimeMillis();
			if (m_lastAutoUpdateTime == null)
			{
				m_lastAutoUpdateTime = currentTime;
			}
			double timeStep = ((double) currentTime - m_lastAutoUpdateTime)/1000.0;

			timeStep = timeStep <= 0.0 ? .00001 : timeStep;
			//--- RB > Not sure why this was put here. If the tick is over a second, we still want to know that, otherwise tasks will end up running longer
			// 			than expected.
//			timeStep = timeStep > 1.0 ? 1.0 : timeStep;


			update(timeStep, currentTime);

			m_lastAutoUpdateTime = currentTime;

			if (!m_shutdown)
			{
				m_postManager.postToUpdateThreadDelayed(this, m_autoUpdateRate);
			}
		}
	}
}
