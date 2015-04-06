package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleManagerState.*;

import java.util.List;
import java.util.UUID;

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
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice.BondListener.BondEvent;
import com.idevicesinc.sweetblue.BleDevice.BondListener.Status;
import com.idevicesinc.sweetblue.BleDevice.BondListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.BleManager.ResetListener.ResetEvent;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.Please;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.P_Task_Scan.E_Mode;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Percent;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.UpdateLoop;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;

/**
 * The entry point to the library. Get a singleton instance using {@link #get(android.content.Context, BleManagerConfig)} or its overloads. Make sure
 * to hook up this manager to lifecycle events for your app as a whole: {@link #onPause()} and {@link #onResume()}.
 * <br><br>
 * Also put the following entries (or something similar) in the root of your AndroidManifest.xml:
 * <br><br>
 * {@code <uses-sdk android:minSdkVersion="18" android:targetSdkVersion="21" />}<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH" /> }<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" /> }<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED" /> }<br>
 * {@code <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" /> }<br>
 * {@code <uses-permission android:name="android.permission.WAKE_LOCK" /> } <br>
 * <br><br>
 * {@link android.Manifest.permission#WAKE_LOCK} is recommended but optional, needed if {@link BleManagerConfig#manageCpuWakeLock} is enabled to aid with reconnect loops.
 * As of now it's enabled by default.
 * <br><br><br>
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
 *          m_bleManager = BleManager.get(this);
 *
 *          m_bleManager.startScan(new BleManager.DiscoveryListener()
 *          {
 *              {@literal @}Override public void onEvent(DiscoveryEvent event)
 *              {
 *                  m_bleManager.stopScan();
 *
 *                  if( event.was(LifeCycle.DISCOVERED) )
 *                  {
 *                      event.device().connect(new BleDevice.StateListener()
 *                      {
 *                          {@literal @}Override public void onEvent(StateEvent event)
 *                          {
 *                              if( event.didEnter(BleDeviceState.INITIALIZED) )
 *                              {
 *                                  String toastText = event.device().getDebugName() + " just initialized!";
 *                                  Toast.makeText(MyActivity.this, toastText, Toast.LENGTH_LONG).show();
 *                              }
 *                          }
 *                      });
 *                  }
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
 */
public class BleManager
{
	/**
	 * Provide an implementation to {@link com.idevicesinc.sweetblue.BleManager#setListener_Discovery(com.idevicesinc.sweetblue.BleManager.DiscoveryListener)} to receive
	 * callbacks when a device is discovered after calling various {@link com.idevicesinc.sweetblue.BleManager#startScan()}
	 * or {@link com.idevicesinc.sweetblue.BleManager#startPeriodicScan(Interval, Interval)} methods. You can also provide this to various
	 * overloads of {@link com.idevicesinc.sweetblue.BleManager#startScan()} and {@link com.idevicesinc.sweetblue.BleManager#startPeriodicScan(Interval, Interval)}.
	 * <br><br>
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface DiscoveryListener
	{
		/**
		 * Enumerates changes in the "discovered" state of a device.
		 * Used at {@link com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent#lifeCycle()}.
		 */
		public static enum LifeCycle
		{
			/**
			 * Used when a device is discovered for the first time after
			 * calling {@link com.idevicesinc.sweetblue.BleManager#startScan()} (or its overloads)
			 * or {@link com.idevicesinc.sweetblue.BleManager#startPeriodicScan(Interval, Interval)}.
			 */
			DISCOVERED,
			
			/**
			 * Used when a device is rediscovered after already being discovered at least once.
			 */
			REDISCOVERED,
			
			/**
			 * Used when a device is "undiscovered" after being discovered at least once. There is no native equivalent
			 * for this callback. Undiscovery is approximated with a timeout based on the last time we discovered a device.
			 * Consequently you should expect that the callback will take some amount of time to receive after an
			 * advertising device is turned off or goes out of range or what have you. It's generally not as fast as other
			 * state changes like {@link BleDeviceState#DISCONNECTED} or getting {@link BleDeviceState#DISCOVERED} in the first place.
			 *
			 * @see BleDeviceConfig#minScanTimeNeededForUndiscovery
			 * @see BleDeviceConfig#undiscoveryKeepAlive
			 */
			UNDISCOVERED;
		}
		
		/**
		 * Struct passed to {@link com.idevicesinc.sweetblue.BleManager.DiscoveryListener#onEvent(com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent)}.
		 */
		@Immutable
		public static class DiscoveryEvent
		{
			/**
			 * The {@link com.idevicesinc.sweetblue.BleManager} which is currently {@link BleManagerState#SCANNING}.
			 */
			public BleManager manager(){  return m_manager;  }
			private final BleManager m_manager;
			
			/**
			 * The device in question.
			 */
			public BleDevice device(){  return m_device;  }
			private final BleDevice m_device;
			
			/**
			 * The discovery {@link com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle} that the device has undergone.
			 */
			public LifeCycle lifeCycle(){  return m_lifeCycle;  }
			private final LifeCycle m_lifeCycle;
			
			public DiscoveryEvent(BleManager manager, BleDevice device, LifeCycle lifeCycle)
			{
				m_manager = manager;
				m_device = device;
				m_lifeCycle = lifeCycle;
			}
			
			/**
			 * Forwards {@link BleDevice#getRssi()}.
			 */
			public int rssi()
			{
				return device().getRssi();
			}
			
			/**
			 * Forwards {@link BleDevice#getRssiPercent()}.
			 */
			public Percent rssi_percent()
			{
				return device().getRssiPercent();
			}
			
			/**
			 * Convenience method for checking equality of given {@link com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle} and {@link #lifeCycle()}.
			 */
			public boolean was(LifeCycle lifeCycle)
			{
				return lifeCycle == lifeCycle();
			}
			
			@Override public String toString()
			{
				return Utils.toString
				(
					this.getClass(),
					"device",				device().getName_debug(),
					"lifeCycle",			lifeCycle(),
					"rssi",					rssi(),
					"rssi_percent",			rssi_percent()
				);
			}
		}
		
		/**
		 * Called when the discovery lifecycle of a device is updated.
		 * <br><br> 
		 * TIP: Take a look at {@link BleDevice#getLastDisconnectIntent()}. If it is {@link State.ChangeIntent#UNINTENTIONAL}
		 * then from a user-experience perspective it's most often best to automatically connect without user confirmation.
		 */
		void onEvent(final DiscoveryEvent e);
	}

	/**
	 * Provide an implementation to {@link com.idevicesinc.sweetblue.BleManager#setListener_State(com.idevicesinc.sweetblue.BleManager.StateListener)} to receive callbacks
	 * when the {@link com.idevicesinc.sweetblue.BleManager} undergoes a {@link BleManagerState} change.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface StateListener
	{
		/**
		 * Subclass that adds the manager field.
		 */
		@Immutable
		public static class StateEvent extends State.ChangeEvent<BleManagerState>
		{
			/**
			 * The manager undergoing the state change.
			 */
			public BleManager manager(){  return m_manager;  }
			private final BleManager m_manager;
			
			StateEvent(final BleManager manager, final int oldStateBits, final int newStateBits, final int intentMask)
			{
				super(oldStateBits, newStateBits, intentMask);
				
				this.m_manager = manager;
			}
			
			@Override public String toString()
			{
				return Utils.toString
				(
					this.getClass(),
					"entered",			Utils.toString(enterMask(),		BleManagerState.VALUES()),
					"exited",			Utils.toString(exitMask(),		BleManagerState.VALUES()),
					"current",			Utils.toString(newStateBits(),	BleManagerState.VALUES())
				);
			}
		}
		
		/**
		 * Called when the manager's abstracted {@link BleManagerState} changes.
		 */
		void onEvent(final StateEvent e);
	}

	/**
	 * Provide an implementation to {@link com.idevicesinc.sweetblue.BleManager#setListener_NativeState(com.idevicesinc.sweetblue.BleManager.NativeStateListener)} to receive callbacks
	 * when the {@link com.idevicesinc.sweetblue.BleManager} undergoes a *native* {@link BleManagerState} change. This is similar to {@link com.idevicesinc.sweetblue.BleManager.StateListener}
	 * but reflects what is going on in the actual underlying stack, which may lag slightly behind the
	 * abstracted state reflected by {@link com.idevicesinc.sweetblue.BleManager.StateListener}. Most apps will not find this callback useful.
	 */
	@Advanced
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface NativeStateListener
	{
		/**
		 * Class declared here to be make it implicitly imported for overrides.
		 */
		@Advanced
		@Immutable
		public static class NativeStateEvent extends StateListener.StateEvent
		{
			NativeStateEvent(final BleManager manager, final int oldStateBits, final int newStateBits, final int intentMask)
			{
				super(manager, oldStateBits, newStateBits, intentMask);
			}
		}
		
		/**
		 * Called when the manager's native bitwise {@link BleManagerState} changes. As many bits as possible are flipped at the same time.
		 */
		@Advanced
		void onEvent(final NativeStateEvent e);
	}

	/**
	 * Provide an implementation to {@link com.idevicesinc.sweetblue.BleManager#setListener_UhOh(com.idevicesinc.sweetblue.BleManager.UhOhListener)}
	 * to receive a callback when an {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh} occurs.
	 *
	 * @see com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface UhOhListener
	{
		/**
		 * An UhOh is a warning about an exceptional (in the bad sense) and unfixable problem with the underlying stack that
		 * the app can warn its user about. It's kind of like an {@link Exception} but they can be so common
		 * that using {@link Exception} would render this library unusable without a rat's nest of try/catches.
		 * Instead you implement {@link com.idevicesinc.sweetblue.BleManager.UhOhListener} to receive them. Each {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh} has a {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh#getRemedy()}
		 * that suggests what might be done about it.
		 * 
		 * @see com.idevicesinc.sweetblue.BleManager.UhOhListener
		 * @see com.idevicesinc.sweetblue.BleManager#setListener_UhOh(com.idevicesinc.sweetblue.BleManager.UhOhListener)
		 */
		public enum UhOh
		{
			/**
			 * A {@link BleTask#BOND} operation timed out. This can happen a lot with the Galaxy Tab 4, and doing {@link com.idevicesinc.sweetblue.BleManager#reset()} seems to fix it.
			 * SweetBlue does as much as it can to work around the issue that causes bond timeouts, but some might still slip through.
			 */
			BOND_TIMED_OUT,
			
			/**
			 * A {@link BleDevice#read(java.util.UUID, BleDevice.ReadWriteListener)}
			 * took longer than timeout set by {@link BleDeviceConfig#timeoutRequestFilter}.
			 * You will also get a {@link BleDevice.ReadWriteListener.Result} with {@link BleDevice.ReadWriteListener.Status#TIMED_OUT}
			 * but a timeout is a sort of fringe case that should not regularly happen.
			 */
			READ_TIMED_OUT,
			
			/**
			 * A {@link BleDevice#read(java.util.UUID, BleDevice.ReadWriteListener)} returned with a <code>null</code>
			 * characteristic value. The <code>null</code> value will end up as an empty array in {@link Result#data}
			 * so app-land doesn't have to do any special <code>null</code> handling.
			 */
			READ_RETURNED_NULL,
			
			/**
			 * Similar to {@link #READ_TIMED_OUT} but for {@link BleDevice#write(java.util.UUID, byte[])}.
			 */
			WRITE_TIMED_OUT,
			
			/**
			 * When the underlying stack meets a race condition where {@link android.bluetooth.BluetoothAdapter#getState()} does not
			 * match the value provided through {@link android.bluetooth.BluetoothAdapter#ACTION_STATE_CHANGED} with {@link android.bluetooth.BluetoothAdapter#EXTRA_STATE}.
			 */
			INCONSISTENT_NATIVE_BLE_STATE,
			
			/**
			 * A {@link BleDevice} went from {@link BleDeviceState#BONDING} to {@link BleDeviceState#UNBONDED}.
			 * UPDATE: This can happen under normal circumstances, so not listing it as an uh oh for now.
			 */
//			WENT_FROM_BONDING_TO_UNBONDED,
			
			/**
			 * A {@link android.bluetooth.BluetoothGatt#discoverServices()} operation returned two duplicate services. Not the same instance
			 * necessarily but the same UUID.
			 */
			DUPLICATE_SERVICE_FOUND,
			
			/**
			 * A {@link android.bluetooth.BluetoothGatt#discoverServices()} operation returned a service instance that we already received before
			 * after disconnecting and reconnecting.
			 */
			OLD_DUPLICATE_SERVICE_FOUND,
			
			/**
			 * {@link android.bluetooth.BluetoothAdapter#startLeScan()} failed for an unknown reason. The library is now using
			 * {@link android.bluetooth.BluetoothAdapter#startDiscovery()} instead.
			 * 
			 * @see BleManagerConfig#revertToClassicDiscoveryIfNeeded
			 */
			START_BLE_SCAN_FAILED__USING_CLASSIC,
			
			/**
			 * {@link android.bluetooth.BluetoothGatt#getConnectionState()} says we're connected but we never tried to connect in the first place.
			 * My theory is that this can happen on some phones when you quickly restart the app and the stack doesn't have 
			 * a chance to disconnect from the device entirely. 
			 */
			CONNECTED_WITHOUT_EVER_CONNECTING,
			
			/**
			 * Similar in concept to {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh#RANDOM_EXCEPTION} but used when {@link android.os.DeadObjectException} is thrown.
			 */
			DEAD_OBJECT_EXCEPTION,
			
			/**
			 * The underlying native BLE stack enjoys surprising you with random exceptions. Every time a new one is discovered
			 * it is wrapped in a try/catch and this {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh} is dispatched.
			 */
			RANDOM_EXCEPTION,
			
			/**
			 * {@link android.bluetooth.BluetoothAdapter#startLeScan()} failed and {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is <code>false</code>.
			 * 
			 * @see BleManagerConfig#revertToClassicDiscoveryIfNeeded
			 */
			START_BLE_SCAN_FAILED,
			
			/**
			 * {@link android.bluetooth.BluetoothAdapter#startLeScan()} failed and {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is <code>true</code>
			 * so we try {@link android.bluetooth.BluetoothAdapter#startDiscovery()} but that also fails...fun!
			 */
			CLASSIC_DISCOVERY_FAILED,
			
			/**
			 * {@link android.bluetooth.BluetoothGatt#discoverServices()} failed right off the bat and returned false.
			 */
			SERVICE_DISCOVERY_IMMEDIATELY_FAILED,
			
			/**
			 * {@link android.bluetooth.BluetoothAdapter#disable()}, through {@link com.idevicesinc.sweetblue.BleManager#turnOff()}, is failing to complete.
			 * We always end up back at {@link android.bluetooth.BluetoothAdapter#STATE_ON}.
			 */
			CANNOT_DISABLE_BLUETOOTH,
			
			/**
			 * {@link android.bluetooth.BluetoothAdapter#enable()}, through {@link com.idevicesinc.sweetblue.BleManager#turnOn()}, is failing to complete.
			 * We always end up back at {@link android.bluetooth.BluetoothAdapter#STATE_OFF}. Opposite problem of {@link #CANNOT_DISABLE_BLUETOOTH}
			 */
			CANNOT_ENABLE_BLUETOOTH,
			
			/**
			 * Just a blanket case for when the library has to completely shrug its shoulders.
			 */
			UNKNOWN_BLE_ERROR;
			
			/**
			 * Returns the {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.Remedy} for this {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh}.
			 */
			public Remedy getRemedy()
			{
				if( this.ordinal() >= CANNOT_DISABLE_BLUETOOTH.ordinal() )
				{
					return Remedy.RESTART_PHONE;
				}
				else if( this.ordinal() >= START_BLE_SCAN_FAILED.ordinal() )
				{
					return Remedy.RESET_BLE;
				}
				else
				{
					return Remedy.WAIT_AND_SEE;
				}
			}
		}
		
		/**
		 * The suggested remedy for each {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh}. This can be used as a proxy for the severity
		 * of the issue.
		 */
		public static enum Remedy
		{
			/**
			 * Nothing you can really do, hopefully the library can soldier on.
			 */
			WAIT_AND_SEE,
			
			/**
			 * Calling {@link com.idevicesinc.sweetblue.BleManager#reset()} is probably in order.
			 * 
			 * @see com.idevicesinc.sweetblue.BleManager#reset()
			 */
			RESET_BLE,
			
			/**
			 * Might want to notify your user that a phone restart is in order.
			 */
			RESTART_PHONE;
		}
		
		/**
		 * Struct passed to {@link com.idevicesinc.sweetblue.BleManager.UhOhListener#onEvent(com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOhEvent)}.
		 */
		@Immutable
		public static class UhOhEvent
		{
			/**
			 * The manager associated with the {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOhEvent}
			 */
			public BleManager manager(){  return m_manager;  }
			private final BleManager m_manager;
			
			/**
			 * Returns the type of {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh} that occurred.
			 */
			public UhOh uhOh(){  return m_uhOh;  }
			private final UhOh m_uhOh;
			
			/**
			 * Forwards {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh#getRemedy()}.
			 */
			public Remedy remedy(){  return uhOh().getRemedy();  };
			
			UhOhEvent(BleManager manager, UhOh uhoh)
			{
				m_manager = manager;
				m_uhOh = uhoh;
			}
			
			@Override public String toString()
			{
				return Utils.toString
				(
					this.getClass(),
					"uhOh",			uhOh(),
					"remedy",		remedy()
				);
			}
		}
		
		/**
		 * Run for the hills.
		 */
		void onEvent(final UhOhEvent e);
	}

	/**
	 * Provide an implementation to {@link com.idevicesinc.sweetblue.BleManager#reset(com.idevicesinc.sweetblue.BleManager.ResetListener)}
	 * to be notified when a reset operation is complete.
	 *
	 * @see com.idevicesinc.sweetblue.BleManager#reset(com.idevicesinc.sweetblue.BleManager.ResetListener)
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ResetListener
	{
		/**
		 * Enumeration of the progress of the reset.
		 * More entries will be added in the future.
		 */
		public static enum Progress
		{
			/**
			 * The reset has completed successfully.
			 */
			COMPLETED;
		}
		
		/**
		 * Struct passed to {@link com.idevicesinc.sweetblue.BleManager.ResetListener#onEvent(com.idevicesinc.sweetblue.BleManager.ResetListener.ResetEvent)}.
		 */
		@Immutable
		public static class ResetEvent
		{
			/**
			 * The {@link com.idevicesinc.sweetblue.BleManager} the reset was applied to.
			 */
			public BleManager manager(){  return m_manager;  }
			private final BleManager m_manager;
			
			/**
			 * The progress of the reset.
			 */
			public Progress progress(){  return m_progress;  }
			private final Progress m_progress;
			
			ResetEvent(BleManager manager, Progress progress)
			{
				m_manager = manager;
				m_progress = progress;
			}
			
			@Override public String toString()
			{
				return Utils.toString
				(
					this.getClass(),
					"progress",		progress()
				);
			}
		}
		
		/**
		 * The reset event, for now only fired when the reset is completed. Hopefully the bluetooth stack is OK now.
		 */
		void onEvent(final ResetEvent e);
	}

	/**
	 * Mostly only for SweetBlue library developers. Provide an implementation to
	 * {@link com.idevicesinc.sweetblue.BleManager#setListener_Assert(com.idevicesinc.sweetblue.BleManager.AssertListener)} to be notified whenever
	 * an assertion fails through {@link com.idevicesinc.sweetblue.BleManager#ASSERT(boolean, String)}.
	 */
	@Advanced
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface AssertListener
	{
		/**
		 * Struct passed to {@link com.idevicesinc.sweetblue.BleManager.AssertListener#onEvent(com.idevicesinc.sweetblue.BleManager.AssertListener.AssertEvent)}.
		 */
		@Immutable
		public static class AssertEvent
		{
			/**
			 * The {@link com.idevicesinc.sweetblue.BleManager} instance for your application.
			 */
			public BleManager manager(){  return m_manager;  }
			private final BleManager m_manager;
			
			/**
			 * Message associated with the assert, or an empty string.
			 */
			public String message(){  return m_message;  }
			private final String m_message;
			
			/**
			 * Stack trace leading up to the assert.
			 */
			public StackTraceElement[] stackTrace(){  return m_stackTrace;  }
			private final StackTraceElement[] m_stackTrace;
			
			AssertEvent(BleManager manager, String message, StackTraceElement[] stackTrace)
			{
				m_manager = manager;
				m_message = message;
				m_stackTrace = stackTrace;
			}
			
			@Override public String toString()
			{
				return Utils.toString
				(
					this.getClass(),
					"message",			message(),
					"stackTrace",		stackTrace()
				);
			}
		}
		
		/**
		 * Provides additional info about the circumstances surrounding the assert.
		 */
		void onEvent(final AssertEvent e);
	}

	private final UpdateLoop.Callback m_updateLoopCallback = new UpdateLoop.Callback()
	{
		@Override public void onUpdate(double timestep)
		{
			update(timestep);
		}
	};

	/**
	 * Create an instance or retrieve an already-created instance with default configuration options set.
	 * If you call this after you call {@link #get(android.content.Context, BleManagerConfig)} (for example in another
	 * {@link android.app.Activity}), the {@link BleManagerConfig} originally passed in will be used.
	 * Otherwise this calls {@link #get(android.content.Context, BleManagerConfig)} with a {@link BleManagerConfig}
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
	 * Create an instance or retrieve an already-created instance with custom configuration options set.
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
	
	private final Context m_context;
	final Handler m_mainThreadHandler;
	private final BluetoothManager m_btMngr;
	private final P_ScanFilterManager m_filterMngr;
	private final P_BluetoothCrashResolver m_crashResolver;
	private			P_Logger m_logger;
			  BleManagerConfig m_config;
		final P_DeviceManager m_deviceMngr;
		final P_DeviceManager m_deviceMngr_cache;
	private final P_BleManager_Listeners m_listeners;
	private final P_BleStateTracker m_stateTracker;
	private final P_NativeBleStateTracker m_nativeStateTracker;
	private 	 UpdateLoop m_updateLoop;
	private final P_TaskQueue m_taskQueue;
	private 	P_UhOhThrottler m_uhOhThrottler;
				P_WakeLockManager m_wakeLockMngr;
	
		final Object m_threadLock = new Object();
	
			DiscoveryListener m_discoveryListener;
	private P_WrappingResetListener m_resetListeners;
	private AssertListener m_assertionListener;
			BleDevice.StateListener m_defaultDeviceStateListener;
			BleDevice.ConnectionFailListener m_defaultConnectionFailListener;
			BleDevice.BondListener m_defaultBondListener;
			BleDevice.ReadWriteListener m_defaultReadWriteListener;
	final P_DiskOptionsManager m_diskOptionsMngr;
	
	private double m_timeForegrounded = 0.0;
	private double m_timeNotScanning = 0.0;
	private boolean m_doingInfiniteScan = false;
	
	private boolean m_isForegrounded = false;
	private boolean m_triedToStartScanAfterResume = false;
	
	static BleManager s_instance = null;
	
	/**
	 * Field for app to associate any data it wants with the singleton instance of this class
	 * instead of having to subclass or manage associative hash maps or something.
	 * The library does not touch or interact with this data in any way.
	 * 
	 * @see BleDevice#appData
	 */
	public Object appData;

	private BleManager(Context context, BleManagerConfig config)
	{
		m_context = context.getApplicationContext();
		m_config = config.clone();
		initLogger();
		m_diskOptionsMngr = new P_DiskOptionsManager(m_context);
		m_filterMngr = new P_ScanFilterManager(m_config.defaultScanFilter);
		m_btMngr = (BluetoothManager) m_context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        // Account for unit testing. When using robolectric, the bluetooth manager comes back null. However, it includes
        // shadow classes to simulate Bluetooth devices, so we shouldn't need the manager to run tests.
        BleManagerState nativeState;
        if (m_btMngr == null) {
            nativeState = BleManagerState.get(BluetoothAdapter.STATE_ON);
        } else {
            nativeState = BleManagerState.get(m_btMngr.getAdapter().getState());
        }
		m_stateTracker = new P_BleStateTracker(this);
		m_stateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		m_nativeStateTracker = new P_NativeBleStateTracker(this);
		m_nativeStateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		m_mainThreadHandler = new Handler(m_context.getMainLooper());
		m_taskQueue = new P_TaskQueue(this);
		m_crashResolver = new P_BluetoothCrashResolver(m_context);
		m_deviceMngr = new P_DeviceManager(this);
		m_deviceMngr_cache = new P_DeviceManager(this);
		m_listeners = new P_BleManager_Listeners(this);

		initConfigDependentMembers();
		
		m_logger.printBuildInfo();
	}
	
	/**
	 * Updates the config options for this instance after calling {@link #get(android.content.Context)} or {@link #get(android.content.Context, BleManagerConfig)}.
	 * Providing a <code>null</code> value will set everything back to default values.
	 */
	public void setConfig(@Nullable(Prevalence.RARE) BleManagerConfig config_nullable)
	{
		this.m_config = config_nullable != null ? config_nullable.clone() : new BleManagerConfig();
		this.initLogger();
		this.initConfigDependentMembers();
	}

	private void initLogger()
	{
		m_logger = new P_Logger(m_config.debugThreadNames, m_config.uuidNameMaps, m_config.loggingEnabled);
	}

	private void initConfigDependentMembers()
	{
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

		if( m_updateLoop != null )
		{
			m_updateLoop.stop();
			m_updateLoop = null;
		}

		if( m_config.runOnMainThread )
		{
			m_updateLoop = UpdateLoop.newMainThreadLoop(m_updateLoopCallback);
		}
		else
		{
			m_updateLoop = UpdateLoop.newAnonThreadLoop(m_updateLoopCallback);
		}

		if( Interval.isEnabled(m_config.autoUpdateRate) )
		{
			startAutoUpdate(Interval.secs(m_config.autoUpdateRate));
		}
	}

	/**
	 * Returns whether the manager is in any of the provided states.
	 */
	public boolean isAny(BleManagerState ... states)
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
	public boolean isAll(BleManagerState... states)
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
	public boolean is(final BleManagerState state)
	{
		return state.overlaps(getStateMask());
	}
	
	/**
	 * @deprecated Use {@link #isAny(int)}.
	 */
	public boolean is(final int mask_BleManagerState)
	{
		return (getStateMask() & mask_BleManagerState) != 0x0;
	}
	
	/**
	 * Returns <code>true</code> if there is partial bitwise overlap between the provided value and {@link #getStateMask()}.
	 * 
	 * @see #isAll(int)
	 */
	public boolean isAny(final int mask_BleManagerState)
	{
		return (getStateMask() & mask_BleManagerState) != 0x0;
	}
	
	/**
	 * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask()}.
	 * 
	 * @see #isAny(int)
	 */
	public boolean isAll(final int mask_BleManagerState)
	{
		return (getStateMask() & mask_BleManagerState) == mask_BleManagerState;
	}

	/**
	 * See similar comment for {@link BleDevice#getTimeInState(BleDeviceState)}.
	 *
	 * @see BleDevice#getTimeInState(BleDeviceState)
	 */
	public Interval getTimeInState(BleManagerState state)
	{
		return Interval.millis(m_stateTracker.getTimeInState(state.ordinal()));
	}

	/**
	 * See similar comment for {@link BleDevice#getTimeInState(BleDeviceState)}.
	 *
	 * @see BleDevice#getTimeInState(BleDeviceState)
	 */
	public Interval getTimeInNativeState(BleManagerState state)
	{
		return Interval.millis(m_nativeStateTracker.getTimeInState(state.ordinal()));
	}

	/**
	 * Checks the underlying stack to see if BLE is supported on the phone.
	 */
	public boolean isBleSupported()
	{
		PackageManager pm = m_context.getPackageManager();
		boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

		return hasBLE;
	}

	/**
	 * Disables BLE if manager is {@link BleManagerState#ON}. This disconnects all current
	 * connections, stops scanning, and forgets all discovered devices.
	 */
	public void turnOff()
	{
		turnOff(false);
	}

	/**
	 * Returns the native manager.
	 */
	@Advanced
	public BluetoothManager getNative()
	{
		return m_btMngr;
	}

	/**
	 * Set a listener here to be notified whenever we encounter an {@link UhOh}.
	 */
	public void setListener_UhOh(@Nullable(Prevalence.NORMAL) UhOhListener listener_nullable)
	{
		m_uhOhThrottler.setListener(listener_nullable);
	}

	/**
	 * Set a listener here to be notified whenever {@link #ASSERT(boolean)} fails.
	 * Mostly for use by internal library developers.
	 */
	public void setListener_Assert(@Nullable(Prevalence.NORMAL) AssertListener listener_nullable)
	{
		if( listener_nullable != null )
		{
			m_assertionListener = new P_WrappingAssertionListener(listener_nullable, m_mainThreadHandler, m_config.postCallbacksToMainThread);
		}
		else
		{
			m_assertionListener = null;
		}
	}

	/**
	 * Set a listener here to be notified whenever a {@link BleDevice} is discovered, rediscovered, or undiscovered.
	 */
	public void setListener_Discovery(@Nullable(Prevalence.NORMAL) DiscoveryListener listener_nullable)
	{
		if( listener_nullable != null )
		{
			m_discoveryListener = new P_WrappingDiscoveryListener(listener_nullable, m_mainThreadHandler, m_config.postCallbacksToMainThread);
		}
		else
		{
			m_discoveryListener = null;
		}
	}

	/**
	 * Returns the discovery listener set with {@link #setListener_Discovery(com.idevicesinc.sweetblue.BleManager.DiscoveryListener)} or
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
	 * Set a listener here to be notified whenever this manager's {@link BleManagerState} changes.
	 */
	public void setListener_State(@Nullable(Prevalence.NORMAL) StateListener listener_nullable)
	{
		m_stateTracker.setListener(listener_nullable);
	}

	/**
	 * Convenience method to listen for all changes in {@link BleDeviceState} for all devices.
	 * The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleDevice#setListener_State(BleDevice.StateListener)}.
	 *
	 * @see BleDevice#setListener_State(BleDevice.StateListener)
	 */
	public void setListener_DeviceState(@Nullable(Prevalence.NORMAL) BleDevice.StateListener listener_nullable)
	{
		if( listener_nullable != null )
		{
			m_defaultDeviceStateListener = new P_WrappingDeviceStateListener(listener_nullable, m_mainThreadHandler, m_config.postCallbacksToMainThread);
		}
		else
		{
			m_defaultDeviceStateListener = null;
		}
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
	public void setListener_ConnectionFail(@Nullable(Prevalence.NORMAL) BleDevice.ConnectionFailListener listener_nullable)
	{
		if( listener_nullable != null )
		{
			m_defaultConnectionFailListener = new P_WrappingDeviceStateListener(listener_nullable, m_mainThreadHandler, m_config.postCallbacksToMainThread);
		}
		else
		{
			m_defaultConnectionFailListener = null;
		}
		
	}
	
	/**
	 * Convenience method to set a default back up listener for all {@link BondEvent}s across all {@link BleDevice} instances.
	 */
	public void setListener_Bond(@Nullable(Prevalence.NORMAL) BleDevice.BondListener listener_nullable)
	{
		if( listener_nullable != null )
		{
			m_defaultBondListener = new P_WrappingBondListener(listener_nullable, m_mainThreadHandler, m_config.postCallbacksToMainThread);
		}
		else
		{
			m_defaultBondListener = null;
		}
	}
	
	/**
	 * Sets a default backup {@link ReadWriteListener} that will be called for all {@link BleDevice} instances.
	 * <br><br>
	 * TIP: Place some analytics code in the listener here. 
	 */
	public void setListener_ReadWrite(@Nullable(Prevalence.NORMAL) ReadWriteListener listener_nullable)
	{
		if( listener_nullable != null )
		{
			m_defaultReadWriteListener = new P_WrappingReadWriteListener(listener_nullable, m_mainThreadHandler, m_config.postCallbacksToMainThread);
		}
		else
		{
			m_defaultReadWriteListener = null;
		}
	}

	/**
	 * Set a listener here to be notified whenever this manager's native {@link BleManagerState} changes.
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
		startPeriodicScan(scanActiveTime, scanPauseTime, (ScanFilter)null, (DiscoveryListener)null);
	}

	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(com.idevicesinc.sweetblue.BleManager.DiscoveryListener)} for you too.
	 */
	public void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, DiscoveryListener discoveryListener)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, (ScanFilter)null, discoveryListener);
	}

	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but adds a filter too.
	 */
	public void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, BleManagerConfig.ScanFilter filter)
	{
		startPeriodicScan(scanActiveTime, scanPauseTime, filter, (DiscoveryListener)null);
	}

	/**
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(com.idevicesinc.sweetblue.BleManager.DiscoveryListener)} for you too and adds a filter.
	 */
	public void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, BleManagerConfig.ScanFilter filter, DiscoveryListener discoveryListener)
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
	 * Calls {@link #startScan(Interval, BleManagerConfig.ScanFilter)} with {@link Interval#INFINITE}.
	 */
	public void startScan(ScanFilter filter)
	{
		startScan(Interval.INFINITE, filter, (DiscoveryListener)null);
	}

	/**
	 * Same as {@link #startScan()} but also calls {@link #setListener_Discovery(com.idevicesinc.sweetblue.BleManager.DiscoveryListener)} for you.
	 */
	public void startScan(DiscoveryListener discoveryListener)
	{
		startScan(Interval.INFINITE, (ScanFilter)null, discoveryListener);
	}

	/**
	 * Overload of {@link #startScan(Interval, BleManagerConfig.ScanFilter, com.idevicesinc.sweetblue.BleManager.DiscoveryListener)}
	 */
	public void startScan(Interval scanTime, ScanFilter filter)
	{
		startScan(scanTime, filter, (DiscoveryListener)null);
	}

	/**
	 * Overload of {@link #startScan(Interval, BleManagerConfig.ScanFilter, com.idevicesinc.sweetblue.BleManager.DiscoveryListener)}
	 */
	public void startScan(Interval scanTime, DiscoveryListener discoveryListener)
	{
		startScan(scanTime, (ScanFilter)null, discoveryListener);
	}

	/**
	 * Same as {@link #startScan()} but also calls {@link #setListener_Discovery(com.idevicesinc.sweetblue.BleManager.DiscoveryListener)} for you.
	 */
	public void startScan(ScanFilter filter, DiscoveryListener discoveryListener)
	{
		startScan(Interval.INFINITE, filter, discoveryListener);
	}

	/**
	 * Starts a scan that will generally last for the given time (roughly).
	 */
	public void startScan(Interval scanTime)
	{
		startScan(scanTime, (ScanFilter)null, (DiscoveryListener) null);
	}

	/**
	 * Same as {@link #startScan(Interval)} but also calls {@link #setListener_Discovery(com.idevicesinc.sweetblue.BleManager.DiscoveryListener)} for you.
	 */
	public void startScan(Interval scanTime, ScanFilter filter, DiscoveryListener discoveryListener)
	{
		m_timeNotScanning = 0.0;
		scanTime = scanTime.secs() < 0.0 ? Interval.INFINITE : scanTime;

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
			scanTask.resetTimeout(scanTime.secs());
		}
		else
		{
			ASSERT(!m_taskQueue.isCurrentOrInQueue(P_Task_Scan.class, this));

			m_stateTracker.append(BleManagerState.SCANNING, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

			m_taskQueue.add(new P_Task_Scan(this, m_listeners.getScanTaskListener(), scanTime.secs()));
		}
	}

	/**
	 * Requires the {@link android.Manifest.permission#WAKE_LOCK} permission. Gives you access to the internal
	 * wake lock as a convenience and eventually calls {@link android.os.PowerManager.WakeLock#acquire()}.
	 *
	 * @see BleManagerConfig#manageCpuWakeLock
	 */
	@Advanced
	public void pushWakeLock()
	{
		m_wakeLockMngr.push();
	}

	/**
	 * Opposite of {@link #pushWakeLock()}, eventually calls {@link android.os.PowerManager.WakeLock#release()}.
	 */
	@Advanced
	public void popWakeLock()
	{
		m_wakeLockMngr.pop();
	}

	/**
	 * Fires a callback to {@link com.idevicesinc.sweetblue.BleManager.AssertListener} if condition is false. Will post a {@link android.util.Log#ERROR}-level
	 * message with a stack trace to the console as well if {@link BleManagerConfig#loggingEnabled} is true.
	 */
	@Advanced
	public boolean ASSERT(boolean condition)
	{
		return ASSERT(condition, "");
	}

	/**
	 * Same as {@link #ASSERT(boolean)} but with an added message.
	 */
	@Advanced
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
				AssertListener.AssertEvent info = new AssertListener.AssertEvent(this, message, dummyException.getStackTrace());
				m_assertionListener.onEvent(info);
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
	public int getStateMask()
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
	public int getNativeStateMask()
	{
		return m_nativeStateTracker.getState();
	}

	/**
	 * Enables BLE if manager is currently {@link BleManagerState#OFF} or {@link BleManagerState#TURNING_OFF}, otherwise does nothing.
	 * For a convenient way to ask your user first see {@link #turnOnWithIntent(android.app.Activity, int)}.
	 */
	public void turnOn()
	{
		if( isAny(TURNING_ON, ON) )  return;

		if( is(OFF) )
		{
			m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_ON, true, OFF, false);
		}

		m_taskQueue.add(new P_Task_TurnBleOn(this, /*implicit=*/false));
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
	public void reset()
	{
		reset(null);
	}

	/**
	 * Same as {@link #reset()} but with a convenience callback for when the reset is
	 * completed and the native BLE stack is (should be) back to normal.
	 *
	 * @see BleManagerState#RESETTING
	 */
	public void reset(ResetListener listener)
	{
		reset_synchronized(listener);
	}

	/**
	 * Removes bonds for all devices that are {@link BleDeviceState#BONDED}.
	 * Essentially a convenience method for calling {@link BleDevice#unbond()},
	 * on each device individually.
	 */
	public void unbondAll()
	{
		m_deviceMngr.unbondAll(null, Status.CANCELLED_FROM_UNBOND);
	}

	/**
	 * Convenience method to request your user to enable ble in a "standard" way
	 * with an {@link android.content.Intent} instead of using {@link #turnOn()} directly.
	 * Result will be posted as normal to {@link android.app.Activity#onActivityResult()}.
	 * If current state is {@link BleManagerState#ON} or {@link BleManagerState#TURNING_ON}
	 * this method early outs and does nothing.
	 */
	public void turnOnWithIntent(Activity callingActivity, int requestCode)
	{
		if( isAny(ON, TURNING_ON) )  return;

		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		callingActivity.startActivityForResult(enableBtIntent, requestCode);
	}

	/**
	 * Opposite of {@link #onPause()}, to be called from your override of {@link android.app.Activity#onResume()} for each {@link android.app.Activity}
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
	 * It's generally recommended to call this in your override of {@link android.app.Activity#onPause()} for each {@link android.app.Activity}
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
			stopScan_private(E_Intent.UNINTENTIONAL);
		}
	}

	/**
	 * Call this from your app's {@link android.app.Activity#onDestroy()} method.
	 * NOTE: Apparently no good way to know when app as a whole is being destroyed
	 * and not individual Activities, so keeping this package-private for now.
	 */
	void onDestroy()
	{
		m_wakeLockMngr.clear();
		m_listeners.onDestroy();
	}

	/**
	 * Returns the {@link android.app.Application} provided to the constructor.
	 */
	public Context getApplicationContext()
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

		stopScan_private(E_Intent.INTENTIONAL);
	}

	/**
	 * Same as {@link #stopScan()} but also unregisters any filter supplied to various overloads of
	 * {@link #startScan()} or {@link #startPeriodicScan(Interval, Interval)} that take an {@link BleManagerConfig.ScanFilter}.
	 * Calling {@link #stopScan()} alone will keep any previously registered filters active.
	 */
	public void stopScan(ScanFilter filter)
	{
		m_filterMngr.remove(filter);

		stopScan();
	}

	private void stopScan_private(E_Intent intent)
	{
		m_timeNotScanning = 0.0;

		if( !m_taskQueue.succeed(P_Task_Scan.class, this) )
		{
			m_taskQueue.clearQueueOf(P_Task_Scan.class, this);
		}

		m_stateTracker.remove(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	/**
	 * Gets a known {@link BleDeviceState#DISCOVERED} device by MAC address, or {@link BleDevice#NULL} if there is no such device.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getDevice(String macAddress)
	{
		BleDevice device = m_deviceMngr.get(macAddress);
		
		if( device != null )  return device;
		
		return BleDevice.NULL;
	}

	/**
	 * Shortcut for checking if {@link #getDevice(String)} returns {@link BleDevice#NULL}.
	 */
	public boolean hasDevice(String macAddress)
	{
		return !getDevice(macAddress).isNull();
	}

	/**
	 * Might not be useful to outside world. Used for sanity/early-out checks internally. Keeping private for now.
	 * Does referential equality check.
	 */
	private boolean hasDevice(BleDevice device)
	{
		return m_deviceMngr.has(device);
	}

	/**
	 * Returns the first device that is in the given state, or {@link BleDevice#NULL} if no match is found.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getDevice(BleDeviceState state)
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
	public boolean hasDevice(BleDeviceState state)
	{
		return !getDevice(state).isNull();
	}

	/**
	 * Returns the first device that matches the query, or {@link BleDevice#NULL} if no match is found.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getDevice(Object ... query)
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
	public boolean hasDevice(Object ... query)
	{
		return !getDevice(query).isNull();
	}
	
	/**
	 * Returns the first device which returns <code>true</code> for {@link BleDevice#isAny(int)}, or {@link BleDevice#NULL} if no such device is found.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getDevice(final int mask_BleDeviceState)
	{
		return m_deviceMngr.getDevice(mask_BleDeviceState);
	}
	
	/**
	 * Returns <code>true</code> if there is any {@link BleDevice} for which {@link BleDevice#isAny(int)} with the given mask returns <code>true</code>.
	 */
	public boolean hasDevice(final int mask_BleDeviceState)
	{
		return !getDevice(mask_BleDeviceState).isNull();
	}

	/**
	 * Returns all the devices managed by this class. This generally includes all devices that are either.
	 * {@link BleDeviceState#ADVERTISING} or {@link BleDeviceState#CONNECTED}.
	 */
	public @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices()
	{
		return new BleDeviceIterator(getDevices_List());
	}
	
	/**
	 * Overload of {@link #getDevices()} that returns a {@link java.util.List} for you.
	 */
	public @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List()
	{
		return (List<BleDevice>) m_deviceMngr.getList().clone();
	}

	/**
	 * Returns the total number of devices this manager is...managing.
	 * This includes all devices that are {@link BleDeviceState#DISCOVERED}.
	 */
	public int getDeviceCount()
	{
		return m_deviceMngr.getCount();
	}

	/**
	 * Returns the number of devices that are in the current state.
	 */
	public int getDeviceCount(BleDeviceState state)
	{
		return m_deviceMngr.getCount(state);
	}

	/**
	 * Returns the number of devices that match the given query.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public int getDeviceCount(Object ... query)
	{
		return m_deviceMngr.getCount(query);
	}

	/**
	 * Accessor into the underlying array used to store {@link BleDevice} instances.
	 * Combine with {@link #getDeviceCount()} to iterate, or you may want to use the
	 * {@link java.util.Iterator} returned from {@link #getDevices()} and its various overloads instead.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getDeviceAt(final int index)
	{
		return m_deviceMngr.get(index);
	}

	/**
	 * Returns whether we have any devices. For example if you have never called {@link #startScan()}
	 * or {@link #newDevice(String)} (or similar) then this will return false.
	 */
	public boolean hasDevices()
	{
		return m_deviceMngr.getCount() > 0;
	}

	/**
	 * Same as {@link #getDevice(BleDeviceState)} except returns all matching devices.
	 */
	public @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final BleDeviceState state)
	{
		return new BleDeviceIterator(getDevices_List(), state, true);
	}
	
	/**
	 * Overload of {@link #getDevices(BleDeviceState)} that returns a {@link java.util.List} for you.
	 */
	public @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final BleDeviceState state)
	{
		return m_deviceMngr.getDevices_List(state);
	}

	/**
	 * Same as {@link #getDevice(Object...)} except returns all matching devices.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final Object ... query)
	{
		return new BleDeviceIterator(getDevices_List(), query);
	}
	
	/**
	 * Overload of {@link #getDevices(Object...)} that returns a {@link java.util.List} for you.
	 */
	public @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final Object ... query)
	{
		return m_deviceMngr.getDevices_List(query);
	}
	
	/**
	 * Same as {@link #getDevices()} except filters using {@link BleDevice#isAny(int)}.
	 */
	public @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final int mask_BleDeviceState)
	{
		return new BleDeviceIterator(getDevices_List(), mask_BleDeviceState);
	}
	
	/**
	 * Overload of {@link #getDevices(int)} that returns a {@link java.util.List} for you.
	 */
	public @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final int mask_BleDeviceState)
	{
		return m_deviceMngr.getDevices_List(mask_BleDeviceState);
	}

	/**
	 * Same as {@link #newDevice(String, String, BleDeviceConfig)} but uses an empty string for the name
	 * and passes a <code>null</code> {@link BleDeviceConfig}, which results in inherited options from {@link BleManagerConfig}.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice newDevice(String macAddress)
	{
		return newDevice(macAddress, null, null);
	}
	
	/**
	 * Same as {@link #newDevice(String)} but allows a custom name also.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final String name)
	{
		return newDevice(macAddress, name, null);
	}
	
	/**
	 * Same as {@link #newDevice(String)} but passes a {@link BleDeviceConfig} to be used as well.
	 */
	
	public @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final BleDeviceConfig config)
	{
		return newDevice(macAddress, "", config);
	}

	/**
	 * Creates a new {@link BleDevice} or returns an existing one if the macAddress matches.
	 * {@link com.idevicesinc.sweetblue.BleManager.DiscoveryListener#onEvent(DiscoveryEvent)} will be called if a new device
	 * is created.
	 * <br><br>
	 * NOTE: You should always do a {@link BleDevice#isNull()} check on this method's return value just in case. Android
	 * documentation says that underlying stack will always return a valid {@link android.bluetooth.BluetoothDevice}
	 * instance (which is required to create a valid {@link BleDevice} instance), but you really never know.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final String name, final BleDeviceConfig config)
	{
		final BleDevice existingDevice = this.getDevice(macAddress);

		if( !existingDevice.isNull() )
		{
			if( config != null )
			{
				existingDevice.setConfig(config);
			}
			
			return existingDevice;
		}

		final BluetoothDevice device_native = newNativeDevice(macAddress);

		if( device_native == null ) //--- DRK > API says this should never happen...not trusting it!
		{
			return BleDevice.NULL;
		}

		final String name_normalized = Utils.normalizeDeviceName(name);

		final BleDevice newDevice = newDevice_private(device_native, name_normalized, name, BleDeviceOrigin.EXPLICIT, config);
		
		onDiscovered_wrapItUp(newDevice, /*newlyDiscovered=*/true, null, null, 0, BleDeviceOrigin.EXPLICIT);

		return newDevice;
	}
	
	BluetoothDevice newNativeDevice(final String macAddress)
	{
		return getNative().getAdapter().getRemoteDevice(macAddress);
	}

	/**
	 * Forcefully undiscovers a device, disconnecting it first if needed and removing it from this manager's internal list.
	 * {@link com.idevicesinc.sweetblue.BleManager.DiscoveryListener#onEvent(DiscoveryEvent)} with {@link LifeCycle#UNDISCOVERED} will be called.
	 * No clear use case has been thought of but the method is here just in case anyway.
	 *
	 * @return	<code>true</code> if the device was undiscovered, <code>false</code> if device is already {@link BleDeviceState#UNDISCOVERED} or manager
	 * 			doesn't contain an instance, checked referentially, not through {@link BleDevice#equals(BleDevice)} (i.e. by mac address).
	 */
	public boolean undiscover(final BleDevice device)
	{
		if( device == null )							return false;
		if( device.isNull() )							return false;
		if( !hasDevice(device) )						return false;
		if( device.is(BleDeviceState.UNDISCOVERED) )	return false;

		if( device.is(BleDeviceState.CONNECTED) )
		{
			device.disconnect();
		}

		m_deviceMngr.undiscoverAndRemove(device, m_discoveryListener, m_deviceMngr_cache, E_Intent.INTENTIONAL);

		return true;
	}

	//--- DRK > Smooshing together a bunch of package-private accessors here.
	P_BleStateTracker			getStateTracker(){				return m_stateTracker;				}
	P_NativeBleStateTracker		getNativeStateTracker(){		return m_nativeStateTracker;		}
	UpdateLoop					getUpdateLoop(){				return m_updateLoop;				}
	P_BluetoothCrashResolver	getCrashResolver(){				return m_crashResolver;				}
	P_TaskQueue					getTaskQueue(){					return m_taskQueue;					}
	P_Logger					getLogger(){					return m_logger;					}


	private void turnOff(final boolean removeAllBonds)
	{
		turnOff_synchronized(removeAllBonds);
	}

	private void turnOff_synchronized(final boolean removeAllBonds)
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

		P_Task_TurnBleOff task = new P_Task_TurnBleOff(this, /*implicit=*/false, new PA_Task.I_StateListener()
		{
			@Override
			public void onStateChange(PA_Task taskClass, PE_TaskState state)
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

		m_taskQueue.add(task);
	}

	private void reset_synchronized(final ResetListener listener)
	{
		if( listener != null )
		{
			if( m_resetListeners != null )
			{
				m_resetListeners.addListener(listener);
			}
			else
			{
				m_resetListeners = new P_WrappingResetListener(listener, m_mainThreadHandler, m_config.postCallbacksToMainThread);
			}
		}

		if( is(BleManagerState.RESETTING) )
		{
			return;
		}

		m_stateTracker.append(RESETTING, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		if( m_config.enableCrashResolverForReset )
		{
			m_taskQueue.add(new P_Task_CrashResolver(BleManager.this, m_crashResolver));
		}

		turnOff(/*removeAllBonds=*/true);

		m_taskQueue.add(new P_Task_TurnBleOn(this, /*implicit=*/false, new PA_Task.I_StateListener()
		{
			@Override public void onStateChange(PA_Task taskClass, PE_TaskState state)
			{
				if( state.isEndingState() )
				{
					ResetListener nukeListeners = m_resetListeners;
					m_resetListeners = null;
					m_nativeStateTracker.remove(RESETTING, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
					m_stateTracker.remove(RESETTING, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

					if( nukeListeners != null )
					{
						ResetEvent event = new ResetEvent(BleManager.this, ResetListener.Progress.COMPLETED);
						nukeListeners.onEvent(event);
					}
				}
			}
		}));
	}

	void startAutoUpdate(final double updateRate)
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

	P_Task_Scan.E_Mode startNativeScan(final E_Intent intent)
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
					//---
					//---		UPDATE: It's been observed through simple test apps that when restarting an app through eclipse,
					//---		Android somehow, sometimes, keeps the same actual BleManager instance in memory, so it's not 
					//---		far-fetched to assume that the scan from the previous app run can sometimes still be ongoing.
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
					m_nativeStateTracker.append(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

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

			m_nativeStateTracker.append(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

			return E_Mode.BLE;
		}

		return null;
	}

	void onDiscovered(final BluetoothDevice device_native, final int rssi, final byte[] scanRecord_nullable)
	{
		onDiscovered_synchronized(device_native, rssi, scanRecord_nullable);
	}

	private void onDiscovered_synchronized(final BluetoothDevice device_native, final int rssi, final byte[] scanRecord_nullable)
	{
		//--- DRK > Protects against fringe case where scan task is executing and app calls turnOff().
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
		
		final Please please;

		if( device == null )
		{
			normalizedDeviceName = Utils.normalizeDeviceName(rawDeviceName);
	    	services_nullable = Utils_ScanRecord.parseServiceUuids(scanRecord_nullable);
	    	byte[] scanRecord = scanRecord_nullable != null ? scanRecord_nullable : BleDevice.EMPTY_BYTE_ARRAY;
	    	String deviceName = rawDeviceName;
	    	deviceName = deviceName != null ? deviceName : "";
	    	boolean hitDisk = BleDeviceConfig.boolOrDefault(m_config.manageLastDisconnectOnDisk);
	    	State.ChangeIntent lastDisconnectIntent = m_diskOptionsMngr.loadLastDisconnect(macAddress, hitDisk);
	    	please = m_filterMngr.allow(m_logger, device_native, services_nullable, deviceName, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent);

	    	if( please != null && !please.ack() )  return;
		}
		else
		{
			please = null;
		}

    	boolean newlyDiscovered = false;

    	if ( device == null )
    	{
    		final BleDeviceConfig config_nullable = please != null ? please.getConfig() : null;
    		device = newDevice_private(device_native, normalizedDeviceName, device_native.getName(), BleDeviceOrigin.FROM_DISCOVERY, config_nullable);
    		newlyDiscovered = true;
    	}

    	onDiscovered_wrapItUp(device, newlyDiscovered, services_nullable, scanRecord_nullable, rssi, BleDeviceOrigin.FROM_DISCOVERY);
	}

	private BleDevice newDevice_private(final BluetoothDevice device_native, final String name_normalized, final String name_native, final BleDeviceOrigin origin, final BleDeviceConfig config_nullable)
	{
		final boolean hitCache = true; // TODO: for now always true...should it be behind a config option?
		
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
	
	void onDiscovered_fromRogueAutoConnect(final BleDevice device, final boolean newlyDiscovered, final List<UUID> services_nullable, final byte[] scanRecord_nullable, final int rssi)
	{
		if( !m_deviceMngr.has(device) ) // DRK > as of now checked upstream also, so just being anal
		{
			m_deviceMngr.add(device);
		}
		
		onDiscovered_wrapItUp(device, newlyDiscovered, services_nullable, scanRecord_nullable, rssi, BleDeviceOrigin.FROM_DISCOVERY);
	}

    private void onDiscovered_wrapItUp(final BleDevice device, final boolean newlyDiscovered, final List<UUID> services_nullable, final byte[] scanRecord_nullable, final int rssi, final BleDeviceOrigin origin)
    {
    	if( newlyDiscovered )
    	{
    		device.onNewlyDiscovered(services_nullable, rssi, scanRecord_nullable, origin);

    		if( m_discoveryListener != null )
    		{
    			DiscoveryEvent event = new DiscoveryEvent(this, device, LifeCycle.DISCOVERED);
    			m_discoveryListener.onEvent(event);
    		}
    	}
    	else
    	{
    		device.onRediscovered(services_nullable, rssi, scanRecord_nullable, BleDeviceOrigin.FROM_DISCOVERY);

    		if( m_discoveryListener != null )
    		{
    			DiscoveryEvent event = new DiscoveryEvent(this, device, LifeCycle.REDISCOVERED);
    			m_discoveryListener.onEvent(event);
    		}
    	}
    }

	void stopNativeScan(final P_Task_Scan scanTask)
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

		m_nativeStateTracker.remove(BleManagerState.SCANNING, scanTask.isExplicit() ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	void clearScanningRelatedMembers(final E_Intent intent)
	{
//		m_filterMngr.clear();

		m_timeNotScanning = 0.0;

		m_stateTracker.remove(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	void tryPurgingStaleDevices(final double scanTime)
	{
		m_deviceMngr.purgeStaleDevices(scanTime, m_deviceMngr_cache, m_discoveryListener);
	}

	/**
	 * This method is made public in case you want to tie the library in to an update loop
	 * from another codebase. Generally you should leave {@link BleManagerConfig#autoUpdateRate}
	 * alone and let the library handle the calling of this method.
	 */
	@Advanced
	public void update(final double timeStep_seconds)
	{
		update_synchronized(timeStep_seconds);
	}

	private void update_synchronized(final double timeStep)
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
			if( m_isForegrounded && Interval.isEnabled(m_config.autoScanDelayAfterResume) && !m_triedToStartScanAfterResume && m_timeForegrounded >= Interval.secs(m_config.autoScanDelayAfterResume) )
			{
				m_triedToStartScanAfterResume = true;

				if( !is(SCANNING) )
				{
					startScan = true;
				}
			}
			else if( !is(SCANNING) )
			{
				double scanInterval = Interval.secs(m_isForegrounded ? m_config.autoScanInterval : m_config.autoScanIntervalWhileAppIsPaused);

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
		return m_config.autoScanDuringOta || !m_deviceMngr.hasDevice(BleDeviceState.PERFORMING_OTA);
	}

	void uhOh(UhOh reason)
	{
//		if( reason == UhOh.UNKNOWN_CONNECTION_ERROR )
//		{
//			m_connectionFailTracker = 0;
//		}

		m_uhOhThrottler.uhOh(reason);
	}

	@Override public String toString()
	{
		return m_stateTracker.toString();
	}
}
