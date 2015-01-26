package com.idevicesinc.sweetblue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * 
 * 
 *
 */
class P_ServiceManager
{
	private final BleDevice m_device;
	
	private final HashMap<UUID, P_Service> m_serviceMap = new HashMap<UUID, P_Service>();
	private final ArrayList<P_Service> m_serviceList = new ArrayList<P_Service>();
	private final ArrayList<WeakReference<BluetoothGattService>> m_oldServices = new ArrayList<WeakReference<BluetoothGattService>>();
	
	public P_ServiceManager(BleDevice device)
	{
		m_device = device;
	}
	
	public boolean has(UUID uuid)
	{
		return get(uuid) != null;
	}
	
	public P_Service get(UUID uuid)
	{
		return m_serviceMap.get(uuid);
	}
	
	public P_Characteristic getCharacteristic(UUID uuid)
	{
//		int properties = BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE;
//		int permissions = BluetoothGattCharacteristic.PERMISSION_WRITE;
//		
//		BluetoothGattCharacteristic char_native = new BluetoothGattCharacteristic(uuid, properties, permissions);
//		
//		BleCharacteristic char_sim = new BleCharacteristic(m_device, char_native);
//		
//		return char_sim;
		
		for( int i = 0; i < m_serviceList.size(); i++ )
		{
			P_Service ithService = m_serviceList.get(i);
			P_Characteristic characteristic = ithService.get(uuid);
			
			if( characteristic != null )
			{
				return characteristic;
			}
		}
		
		return null;
	}
	
	private void put(BluetoothGattService service_native)
	{
//		m_device.getManager().ASSERT(!has(service.getUuid()));
		
		P_Service service = new P_Service(m_device, service_native);
		m_serviceMap.put(service.getUuid(), service);
		m_serviceList.add(service);
		service.loadCharacteristics();
	}
	
	void clear()
	{
		synchronized (m_serviceMap)
		{
			for( int i = m_oldServices.size()-1; i >= 0; i-- )
			{
				WeakReference<BluetoothGattService> ithReference = m_oldServices.get(i);
				
				if( ithReference.get() == null )
				{
					m_oldServices.remove(i);
				}
			}
			
			for( int i = 0; i < m_serviceList.size(); i++ )
			{
				m_oldServices.add(new WeakReference<BluetoothGattService>(m_serviceList.get(i).getNative()));
			}
			
			//--- DRK > just a sanity check here...might still trip if GC is slow.
			if( m_oldServices.size() > 100 )
			{
				m_device.getManager().ASSERT(false);
			}
			
			m_serviceMap.clear();
			m_serviceList.clear();			
		}
	}
	
	private BleDevice.ReadWriteListener.Result newNoMatchingTargetResult(Type type, byte[] data, UUID uuid)
	{
		int gattStatus = BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE;
		return new Result(m_device, uuid, null, type, Target.CHARACTERISTIC, data, Status.NO_MATCHING_TARGET, gattStatus, 0.0, 0.0);
	}
	
	BleDevice.ReadWriteListener.Result getEarlyOutResult(UUID uuid, byte[] data, BleDevice.ReadWriteListener.Type type)
	{
		Target target = uuid == Uuids.INVALID ? Target.RSSI : Target.CHARACTERISTIC;
		final int gattStatus = BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE;
		
		if( !m_device.is(BleDeviceState.CONNECTED) )
		{
			if( type != BleDevice.ReadWriteListener.Type.ENABLING_NOTIFICATION && type != BleDevice.ReadWriteListener.Type.DISABLING_NOTIFICATION)
			{				
				Result result = new Result(m_device, uuid, null, type, target, data, Status.NOT_CONNECTED, gattStatus, 0.0, 0.0);
				
				return result;
			}
			else
			{
				return null;
			}
		}
		
		if( type == Type.WRITE )
		{
			if( data == null )
			{
				return new Result(m_device, uuid, null, type, target, data, Status.NULL_DATA, gattStatus, 0.0, 0.0);
			}
			else if( data.length == 0 )
			{
				return new Result(m_device, uuid, null, type, target, data, Status.EMPTY_DATA, gattStatus, 0.0, 0.0);
			}
		}
		
		if( target == Target.RSSI )  return null;
		
		P_Characteristic characteristic = getCharacteristic(uuid);
		
		if( characteristic == null )
		{
			return newNoMatchingTargetResult(type, data, uuid);
		}
		
		BluetoothGattCharacteristic char_native = characteristic.getGuaranteedNative();
		type = modifyResultType(char_native, type);
		
		if( char_native == null )
		{
			return newNoMatchingTargetResult(type, data, uuid);
		}
		
		int property = getProperty(type);
		
		if( (char_native.getProperties() & property) == 0x0 )
		{
			//TODO: Use correct gatt status even though we never reach gatt layer?
			Result result = new Result(m_device, uuid, null, type, target, data, Status.OPERATION_NOT_SUPPORTED, gattStatus, 0.0, 0.0);
			
			return result;
		}
		
		return null;
	}
	
	BleDevice.ReadWriteListener.Type modifyResultType(BluetoothGattCharacteristic char_native, BleDevice.ReadWriteListener.Type type)
	{
		if( char_native != null )
		{
			if( type == Type.NOTIFICATION )
			{
				if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0x0 )
				{
					if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0x0 )
					{
						type = Type.INDICATION;
					}
				}
			}
		}
		
		return type;
	}
	
	private int getProperty(BleDevice.ReadWriteListener.Type type)
	{
		switch(type)
		{
			case READ:
			case POLL:
			case PSUEDO_NOTIFICATION:	return BluetoothGattCharacteristic.PROPERTY_READ;
			
			case WRITE:					return BluetoothGattCharacteristic.PROPERTY_WRITE;
			
			case ENABLING_NOTIFICATION:
			case DISABLING_NOTIFICATION:
			case NOTIFICATION:
			case INDICATION:			return BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_NOTIFY;
		}
		
		return 0x0;
	}
	
	private boolean hasOldService(BluetoothGattService service_native)
	{
		for( int i = 0; i < m_oldServices.size(); i++ )
		{
			WeakReference<BluetoothGattService> ithReference = m_oldServices.get(i);
			
			if( ithReference.get() == service_native )
			{
				return true;
			}
		}
		
		return false;
	}
	
	void loadDiscoveredServices()
	{
		synchronized (m_serviceMap)
		{
			if( !m_device.getManager().ASSERT(m_device.getNativeGatt() != null) )  return;
			
			//--- DRK > Observed a random concurrent modification exception a few times, so
			//---		applying this blanket fix to at least avoid that.
			List<BluetoothGattService> services = m_device.getNativeGatt().getServices();
			Object[] raw = services.toArray();
			
			for( int i = 0; i < raw.length; i++ )
			{
				BluetoothGattService ithService_native = (BluetoothGattService) raw[i];
				
				if( ithService_native == null )  continue;
				
				boolean alreadySeenIth = hasOldService(ithService_native);
				
				if( alreadySeenIth )
				{
					m_device.getManager().uhOh(UhOh.OLD_DUPLICATE_SERVICE_FOUND);
				}
				
//				Log.i(TAG, BluetoothUtils.debugThread() + "loadDiscoveredServices()_"+i+" for " + BluetoothUtils.debugServiceUUID(service.getUuid()));
				
				P_Service existingService = get(ithService_native.getUuid());
				
				if( existingService != null )
				{
					BluetoothGattService existingService_native = existingService.getNative();
					
					boolean alreadySeenExisting = hasOldService(existingService_native);
					
					if( alreadySeenExisting )
					{
						if( alreadySeenIth )
						{
							//--- DRK > worst case of weirdness...just keep using the first old service found.
						}
						else
						{
							m_serviceList.remove(existingService);
							
							put(ithService_native);
						}
					}
					else
					{
						if( alreadySeenIth )
						{
							//--- DRK > second-worst case of weirdness...just keep using the first old service found.
						}
						else
						{
							//--- DRK > Haven't seen either service so BluetoothGatt just decided to create two new fresh instances
							//---		for some reason. We arbitrarily use the "latest" one.
							m_serviceList.remove(existingService);
							put(ithService_native);
						}
					}
					
					m_device.getManager().uhOh(UhOh.DUPLICATE_SERVICE_FOUND);
				}
				else
				{
					put(ithService_native);
				}
			}
		}
	}
}

