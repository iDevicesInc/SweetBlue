package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.app.Application;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener;
import com.idevicesinc.sweetblue.BleManager.UhOhListener;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.ReflectionUuidNameMap;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.UuidNameMap;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * Provides a number of options to pass to {@link BleDevice#setConfig(BleDeviceConfig)}.
 * This class is also the super class of {@link BleManagerConfig}, which you can pass
 * to {@link BleManager#get(Context, BleManagerConfig)} to set base options for all devices at once.
 * For all options in this class, you may set the value to <code>null</code> and the value will
 * be inherited from the {@link BleManagerConfig}.
 */
public class BleDeviceConfig implements Cloneable
{
	public static final double DEFAULT_MINIMUM_SCAN_TIME				= 5.0;
	public static final int DEFAULT_RUNNING_AVERAGE_N					= 10;
	public static final double DEFAULT_SCAN_KEEP_ALIVE					= DEFAULT_MINIMUM_SCAN_TIME*2.5;
	
	/**
	 * In at least some cases it's not possible to determine beforehand whether a given characteristic requires
	 * bonding, so implementing this interface on {@link BleManagerConfig#bondingFilter} lets the app give
	 * a hint to the library so it can bond before attempting to read or write an encrypted characteristic.
	 * Providing these hints lets the library handle things in a more deterministic and optimized fashion, but is not required.
	 */
	public static interface BondingFilter
	{
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
		 * Return this from {@link #getTimeToNextReconnect(BleDevice, int, Interval, Interval)} to instantly reconnect.
		 */
		public static final Interval INSTANTLY = Interval.ZERO;
		
		/**
		 * Return this from {@link #getTimeToNextReconnect(BleDevice, int, Interval, Interval)} to stop a reconnect attempt loop.
		 * Note that {@link BleDevice#disconnect()} will also cancel any ongoing reconnect loop.
		 */
		public static final Interval CANCEL = Interval.seconds(-1.0);
		
		/**
		 * Called for every connection failure while device is {@link BleDeviceState#ATTEMPTING_RECONNECT}.
		 * Use the static members of this interface as return values to stop reconnection ({@link #CANCEL}) or try again
		 * instantly ({@link #INSTANTLY}). Use static methods of {@link Interval} to try again after some amount of time. Numeric parameters
		 * are provided in order to give the app a variety of ways to calculate the next delay. Use all, some, or none of them.
		 */
		Interval getTimeToNextReconnect(BleDevice device, int connectFailureCount, Interval totalTimeReconnecting, Interval previousDelay);
	}
	
	/**
	 * Default implementation of {@link ReconnectRateLimiter} that uses {@link #DEFAULT_INITIAL_RECONNECT_DELAY}
	 * and {@link #DEFAULT_RECONNECT_ATTEMPT_RATE} to infinitely try to reconnect.
	 */
	public static class DefaultReconnectRateLimiter implements ReconnectRateLimiter
	{
		public static final Interval DEFAULT_INITIAL_RECONNECT_DELAY = INSTANTLY;
		public static final Interval DEFAULT_RECONNECT_ATTEMPT_RATE = Interval.seconds(3.0);
		
		@Override public Interval getTimeToNextReconnect(BleDevice device, int connectFailureCount, Interval totalTimeReconnecting, Interval previousDelay)
		{
			if( connectFailureCount == 0 )
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
	 * Default is false - use this option to force bonding after a
	 * {@link BleDevice} is {@link BleDeviceState#CONNECTED} if it is not {@link BleDeviceState#BONDED} already.
	 */
	public Boolean autoBondAfterConnect					= false;
	
	/**
	 * Default is true - whether to automatically get services immediately after a {@link BleDevice} is
	 * {@link BleDeviceState#CONNECTED}. Currently this is the only way to get a device's services.
	 */
	public Boolean autoGetServices						= true;
	
	/**
	 * Default is true if phone is manufactured by Sony, false otherwise (sorry Sony) - Some
	 * android devices have known issues when it comes to bonding. So far the worst culprits
	 * are Xperias. To be safe this is set to true by default if we're running on a Sony device.
	 * The problem seems to be associated with mismanagement of pairing keys by the OS and
	 * this brute force solution seems to be the only way to smooth things out.
	 */
	public Boolean removeBondOnDisconnect				= Utils.isSony();
	
	/**
	 * Default is same as {@link #removeBondOnDisconnect} - see {@link #removeBondOnDisconnect} for explanation.
	 */
	public Boolean removeBondOnDiscovery				= removeBondOnDisconnect;
	
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
	 * HOWEVER, it's important to note that the library WILL automatically revert to autoConnect==true after a first failed
	 * connection if you do a retry by returning {@link BleDevice.ConnectionFailListener.Please#RETRY} from
	 * {@link ConnectionFailListener#onConnectionFail(BleDevice, BleDevice.ConnectionFailListener.Reason, int)}.
	 * <br><br>
	 * So really this option mainly exists for those situations where you KNOW that you have a device that only works
	 * with autoConnect==true and you want connection time to be faster (i.e. you don't want to wait for that first
	 * failed connection for the library to internally start using autoConnect==true).
	 */
	public Boolean alwaysUseAutoConnect = false;
	
	/**
	 * Default is {@link #DEFAULT_MINIMUM_SCAN_TIME} seconds - Undiscovery of devices must be
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
	 * Default is {@link #DEFAULT_SCAN_KEEP_ALIVE} seconds - If a device exceeds this amount of time since its
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
	 * Default is null, meaning the library won't preemptively attempt to bond for any characteristic operations.
	 * 
	 * @see BondingFilter
	 */
	public BondingFilter bondingFilter;
	
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
	
	static boolean bool(Boolean bool)
	{
		return bool == null ? false : bool;
	}
	
	public BleDeviceConfig()
	{
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
