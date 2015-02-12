package com.idevicesinc.sweetblue;

/**
 * Under the hood, SweetBlue uses a priority task queue to serialize all interaction with the native BLE stack.
 * This enumeration represents all the tasks that are used and lets you control various timing options in
 * {@link BleDeviceConfig} and {@link BleManagerConfig}, for example {@link BleDeviceConfig#timeouts}.
 */
public enum BleTask
{
	TURN_BLE_OFF,
	
	TURN_BLE_ON,
	
	RESOLVE_CRASHES,
	
	CONNECT,
	
	DISCONNECT,
	
	BOND,
	
	UNBOND,
	
	READ,
	
	WRITE,
	
	TOGGLE_NOTIFY,
	
	READ_RSSI,
	
	DISCOVER_SERVICES;
}
