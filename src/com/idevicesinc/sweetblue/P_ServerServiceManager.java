package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.EmptyIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

class P_ServerServiceManager
{
	private static final Iterator<BluetoothGattService> EMPTY_SERVICE_ITERATOR = new EmptyIterator<>();

	private static final List<BluetoothGattService> EMPTY_SERVICE_LIST = new ArrayList<BluetoothGattService>()
	{
		@Override public Iterator<BluetoothGattService> iterator()
		{
			return EMPTY_SERVICE_ITERATOR;
		}
	};

	private static final List<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_LIST = new ArrayList<>();
	private static final Iterator<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_ITERATOR = new EmptyIterator<>();

	private static final List<BluetoothGattDescriptor> EMPTY_DESCRIPTOR_LIST = new ArrayList<>();

	private final BleServer m_server;

	private BleServer.ServiceAddListener m_listener = null;

	P_ServerServiceManager(final BleServer server)
	{
		m_server = server;
	}

	public void setListener(BleServer.ServiceAddListener listener)
	{
		m_listener = listener;
	}

	public BluetoothGattService getService(final UUID uuid)
	{
		final BluetoothGattService service = m_server.getNative().getService(uuid);

		return service;
	}

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
			final BluetoothGattService service_nullable = getService(serviceUuid_nullable);

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

	private List<BluetoothGattService> getNativeServiceList_original()
	{
		final List<BluetoothGattService> list_native = m_server.getNative().getServices();

		return list_native == null ? EMPTY_SERVICE_LIST : list_native;
	}

	private List<BluetoothGattService> getNativeServiceList_cloned()
	{
		final List<BluetoothGattService> list_native = getNativeServiceList_original();

		return list_native == EMPTY_SERVICE_LIST ? list_native : new ArrayList<>(list_native);
	}

	private List<BluetoothGattCharacteristic> getNativeCharacteristicList_original(final BluetoothGattService service)
	{
		final List<BluetoothGattCharacteristic> list_native = service.getCharacteristics();

		return list_native == null ? EMPTY_CHARACTERISTIC_LIST : list_native;
	}

	private List<BluetoothGattCharacteristic> getNativeCharacteristicList_cloned(final BluetoothGattService service)
	{
		final List<BluetoothGattCharacteristic> list_native = getNativeCharacteristicList_original(service);

		return list_native == EMPTY_CHARACTERISTIC_LIST ? list_native : new ArrayList<>(list_native);
	}

	private List<BluetoothGattDescriptor> getNativeDescriptorList_original(final BluetoothGattCharacteristic characteristic)
	{
		final List<BluetoothGattDescriptor> list_native = characteristic.getDescriptors();

		return list_native == null ? EMPTY_DESCRIPTOR_LIST : list_native;
	}

	private List<BluetoothGattDescriptor> getNativeDescriptorList_cloned(final BluetoothGattCharacteristic characteristic)
	{
		final List<BluetoothGattDescriptor> list_native = getNativeDescriptorList_original(characteristic);

		return list_native == EMPTY_DESCRIPTOR_LIST ? list_native : new ArrayList<>(list_native);
	}

	private List<BluetoothGattCharacteristic> collectAllNativeCharacteristics()
	{
		final ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
		final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

		for( int i = 0; i < serviceList_native.size(); i++ )
		{
			final BluetoothGattService service_ith = serviceList_native.get(i);

			characteristics.addAll(getNativeCharacteristicList_original(service_ith));
		}

		return characteristics;
	}

	public Iterator<BluetoothGattService> getServices()
	{
		return getNativeServiceList_cloned().iterator();
	}

	public List<BluetoothGattService> getServices_List()
	{
		return getNativeServiceList_cloned();
	}

	public Iterator<BluetoothGattCharacteristic> getCharacteristics(final UUID serviceUuid_nullable)
	{
		if( serviceUuid_nullable == null )
		{
			return collectAllNativeCharacteristics().iterator();
		}
		else
		{
			final BluetoothGattService service_nullable = getService(serviceUuid_nullable);

			if( service_nullable == null )
			{
				return EMPTY_CHARACTERISTIC_ITERATOR;
			}
			else
			{
				return getNativeCharacteristicList_original(service_nullable).iterator();
			}
		}
	}

	public List<BluetoothGattCharacteristic> getCharacteristics_List(final UUID serviceUuid_nullable)
	{
		if( serviceUuid_nullable == null )
		{
			return collectAllNativeCharacteristics();
		}
		else
		{
			final BluetoothGattService service_nullable = getService(serviceUuid_nullable);

			if( service_nullable == null )
			{
				return EMPTY_CHARACTERISTIC_LIST;
			}
			else
			{
				return getNativeCharacteristicList_cloned(service_nullable);
			}
		}
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
			final BluetoothGattService service_nullable = getService(serviceUuid_nullable);

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



	private boolean alreadyHas(final BluetoothGattService service)
	{

	}

	public BleServer.ServiceAddListener.ServiceAddEvent addService(final BleService service, final BleServer.ServiceAddListener listener_specific_nullable)
	{
		service.init();

		return addService_native(service.m_native, listener_specific_nullable);
	}

	public BleServer.ServiceAddListener.ServiceAddEvent addService_native(final BluetoothGattService service, final BleServer.ServiceAddListener listener_specific_nullable)
	{
		if( m_server.isNull() )
		{
			final BleServer.ServiceAddListener.ServiceAddEvent e = BleServer.ServiceAddListener.ServiceAddEvent.EARLY_OUT(m_server, service, BleServer.ServiceAddListener.Status.NULL_SERVER);

			invokeListeners(e, listener_specific_nullable);

			return e;
		}
		else if( alreadyHas(service) )
		{
			final BleServer.ServiceAddListener.ServiceAddEvent e = BleServer.ServiceAddListener.ServiceAddEvent.EARLY_OUT(m_server, service, BleServer.ServiceAddListener.Status.DUPLICATE);

			invokeListeners(e, listener_specific_nullable);

			return e;
		}
		else
		{

			final P_Task_AddService task = new P_Task_AddService(m_server, service, listener_specific_nullable);
			m_server.getManager().getTaskQueue().add(task);

			return BleServer.ServiceAddListener.ServiceAddEvent.NULL(m_server, service);
		}
	}

	public void invokeListeners(final BleServer.ServiceAddListener.ServiceAddEvent e, final BleServer.ServiceAddListener listener_specific_nullable)
	{
		if( listener_specific_nullable != null )
		{
			listener_specific_nullable.onEvent(e);
		}

		if( m_listener != null )
		{
			m_listener.onEvent(e);
		}

		if( m_server.getManager().m_serviceAddListener != null )
		{
			m_server.getManager().m_serviceAddListener.onEvent(e);
		}
	}
}
