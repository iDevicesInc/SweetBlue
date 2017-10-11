package com.idevicesinc.sweetblue;

import java.util.UUID;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import com.idevicesinc.sweetblue.BleDevice.BondListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Extendable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;
import com.idevicesinc.sweetblue.utils.*;

import org.json.JSONObject;

/**
 * Provides a number of options to (optionally) pass to {@link BleDevice#setConfig(BleDeviceConfig)}.
 * This class is also a super class of {@link BleManagerConfig}, which you can pass
 * to {@link BleManager#get(Context, BleManagerConfig)} or {@link BleManager#setConfig(BleManagerConfig)} to set default base options for all devices at once.
 * For all options in this class, you may set the value to <code>null</code> when passed to {@link BleDevice#setConfig(BleDeviceConfig)}
 * and the value will then be inherited from the {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}.
 * Otherwise, if the value is not <code>null</code> it will override any option in the {@link BleManagerConfig}.
 * If an option is ultimately <code>null</code> (<code>null</code> when passed to {@link BleDevice#setConfig(BleDeviceConfig)}
 * *and* {@link BleManager#get(Context, BleManagerConfig)}) then it is interpreted as <code>false</code> or {@link Interval#DISABLED}.
 * <br><br>
 * TIP: You can use {@link Interval#DISABLED} instead of <code>null</code> to disable any keepalive-based options, for code readability's sake.
 * <br><br>
 * TIP: You can use {@link #newNulled()} (or {@link #nullOut()}) then only set the few options you want for {@link BleDevice#setConfig(BleDeviceConfig)}.
 */
@Extendable
public class BleDeviceConfig extends BleNodeConfig implements Cloneable
{
	/**
	 * Default value for {@link #minScanTimeNeededForUndiscovery}.
	 */
	public static final double DEFAULT_MINIMUM_SCAN_TIME		= 5.0;

	/**
	 * Default value for {@link #nForAverageRunningReadTime} and {@link #nForAverageRunningWriteTime}.
	 */
	public static final int DEFAULT_RUNNING_AVERAGE_N			= 10;

	/**
	 * This is a good default value for {@link #undiscoveryKeepAlive}. By default {@link #undiscoveryKeepAlive} is {@link Interval#DISABLED}.
	 */
	public static final double DEFAULT_SCAN_KEEP_ALIVE			= DEFAULT_MINIMUM_SCAN_TIME*2.5;
	
	/**
	 * Default value for {@link #rssiAutoPollRate}.
	 */
	public static final double DEFAULT_RSSI_AUTO_POLL_RATE		= 10.0;
	
	/**
	 * Default fallback value for {@link #rssi_min}.
	 */
	public static final int DEFAULT_RSSI_MIN					= -120;
	
	/**
	 * Default fallback value for {@link #rssi_max}.
	 */
	public static final int DEFAULT_RSSI_MAX					= -30;

	/**
	 * Default value for {@link #defaultTxPower}.
	 */
	public static final int DEFAULT_TX_POWER					= -50;

	/**
	 * The default value of {@link #maxConnectionFailHistorySize}, the size of the list that keeps track of a {@link BleNode}'s connection failure history.
	 * This is to prevent the list from growing too large, if the device is unable to connect, and you have a large long term reconnect time set
	 * with {@link #reconnectFilter}.
	 */
	public static final int DEFAULT_MAX_CONNECTION_FAIL_HISTORY_SIZE	= 25;

	/**
	 * This only applies when {@link #useGattRefresh} is <code>true</code>. This is the default amount of time to delay after
	 * refreshing the gatt database before actually performing the discover services operation. It has been observed that this delay
	 * alleviates some instability when {@link #useGattRefresh} is <code>true</code>.
	 */
	public static final int DEFAULT_GATT_REFRESH_DELAY		= 500;

	/**
	 * The default value used for {@link BondRetryFilter.DefaultBondRetryFilter}. Bond retries only apply when calling {@link BleDevice#bond()}, or {@link BleDevice#bond(BondListener)}.
	 * Like connecting, sometimes in order to get bonding to work, you just have to try multiple times. If you require bonding for the device you're connecting
	 * to, it's recommended to use one of the bond methods.
	 */
	public static final int DEFAULT_MAX_BOND_RETRIES = 3;



	/**
	 * Default is <code>false</code>. If the bluetooth device you are trying to connect to requires a pairing dialog to show up, you should
	 * set this to <code>true</code>. Android will do one of two things when you try to pair to the device. It will either A) show the pairing dialog, or
	 * B) show a notification in the notification area. When B happens, most people probably won't notice it, and think your app can't connect to the device.
	 * This uses an ugly hack to get the dialog to always display...it starts a CLASSIC bluetooth scan for a second, then stops it, and starts the bond. As crazy
	 * as it sounds, it works. Note that no devices will be discovered during this one second scan.
	 */
	public boolean forceBondDialog								= false;

	/**
	 * Default is {@link Interval#ONE_SEC}. This setting only applies if {@link #forceBondDialog} is <code>true</code>. This sets the amount of time to run the classic
	 * scan for before attempting to bond. If this is set to {@link Interval#DISABLED}, or is <code>null</code>, and {@link #forceBondDialog} is set to <code>true</code>,
	 * then the default value will be used.
	 *
	 * @see #forceBondDialog
	 */
	public Interval forceBondHackInterval						= Interval.ONE_SEC;

	/**
	 * Default is {@link #DEFAULT_GATT_REFRESH_DELAY}. This only applies when {@link #useGattRefresh} is <code>true</code>. This is the amount of time to delay after
	 * refreshing the gatt database before actually performing the discover services operation. It has been observed that this delay
	 * alleviates some instability when {@link #useGattRefresh} is <code>true</code>.
	 */
	public Interval gattRefreshDelay							= Interval.millis(DEFAULT_GATT_REFRESH_DELAY);

	/**
	 * Default is {@link Interval#DISABLED}. This option adds a delay between establishing a BLE connection, and service discovery, if {@link #autoGetServices} is
	 * <code>true</code>. This value will be ignored if {@link #useGattRefresh} is <code>true</code>, as the library will use {@link #gattRefreshDelay} instead.
	 */
	public Interval serviceDiscoveryDelay						= Interval.DISABLED;
	
	/**
	 * Default is <code>true</code> - some devices can only reliably become {@link BleDeviceState#BONDED} while {@link BleDeviceState#DISCONNECTED},
	 * so this option controls whether the library will internally change any bonding flow dictated by {@link #bondFilter} when a bond fails and try
	 * to bond again the next time the device is {@link BleDeviceState#DISCONNECTED}.
	 * <br><br>
	 * NOTE: This option was added after noticing this behavior with the Samsung Tab 4 running 4.4.4.
	 */
	@Nullable(Prevalence.NORMAL)
	public Boolean tryBondingWhileDisconnected					= true;
	
	/**
	 * Default is <code>true</code> - controls whether any bonding issues worked around if {@link #tryBondingWhileDisconnected} is <code>true</code> are remembered on disk
	 * (through {@link SharedPreferences}) so that bonding is as stable as possible across application sessions. 
	 */
	@Nullable(Prevalence.NORMAL)
	public Boolean tryBondingWhileDisconnected_manageOnDisk		= true;

	/**
	 * Default is <code>false</code> - Controls whether SweetBlue will automatically bond when connecting to a peripheral (rather than letting Android do it itself).
	 * If the device is already bonded, this will do nothing. In most cases, it's best to bond <i>before</i> connecting, but there are rare devices which work better
	 * to bond <i>after</i> becoming connected. To adjust this behavior, adjust {@link #tryBondingWhileDisconnected} (if it's <code>true</code>, then the bond will happen
	 * before connecting, otherwise it will happen after).
	 */
	@Advanced
	public boolean alwaysBondOnConnect							= false;

	/**
	 * Default is <code>true</code> - controls whether changes to a device's name through {@link BleDevice#setName(String)} are remembered on disk through
	 * {@link SharedPreferences}. If true, this means calls to {@link com.idevicesinc.sweetblue.BleDevice#getName_override()} will return the same thing
	 * even across app restarts.
	 */
	@Nullable(Prevalence.NORMAL)
	public Boolean saveNameChangesToDisk						= true;
	
	/**
	 * Default is <code>true</code> - whether to automatically get services immediately after a {@link BleDevice} is
	 * {@link BleDeviceState#CONNECTED}. Currently this is the only way to get a device's services.
	 */
	@Nullable(Prevalence.NORMAL)
	public Boolean autoGetServices								= true;

	/**
	 * Default is <code>true</code> - whether to automatically enable notifications that were enabled via a call to any of the enableNotify() methods
	 * in {@link BleDevice} upon device reconnection. Basically, if you enable notifications in an {@link com.idevicesinc.sweetblue.BleTransaction.Init} transaction,
	 * then set this to <code>false</code>, as the transaction will run on reconnection.
	 */
	public boolean autoEnableNotifiesOnReconnect				= true;

	/**
	 * Default is <code>true</code> - whether to automatically renegotiate the MTU size that was set via {@link BleDevice#setMtu(int, ReadWriteListener)}, or
	 * {@link BleDevice#setMtu(int)}. If you use either of those methods in a {@link com.idevicesinc.sweetblue.BleTransaction.Init} transaction, you should set
	 * this to <code>false</code>, as the transaction will run on reconnection.
	 */
	public boolean autoNegotiateMtuOnReconnect					= true;
	
	/**
	 * Default is <code>false</code> - if <code>true</code> and you call {@link BleDevice#startPoll(UUID, Interval, BleDevice.ReadWriteListener)}
	 * or {@link BleDevice#startChangeTrackingPoll(UUID, Interval, BleDevice.ReadWriteListener)()} with identical
	 * parameters then two identical polls would run which would probably be wasteful and unintentional.
	 * This option provides a defense against that situation.
	 */
	@Nullable(Prevalence.NORMAL)
	public Boolean allowDuplicatePollEntries					= false;
	
	/**
	 * Default is <code>false</code> - {@link BleDevice#getAverageReadTime()} and {@link BleDevice#getAverageWriteTime()} can be 
	 * skewed if the peripheral you are connecting to adjusts its maximum throughput for OTA firmware updates and the like.
	 * Use this option to let the library know whether you want read/writes to factor in while {@link BleDeviceState#PERFORMING_OTA}.
	 * 
	 * @see BleDevice#getAverageReadTime()
	 * @see BleDevice#getAverageWriteTime() 
	 */
	@Nullable(Prevalence.NORMAL)
	public Boolean includeOtaReadWriteTimesInAverage			= false;
	
	/**
	 * Default is <code>true</code> - controls whether {@link BleManager} will keep a device in active memory when it goes {@link BleManagerState#OFF}.
	 * If <code>false</code> then a device will be purged and you'll have to do {@link BleManager#startScan()} again to discover devices
	 * if/when {@link BleManager} goes back {@link BleManagerState#ON}.
	 * <br><br>
	 * NOTE: if this flag is true for {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)} then this
	 * applies to all devices.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Boolean retainDeviceWhenBleTurnsOff					= true;
	
	/**
	 * Default is <code>true</code> - only applicable if {@link #retainDeviceWhenBleTurnsOff} is also true. If {@link #retainDeviceWhenBleTurnsOff}
	 * is false then devices will be undiscovered when {@link BleManager} goes {@link BleManagerState#OFF} regardless.
	 * <br><br>
	 * NOTE: See NOTE for {@link #retainDeviceWhenBleTurnsOff} for how this applies to {@link BleManagerConfig}. 
	 * 
	 * @see #retainDeviceWhenBleTurnsOff
	 * @see #autoReconnectDeviceWhenBleTurnsBackOn
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Boolean undiscoverDeviceWhenBleTurnsOff				= true;
	
	/**
	 * Default is <code>true</code> - if devices are kept in memory for a {@link BleManager#turnOff()}/{@link BleManager#turnOn()} cycle
	 * (or a {@link BleManager#reset()}) because {@link #retainDeviceWhenBleTurnsOff} is <code>true</code>, then a {@link BleDevice#connect()}
	 * will be attempted for any devices that were previously {@link BleDeviceState#CONNECTED}.
	 * <br><br>
	 * NOTE: See NOTE for {@link #retainDeviceWhenBleTurnsOff} for how this applies to {@link BleManagerConfig}.
	 * 
	 * @see #retainDeviceWhenBleTurnsOff
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Boolean autoReconnectDeviceWhenBleTurnsBackOn 		= true;
	
	/**
	 * Default is <code>true</code> - controls whether the {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent} behind a device going {@link BleDeviceState#DISCONNECTED}
	 * is saved to and loaded from disk so that it can be restored across app sessions, undiscoveries, and BLE
	 * {@link BleManagerState#OFF}->{@link BleManagerState#ON} cycles. This uses Android's {@link SharedPreferences} so does not require
	 * any extra permissions. The main advantage of this is the following scenario: User connects to a device through your app,
	 * does what they want, kills the app, then opens the app sometime later. {@link BleDevice#getLastDisconnectIntent()} returns
	 * {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL}, which lets you know that you can probably automatically connect to this device without user confirmation.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Boolean manageLastDisconnectOnDisk					= true;
	
	/**
	 * Default is <code>true</code> - controls whether a {@link BleDevice} is placed into an in-memory cache when it becomes {@link BleDeviceState#UNDISCOVERED}.
	 * If <code>true</code>, subsequent calls to {@link BleManager.DiscoveryListener#onEvent(BleManager.DiscoveryListener.DiscoveryEvent)} with
	 * {@link LifeCycle#DISCOVERED} (or calls to {@link BleManager#newDevice(String)}) will return the cached {@link BleDevice} instead of creating a new one.
	 * <br><br>
	 * The advantages of caching are:<br>
	 * <ul>
	 * <li>Slightly better performance at the cost of some retained memory, especially in situations where you're frequently discovering and undiscovering many devices.
	 * <li>Resistance to future stack failures that would otherwise mean missing data like {@link BleDevice#getAdvertisedServices()} for future discovery events.
	 * <li>More resistant to potential "user error" of retaining devices in app-land after BleManager undiscovery.
	 * <ul><br>
	 * This is kept as an option in case there's some unforeseen problem with devices being cached for a certain application.
	 * 
	 * See also {@link #minScanTimeNeededForUndiscovery}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Boolean cacheDeviceOnUndiscovery						= true;
	
	/**
	 * Default is <code>true</code> - controls whether {@link BleDevice.ConnectionFailListener.Status#BONDING_FAILED} is capable of
	 * inducing {@link ConnectionFailListener#onEvent(com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.ConnectionFailEvent)}
	 * while a device is {@link BleDeviceState#CONNECTING_OVERALL}.
	 */
	@Nullable(Prevalence.NORMAL)
	public Boolean bondingFailFailsConnection					= true;
	
	/**
	 * Default is <code>false</code> - whether to use <code>BluetoothGatt.refresh()</code> right before service discovery.
	 * This method is not in the public Android API, so its use is disabled by default. You may find it useful to enable
	 * if your remote device is routinely changing its gatt service profile. This method call supposedly clears a cache
	 * that would otherwise prevent changes from being discovered.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Boolean useGattRefresh								= false;

	/**
	 * Default is {@link RefreshOption#BEFORE_SERVICE_DISCOVERY} - This determines when SweetBlue will refresh the gatt database.
	 * This only applies if you have set {@link #useGattRefresh} to <code>true</code>.
	 */
	public RefreshOption gattRefreshOption						= RefreshOption.BEFORE_SERVICE_DISCOVERY;


	/**
	 * Default is <code>null</code> - whether SweetBlue should retry a connect <i>after</i> successfully connecting via
	 * BLE. This means that if discovering services, or {@link com.idevicesinc.sweetblue.BleTransaction.Init}, or {@link com.idevicesinc.sweetblue.BleTransaction.Auth}
	 * fail for any reason, SweetBlue will disconnect, then retry the connection.
	 * The default is <code>null</code> so that it pulls the default from {@link BleManagerConfig}, unless you need to specify a particular
	 * device which should behave differently.
	 */
	public Boolean connectFailRetryConnectingOverall			= null;


	/**
	 * The below explanation is wrong, only in that the default is now <code>false</code>. This is for backwards
	 * compatibility, as a customer noted bonding not working after this change. This will most likely go back to being
	 * <code>true</code> when version 3 comes out.
	 *
	 * Default is <code>true</code> - The normal way to bond in the native API is to use {@link BluetoothDevice#createBond()}.
	 * There is however also a overload method that's made invisible using the "hide" annotation that takes an int
	 * representing the desired transport mode. The default for {@link BluetoothDevice#createBond()} is {@link BluetoothDevice#TRANSPORT_AUTO}.
	 * You can look at the source to see that this is the case. The thing is, you *never* want the Android stack to automatically decide something.
	 * So if you set <code>useLeTransportForBonding</code> to true then SweetBlue will use the "private" overloaded method with
	 * {@link BluetoothDevice#TRANSPORT_LE}. This workaround anecdotally fixed bonding issues with LG G4 and Samsung S6 phones.
	 * Anecdotally because the public {@link BluetoothDevice#createBond()} was not working, tried the private one, it worked,
	 * but then the public {@link BluetoothDevice#createBond()} also worked flawlessly after that.
	 * But again, regardless, you should always choose explicit behavior over automatic when dealing with Android BLE.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Boolean useLeTransportForBonding						= false;

	/**
	 * Default is {@link BondRetryFilter.DefaultBondRetryFilter} - This allows to you implement your own logic on whether or not SweetBlue should
	 * retry a failed bond.
	 */
	@Advanced
	public BondRetryFilter bondRetryFilter						= new BondRetryFilter.DefaultBondRetryFilter();

	/**
	 * Default is <code>true</code> - By default SweetBlue will force a bond/unbond for certain phones (mostly Sony, Motorola) because it has been found to
	 * improve connection rates with them, see {@link BondFilter} docs. This option is here in the case you don't want this behavior (for instance, the BLE
	 * device you're connecting to needs a pairing dialog to come up). However, you should use this at your own risk because it may make further connections
	 * to the device less reliable.
	 */
	@Advanced
	public Boolean autoBondFixes								= true;
	
	/**
	 * Default is {@link #DEFAULT_MINIMUM_SCAN_TIME} seconds - Undiscovery of devices must be
	 * approximated by checking when the last time was that we discovered a device,
	 * and if this time is greater than {@link #undiscoveryKeepAlive} then the device is undiscovered. However a scan
	 * operation must be allowed a certain amount of time to make sure it discovers all nearby devices that are
	 * still advertising. This is that time in seconds.
	 * <br><br>
	 * Use {@link Interval#DISABLED} to disable undiscovery altogether.
	 * 
	 * @see BleManager.DiscoveryListener#onEvent(DiscoveryEvent)
	 * @see #undiscoveryKeepAlive
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval	minScanTimeNeededForUndiscovery				= Interval.secs(DEFAULT_MINIMUM_SCAN_TIME);
	
	/**
	 * Default is disabled - If a device exceeds this amount of time since its
	 * last discovery then it is a candidate for being undiscovered.
	 * The default for this option attempts to accommodate the worst Android phones (BLE-wise), which may make it seem
	 * like it takes a long time to undiscover a device. You may want to configure this number based on the phone or
	 * manufacturer. For example, based on testing, in order to make undiscovery snappier the Galaxy S5 could use lower times.
	 * <br><br>
	 * Use {@link Interval#DISABLED} to disable undiscovery altogether.
	 * 
	 * @see BleManager.DiscoveryListener#onEvent(DiscoveryEvent)
	 * @see #minScanTimeNeededForUndiscovery
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval	undiscoveryKeepAlive						= Interval.DISABLED;
	
	/**
	 * Default is {@link #DEFAULT_RSSI_AUTO_POLL_RATE} - The rate at which a {@link BleDevice} will automatically poll for its {@link BleDevice#getRssi()} value
	 * after it's {@link BleDeviceState#CONNECTED}. You may also use {@link BleDevice#startRssiPoll(Interval, ReadWriteListener)} for more control and feedback.
	 */
	@Nullable(Prevalence.NORMAL)
	public Interval rssiAutoPollRate							= Interval.secs(DEFAULT_RSSI_AUTO_POLL_RATE);

	/**
	 * Default is {@link #DEFAULT_RUNNING_AVERAGE_N} - The number of historical write times that the library should keep track of when calculating average time.
	 * 
	 * @see BleDevice#getAverageWriteTime()
	 * @see #nForAverageRunningReadTime
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Integer		nForAverageRunningWriteTime				= DEFAULT_RUNNING_AVERAGE_N;
	
	/**
	 * Default is {@link #DEFAULT_RUNNING_AVERAGE_N} - Same thing as {@link #nForAverageRunningWriteTime} but for reads.
	 * 
	 * @see BleDevice#getAverageWriteTime()
	 * @see #nForAverageRunningWriteTime
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Integer		nForAverageRunningReadTime				= DEFAULT_RUNNING_AVERAGE_N;
	
	/**
	 * Default is {@link #DEFAULT_TX_POWER} - this value is used if we can't establish a device's calibrated transmission power from the device itself,
	 * either through its scan record or by reading the standard characteristic. To get a good value for this on a per-remote-device basis
	 * experimentally, simply run a sample app and use {@link BleDevice#startRssiPoll(Interval, ReadWriteListener)} and spit {@link BleDevice#getRssi()}
	 * to your log. The average value of {@link BleDevice#getRssi()} at one meter away is the value you should use for this config option.
	 * 
	 * @see BleDevice#getTxPower()
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Nullable(Prevalence.NORMAL)
	public Integer		defaultTxPower							= DEFAULT_TX_POWER;
	
	/**
	 * Default is {@link #DEFAULT_RSSI_MIN} - the estimated minimum value for {@link BleDevice#getRssi()}.
	 */
	@Nullable(Prevalence.NORMAL)
	public Integer		rssi_min								= DEFAULT_RSSI_MIN;
	
	/**
	 * Default is {@link #DEFAULT_RSSI_MAX} - the estimated maximum value for {@link BleDevice#getRssi()}.
	 */
	@Nullable(Prevalence.NORMAL)
	public Integer		rssi_max								= DEFAULT_RSSI_MAX;
	
	/**
	 * Default is instance of {@link DefaultBondFilter}.
	 * 
	 * @see BondFilter
	 */
	@Nullable(Prevalence.NORMAL)
	public BondFilter bondFilter								= new DefaultBondFilter();

	/**
	 * Set a default {@link com.idevicesinc.sweetblue.BleTransaction.Auth} which will be used when
	 * connecting to a {@link BleDevice}. This transaction will also be called if the {@link BleDevice} has
	 * to reconnect for any reason.
	 *
	 * @deprecated This is still here only so we don't break current builds. It will be removed in version 3. Use
	 * {@link #defaultAuthFactory} instead, so each device gets it's own instance, instead of sharing the same one.
	 * If you are connecting to more than 1 device at a time, SweetBlue will throw an Exception.
	 */
	@Deprecated
	@Nullable(Prevalence.NORMAL)
	public BleTransaction.Auth defaultAuthTransaction			= null;


	/**
	 * Set a default {@link com.idevicesinc.sweetblue.BleTransaction.Auth} factory which will be used to dispatch a new instance
	 * of the transaction when connecting to a {@link BleDevice}. This transaction will also be called if the {@link BleDevice} has
	 * to reconnect for any reason.
	 */
	@Nullable(Prevalence.NORMAL)
	public AuthTransactionFactory defaultAuthFactory			= null;

	/**
	 * Set a default {@link com.idevicesinc.sweetblue.BleTransaction.Init} which will be used when
	 * connecting to a {@link BleDevice}. This transaction will also be called if the {@link BleDevice} has
	 * to reconnect for any reason.
	 *
	 * @deprecated This is still here only so we don't break current builds. It will be removed in version 3. Use
	 * {@link #defaultInitFactory} instead, so each device gets it's own instance, instead of sharing the same one. If you are connecting
	 * to more than 1 device at a time, SweetBlue will throw an Exception.
	 */
	@Deprecated
	@Nullable(Prevalence.NORMAL)
	public BleTransaction.Init defaultInitTransaction			= null;


	/**
	 * Set a default {@link com.idevicesinc.sweetblue.BleTransaction.Init} factory which will be used to dispatch a new instance
	 * of the transaction when connecting to a {@link BleDevice}. This transaction will also be called if the {@link BleDevice} has
	 * to reconnect for any reason.
	 */
	@Nullable(Prevalence.NORMAL)
	public InitTransactionFactory defaultInitFactory			= null;

	/**
	 * Default is {@link #DEFAULT_MAX_CONNECTION_FAIL_HISTORY_SIZE} - This sets the size of the list that tracks the history
	 * of {@link com.idevicesinc.sweetblue.BleNode.ConnectionFailListener.ConnectionFailEvent}s. Note that this will always be
	 * at least 1. If set to anything lower, it will be ignored, and the max size will be 1.
	 */
	public int maxConnectionFailHistorySize									= DEFAULT_MAX_CONNECTION_FAIL_HISTORY_SIZE;

	/**
	 * Enumeration used with {@link #useGattRefresh}. This specifies where SweetBlue will refresh the gatt database for a device.
	 */
	public enum RefreshOption
	{
		/**
		 * The gatt database will be refreshed after connecting, and before service discovery. This is the original behavior (and current
		 * default) of the library.
		 */
		BEFORE_SERVICE_DISCOVERY,

		/**
		 * The gatt database will be refreshed after disconnecting from a device. It's been found that at least some devices connect better
		 * when the database is refreshed prior to connecting, if you have connected at least once already.
		 */
		AFTER_DISCONNECTING
	}

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
	@com.idevicesinc.sweetblue.annotations.Advanced
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface BondFilter
	{
		/**
		 * Just a dummy subclass of {@link BleDevice.StateListener.StateEvent} so that this gets auto-imported for implementations of {@link BondFilter}.
		 */
		@com.idevicesinc.sweetblue.annotations.Advanced
		public static class StateChangeEvent extends BleDevice.StateListener.StateEvent
		{
			StateChangeEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
			{
				super(device, oldStateBits, newStateBits, intentMask, gattStatus);
			}
		}

		/**
		 * An enumeration of the type of characteristic operation for a {@link CharacteristicEvent}.
		 */
		@com.idevicesinc.sweetblue.annotations.Advanced
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
		 * Struct passed to {@link BondFilter#onEvent(CharacteristicEvent)}.
		 */
		@com.idevicesinc.sweetblue.annotations.Advanced
		@Immutable
		public static class CharacteristicEvent extends Event
		{
			/**
			 * Returns the {@link BleDevice} in question.
			 */
			public BleDevice device(){  return m_device;  }
			private final BleDevice m_device;

			/**
			 * Convience to return the mac address of {@link #device()}.
			 */
			public String macAddress()  {  return m_device.getMacAddress();  }

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
				return Utils_String.toString
				(
					this.getClass(),
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
		@com.idevicesinc.sweetblue.annotations.Advanced
		@Immutable
		public static class Please
		{
			private final Boolean m_bond;
			private final BondListener m_bondListener;

			Please(Boolean bond, BondListener listener)
			{
				m_bond = bond;
				m_bondListener = listener;
			}

			Boolean bond_private()
			{
				return m_bond;
			}

			BondListener listener()
			{
				return m_bondListener;
			}

			/**
			 * Device should be bonded if it isn't already.
			 */
			public static Please bond()
			{
				return new Please(true, null);
			}

			/**
			 * Returns {@link #bond()} if the given condition holds <code>true</code>, {@link #doNothing()} otherwise.
			 */
			public static Please bondIf(boolean condition)
			{
				return condition ? bond() : doNothing();
			}

			/**
			 * Same as {@link #bondIf(boolean)} but lets you pass a {@link BondListener} as well.
			 */
			public static Please bondIf(boolean condition, BondListener listener)
			{
				return condition ? bond(listener) : doNothing();
			}

			/**
			 * Same as {@link #bond()} but lets you pass a {@link BondListener} as well.
			 */
			public static Please bond(BondListener listener)
			{
				return new Please(true, listener);
			}

			/**
			 * Device should be unbonded if it isn't already.
			 */
			public static Please unbond()
			{
				return new Please(false, null);
			}

			/**
			 * Returns {@link #bond()} if the given condition holds <code>true</code>, {@link #doNothing()} otherwise.
			 */
			public static Please unbondIf(boolean condition)
			{
				return condition ? unbond() : doNothing();
			}

			/**
			 * Device's bond state should not be affected.
			 */
			public static Please doNothing()
			{
				return new Please(null, null);
			}
		}

		/**
		 * Called after a device undergoes a change in its {@link BleDeviceState}.
		 */
		Please onEvent(StateChangeEvent e);

		/**
		 * Called immediately before reading, writing, or enabling notification on a characteristic.
		 */
		Please onEvent(CharacteristicEvent e);
	}

	/**
	 * Default implementation of {@link BondFilter} that unbonds for certain phone models upon discovery and disconnects.
	 * See further explanation in documentation for {@link BondFilter}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@Immutable
	public static class DefaultBondFilter implements BondFilter
	{
		/**
		 * Forwards {@link Utils#phoneHasBondingIssues()}. Override to make this <code>true</code> for more (or fewer) phones.
		 */
		public boolean phoneHasBondingIssues()
		{
			return Utils.phoneHasBondingIssues();
		}

		@Override public Please onEvent(StateChangeEvent e)
		{
			final boolean autoBondFix = bool(e.device().conf_device().autoBondFixes, e.device().conf_mngr().autoBondFixes);
			if( phoneHasBondingIssues() && autoBondFix )
			{
				if( !e.device().is(BleDeviceState.BONDING) )
				{
					return Please.unbondIf( e.didEnterAny(BleDeviceState.DISCOVERED, BleDeviceState.DISCONNECTED) );
				}
			}

			return Please.doNothing();
		}

		@Override public Please onEvent(CharacteristicEvent e)
		{
			return Please.doNothing();
		}
	}

	public interface InitTransactionFactory<T extends BleTransaction.Init>
	{
		T newInitTxn();
	}

	public interface AuthTransactionFactory<T extends BleTransaction.Auth>
	{
		T newAuthTxn();
	}

	/**
	 * Creates a {@link BleDeviceConfig} with all default options set. See each member of this class
	 * for what the default options are set to. Consider using {@link #newNulled()} also.
	 */
	public BleDeviceConfig()
	{
	}

	/**
	 * Creates a {@link BleDeviceConfig} with all default options set. Then, any configuration options
	 * specified in the given JSONObject will be applied over the defaults.  See {@link BleNodeConfig#writeJSON}
	 * regarding the creation of the JSONObject
	 */
	public BleDeviceConfig(JSONObject jo)
	{
		super();
		readJSON(jo);
	}
	
	/**
	 * Convenience method that returns a nulled out {@link BleDeviceConfig}, which is useful
	 * when using {@link BleDevice#setConfig(BleDeviceConfig)} to only override a few options
	 * from {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}
	 * or {@link BleManager#setConfig(BleManagerConfig)}.
	 */
	public static BleDeviceConfig newNulled()
	{
		final BleDeviceConfig config = new BleDeviceConfig();
		config.nullOut();
		
		return config;
	}

	@Override public BleDeviceConfig clone()
	{
		return (BleDeviceConfig) super.clone();
	}
}
