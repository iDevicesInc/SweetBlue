package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

abstract class PA_ServiceManager
{
	private static final Iterator<BluetoothGattService> EMPTY_SERVICE_ITERATOR = new EmptyIterator<BluetoothGattService>();

	protected static final List<BluetoothGattService> EMPTY_SERVICE_LIST = new ArrayList<BluetoothGattService>()
	{
		@Override public Iterator<BluetoothGattService> iterator()
		{
			return EMPTY_SERVICE_ITERATOR;
		}
	};

	private static final List<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_LIST = new ArrayList<BluetoothGattCharacteristic>();
	private static final Iterator<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_ITERATOR = new EmptyIterator<BluetoothGattCharacteristic>();

	private static final List<BluetoothGattDescriptor> EMPTY_DESCRIPTOR_LIST = new ArrayList<BluetoothGattDescriptor>();


	PA_ServiceManager()
	{
	}

	public abstract BluetoothGattService getServiceDirectlyFromNativeNode(final UUID uuid);

	protected abstract List<BluetoothGattService> getNativeServiceList_original();


	public BluetoothGattCharacteristic getCharacteristic(final UUID serviceUuid_nullable, final UUID charUuid)
	{
		if( serviceUuid_nullable == null )
		{
			final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

			for( int i = 0; i < serviceList_native.size(); i++ )
			{
				final BluetoothGattService service_ith = serviceList_native.get(i);
				final BluetoothGattCharacteristic characteristic = getCharacteristic(service_ith, charUuid);

				if( characteristic != null)
				{
					return characteristic;
				}
			}

			return null;
		}
		else
		{
			final BluetoothGattService service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

			if( service_nullable != null )
			{
				return getCharacteristic(service_nullable, charUuid);
			}
			else
			{
				return null;
			}
		}
	}

	public BluetoothGattCharacteristic getCharacteristic(final UUID serviceUuid_nullable, final UUID charUuid, final DescriptorFilter filter)
	{
		if( serviceUuid_nullable == null )
		{
			final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

			for( int i = 0; i < serviceList_native.size(); i++ )
			{
				final BluetoothGattService service_ith = serviceList_native.get(i);
				final BluetoothGattCharacteristic characteristic = getCharacteristic(service_ith, charUuid, filter);

				if( characteristic != null)
				{
					return characteristic;
				}
			}

			return null;
		}
		else
		{
			final BluetoothGattService service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

			if( service_nullable != null )
			{
				return getCharacteristic(service_nullable, charUuid, filter);
			}
			else
			{
				return null;
			}
		}
	}

	private BluetoothGattCharacteristic getCharacteristic(final BluetoothGattService service, final UUID charUuid)
	{
		final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service);

		for( int j = 0; j < charList_native.size(); j++ )
		{
			final BluetoothGattCharacteristic char_jth = charList_native.get(j);

			if( char_jth.getUuid().equals(charUuid) )
			{
				return char_jth;
			}
		}

		return null;
	}

	private BluetoothGattCharacteristic getCharacteristic(final BluetoothGattService service, final UUID charUuid, DescriptorFilter filter)
	{
		final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service);

		for( int j = 0; j < charList_native.size(); j++ )
		{
			final BluetoothGattCharacteristic char_jth = charList_native.get(j);

			if( char_jth.getUuid().equals(charUuid) )
			{
				final BluetoothGattDescriptor desc = char_jth.getDescriptor(filter.descriptorUuid());
				if (desc != null)
				{
					final DescriptorFilter.DescriptorEvent event = new DescriptorFilter.DescriptorEvent(service, char_jth, desc, new PresentData(desc.getValue()));
					final DescriptorFilter.Please please = filter.onEvent(event);
					if (please.isAccepted())
					{
						return char_jth;
					}
				}
			}
		}

		return null;
	}

	private List<BluetoothGattService> getNativeServiceList_cloned()
	{
		final List<BluetoothGattService> list_native = getNativeServiceList_original();

		return list_native == EMPTY_SERVICE_LIST ? list_native : new ArrayList<BluetoothGattService>(list_native);
	}

	private List<BluetoothGattCharacteristic> getNativeCharacteristicList_original(final BluetoothGattService service)
	{
		final List<BluetoothGattCharacteristic> list_native = service.getCharacteristics();

		return list_native == null ? EMPTY_CHARACTERISTIC_LIST : list_native;
	}

	private List<BluetoothGattCharacteristic> getNativeCharacteristicList_cloned(final BluetoothGattService service)
	{
		final List<BluetoothGattCharacteristic> list_native = getNativeCharacteristicList_original(service);

		return list_native == EMPTY_CHARACTERISTIC_LIST ? list_native : new ArrayList<BluetoothGattCharacteristic>(list_native);
	}

	private List<BluetoothGattDescriptor> getNativeDescriptorList_original(final BluetoothGattCharacteristic characteristic)
	{
		final List<BluetoothGattDescriptor> list_native = characteristic.getDescriptors();

		return list_native == null ? EMPTY_DESCRIPTOR_LIST : list_native;
	}

	private List<BluetoothGattDescriptor> getNativeDescriptorList_cloned(final BluetoothGattCharacteristic characteristic)
	{
		final List<BluetoothGattDescriptor> list_native = getNativeDescriptorList_original(characteristic);

		return list_native == EMPTY_DESCRIPTOR_LIST ? list_native : new ArrayList<BluetoothGattDescriptor>(list_native);
	}

	private List<BluetoothGattCharacteristic> collectAllNativeCharacteristics(final UUID serviceUuid_nullable, final Object forEach_nullable)
	{
		final ArrayList<BluetoothGattCharacteristic> characteristics = forEach_nullable == null ? new ArrayList<BluetoothGattCharacteristic>() : null;
		final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

		for( int i = 0; i < serviceList_native.size(); i++ )
		{
			final BluetoothGattService service_ith = serviceList_native.get(i);

			if( serviceUuid_nullable == null || serviceUuid_nullable != null && serviceUuid_nullable.equals(service_ith.getUuid()) )
			{
				final List<BluetoothGattCharacteristic> nativeChars = getNativeCharacteristicList_original(service_ith);

				if( forEach_nullable != null )
				{
					if( Utils.doForEach_break(forEach_nullable, nativeChars))
					{
						return null;
					}
				}
				else
				{
					characteristics.addAll(nativeChars);
				}
			}
		}

		return characteristics;
	}

	private List<BluetoothGattDescriptor> collectAllNativeDescriptors(final UUID serviceUuid_nullable, final UUID charUuid_nullable, final Object forEach_nullable)
	{
		final ArrayList<BluetoothGattDescriptor> toReturn = forEach_nullable == null ? new ArrayList<BluetoothGattDescriptor>() : null;
		final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

		for( int i = 0; i < serviceList_native.size(); i++ )
		{
			final BluetoothGattService service_ith = serviceList_native.get(i);

			if( serviceUuid_nullable == null || serviceUuid_nullable != null && serviceUuid_nullable.equals(service_ith.getUuid()) )
			{
				final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service_ith);

				for( int j = 0; j < charList_native.size(); j++ )
				{
					final BluetoothGattCharacteristic char_jth = charList_native.get(j);

					if( charUuid_nullable == null || charUuid_nullable != null && charUuid_nullable.equals(char_jth.getUuid()) )
					{
						final List<BluetoothGattDescriptor> descriptors = getNativeDescriptorList_original(char_jth);

						if( forEach_nullable != null )
						{
							if( Utils.doForEach_break(forEach_nullable, descriptors) )
							{
								return null;
							}
						}
						else
						{
							toReturn.addAll(descriptors);
						}
					}
				}
			}
		}

		return toReturn;
	}

	public Iterator<BluetoothGattService> getServices()
	{
		return getServices_List().iterator();
	}

	public List<BluetoothGattService> getServices_List()
	{
		return getNativeServiceList_cloned();
	}

	public Iterator<BluetoothGattCharacteristic> getCharacteristics(final UUID serviceUuid_nullable)
	{
		return getCharacteristics_List(serviceUuid_nullable).iterator();
	}

	public List<BluetoothGattCharacteristic> getCharacteristics_List(final UUID serviceUuid_nullable)
	{
		return collectAllNativeCharacteristics(serviceUuid_nullable, /*forEach=*/null);
	}

	private BluetoothGattDescriptor getDescriptor(final BluetoothGattCharacteristic characteristic, final UUID descUuid)
	{
		final List<BluetoothGattDescriptor> list_native = getNativeDescriptorList_original(characteristic);

		for( int i = 0; i < list_native.size(); i++ )
		{
			final BluetoothGattDescriptor ith = list_native.get(i);

			if( ith.getUuid().equals(descUuid) )
			{
				return ith;
			}
		}

		return null;
	}

	private BluetoothGattDescriptor getDescriptor(final BluetoothGattService service, final UUID charUuid_nullable, final UUID descUuid)
	{
		final List<BluetoothGattCharacteristic> charList = getNativeCharacteristicList_original(service);

		for( int j = 0; j < charList.size(); j++ )
		{
			final BluetoothGattCharacteristic char_jth = charList.get(j);

			if( charUuid_nullable == null || charUuid_nullable != null && charUuid_nullable.equals(char_jth.getUuid()) )
			{
				final BluetoothGattDescriptor descriptor = getDescriptor(char_jth, descUuid);

				if( descriptor != null )
				{
					return descriptor;
				}
			}
		}

		return null;
	}

	public Iterator<BluetoothGattDescriptor> getDescriptors(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
	{
		return getDescriptors_List(serviceUuid_nullable, charUuid_nullable).iterator();
	}

	public List<BluetoothGattDescriptor> getDescriptors_List(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
	{
		return collectAllNativeDescriptors(serviceUuid_nullable, charUuid_nullable, null);
	}

	public BluetoothGattDescriptor getDescriptor(final UUID serviceUuid_nullable, final UUID charUuid_nullable, final UUID descUuid)
	{
		if( serviceUuid_nullable == null )
		{
			final List<BluetoothGattService> serviceList = getNativeServiceList_original();

			for( int i = 0; i < serviceList.size(); i++ )
			{
				final BluetoothGattService service_ith = serviceList.get(i);
				final BluetoothGattDescriptor descriptor = getDescriptor(service_ith, charUuid_nullable, descUuid);

				if( descriptor != null )
				{
					return descriptor;
				}
			}
		}
		else
		{
			final BluetoothGattService service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

			if( service_nullable == null )
			{
				return null;
			}
			else
			{
				return getDescriptor(service_nullable, charUuid_nullable, descUuid);
			}
		}

		return null;
	}

	public void getServices(final Object forEach)
	{
		Utils.doForEach_break(forEach, getNativeServiceList_original());
	}

	public void getCharacteristics(final UUID serviceUuid, final Object forEach)
	{
		collectAllNativeCharacteristics(serviceUuid, forEach);
	}

	public void getDescriptors(final UUID serviceUuid, final UUID charUuid, final Object forEach)
	{
		collectAllNativeDescriptors(serviceUuid, charUuid, forEach);
	}

	protected static boolean equals(final BluetoothGattService one, final BluetoothGattService another)
	{
		if( one == another )
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
