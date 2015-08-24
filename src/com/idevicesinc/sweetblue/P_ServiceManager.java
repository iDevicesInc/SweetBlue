package com.idevicesinc.sweetblue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.FutureData;

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
	
	public P_Characteristic getCharacteristic(final UUID serviceUuid_nullable, final UUID characteristicUuid)
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

			if( serviceUuid_nullable != null && !ithService.getUuid().equals(serviceUuid_nullable) )  continue;

			P_Characteristic characteristic = ithService.get(characteristicUuid);
			
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
				//--- DRK > NOTE: This did trip during some stress testing, so converting to warning now so it's quieter.
//				m_device.getManager().ASSERT(false);
				m_device.getManager().getLogger().w("Weak old services array is getting pretty big...GC lagging behind");
			}
			
			m_serviceMap.clear();
			m_serviceList.clear();			
		}
	}
	
	private BleDevice.ReadWriteListener.ReadWriteEvent newNoMatchingTargetEvent(Type type, byte[] data, UUID serviceUuid, UUID characteristicUuid)
	{
		final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
		
		return new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, null, type, Target.CHARACTERISTIC, data, Status.NO_MATCHING_TARGET, gattStatus, 0.0, 0.0);
	}
	
	BleDevice.ReadWriteListener.ReadWriteEvent getEarlyOutEvent(UUID serviceUuid, UUID characteristicUuid, FutureData futureData, BleDevice.ReadWriteListener.Type type, final Target target)
	{
		final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
		
		if( m_device.isNull() )
		{
			ReadWriteEvent result = new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, null, type, target, futureData.getData(), Status.NULL_DEVICE, gattStatus, 0.0, 0.0);
			
			return result;
		}
		
		if( !m_device.is(BleDeviceState.CONNECTED) )
		{
			if( type != BleDevice.ReadWriteListener.Type.ENABLING_NOTIFICATION && type != BleDevice.ReadWriteListener.Type.DISABLING_NOTIFICATION)
			{				
				ReadWriteEvent result = new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, null, type, target, futureData.getData(), Status.NOT_CONNECTED, gattStatus, 0.0, 0.0);
				
				return result;
			}
			else
			{
				return null;
			}
		}
		
		if( target == Target.RSSI )  return null;
		
		final P_Characteristic characteristic = getCharacteristic(serviceUuid, characteristicUuid);
		
		if( characteristic == null )
		{
			return newNoMatchingTargetEvent(type, futureData.getData(), serviceUuid, characteristicUuid);
		}
		
		final BluetoothGattCharacteristic char_native = characteristic.getGuaranteedNative();
		type = modifyResultType(char_native, type);
		
		if( char_native == null )
		{
			return newNoMatchingTargetEvent(type, futureData.getData(), serviceUuid, characteristicUuid);
		}
		
		if( type != null && type.isWrite() )
		{
			if( futureData == null )
			{
				return new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, null, type, target, (byte[]) null, Status.NULL_DATA, gattStatus, 0.0, 0.0);
			}
//			else if( data.length == 0 )
//			{
//				return new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, null, type, target, data, Status.EMPTY_DATA, gattStatus, 0.0, 0.0);
//			}
		}
		
		int property = getProperty(type);
		
		if( (char_native.getProperties() & property) == 0x0 )
		{
			//TODO: Use correct gatt status even though we never reach gatt layer?
			ReadWriteEvent result = new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, null, type, target, futureData.getData(), Status.OPERATION_NOT_SUPPORTED, gattStatus, 0.0, 0.0);
			
			return result;
		}
		
		return null;
	}
	
	static BleDevice.ReadWriteListener.Type modifyResultType(BluetoothGattCharacteristic char_native, BleDevice.ReadWriteListener.Type type)
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
			else if( type == Type.WRITE )
			{
				if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0x0 )
				{
					if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0x0 )
					{
						type = Type.WRITE_NO_RESPONSE;
					}
					else if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0x0 )
					{
						type = Type.WRITE_SIGNED;
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
	
	private List<BluetoothGattService> newServiceList()
	{
		final ArrayList<BluetoothGattService> toReturn = new ArrayList<BluetoothGattService>();
		for( int i = 0; i < m_serviceList.size(); i++ )
		{
			toReturn.add(m_serviceList.get(i).getNative());
		}
		
		return toReturn;
	}
	
	private List<BluetoothGattCharacteristic> newCharacteristicList(UUID uuid_nullable)
	{
		final ArrayList<BluetoothGattCharacteristic> toReturn = new ArrayList<BluetoothGattCharacteristic>();
		for( int i = 0; i < m_serviceList.size(); i++ )
		{
			final P_Service service_ith = m_serviceList.get(i);
			
			if( uuid_nullable == null || uuid_nullable.equals(service_ith.getUuid()) )
			{
				service_ith.addToList(toReturn);
			}
		}
		
		return toReturn;
	}
	
	
	public Iterator<BluetoothGattService> getNativeServices()
	{
		return newServiceList().iterator();
	}
	
	public List<BluetoothGattService> getNativeServices_List()
	{
		return newServiceList();
	}
	
	public Iterator<BluetoothGattCharacteristic> getNativeCharacteristics()
	{
		return newCharacteristicList(null).iterator();
	}
	
	public List<BluetoothGattCharacteristic> getNativeCharacteristics_List()
	{
		return newCharacteristicList(null);
	}
	
	public Iterator<BluetoothGattCharacteristic> getNativeCharacteristics(UUID service)
	{
		return newCharacteristicList(service).iterator();
	}
	
	public List<BluetoothGattCharacteristic> getNativeCharacteristics_List(UUID service)
	{
		return newCharacteristicList(service);
	}
}

