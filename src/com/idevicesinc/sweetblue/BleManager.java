package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleState.*;

import java.util.List;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleManagerConfig.AdvertisingFilter;
import com.idevicesinc.sweetblue.P_Task_Scan.E_Mode;
import com.idevicesinc.sweetblue.utils.BleDeviceIterator;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UpdateLoop;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * The entry point to the library. Typical pattern is to create one instance of this class per application
 * and keep it around for the duration of your application, across multiple {@link Activity} instances. Make sure
 * to hook up this manager to lifecycle events for your app as a whole: {@link #onPause()}, {@link #onResume()}, and {@link #onDestroy()}.
 * <br><br>
 * Also put the following entries (or something similar) in the root of your AndroidManifest.xml:
 * <br><br>
 * {@code <uses-sdk android:minSdkVersion="18" android:targetSdkVersion="19" />}<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH" /> }<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" /> }<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED" /> }<br>
 * {@code <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" /> }<br>
 * <br><br>
 * If you want {@link BleManagerConfig#manageCpuWakeLock} to aid with reconnect loops you will also need:
 * <br><br>
 * {@code <uses-permission android:name="android.permission.WAKE_LOCK" /> } <br><br><br>
 * 
 * Then here is a simple example usage:<pre><code>
 * public class MyActivity extends Activity
 * {
 *      private BleManager m_bleManager;
 *     
 *      {@literal @}Override protected void onCreate(Bundle savedInstanceState)
 *      {
 *          super.onCreate(savedInstanceState);
 *         
 *          m_bleManager = new BleManager(getApplication());
 *         
 *          m_bleManager.startScan(new BleManager.DiscoveryListener()
 *          {
 *              {@literal @}Override public void onDeviceDiscovered(BleDevice device)
 *              {
 *                  m_bleManager.stopScan();
 *                 
 *                  device.connect(new BleDevice.StateListener()
 *                  {
 *                      {@literal @}Override public void onStateChange(BleDevice device, int oldStateBits, int newStateBits)
 *                      {
 *                          if( BleDeviceState.INITIALIZED.wasEntered(oldStateBits, newStateBits) )
 *                          {
 *                              String toastText = device.getDebugName() + " just initialized!";
 *                              Toast.makeText(MyActivity.this, toastText, Toast.LENGTH_LONG).show();
 *                          }
 *                      }
 *                  });
 *              }
 *          });
 *       }
 *     
 *      {@literal @}Override protected void onResume()
 *      {
 *          super.onResume();
 *         
 *          m_bleManager.onResume();
 *      }
 *     
 *      {@literal @}Override protected void onPause()
 *      {
 *          super.onPause();
 *         
 *          m_bleManager.onPause();
 *      }
 * }
 * </code>
 * </pre>
 * 
 * @author dougkoellmer
 */
public class BleManager
{
	/**
	 * Provide an implementation to {@link BleManager#setListener_Discovery(DiscoveryListener)} to receive
	 * callbacks when a device is discovered after calling various {@link BleManager#startScan()}
	 * or {@link BleManager#startPeriodicScan(Interval, Interval)} methods. You can also provide this to various
	 * overloads of {@link BleManager#startScan()} and {@link BleManager#startPeriodicScan(Interval, Interval)}.
	 * <br><br>
	 * Use {@link DiscoveryListener_Full} for additional discovery lifecycle events.
	 * 
	 * @author dougkoellmer
	 */
	public static interface DiscoveryListener
	{
		/**
		 * Called when a device is discovered for the first time after
		 * calling {@link BleManager#startScan()} (or its overloads)
		 * or {@link BleManager#startPeriodicScan(Interval, Interval)}.
		 */
		void onDeviceDiscovered(BleDevice device);
	}
	
	/**
	 * Use this listener instead of {@link DiscoveryListener} to get more info on the discovery lifecycle.
	 * 
	 * @author dougkoellmer
	 */
	public static interface DiscoveryListener_Full extends DiscoveryListener
	{
		/**
		 * Called when a device is rediscovered after already being discovered at least once.
		 * 
		 * @see DiscoveryListener#onDeviceDiscovered(BleDevice)
		 */
		void onDeviceRediscovered(BleDevice device);
		
		/**
		 * Called when a device is "undiscovered" after being discovered at least once. There is no native equivalent
		 * for this callback. Undiscovery is approximated with a timeout based on the last time we discovered a device.
		 * Consequently you should expect that this callback will take some amount of time to receive after an
		 * advertising device is turned off or goes out of range or what have you. It's generally not as fast as other
		 * state changes like {@link BleDeviceState#DISCONNECTED} or getting {@link BleDeviceState#DISCOVERED} in the first place.
		 *  
		 * @see BleManagerConfig#minScanTimeToInvokeUndiscovery
		 * @see BleManagerConfig#scanKeepAlive
		 */
		void onDeviceUndiscovered(BleDevice device);
	}
	
	/**
	 * Provide an implementation to {@link BleManager#setListener_State(StateListener)} to receive callbacks
	 * when the {@link BleManager} undergoes a {@link BleState} change.
	 * 
	 * @author dougkoellmer
	 */
	public static interface StateListener
	{
		/**
		 * Called when the manager's bitwise {@link BleState} changes. As many bits as possible are flipped at the same time.
		 *  
		 * @param oldStateBits The previous bitwise representation of {@link BleState}.
		 * @param newStateBits The new and now current bitwise representation of {@link BleState}. Will be the same as {@link BleManager#getStateMask()}.
		 */
		void onBleStateChange(BleManager manager, int oldStateBits, int newStateBits);
	}
	
	/**
	 * Provide an implementation to {@link BleManager#setListener_NativeState(NativeStateListener)} to receive callbacks
	 * when the {@link BleManager} undergoes a *native* {@link BleState} change. This is similar to {@link StateListener}
	 * but reflects what is going on in the actual underlying stack, which may lag slightly behind the
	 * abstracted state reflected by {@link StateListener}. Most apps will not find this callback useful.
	 * 
	 * @author dougkoellmer
	 */
	public static interface NativeStateListener
	{
		/**
		 * Called when the manager's native bitwise {@link BleState} changes. As many bits as possible are flipped at the same time.
		 *  
		 * @param oldStateBits The previous bitwise representation of {@link BleState}.
		 * @param newStateBits The new and now current bitwise representation of {@link BleState}. Will be the same as {@link BleManager#getNativeStateMask()}.
		 */
		void onNativeBleStateChange(BleManager manager, int oldStateBits, int newStateBits);
	}
	
	/**
	 * Provide an implementation to {@link BleManager#setListener_UhOh(UhOhListener)}
	 * to receive a callback when an {@link UhOh} occurs.
	 * 
	 * @see UhOh
	 * 
	 * @author dougkoellmer
	 */
	public static interface UhOhListener
	{
		/**
		 * Run for the hills.
		 */
		void onUhOh(BleManager manager, UhOh reason);
	}
	
	/**
	 * Provide an implementation to {@link BleManager#dropTacticalNuke(NukeEndListener)}
	 * to be notified when a nuke operation is complete.
	 * 
	 * @see BleManager#dropTacticalNuke(NukeEndListener)
	 * 
	 * @author dougkoellmer
	 */
	public static interface NukeEndListener
	{
		/**
		 * The nuke completed. Hopefully the bluetooth stack is OK now.
		 */
		void onNukeEnded(BleManager manager);
	}
	
	/**
	 * Mostly only for SweetBlue library developers. Provide an implementation to
	 * {@link BleManager#setListener_Assert(AssertListener)} to be notified whenever
	 * an assertion fails through {@link BleManager#ASSERT(boolean, String)}.
	 */
	public static interface AssertListener
	{
		/**
		 * Provides the message (or empty string) along with the stack trace if an assertion fails.
		 */
		void onAssertFailed(BleManager manager, String message, StackTraceElement[] stackTrace);
	}
	
	private final UpdateLoop.Callback m_updateLoopCallback = new UpdateLoop.Callback()
	{
		@Override public void onUpdate(double timestep)
		{
			update(timestep);
		}
	};
	
	private final Context m_context;
			final Handler m_mainThreadHandler;
	private final BluetoothManager m_btMngr;
	private final P_AdvertisingFilterManager m_filterMngr;
	private final P_BluetoothCrashResolver m_crashResolver;
	private final P_Logger m_logger;
			final BleManagerConfig m_config;
			final P_DeviceManager m_deviceMngr;
	private final P_BleManager_Listeners m_listeners;
	private final P_StateTracker m_stateTracker;
	private final P_NativeStateTracker m_nativeStateTracker;
	private final UpdateLoop m_updateLoop;
	private final P_TaskQueue m_taskQueue;
	private final P_UhOhThrottler m_uhOhThrottler;
			final P_WakeLockManager m_wakeLockMngr;
	
	private int m_connectionFailTracker = 0;
	
			final Object m_threadLock = new Object();
	
			DiscoveryListener m_discoveryListener;
	private P_WrappingNukeListener m_nukeListeners;
	private AssertListener m_assertionListener;
			BleDevice.StateListener m_defaultDeviceStateListener;
			BleDevice.ConnectionFailListener m_defaultConnectionFailListener;

	private double m_timeForegrounded = 0.0;
	private double m_timeNotScanning = 0.0;
	private boolean m_doingInfiniteScan = false;
	
	private boolean m_isForegrounded = false;
	private boolean m_triedToStartScanAfterResume = false;
	
	private static int s_instanceCount = 0;
	
	/**
	 * Create an instance with default configuration options set.
	 * This calls {@link #BleManager(Application, BleManagerConfig)} with a {@link BleManagerConfig}
	 * created using the default constructor of {@link BleManagerConfig#BleManagerConfig()}.
	 * 
	 * @throws InstantiationError if you try to create more than one instance.
	 */
	public BleManager(Application application)
	{
		this(application, new BleManagerConfig());
	}
	
	/**
	 * Create an instance with special configuration options set.
	 * 
	 * @throws InstantiationError if you try to create more than one instance.
	 */
	public BleManager(Application application, BleManagerConfig config)
	{
		if( s_instanceCount >= 1 )  throw new InstantiationError("There can only be one instance of "+BleManager.class.getSimpleName() + " created per application.");
		
		m_context = application;
		m_config = config.clone();
		m_filterMngr = new P_AdvertisingFilterManager(m_config.defaultAdvertisingFilter);
		m_logger = new P_Logger(m_config.debugThreadNames, m_config.uuidNameMaps, m_config.loggingEnabled);
		m_uhOhThrottler = new P_UhOhThrottler(this, Interval.asDouble(m_config.uhOhCallbackThrottle));
		m_btMngr = (BluetoothManager) application.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
		BleState nativeState = BleState.get(m_btMngr.getAdapter().getState());
		m_stateTracker = new P_StateTracker(this);
		m_stateTracker.append(nativeState);
		m_nativeStateTracker = new P_NativeStateTracker(this);
		m_nativeStateTracker.append(nativeState);
		m_mainThreadHandler = new Handler(m_context.getMainLooper());
		m_taskQueue = new P_TaskQueue(this);
		m_crashResolver = new P_BluetoothCrashResolver(application);
		m_deviceMngr = new P_DeviceManager(this);
		m_listeners = new P_BleManager_Listeners(this);
		m_wakeLockMngr = new P_WakeLockManager(this, m_config.manageCpuWakeLock);
		
		if( m_config.defaultDiscoveryListener != null )
		{
			this.setListener_Discovery(m_config.defaultDiscoveryListener);
		}
		
		if( Interval.isEnabled(m_config.autoUpdateRate) )
		{
			if( m_config.runOnMainThread )
			{
				m_updateLoop = UpdateLoop.newMainThreadLoop(m_updateLoopCallback);
			}
			else
			{
				m_updateLoop = UpdateLoop.newAnonThreadLoop(m_updateLoopCallback);
			}
			
			startAutoUpdate(Interval.asDouble(m_config.autoUpdateRate));
		}
		else
		{
			m_updateLoop = null;
		}
		
		s_instanceCount++;
	}
	
	/**
	 * Returns whether the manager is in any of the provided states.
	 */
	public boolean isAny(BleState ... states)
	{
		for( int i = 0; i < states.length; i++ )
		{
			if( is(states[i]) )  return true;
		}
		
		return false;
	}
	
	/**
	 * Returns whether the manager is in the provided state.
	 * 
	 * @see #isAny(BleState...)
	 */
	public boolean is(BleState state)
	{
		return state.overlaps(getStateMask());
	}
	
	/**
	 * Checks the underlying stack to see if BLE is supported on the phone.
	 * Note that this call can lie on at least one Sony Xperia model, returning 
	 * true when it should return false. Perhaps other models as well.
	 */
	public boolean isBleSupported()
	{
		PackageManager pm = m_context.getPackageManager();
		boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		
		return hasBLE;
	}
	
	/**
	 * Disables BLE if manager is {@link BleState#ON}. This disconnects all current
	 * connections, stops scanning, and forgets all discovered devices.
	 */
	public void disableBle()
	{
		disableBle(false);
	}
	
	/**
	 * Returns the native manager.
	 */
	public BluetoothManager getNative()
	{
		return m_btMngr;
	}
	
	/**
	 * Set a listener here to be notified whenever we encounter an {@link UhOh}.
	 */
	public void setListener_UhOh(UhOhListener listener)
	{
		m_uhOhThrottler.setListener(listener);
	}
	
	/**
	 * Set a listener here to be notified whenever {@link #ASSERT(boolean)} fails.
	 * Mostly for use by internal library developers.
	 */
	public void setListener_Assert(AssertListener listener)
	{
		if( listener != null )
		{
			m_assertionListener = new P_WrappingAssertionListener(listener, m_mainThreadHandler, m_config.postCallbacksToMainThread);
		}
		else
		{
			m_assertionListener = null;
		}
	}

	/**
	 * Set a listener here to be notified whenever a {@link BleDevice} is discovered, rediscovered, or undiscovered.
	 */
	public void setListener_Discovery(DiscoveryListener listener)
	{
		if( listener != null )
		{
			m_discoveryListener = new P_WrappingDiscoveryListener(listener, m_mainThreadHandler, m_config.postCallbacksToMainThread);
		}
		else
		{
			m_discoveryListener = null;
		}
	}
	
	/**
	 * Returns the discovery listener set with {@link #setListener_Discovery(DiscoveryListener)} or
	 * {@link BleManagerConfig#defaultDiscoveryListener}, or <code>null</code> if not set.
	 */
	public DiscoveryListener getListener_Discovery()
	{
		if( m_discoveryListener != null )
		{
			return ((P_WrappingDiscoveryListener)m_discoveryListener).m_listener;
		}
		
		return null;
	}
	
	/**
	 * Set a listener here to be notified whenever this manager's {@link BleState} changes.
	 */
	public void setListener_State(StateListener listener)
	{
		m_stateTracker.setListener(listener);
	}
	
	/**
	 * Convenience method to listen for all changes in {@link BleDeviceState} for all devices.
	 * The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleDevice#setListener_State(BleDevice.StateListener)}.
	 * 
	 * @see BleDevice#setListener_State(BleDevice.StateListener)
	 */
	public void setListener_DeviceState(BleDevice.StateListener listener)
	{
		m_defaultDeviceStateListener = new P_WrappingDeviceStateListener(listener, m_mainThreadHandler, m_config.postCallbacksToMainThread);
	}
	
	/**
	 * Convenience method to handle connection fail events at the manager level. The listener provided
	 * will only get called if the device whose connection failed doesn't have a listener provided to
	 * {@link BleDevice#setListener_ConnectionFail(ConnectionFailListener)}. This is unlike the behavior
	 * behind {@link #setListener_DeviceState(BleDevice.StateListener)} because {@link BleDevice.ConnectionFailListener}
	 * requires a return value.
	 * 
	 * @see BleDevice#setListener_ConnectionFail(BleDevice.ConnectionFailListener)
	 */
	public void setListener_ConnectionFail(BleDevice.ConnectionFailListener listener)
	{
		m_defaultConnectionFailListener = new P_WrappingDeviceStateListener(listener, m_mainThreadHandler, m_config.postCallbacksToMainThread);
	}
	
	/**
	 * Set a listener here to be notified whenever this manager's native {@link BleState} changes.
	 */
	public void setListener_NativeState(NativeStateListener listener)
	{
		m_nativeStateTracker.setListener(listener);
	}
	
	/**
	 * Manually starts a periodic scan. This is the post-constructor runtime equivalent to setting
	 * {@link BleManagerConfig#autoScanTime} and {@link BleManagerConfig#autoScanInterval}, so see
	 * their comments for more detail. Calling this forever-after overrides the options you set 
	 * in {@link BleManagerConfig}.
	 * 
	 * @see BleManagerConfig#autoScanTime
	 * @see BleManagerConfig#autoScanInterval
	 */
	public void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, (AdvertisingFilter)null, (DiscoveryListener)null);
	}
	
	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(DiscoveryListener)} for you too.
	 */
	public void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, DiscoveryListener discoveryListener)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, (AdvertisingFilter)null, discoveryListener);
	}
	
	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but adds a filter too.
	 */
	public void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, BleManagerConfig.AdvertisingFilter filter)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, filter, (DiscoveryListener)null);
	}
	
	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(DiscoveryListener)} for you too and adds a filter.
	 */
	public void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, BleManagerConfig.AdvertisingFilter filter, DiscoveryListener discoveryListener)
	{
		if( discoveryListener != null )
		{
			setListener_Discovery(discoveryListener);
		}
		
		m_filterMngr.add(filter);
		
		m_config.autoScanTime = scanActiveTime;
		m_config.autoScanInterval = scanPauseTime;
		
		if( Interval.isEnabled(m_config.autoScanTime) )
		{
			if( doAutoScan() )
			{
				startScan(m_config.autoScanTime);
			}
		}
	}
	
	/**
	 * Stops a periodic scan previously started either explicitly with {@link #startPeriodicScan(Interval, Interval)} or through
	 * the {@link BleManagerConfig#autoScanTime} and {@link BleManagerConfig#autoScanInterval} config options.
	 */
	public void stopPeriodicScan()
	{
		m_config.autoScanTime = Interval.DISABLED;
		
		if( !m_doingInfiniteScan )
		{
			this.stopScan();
		}
	}

	/**
	 * Starts a scan that will continue indefinitely until {@link #stopScan()} is called.
	 */
	public void startScan()
	{
		startScan(Interval.INFINITE);
	}
	
	/**
	 * Calls {@link #startScan(Interval, BleManagerConfig.AdvertisingFilter)} with {@link Interval#INFINITE}.
	 */
	public void startScan(AdvertisingFilter filter)
	{
		startScan(Interval.INFINITE, filter, (DiscoveryListener)null);
	}
	
	/**
	 * Same as {@link #startScan()} but also calls {@link #setListener_Discovery(DiscoveryListener)} for you.
	 */
	public void startScan(DiscoveryListener discoveryListener)
	{
		startScan(Interval.INFINITE, (AdvertisingFilter)null, discoveryListener);
	}
	
	/**
	 * Overload of {@link #startScan(Interval, BleManagerConfig.AdvertisingFilter, DiscoveryListener)}
	 */
	public void startScan(Interval scanTime, AdvertisingFilter filter)
	{
		startScan(scanTime, filter, (DiscoveryListener)null);
	}
	
	/**
	 * Overload of {@link #startScan(Interval, BleManagerConfig.AdvertisingFilter, DiscoveryListener)}
	 */
	public void startScan(Interval scanTime, DiscoveryListener discoveryListener)
	{
		startScan(scanTime, (AdvertisingFilter)null, discoveryListener);
	}
	
	/**
	 * Same as {@link #startScan()} but also calls {@link #setListener_Discovery(DiscoveryListener)} for you.
	 */
	public void startScan(AdvertisingFilter filter, DiscoveryListener discoveryListener)
	{
		startScan(Interval.INFINITE, filter, discoveryListener);
	}
	
	/**
	 * Starts a scan that will generally last for the given time (roughly).
	 */
	public void startScan(Interval scanTime)
	{
		startScan(scanTime, (AdvertisingFilter)null, (DiscoveryListener) null);
	}
	
	/**
	 * Same as {@link #startScan(Interval)} but also calls {@link #setListener_Discovery(DiscoveryListener)} for you.
	 */
	public void startScan(Interval scanTime, AdvertisingFilter filter, DiscoveryListener discoveryListener)
	{
		m_timeNotScanning = 0.0;
		scanTime = scanTime.seconds < 0.0 ? Interval.INFINITE : scanTime;
		
		if( !is(ON) )  return;
		
		m_doingInfiniteScan = scanTime.equals(Interval.INFINITE);
		
		if( discoveryListener != null )
		{
			setListener_Discovery(discoveryListener);
		}
		
		m_filterMngr.add(filter);
		
		P_Task_Scan scanTask = m_taskQueue.get(P_Task_Scan.class, this);
		
		if( scanTask != null )
		{
			scanTask.resetTimeout(scanTime.seconds);
		}
		else
		{
			ASSERT(!m_taskQueue.isCurrentOrInQueue(P_Task_Scan.class, this));
			
			m_stateTracker.append(BleState.SCANNING);
			
			m_taskQueue.add(new P_Task_Scan(this, m_listeners.getScanTaskListener(), scanTime.seconds));
		}
	}
	
	/**
	 * Requires the {@link Manifest.permission#WAKE_LOCK} permission. Gives you access to the internal
	 * wake lock as a convenience and eventually calls {@link WakeLock#acquire()}.
	 * 
	 * @see BleManagerConfig#manageCpuWakeLock
	 */
	public void pushWakeLock()
	{
		m_wakeLockMngr.push();
	}
	
	/**
	 * Opposite of {@link #pushWakeLock()}, eventually calls {@link WakeLock#release()}.
	 */
	public void popWakeLock()
	{
		m_wakeLockMngr.pop();
	}
	
	/**
	 * Fires a callback to {@link AssertListener} if condition is false. Will post a {@link Log#ERROR}-level
	 * message with a stack trace to the console as well if {@link BleManagerConfig#loggingEnabled} is true.
	 */
	public boolean ASSERT(boolean condition)
	{
		return ASSERT(condition, "");
	}
	
	/**
	 * Same as {@link #ASSERT(boolean)} but with an added message.
	 */
	public boolean ASSERT(boolean condition, String message)
	{
		if( !condition )
		{
			Exception dummyException = null;
			message = message != null ? message : "";
			
			if( m_config.loggingEnabled || m_assertionListener != null )
			{
				dummyException = new Exception();
			}
			
			if( m_config.loggingEnabled )
			{
				Log.e(BleManager.class.getSimpleName(), "ASSERTION FAILED " + message, dummyException);
			}
			
			if( m_assertionListener != null )
			{
				m_assertionListener.onAssertFailed(this, message, dummyException.getStackTrace());
			}

			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns the abstracted bitwise state mask representation of {@link BleState} for this device.
	 * 
	 * @see BleState
	 */
	public int getStateMask()
	{
		return m_stateTracker.getState();
	}
	
	/**
	 * Returns the native bitwise state mask representation of {@link BleState} for this device.
	 * Similar to calling {@link BluetoothAdapter#getState()}
	 * 
	 * @see BleState
	 */
	public int getNativeStateMask()
	{
		return m_nativeStateTracker.getState();
	}
	
	/**
	 * Enables BLE if manager is currently {@link BleState#OFF}. Otherwise does nothing.
	 */
	public void enableBle()
	{
		if( isAny(TURNING_ON, ON) )  return;
		
		if( is(OFF) )
		{
			m_stateTracker.update(TURNING_ON, true, OFF, false);
		}
		
		m_taskQueue.add(new P_Task_TurnBleOn(this, /*implicit=*/false));
	}
	
	/**
	 * This is essentially a big red reset button for the Bluetooth stack. Use it ruthlessly
	 * when the stack seems to be acting up, like when you can't connect to a device that you should be 
	 * able to connect to. It's similar to calling {@link #disableBle()} then {@link #enableBle()},
	 * but also does other things like removing all bonds (similar to {@link #unbondAll()}) and
	 * other "special sauce" such that you should use this method instead of trying to reset the
	 * stack manually with component calls.
	 * <br><br>
	 * It's good app etiquette to first prompt the user to get permission to drop a nuke because
	 * it will affect Bluetooth system-wide and in other apps.
	 * 
	 *  @see BleState#NUKING
	 */
	public void dropTacticalNuke()
	{
		dropTacticalNuke(null);
	}
	
	/**
	 * Same as {@link #dropTacticalNuke()} but with a convenience callback for when the nuke is
	 * completed and the native BLE stack is (should be) back to normal.
	 * 
	 * @see BleState#NUKING
	 */
	public void dropTacticalNuke(NukeEndListener listener)
	{
		dropTacticalNuke_synchronized(listener);
	}
	
	/**
	 * Removes bonds for all devices that are {@link BleDeviceState#BONDED}.
	 * Essentially a convenience method for calling {@link BleDevice#unbond()},
	 * on each device individually.
	 */
	public void unbondAll()
	{
		m_deviceMngr.unbondAll(null);
	}
	
	/**
	 * Convenience method to request your user to enable ble in a "standard" way
	 * with an {@link Intent} instead of using {@link #enableBle()} directly.
	 * Result is posted as normal to {@link Activity#onActivityResult()}.
	 */
	public void enableBleWithIntent(Activity callingActivity, int requestCode)
	{
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		callingActivity.startActivityForResult(enableBtIntent, requestCode);
	}
	
	/**
	 * Opposite of {@link #onPause()}, to be called from your override of {@link Activity#onResume()} for each {@link Activity}
	 * in your application. See comment for {@link #onPause()} for a similar explanation for why you should call this method.
	 */
	public void onResume()
	{
		m_triedToStartScanAfterResume = false;
		m_isForegrounded = true;
		m_timeForegrounded = 0.0;
		
		if( m_doingInfiniteScan )
		{
			m_triedToStartScanAfterResume = true;
			
			startScan();
		}
		else if( Interval.isDisabled(m_config.autoScanDelayAfterResume) )
		{
			m_triedToStartScanAfterResume = true;
		}
	}
	
	/**
	 * It's generally recommended to call this in your override of {@link Activity#onPause()} for each {@link Activity}
	 * in your application. This doesn't do much for now, just a little bookkeeping and stops scan automatically if
	 * {@link BleManagerConfig#stopScanOnPause} is <code>true</code>. Strictly speaking you don't *have* to call this method,
	 * but another good reason is for future-proofing. Later releases of this library may do other more important things
	 * in this method so it's good to have it being called just in case.
	 */
	public void onPause()
	{
		m_triedToStartScanAfterResume = false;
		m_isForegrounded = false;
		m_timeForegrounded = 0.0;
		
		if( m_config.stopScanOnPause && is(SCANNING) )
		{
			stopScan_private();
		}
	}
	
	/**
	 * Call this from your app's {@link Activity#onDestroy()} method.
	 * NOTE: Apparently no good way to know when app as a whole is being destroyed
	 * and not individual Activitys, so keeping this package-private for now.
	 */
	void onDestroy()
	{
		m_wakeLockMngr.clear();
		m_listeners.onDestroy();
	}
	
	/**
	 * Returns the {@link Application} provided to the constructor.
	 */
	public Application getApplication()
	{
		return (Application) m_context;
	}
	
	/**
	 * Stops a scan previously started by {@link #startScan()} or its various overloads.
	 * This will also stop the actual scan operation itself that may be ongoing due to
	 * {@link #startPeriodicScan(Interval, Interval)} or defined by {@link BleManagerConfig#autoScanTime},
	 * but scanning in general will still continue periodically until you call {@link #stopPeriodicScan()}.
	 */
	public void stopScan()
	{
		m_doingInfiniteScan = false;
		
		stopScan_private();
	}
	
	/**
	 * Same as {@link #stopScan()} but also unregisters any filter supplied to various overloads of
	 * {@link #startScan()} or {@link #startPeriodicScan(Interval, Interval)} that take an {@link BleManagerConfig.AdvertisingFilter}.
	 * Calling {@link #stopScan()} alone will keep any previously registered filters active.
	 */
	public void stopScan(AdvertisingFilter filter)
	{
		m_filterMngr.remove(filter);
		
		stopScan();
	}
	
	private void stopScan_private()
	{
		m_timeNotScanning = 0.0;
		
		if( !m_taskQueue.succeed(P_Task_Scan.class, this) )
		{
			m_taskQueue.clearQueueOf(P_Task_Scan.class, this);
		}

		m_stateTracker.remove(BleState.SCANNING);
	}
	
	/**
	 * Gets a known {@link BleDeviceState#DISCOVERED} device by MAC address, or <code>null</code> if there is no such device.
	 */
	public BleDevice getDevice(String address)
	{
		return m_deviceMngr.get(address);
	}
	
	/**
	 * Shortcut for checking if {@link #getDevice(String)} returns <code>null</code>.
	 */
	public boolean hasDevice(String address)
	{
		return getDevice(address) != null;
	}
	
	/**
	 * Returns the first device that is in the given state, or null if no match is found.
	 */
	public BleDevice getDevice(BleDeviceState state)
	{
		for( int i = 0; i < m_deviceMngr.getCount(); i++ )
		{
			BleDevice device = m_deviceMngr.get(i);
			
			if( device.is(state) )
			{
				return device;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns true if we have a device in the given state.
	 */
	public boolean hasDevice(BleDeviceState state)
	{
		return getDevice(state) != null;
	}
	
	/**
	 * Returns the first device that matches the query, or null if no match is found.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public BleDevice getDevice(Object ... query)
	{
		for( int i = 0; i < m_deviceMngr.getCount(); i++ )
		{
			BleDevice device = m_deviceMngr.get(i);
			
			if( device.is(query) )
			{
				return device;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns true if we have a device that matches the given query.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public boolean hasDevice(Object ... query)
	{
		return getDevice(query) != null;
	}
	
	/**
	 * Returns all the devices managed by this class. This generally includes all devices that are either.
	 * advertising or connected.
	 */
	public BleDeviceIterator getDevices()
	{
		return new BleDeviceIterator(m_deviceMngr.getList());
	}
	
	/**
	 * Returns whether we have any devices. For example if you have never called {@link #startScan()}
	 * (or similar) then this will return false.
	 */
	public boolean hasDevices()
	{
		return m_deviceMngr.getCount() > 0;
	}
	
	/**
	 * Same as {@link #getDevice(BleDeviceState)} except returns all matching devices.
	 */
	public BleDeviceIterator getDevices(BleDeviceState state)
	{
		return new BleDeviceIterator(m_deviceMngr.getList(), state, true);
	}
	
	/**
	 * Same as {@link #getDevice(Object...)} except returns all matching devices.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public BleDeviceIterator getDevices(Object ... query)
	{
		return new BleDeviceIterator(m_deviceMngr.getList(), query);
	}
	
	//--- DRK > Smooshing together a bunch of package-private accessors here.
	P_StateTracker				getStateTracker(){				return m_stateTracker;				}
	P_NativeStateTracker		getNativeStateTracker(){		return m_nativeStateTracker;		}
	UpdateLoop					getUpdateLoop(){				return m_updateLoop;				}
	P_BluetoothCrashResolver	getCrashResolver(){				return m_crashResolver;				}
	P_TaskQueue					getTaskQueue(){					return m_taskQueue;					}
	P_Logger					getLogger(){					return m_logger;					}
	
	
	private void disableBle(boolean removeAllBonds)
	{
		disableBle_synchronized(removeAllBonds);
	}
	
	private void disableBle_synchronized(boolean removeAllBonds)
	{
		if( isAny(TURNING_OFF, OFF) )  return;
		
		if( is(ON) )
		{
			m_stateTracker.update(TURNING_OFF, true, ON, false);
		}
		
		m_deviceMngr.disconnectAll(PE_TaskPriority.CRITICAL);
		
		if( removeAllBonds )
		{
			m_deviceMngr.unbondAll(PE_TaskPriority.CRITICAL);
		}
		
		P_Task_TurnBleOff task = new P_Task_TurnBleOff(this, /*implicit=*/false, new PA_Task.I_StateListener()
		{
			@Override
			public void onStateChange(PA_Task taskClass, PE_TaskState state)
			{
				if( state == PE_TaskState.EXECUTING )
				{
					if( is(NUKING) )
					{
						m_nativeStateTracker.append(NUKING);
					}
					
					m_deviceMngr.undiscoverAll();
				}
			}
		});
		
		m_taskQueue.add(task);
	}
	
	private void dropTacticalNuke_synchronized(NukeEndListener listener)
	{
		if( listener != null )
		{
			if( m_nukeListeners != null )
			{
				m_nukeListeners.addListener(listener);
			}
			else
			{
				m_nukeListeners = new P_WrappingNukeListener(listener, m_mainThreadHandler, m_config.postCallbacksToMainThread);
			}
		}
		
		if( is(BleState.NUKING) )
		{
			return;
		}
		
		m_stateTracker.append(NUKING);
		
		m_taskQueue.add(new P_Task_CrashResolver(BleManager.this, m_crashResolver));
		
		disableBle(/*removeAllBonds=*/true);

		m_taskQueue.add(new P_Task_TurnBleOn(this, /*implicit=*/false, new PA_Task.I_StateListener()
		{
			@Override public void onStateChange(PA_Task taskClass, PE_TaskState state)
			{
				if( state.isEndingState() )
				{
					NukeEndListener nukeListeners = m_nukeListeners;
					m_nukeListeners = null;
					m_nativeStateTracker.remove(NUKING);
					m_stateTracker.remove(NUKING);
					
					if( nukeListeners != null )  nukeListeners.onNukeEnded(BleManager.this);
				}
			}
		}));
	}
	
	void startAutoUpdate()
	{
		startAutoUpdate(Interval.asDouble(m_config.autoUpdateRate));
	}
	
	void startAutoUpdate(double updateRate)
	{
		if( m_updateLoop != null )
		{
			m_updateLoop.start(updateRate);
		}
	}
	
	void stopAutoUpdate()
	{
		if( m_updateLoop != null )
		{
			m_updateLoop.stop();
		}
	}
	
	P_Task_Scan.E_Mode startNativeScan()
	{
		//--- DRK > Not sure how useful this retry loop is. I've definitely seen startLeScan 
		//---		fail but then work again at a later time (seconds-minutes later), so
		//---		it's possible that it can recover although I haven't observed it in this loop.
		int retryCount = 0;
		final int retryCountMax = 3;
		while( retryCount <= retryCountMax )
		{
			if( m_btMngr.getAdapter().startLeScan(m_listeners.m_scanCallback) )
			{
				if( retryCount >= 1 )
				{
					//--- DRK > Not really an ASSERT case...rather just really want to know if this can happen
					//---		so if/when it does I want it to be loud.
					//---		UPDATE: Yes, this hits...TODO: Now have to determine if this is my fault or Android's.
					//---		Error message is "09-29 16:37:11.622: E/BluetoothAdapter(16286): LE Scan has already started".
					//---		Calling stopLeScan below "fixes" the issue.
//					ASSERT(false, "Started Le scan on attempt number " + retryCount);
				}
				
				break;
			}
			
			retryCount++;
			
			if( retryCount <= retryCountMax )
			{
				if( retryCount == 1 )
				{
					m_logger.w("Failed first startLeScan() attempt. Calling stopLeScan() then trying again...");
					
					//--- DRK > It's been observed that right on app start up startLeScan can fail with a log
					//---		message saying it's already started...not sure if it's my fault or not but throwing
					//---		this in as a last ditch effort to "fix" things.
					m_btMngr.getAdapter().stopLeScan(m_listeners.m_scanCallback);
				}
				else
				{
					m_logger.w("Failed startLeScan() attempt number " + retryCount + ". Trying again...");
				}
			}
			
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
			}
		}
		
		if( retryCount > retryCountMax )
		{
			m_logger.w("LeScan totally failed to start!");
			
			if( m_config.revertToClassicDiscoveryIfNeeded )
			{
				if( !m_btMngr.getAdapter().startDiscovery() )
				{
					m_logger.w("Classic discovery failed to start!");
					
					m_taskQueue.fail(P_Task_Scan.class, this);
					uhOh(UhOh.CLASSIC_DISCOVERY_FAILED);
				}
				else
				{
					m_nativeStateTracker.append(BleState.SCANNING);
					
					uhOh(UhOh.START_BLE_SCAN_FAILED__USING_CLASSIC);
					
					return E_Mode.CLASSIC;
				}
			}
			else
			{
				m_taskQueue.fail(P_Task_Scan.class, this);
				uhOh(UhOh.START_BLE_SCAN_FAILED);
			}
		}
		else
		{
			if( retryCount > 0 )
			{
				m_logger.w("Started native scan with " + (retryCount+1) + " attempts.");
			}
			if( m_config.enableCrashResolver )
			{
				m_crashResolver.start();
			}
			
			m_nativeStateTracker.append(BleState.SCANNING);
			
			return E_Mode.BLE;
		}
		
		return null;
	}
	
	void onDiscovered(BluetoothDevice device_native, int rssi, byte[] scanRecord_nullable)
	{
		onDiscovered_synchronized(device_native, rssi, scanRecord_nullable);
	}
	
	private void onDiscovered_synchronized(BluetoothDevice device_native, int rssi, byte[] scanRecord_nullable)
	{
		//--- DRK > Protects against fringe case where scan task is executing and app calls disableBle().
		//---		Here the scan task will be interrupted but still potentially has enough time to
		//---		discover another device or two. We're checking the enum state as opposed to the native
		//---		integer state because in this case the "turn off ble" task hasn't started yet and thus
		//---		hasn't called down into native code and thus the native state hasn't changed.
		if( !is(ON) )  return;
		
		//--- DRK > Not sure if queued up messages to library's thread can sneak in a device discovery event
		//---		after user called stopScan(), so just a check to prevent unexpected callbacks to the user.
		if( !is(SCANNING) )  return;
		
		String rawDeviceName = "";
		
		try
		{
			rawDeviceName = device_native.getName();
		}
		
		//--- DRK > Can occasionally catch a DeadObjectException or NullPointerException here...nothing we can do about it.
		catch(Exception e)
		{
			m_logger.e(e.getStackTrace().toString());
			
			//--- DRK > Can't actually catch the DeadObjectException itself.
			if( e instanceof DeadObjectException )
			{
				uhOh(UhOh.DEAD_OBJECT_EXCEPTION);
			}
			else
			{
				uhOh(UhOh.RANDOM_EXCEPTION);
			}
			
			return;
		}
		
		String loggedDeviceName = rawDeviceName;
		loggedDeviceName = loggedDeviceName != null ? loggedDeviceName : "<NO_NAME>";
		
		String macAddress = device_native.getAddress();
		BleDevice device = m_deviceMngr.get(macAddress);
		
		if ( device == null )
    	{
//    		m_logger.i("Discovered device " + loggedDeviceName + " " + macAddress + " not in list.");
    	}
    	else
    	{
    		if( device.getNative().equals(device_native) )
    		{
//    			m_logger.i("Discovered device " + loggedDeviceName + " " + macAddress + " already in list.");
    		}
    		else
    		{
    			m_logger.e("Discovered device " + loggedDeviceName + " " + macAddress + " already in list but with new native device instance.");
    			ASSERT(false);
    		}
    	}
		
		List<UUID> services_nullable = null;
		
		String normalizedDeviceName = "";
		
		if( device == null )
		{
			normalizedDeviceName = Utils.normalizeDeviceName(rawDeviceName);
	    	services_nullable = Utils.parseServiceUuids(scanRecord_nullable);
	    	byte[] scanRecord = scanRecord_nullable != null ? scanRecord_nullable : BleDevice.EMPTY_BYTE_ARRAY;
	    	String deviceName = rawDeviceName;
	    	deviceName = deviceName != null ? deviceName : "";
	    	
	    	if( !m_filterMngr.allow(device_native, services_nullable, deviceName, normalizedDeviceName, scanRecord, rssi) )  return;
		}    	
    	
    	boolean newlyDiscovered = false;
		
    	if ( device == null )
    	{
    		device = new BleDevice(BleManager.this, device_native, normalizedDeviceName);
    		m_deviceMngr.add(device);
    		newlyDiscovered = true;
    	}
    	
    	onDiscovered_wrapItUp(device, newlyDiscovered, services_nullable, scanRecord_nullable, rssi);
	}
    	
    void onDiscovered_wrapItUp(BleDevice device, boolean newlyDiscovered, List<UUID> services_nullable, byte[] scanRecord_nullable, int rssi)
    {    	
    	if( newlyDiscovered )
    	{
    		device.onNewlyDiscovered(services_nullable, rssi, scanRecord_nullable);
    		
    		if( m_discoveryListener != null )
    		{
    			m_discoveryListener.onDeviceDiscovered(device);
    		}
    	}
    	else
    	{
    		device.onRediscovered(services_nullable, rssi, scanRecord_nullable);
    		
    		if( m_discoveryListener != null )
    		{
    			if( m_discoveryListener instanceof DiscoveryListener_Full)
    			{
    				((DiscoveryListener_Full)m_discoveryListener).onDeviceRediscovered(device);
    			}
    		}
    	}
    }
	
	void stopNativeScan(P_Task_Scan scanTask)
	{
		if( scanTask.getMode() == P_Task_Scan.E_Mode.BLE )
		{
			try
			{
				m_btMngr.getAdapter().stopLeScan(m_listeners.m_scanCallback);
			}
			catch(NullPointerException e)
			{
				//--- DRK > Catching this because of exception thrown one time...only ever seen once, so not very reproducible.
	//			java.lang.NullPointerException
	//			07-02 15:04:48.149: E/AndroidRuntime(24389): 	at android.bluetooth.BluetoothAdapter$GattCallbackWrapper.stopLeScan(BluetoothAdapter.java:1819)
	//			07-02 15:04:48.149: E/AndroidRuntime(24389): 	at android.bluetooth.BluetoothAdapter.stopLeScan(BluetoothAdapter.java:1722)
				m_logger.w(e.getStackTrace().toString());
				
				uhOh(UhOh.RANDOM_EXCEPTION);
			}
		}
		else if( scanTask.getMode() == P_Task_Scan.E_Mode.CLASSIC )
		{
			//--- DRK > This assert tripped, but not sure what I can do about it. Technically discovery can be cancelled
			//---		by another app or something, so its usefulness as a logic checker is debatable.
//			ASSERT(m_btMngr.getAdapter().isDiscovering(), "Trying to cancel discovery when not natively running.");
			
			if( m_btMngr.getAdapter().isDiscovering() )
			{
				m_btMngr.getAdapter().cancelDiscovery();
			}
		}
		
		if( m_config.enableCrashResolver )
		{
			m_crashResolver.stop();
		}
		
		m_nativeStateTracker.remove(BleState.SCANNING);
	}
	
	void clearScanningRelatedMembers()
	{
//		m_filterMngr.clear();
		
		m_timeNotScanning = 0.0;
		
		m_stateTracker.remove(BleState.SCANNING);
	}
	
	void tryPurgingStaleDevices(double scanTime)
	{
		if( Interval.isDisabled(m_config.minScanTimeToInvokeUndiscovery) )  return;
		if( Interval.isDisabled(m_config.scanKeepAlive) )  return;
		
		if( scanTime < Interval.asDouble(m_config.minScanTimeToInvokeUndiscovery) )  return;
			
		m_deviceMngr.purgeStaleDevices(Interval.asDouble(m_config.scanKeepAlive), m_discoveryListener);
	}
	
	/**
	 * This method is made public in case you want to tie the library in to an update loop
	 * from another codebase. Generally you should leave {@link BleManagerConfig#autoUpdateRate}
	 * alone and let the library handle the calling of this method.
	 */
	public void update(double timeStep)
	{
		update_synchronized(timeStep);
	}
	
	private void update_synchronized(double timeStep)
	{
		m_uhOhThrottler.update(timeStep);
		m_taskQueue.update(timeStep);
		
		if( m_isForegrounded )
		{
			m_timeForegrounded += timeStep;
		}
		else
		{
			m_timeForegrounded = 0.0;
		}
		
		m_deviceMngr.update(timeStep);
		
		if( !is(SCANNING) )
		{
			m_timeNotScanning += timeStep;
		}
		
		boolean startScan = false;
		
		if( Interval.isEnabled(m_config.autoScanTime) )
		{
			if( m_isForegrounded && Interval.isEnabled(m_config.autoScanDelayAfterResume) && !m_triedToStartScanAfterResume && m_timeForegrounded >= Interval.asDouble(m_config.autoScanDelayAfterResume) )
			{
				m_triedToStartScanAfterResume = true;
				
				if( !is(SCANNING) )
				{
					startScan = true;
				}
			}
			else if( !is(SCANNING) )
			{
				double scanInterval = Interval.asDouble(m_isForegrounded ? m_config.autoScanInterval : m_config.autoScanIntervalWhileAppIsPaused);
				
				if( Interval.isEnabled(scanInterval) && m_timeNotScanning >= scanInterval )
				{
					startScan = true;
				}
			}
		}
		
		if( startScan )
		{			
			if( doAutoScan() )
			{
				startScan(m_config.autoScanTime);
			}
		}
		
		P_Task_Scan scanTask = m_taskQueue.get(P_Task_Scan.class, this);

		if( scanTask != null )
		{
			if( scanTask.getState() == PE_TaskState.ARMED || scanTask.getState() == PE_TaskState.EXECUTING )
			{
				tryPurgingStaleDevices(scanTask.getTotalTimeExecuting());
			}
		}
	}
	
	private boolean doAutoScan()
	{
		return m_config.autoScanDuringFirmwareUpdates || !m_deviceMngr.hasDevice(BleDeviceState.UPDATING_FIRMWARE);
	}
	
	void uhOh(UhOh reason)
	{
//		if( reason == UhOh.UNKNOWN_CONNECTION_ERROR )
//		{
//			m_connectionFailTracker = 0;
//		}
		
		m_uhOhThrottler.uhOh(reason);
	}
	
	void onConnectionFailed()
	{
		if( m_config.connectionFailUhOhCount <= 0 )  return;
		
		m_connectionFailTracker++;
		
		if( m_connectionFailTracker >= m_config.connectionFailUhOhCount )
		{
			m_connectionFailTracker = 0;
			
//			uhOh(UhOh.MULTIPLE_CONNECTIONS_FAILED);
		}
	}
	
	void onConnectionSucceeded()
	{
		m_connectionFailTracker = 0;
	}
	
	@Override public String toString()
	{
		return m_stateTracker.toString();
	}
}
