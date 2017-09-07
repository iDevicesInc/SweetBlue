package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Pointer;
import java.util.List;
import java.util.UUID;


final class P_ServerServiceManager extends PA_ServiceManager
{
	private final BleServer m_server;

	private BleServer.ServiceAddListener m_listener = null;

	P_ServerServiceManager(final BleServer server)
	{
		m_server = server;
	}

	public final void setListener(BleServer.ServiceAddListener listener)
	{
		m_listener = listener;
	}

	@Override public final BleServiceWrapper getServiceDirectlyFromNativeNode(final UUID uuid)
	{
		final P_NativeServerLayer server_native = m_server.getNativeLayer();

		if( server_native.isServerNull() )
		{
			return BleServiceWrapper.NULL;
		}
		else
		{
			final BluetoothGattService service = server_native.getService(uuid);

			return new BleServiceWrapper(service);
		}
	}

	@Override protected final List<BluetoothGattService> getNativeServiceList_original()
	{
		final P_NativeServerLayer server_native = m_server.getNativeLayer();

		if( server_native.isServerNull() )
		{
			return P_Const.EMPTY_SERVICE_LIST;
		}
		else
		{
			final List<BluetoothGattService> list_native = server_native.getServices();

			return list_native == null ? P_Const.EMPTY_SERVICE_LIST : list_native;
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
		final BleServiceWrapper existingServiceFromServer = getServiceDirectlyFromNativeNode(serviceToBeAdded.getUuid());

		if( !existingServiceFromServer.isNull() && equals(existingServiceFromServer.getService(), serviceToBeAdded) )
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

					if( PA_ServiceManager.equals(service_ith, serviceToBeAdded) )
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

	public final BleServer.ServiceAddListener.ServiceAddEvent addService(final BleService service, final BleServer.ServiceAddListener listener_specific_nullable)
	{
		service.init();

		return addService_native(service.m_native, listener_specific_nullable);
	}

	public final BleServer.ServiceAddListener.ServiceAddEvent addService_native(final BluetoothGattService service, final BleServer.ServiceAddListener listener_specific_nullable)
	{
		if( m_server.isNull() )
		{
			final BleServer.ServiceAddListener.ServiceAddEvent e = BleServer.ServiceAddListener.ServiceAddEvent.EARLY_OUT(m_server, service, BleServer.ServiceAddListener.Status.NULL_SERVER);

			invokeListeners(e, listener_specific_nullable);

			return e;
		}
		else if( false == m_server.getManager().is(BleManagerState.ON) )
		{
			final BleServer.ServiceAddListener.ServiceAddEvent e = BleServer.ServiceAddListener.ServiceAddEvent.EARLY_OUT(m_server, service, BleServer.ServiceAddListener.Status.BLE_NOT_ON);

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

	public final void removeAll(final BleServer.ServiceAddListener.Status status)
	{
		final P_NativeServerLayer server_native = m_server.getNativeLayer();

		if( !server_native.isServerNull() )
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

	public final BluetoothGattService remove(final UUID serviceUuid)
	{
		final BleServiceWrapper service = getServiceDirectlyFromNativeNode(serviceUuid);

		if( service.isNull() )
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
			final P_NativeServerLayer server_native = m_server.getNativeLayer();

			if( server_native.isServerNull() )
			{
				m_server.getManager().ASSERT(false, "Didn't expect native server to be null when removing characteristic.");

				return null;
			}
			else
			{
				server_native.removeService(service.getService());

				return service.getService();
			}
		}
	}

	public final void invokeListeners(final BleServer.ServiceAddListener.ServiceAddEvent e, final BleServer.ServiceAddListener listener_specific_nullable)
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
