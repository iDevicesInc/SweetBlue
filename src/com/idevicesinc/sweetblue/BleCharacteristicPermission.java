package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.BitwiseEnum;

/**
 * This enum enforces compile-time constraints over various public static int PERMISSION_ members
 * of {@link android.bluetooth.BluetoothGattCharacteristic}.
 */
public enum BleCharacteristicPermission implements BitwiseEnum
{
	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PERMISSION_READ}.
	 */
	READ(BluetoothGattCharacteristic.PERMISSION_READ),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PERMISSION_READ_ENCRYPTED}.
	 */
	READ_ENCRYPTED(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PERMISSION_READ_ENCRYPTED_MITM}.
	 */
	READ_ENCRYPTED_MITM(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PERMISSION_WRITE}.
	 */
	WRITE(BluetoothGattCharacteristic.PERMISSION_WRITE),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PERMISSION_WRITE_ENCRYPTED}.
	 */
	WRITE_ENCRYPTED(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PERMISSION_WRITE_ENCRYPTED_MITM}.
	 */
	WRITE_ENCRYPTED_MITM(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PERMISSION_WRITE_SIGNED}.
	 */
	WRITE_SIGNED(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PERMISSION_WRITE_SIGNED_MITM}.
	 */
	WRITE_SIGNED_MITM(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM);

	private final int m_bit;

	private BleCharacteristicPermission(final int bit)
	{
		m_bit = bit;
	}

	@Override public int or(BitwiseEnum state)
	{
		return 0;
	}

	@Override public int or(int bits)
	{
		return m_bit | bits;
	}

	@Override public int bit()
	{
		return m_bit;
	}

	@Override public boolean overlaps(int mask)
	{
		return (m_bit & mask) != 0x0;
	}

}
