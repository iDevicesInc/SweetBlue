package com.idevicesinc.sweetblue;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.P_Const;


final class P_DeviceServiceManager extends PA_ServiceManager
{
	private final BleDevice m_device;
	
	public P_DeviceServiceManager(BleDevice device)
	{
		m_device = device;
	}
	
	private BleDevice.ReadWriteListener.ReadWriteEvent newNoMatchingTargetEvent(Type type, Target target, byte[] data, UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, DescriptorFilter descriptorFilter)
	{
		final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
		
		return new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, descriptorUuid, descriptorFilter, type, target, data, Status.NO_MATCHING_TARGET, gattStatus, 0.0, 0.0, /*solicited=*/true);
	}

	private BleDevice.ReadWriteListener.ReadWriteEvent newExceptionEvent(Type type, Target target, byte[] data, UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, DescriptorFilter descriptorFilter, BleManager.UhOhListener.UhOh uhoh)
	{
		final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

		final Status status;

		if (uhoh == BleManager.UhOhListener.UhOh.CONCURRENT_EXCEPTION)
		{
			status = Status.GATT_CONCURRENT_EXCEPTION;
		}
		else
		{
			status = Status.GATT_RANDOM_EXCEPTION;
		}

		return new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, descriptorUuid, descriptorFilter, type, target, data, status, gattStatus, 0.0, 0.0, /*solicited=*/true);
	}
	
	final BleDevice.ReadWriteListener.ReadWriteEvent getEarlyOutEvent(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, DescriptorFilter descriptorFilter, FutureData futureData, BleDevice.ReadWriteListener.Type type, final Target target)
	{
		final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
		
		if( m_device.isNull() )
		{
			return new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, descriptorUuid, descriptorFilter, type, target, futureData.getData(), Status.NULL_DEVICE, gattStatus, 0.0, 0.0, /*solicited=*/true);
		}
		
		if( false == m_device.is(BleDeviceState.CONNECTED) )
		{
			if( type != BleDevice.ReadWriteListener.Type.ENABLING_NOTIFICATION && type != BleDevice.ReadWriteListener.Type.DISABLING_NOTIFICATION)
			{				
				return new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, descriptorUuid, descriptorFilter, type, target, futureData.getData(), Status.NOT_CONNECTED, gattStatus, 0.0, 0.0, /*solicited=*/true);
			}
			else
			{
				return null;
			}
		}
		
		if( target == Target.RSSI || target == Target.MTU || target == Target.CONNECTION_PRIORITY )  return null;
		
		final BleCharacteristicWrapper characteristic = m_device.getNativeBleCharacteristic(serviceUuid, characteristicUuid);
		final BleDescriptorWrapper descriptor = m_device.getNativeBleDescriptor(serviceUuid, characteristicUuid, descriptorUuid);
		
		if( target == Target.CHARACTERISTIC && characteristic.isNull() || target == Target.DESCRIPTOR && descriptor.isNull())
		{
			if (characteristic.hasUhOh() || descriptor.hasUhOh())
			{
				BleManager.UhOhListener.UhOh uhoh;
				if (characteristic.hasUhOh())
				{
					uhoh = characteristic.getUhOh();
				}
				else
				{
					uhoh = descriptor.getUhOh();
				}
				return newExceptionEvent(type, target, futureData.getData(), serviceUuid, characteristicUuid, descriptorUuid, descriptorFilter, uhoh);
			}
			else
			{
				return newNoMatchingTargetEvent(type, target, futureData.getData(), serviceUuid, characteristicUuid, descriptorUuid, descriptorFilter);
			}
		}

		if( target == Target.CHARACTERISTIC )
		{
			type = modifyResultType(characteristic, type);
		}

		if( type != null && type.isWrite() )
		{
			if( futureData == null )
			{
				return new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, null, descriptorFilter, type, target, (byte[]) null, Status.NULL_DATA, gattStatus, 0.0, 0.0, /*solicited=*/true);
			}
		}

		if( target == Target.CHARACTERISTIC )
		{
			int property = getProperty(type);

			if( (characteristic.getCharacteristic().getProperties() & property) == 0x0 )
			{
				//TODO: Use correct gatt status even though we never reach gatt layer?
				ReadWriteEvent result = new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, null, descriptorFilter, type, target, futureData.getData(), Status.OPERATION_NOT_SUPPORTED, gattStatus, 0.0, 0.0, /*solicited=*/true);

				return result;
			}
		}
		
		return null;
	}
	
	static BleDevice.ReadWriteListener.Type modifyResultType(BleCharacteristicWrapper char_native, BleDevice.ReadWriteListener.Type type)
	{
		if( !char_native.isNull())
		{
			if( type == Type.NOTIFICATION )
			{
				if( (char_native.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0x0 )
				{
					if( (char_native.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0x0 )
					{
						type = Type.INDICATION;
					}
				}
			}
			else if( type == Type.WRITE )
			{
				if( (char_native.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0x0 )
				{
					if( (char_native.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0x0 )
					{
						type = Type.WRITE_NO_RESPONSE;
					}
					else if( (char_native.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0x0 )
					{
						type = Type.WRITE_SIGNED;
					}
				}
				//--- RB > Check the write type on the characteristic, in case this char has multiple write types
				int writeType = char_native.getCharacteristic().getWriteType();
				if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
				{
					type = Type.WRITE_NO_RESPONSE;
				}
				else if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED)
				{
					type = Type.WRITE_SIGNED;
				}
			}
		}
		
		return type;
	}
	
	private static int getProperty(BleDevice.ReadWriteListener.Type type)
	{
		switch(type)
		{
			case READ:
			case POLL:
			case PSUEDO_NOTIFICATION:	return		BluetoothGattCharacteristic.PROPERTY_READ;
			
            case WRITE_NO_RESPONSE:     return      BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
            case WRITE_SIGNED:          return      BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
            case WRITE:					return		BluetoothGattCharacteristic.PROPERTY_WRITE;
    
			case ENABLING_NOTIFICATION:
			case DISABLING_NOTIFICATION:
			case NOTIFICATION:
			case INDICATION:			return		BluetoothGattCharacteristic.PROPERTY_INDICATE			|
													BluetoothGattCharacteristic.PROPERTY_NOTIFY				;
		}
		
		return 0x0;
	}

	@Override public final BleServiceWrapper getServiceDirectlyFromNativeNode(UUID serviceUuid)
	{
		return m_device.layerManager().getService(serviceUuid);
	}

	@Override protected final List<BluetoothGattService> getNativeServiceList_original()
	{
		List<BluetoothGattService> list_native = m_device.layerManager().getNativeServiceList();
		return list_native == null ? P_Const.EMPTY_SERVICE_LIST : list_native;
	}
}

