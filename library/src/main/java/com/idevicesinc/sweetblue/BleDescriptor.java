package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.utils.Utils_Byte;

import java.util.UUID;

/**
 * Proxy of {@link android.bluetooth.BluetoothGattDescriptor} to force stricter compile-time checks and order of operations
 * when creating descriptors for {@link BleServer}.
 *
 * @see BleServices
 */
public final class BleDescriptor
{
	final BluetoothGattDescriptor m_native;

	public BleDescriptor(final UUID uuid, final BleDescriptorPermission permission)
	{
		this(uuid, permission.bit());
	}

	public BleDescriptor(final UUID uuid, final BleDescriptorPermission ... permissions)
	{
		this(uuid, Utils_Byte.toBits(permissions));
	}

	private BleDescriptor(final UUID uuid, final int permissions)
	{
		m_native = new BluetoothGattDescriptor(uuid, permissions);
	}
}
