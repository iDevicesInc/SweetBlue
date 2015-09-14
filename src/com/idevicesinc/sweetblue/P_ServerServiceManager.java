package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.utils.Pointer;

import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.Pointer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

class P_ServerServiceManager
{
	private static final Iterator<BluetoothGattService> EMPTY_SERVICE_ITERATOR = new EmptyIterator<BluetoothGattService>();

	private static final List<BluetoothGattService> EMPTY_SERVICE_LIST = new ArrayList<BluetoothGattService>()
	{
		@Override public Iterator<BluetoothGattService> iterator()
		{
			return EMPTY_SERVICE_ITERATOR;
		}
	};

	private static final List<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_LIST = new ArrayList<BluetoothGattCharacteristic>();
	private static final Iterator<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_ITERATOR = new EmptyIterator<BluetoothGattCharacteristic>();

	private static final List<BluetoothGattDescriptor> EMPTY_DESCRIPTOR_LIST = new ArrayList<BluetoothGattDescriptor>();

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

	public BluetoothGattService getServiceDirectlyFromNativeServer(final UUID uuid)
	{
		final BluetoothGattServer server_native = m_server.getNative();

		if( server_native == null )
		{
			return null;
		}
		else
		{
			final BluetoothGattService service = server_native.getService(uuid);

			return service;
		}
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
			final BluetoothGattService service_nullable = getServiceDirectlyFromNativeServer(serviceUuid_nullable);

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
		final BluetoothGattServer server_native = m_server.getNative();

		if( server_native == null )
		{
			return EMPTY_SERVICE_LIST;
		}
		else
		{
			final List<BluetoothGattService> list_native = server_native.getServices();

			return list_native == null ? EMPTY_SERVICE_LIST : list_native;
		}
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

	private List<BluetoothGattCharacteristic> collectAllNativeCharacteristics(final UUID serviceUuid_nullable)
	{
		final ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<BluetoothGattCharacteristic>();
		final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

		for( int i = 0; i < serviceList_native.size(); i++ )
		{
			final BluetoothGattService service_ith = serviceList_native.get(i);

			if( serviceUuid_nullable == null || serviceUuid_nullable != null && serviceUuid_nullable.equals(service_ith.getUuid()) )
			{
				characteristics.addAll(getNativeCharacteristicList_original(service_ith));
			}
		}

		return characteristics;
	}

	private List<BluetoothGattDescriptor> collectAllNativeDescriptors(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
	{
		final ArrayList<BluetoothGattDescriptor> toReturn = new ArrayList<BluetoothGattDescriptor>();
		final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

		for( int i = 0; i < serviceList_native.size(); i++ )
		{
			final BluetoothGattService service_ith = serviceList_native.get(i);

			if( serviceUuid_nullable == null || serviceUuid_nullable != null && serviceUuid_nullable.equals(service_ith.getUuid()) )
			{
				final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service_ith);

				for( int j = 0; j < charList_native.size(); j++ )
				{
					final BluetoothGattCharacteristic char_ith = charList_native.get(j);

					if( charUuid_nullable == null || charUuid_nullable != null && charUuid_nullable.equals(char_ith.getUuid()) )
					{
						toReturn.addAll(getNativeDescriptorList_original(char_ith));
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
		return collectAllNativeCharacteristics(serviceUuid_nullable);
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
		return collectAllNativeDescriptors(serviceUuid_nullable, charUuid_nullable);
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
			final BluetoothGattService service_nullable = getServiceDirectlyFromNativeServer(serviceUuid_nullable);

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

	private static boolean equals(final BluetoothGattService one, final BluetoothGattService another)
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

	private void getTasks(ForEach_Breakable<P_Task_AddService> forEach)
	{
		final P_TaskQueue queue = m_server.getManager().getTaskQueue();
		final List<PA_Task> queue_raw = queue.getRaw();

		for( int i = queue_raw.size()-1; i >= 0; i-- )
		{
			final PA_Task ith = queue_raw.get(i);

			if( ith.getClass() == P_Task_AddService.class && m_server.equals(ith.getServer()) )
			{
				final P_Task_AddService task_cast = (P_Task_AddService) ith;

				final ForEach_Breakable.Please please = forEach.next(task_cast);

				if( please.shouldBreak() )
				{
					return;
				}
			}
		}

		final PA_Task current = queue.getCurrent();

		if( current != null )
		{
			if( current.getClass() == P_Task_AddService.class && m_server.equals(current.getServer()) )
			{
				final P_Task_AddService current_cast = (P_Task_AddService) current;

				if( !current_cast.cancelledInTheMiddleOfExecuting() )
				{
					forEach.next(current_cast);
				}
			}
		}
	}

	private boolean alreadyAddingOrAdded(final BluetoothGattService serviceToBeAdded)
	{
		final BluetoothGattService existingServiceFromServer = getServiceDirectlyFromNativeServer(serviceToBeAdded.getUuid());

		if( equals(existingServiceFromServer, serviceToBeAdded) )
		{
			return true;
		}
		else
		{
			final Pointer<Boolean> mutableBool = new Pointer<Boolean>(false);

			getTasks(new ForEach_Breakable<P_Task_AddService>()
			{
				@Override public Please next(P_Task_AddService next)
				{
					final BluetoothGattService service_ith = next.getService();

					if( P_ServerServiceManager.equals(service_ith, serviceToBeAdded) )
					{
						mutableBool.value = true;

						return Please.doBreak();
					}
					else
					{
						return Please.doContinue();
					}
				}
			});

			return mutableBool.value;
		}
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
		else if( alreadyAddingOrAdded(service) )
		{
			final BleServer.ServiceAddListener.ServiceAddEvent e = BleServer.ServiceAddListener.ServiceAddEvent.EARLY_OUT(m_server, service, BleServer.ServiceAddListener.Status.DUPLICATE_SERVICE);

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

	public void removeAll(final BleServer.ServiceAddListener.Status status)
	{
		final BluetoothGattServer server_native = m_server.getNative();

		if( server_native != null )
		{
			server_native.clearServices();
		}

		getTasks(new ForEach_Breakable<P_Task_AddService>()
		{
			@Override public Please next(P_Task_AddService next)
			{
				next.cancel(status);

				return Please.doContinue();
			}
		});
	}

	public BluetoothGattService remove(final UUID serviceUuid)
	{
		final BluetoothGattService service = getServiceDirectlyFromNativeServer(serviceUuid);

		if( service == null )
		{
			final Pointer<BluetoothGattService> pointer = new Pointer<BluetoothGattService>();

			getTasks(new ForEach_Breakable<P_Task_AddService>()
			{
				@Override public Please next(final P_Task_AddService next)
				{
					if( next.getService().getUuid().equals(serviceUuid) )
					{
						pointer.value = next.getService();

						next.cancel(BleServer.ServiceAddListener.Status.CANCELLED_FROM_REMOVAL);

						return Please.doBreak();
					}
					else
					{
						return Please.doContinue();
					}
				}
			});

			return pointer.value;
		}
		else
		{
			final BluetoothGattServer server_native = m_server.getNative();

			if( server_native == null )
			{
				m_server.getManager().ASSERT(false, "Didn't expect native server to be null when removing characteristic.");

				return null;
			}
			else
			{
				server_native.removeService(service);

				return service;
			}
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
