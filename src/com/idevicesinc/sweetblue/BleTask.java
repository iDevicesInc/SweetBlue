package com.idevicesinc.sweetblue;

/**
 * Under the hood, SweetBlue uses a priority task queue to serialize all interaction with the native BLE stack.
 * This enumeration represents all the tasks that are used and lets you control various timing options in
 * {@link BleDeviceConfig} and {@link BleManagerConfig}, for example {@link BleDeviceConfig#timeouts}.
 */
public enum BleTask
{
	/**
	 * Associated with {@link BleManager#turnOff()}
	 */
	TURN_BLE_OFF,
	
	/**
	 * Associated with {@link BleManager#turnOn()}
	 */
	TURN_BLE_ON,
	
	/**
	 * Associated with {@link BleManagerConfig#enableCrashResolver}.
	 */
	RESOLVE_CRASHES,
	
	/**
	 * Associated with {@link BleDevice#connect()} and its several overloads.
	 */
	CONNECT,
	
	/**
	 * Associated with {@link BleDevice#disconnect()}.
	 */
	DISCONNECT,
	
	/**
	 * Associated with {@link BleDevice#bond()}.
	 */
	BOND,
	
	/**
	 * Associated with {@link BleDevice#unbond()}.
	 */
	UNBOND,
	
	/**
	 * Associated with {@link BleDevice#read(java.util.UUID, com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
	 */
	READ,
	
	/**
	 * Associated with {@link BleDevice#write(java.util.UUID, byte[], com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
	 */
	WRITE,
	
	/**
	 * Associated with {@link BleDevice#enableNotify(java.util.UUID, com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)} and
	 * {@link BleDevice#disableNotify(java.util.UUID, com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
	 */
	TOGGLE_NOTIFY,
	
	/**
	 * Associated with {@link BleDevice#readRssi()}.
	 */
	READ_RSSI,
	
	/**
	 * Associated with discovering services after a {@link BleDevice} becomes {@link BleDeviceState#CONNECTED}.
	 */
	DISCOVER_SERVICES;
}
