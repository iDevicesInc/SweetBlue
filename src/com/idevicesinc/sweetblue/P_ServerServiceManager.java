package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.EmptyIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

class P_ServerServiceManager
{
	private static final List<BluetoothGattService> EMPTY_SERVICE_LIST = new ArrayList<>();
	private static final List<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_LIST = new ArrayList<>();
	private static final Iterator<BluetoothGattService> EMPTY_SERVICE_ITERATOR = new EmptyIterator<>();
	private static final Iterator<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_ITERATOR = new EmptyIterator<>();

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
		BluetoothGattService service = m_server.getNative().getService(uuid);

		return service;
	}

	private List<BluetoothGattService> getNativeServiceList()
	{
		final List<BluetoothGattService> list_native = m_server.getNative().getServices();

		return list_native == null ? EMPTY_SERVICE_LIST : new ArrayList<>(list_native);
	}

	private List<BluetoothGattCharacteristic> getNativeCharacteristicList(final BluetoothGattService service)
	{
		final List<BluetoothGattCharacteristic> list_native = service.getCharacteristics();

		return list_native == null ? EMPTY_CHARACTERISTIC_LIST : new ArrayList<>(list_native);
	}

	public Iterator<BluetoothGattService> getServices()
	{
		return getNativeServiceList().iterator();
	}

	public List<BluetoothGattService> getServices_List()
	{
		return getNativeServiceList();
	}

	public Iterator<BluetoothGattCharacteristic> getCharacteristics(final UUID serviceUuid_nullable)
	{
		if( serviceUuid_nullable == null )
		{

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
				service_nullable.getCharacteristics();
			}
		}
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
