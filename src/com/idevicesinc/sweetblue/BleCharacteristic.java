package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.Utils;

import java.util.UUID;

/**
 * Proxy of {@link android.bluetooth.BluetoothGattCharacteristic} to force stricter compile-time checks and order of operations
 * when creating characteristics for {@link BleServer}.
 */
public class BleCharacteristic
{
	final BluetoothGattCharacteristic m_native;

	public BleCharacteristic(final UUID uuid, final BleCharacteristicProperty property, final BleCharacteristicPermission permission, final BleDescriptor ... descriptors)
	{
		this(uuid, property.bit(), permission.bit(), descriptors);
	}

	public BleCharacteristic(final UUID uuid, final BleCharacteristicPermission permission, final BleCharacteristicProperty property, final BleDescriptor ... descriptors)
	{
		this(uuid, property.bit(), permission.bit(), descriptors);
	}

	public BleCharacteristic(final UUID uuid, final BleCharacteristicProperty[] properties, final BleCharacteristicPermission[] permissions, final BleDescriptor ... descriptors)
	{
		this(uuid, Utils.toBits(properties), Utils.toBits(permissions), descriptors);
	}

	private BleCharacteristic(final UUID uuid, final int properties, final int permissions, final BleDescriptor[] descriptors)
	{
		m_native = new BluetoothGattCharacteristic(uuid, properties, permissions);

		for( int i = 0; i < descriptors.length; i++ )
		{
			m_native.addDescriptor(descriptors[i].m_native);
		}
	}
}
