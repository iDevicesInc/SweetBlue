package com.idevicesinc.sweetblue;

import android.app.Service;
import android.bluetooth.BluetoothGattService;

class P_ServerServiceManager
{
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

	private boolean alreadyHas(final BluetoothGattService service)
	{

	}

	public BleServer.ServiceAddListener.ServiceAddEvent addService_native(final BluetoothGattService service, final BleServer.ServiceAddListener listener_specific)
	{
		if( m_server.isNull() )
		{
			final BleServer.ServiceAddListener.ServiceAddEvent e = BleServer.ServiceAddListener.ServiceAddEvent.EARLY_OUT(m_server, service, BleServer.ServiceAddListener.Status.NULL_SERVER);

			invokeListeners(e, listener_specific);

			return e;
		}
		else if( alreadyHas(service) )
		{
			final BleServer.ServiceAddListener.ServiceAddEvent e = BleServer.ServiceAddListener.ServiceAddEvent.EARLY_OUT(m_server, service, BleServer.ServiceAddListener.Status.DUPLICATE);

			invokeListeners(e, listener_specific);

			return e;
		}
		else
		{

			final P_Task_AddService task = new P_Task_AddService(m_server, service, listener_specific);
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
