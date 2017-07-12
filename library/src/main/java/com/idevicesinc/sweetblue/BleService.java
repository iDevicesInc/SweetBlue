package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.Utils;

import java.util.UUID;

/**
 * Proxy of {@link BluetoothGattService} to force stricter compile-time checks and order of operations
 * when creating services for {@link BleServer}.
 *
 * @see BleServices
 */
public final class BleService
{
	final BluetoothGattService m_native;

	public BleService(final UUID uuid, final BleCharacteristic... characteristics)
	{
		this(uuid, characteristics, true);
	}

	private BleService(final UUID uuid, final BleCharacteristic[] characteristics, final boolean constructorOverloadEnabler2000)
	{
		final int serviceType = BluetoothGattService.SERVICE_TYPE_PRIMARY;

		m_native = new BluetoothGattService(uuid, serviceType);

		for( int i = 0; i < characteristics.length; i++ )
		{
			m_native.addCharacteristic(characteristics[i].m_native);
		}
	}

	BleService(BluetoothGattService native_service)
	{
		m_native = native_service;
	}

	void init()
	{
		if( m_native != null )  return;

		// DRK > nothing for now but may do actual instantiation of native service here in the future so SweetBlue
		// can implicitly determine whether a service is primary or secondary.
	}
}
