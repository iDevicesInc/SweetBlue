package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;

/**
 * This enum enforces compile-time constraints over various public static int CONNECTION_PRIORITY_* members
 * of {@link android.bluetooth.BluetoothGatt} and is passed to {@link BleDevice#setConnectionPriority(BleConnectionPriority, BleDevice.ReadWriteListener)}
 * and returned from {@link BleDevice#getConnectionPriority()}.
 */
public enum BleConnectionPriority
{
	/**
	 * Strict typing for {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
	 */
	LOW(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER),

	/**
	 * Strict typing for {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED}.
	 */
	MEDIUM(BluetoothGatt.CONNECTION_PRIORITY_BALANCED),

	/**
	 * Strict typing for {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}.
	 */
	HIGH(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

	private final int m_nativeMode;

	private BleConnectionPriority(final int nativeMode)
	{
		m_nativeMode = nativeMode;
	}

	/**
	 * Returns one of the static final int members of {@link BleConnectionPriority} whose name starts with CONNECTION_PRIORITY_.
	 */
	public int getNativeMode()
	{
		return m_nativeMode;
	}
}
