package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.BleManager.UhOhListener;

/**
 * An UhOh is a warning about an exceptional (in the bad sense) and unfixable problem with the underlying stack that
 * the app can warn its user about. It's kind of like an {@link Exception} but they can be so common
 * that using {@link Exception} would render this library unusable without a rat's nest of try/catches.
 * Instead you implement {@link BleManager.UhOhListener} to receive them. Each {@link UhOh} has a {@link UhOh#getRemedy()}
 * that suggests what might be done about it.
 * 
 * @see UhOhListener
 * @see BleManager#setListener_UhOh(UhOhListener)
 */
public enum UhOh
{
	/**
	 * A {@link BleDevice#read(UUID, BleDevice.ReadWriteListener)}
	 * took longer than {@link BleManagerConfig#DEFAULT_TASK_TIMEOUT} seconds.
	 * You will also get a {@link BleDevice.ReadWriteListener.Result} with {@link BleDevice.ReadWriteListener.Status#TIMED_OUT}
	 * but a timeout is a sort of fringe case that should not regularly happen.
	 */
	READ_TIMED_OUT,
	
	/**
	 * A {@link BleDevice#read(UUID, BleDevice.ReadWriteListener)} returned with a <code>null</code>
	 * characteristic value. The <code>null</code> value will end up as an empty array in {@link Result#data}
	 * so app-land doesn't have to do any special <code>null</code> handling.
	 */
	READ_RETURNED_NULL,
	
	/**
	 * Similar to {@link #READ_TIMED_OUT} but for {@link BleDevice#write(UUID, byte[])}.
	 */
	WRITE_TIMED_OUT,
	
	/**
	 * When the underlying stack meets a race condition where {@link BluetoothAdapter#getState()} does not
	 * match the value provided through {@link BluetoothAdapter#ACTION_STATE_CHANGED} with {@link BluetoothAdapter#EXTRA_STATE}.
	 */
	INCONSISTENT_NATIVE_BLE_STATE,
	
	/**
	 * A {@link BleDevice} went from {@link BleDeviceState#BONDING} to {@link BleDeviceState#UNBONDED}.
	 */
	WENT_FROM_BONDING_TO_UNBONDED,
	
	/**
	 * A {@link BluetoothGatt#discoverServices()} operation returned two duplicate services. Not the same instance
	 * necessarily but the same UUID.
	 */
	DUPLICATE_SERVICE_FOUND,
	
	/**
	 * A {@link BluetoothGatt#discoverServices()} operation returned a service instance that we already received before
	 * after disconnecting and reconnecting.
	 */
	OLD_DUPLICATE_SERVICE_FOUND,
	
	/**
	 * {@link BluetoothAdapter#startLeScan()} failed for an unknown reason. The library is now using
	 * {@link BluetoothAdapter#startDiscovery()} instead.
	 * 
	 * @see BleManagerConfig#revertToClassicDiscoveryIfNeeded
	 */
	START_BLE_SCAN_FAILED__USING_CLASSIC,
	
	/**
	 * {@link BluetoothGatt#getConnectionState()} says we're connected but we never tried to connect in the first place.
	 * My theory is that this can happen on some phones when you quickly restart the app and the stack doesn't have 
	 * a chance to disconnect from the device entirely. 
	 */
	CONNECTED_WITHOUT_EVER_CONNECTING,
	
	/**
	 * Similar in concept to {@link UhOh#RANDOM_EXCEPTION} but used when {@link DeadObjectException} is thrown.
	 */
	DEAD_OBJECT_EXCEPTION,
	
	/**
	 * The underlying native BLE stack enjoys surprising you with random exceptions. Every time a new one is discovered
	 * it is wrapped in a try/catch and this {@link UhOh} is dispatched.
	 */
	RANDOM_EXCEPTION,
	
	
	
	
	/**
	 * {@link BluetoothAdapter#startLeScan()} failed and {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is <code>false</code>.
	 * 
	 * @see BleManagerConfig#revertToClassicDiscoveryIfNeeded
	 */
	START_BLE_SCAN_FAILED,
	
	/**
	 * {@link BluetoothAdapter#startLeScan()} failed and {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is <code>true</code>
	 * so we try {@link BluetoothAdapter#startDiscovery()} but that also fails...fun!
	 */
	CLASSIC_DISCOVERY_FAILED,
	
	/**
	 * {@link BluetoothGatt#discoverServices()} failed right off the bat and returned false.
	 */
	SERVICE_DISCOVERY_IMMEDIATELY_FAILED,
	
	
//	UNKNOWN_CONNECTION_ERROR,
//	MULTIPLE_CONNECTIONS_FAILED,
	
	
	
	/**
	 * {@link BluetoothAdapter#disable()}, through {@link BleManager#disableBle()}, is failing to complete.
	 * We always end up back at {@link BluetoothAdapter#STATE_ON}.
	 */
	CANNOT_DISABLE_BLUETOOTH,
	
	/**
	 * {@link BluetoothAdapter#enable()}, through {@link BleManager#enableBle()}, is failing to complete.
	 * We always end up back at {@link BluetoothAdapter#STATE_OFF}. Opposite problem of {@link #CANNOT_DISABLE_BLUETOOTH}
	 */
	CANNOT_ENABLE_BLUETOOTH,
	
	/**
	 * Just a blanket case for when the library has to completely shrug its shoulders.
	 */
	UNKNOWN_BLE_ERROR;
	
	/**
	 * The suggested remedy for each {@link UhOh}. This can be used as a proxy for the severity
	 * of the issue.
	 * 
	 * 
	 */
	public static enum Remedy
	{
		/**
		 * Nothing you can really do, hopefully the library can soldier on.
		 */
		WAIT_AND_SEE,
		
		/**
		 * Calling {@link BleManager#dropTacticalNuke()} is probably in order.
		 * 
		 * @see BleManager#dropTacticalNuke()
		 */
		NUKE,
		
		/**
		 * Might want to notify your user that a phone restart is in order.
		 */
		RESTART_PHONE;
	}
	
	/**
	 * Returns the {@link Remedy} for this {@link UhOh}.
	 */
	public Remedy getRemedy()
	{
		if( this.ordinal() >= CANNOT_DISABLE_BLUETOOTH.ordinal() )
		{
			return Remedy.RESTART_PHONE;
		}
		else if( this.ordinal() >= START_BLE_SCAN_FAILED.ordinal() )
		{
			return Remedy.NUKE;
		}
		else
		{
			return Remedy.WAIT_AND_SEE;
		}
	}
}
