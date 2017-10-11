package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import com.idevicesinc.sweetblue.BleServer.IncomingListener;
import static com.idevicesinc.sweetblue.BleServer.IncomingListener.*;
import static com.idevicesinc.sweetblue.BleServer.OutgoingListener.*;

import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;

class P_BleServer_Listeners extends BluetoothGattServerCallback
{
	private final BleServer m_server;
	private final P_Logger m_logger;
	private final P_TaskQueue m_queue;

	final PA_Task.I_StateListener m_taskStateListener = new PA_Task.I_StateListener()
	{
		@Override public void onStateChange(PA_Task task, PE_TaskState state)
		{
			if ( task.getClass() == P_Task_DisconnectServer.class )
			{
				if( state.isEndingState() )
				{
					if( state == PE_TaskState.SUCCEEDED )
					{
						final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

						m_server.onNativeDisconnect(task_cast.m_nativeDevice.getAddress(), task_cast.isExplicit(), task_cast.getGattStatus());
					}
				}
			}
			else if( task.getClass() == P_Task_ConnectServer.class )
			{
				if( state.isEndingState() )
				{
					final P_Task_ConnectServer task_cast = (P_Task_ConnectServer) task;

					if( state == PE_TaskState.SUCCEEDED )
					{
						m_server.onNativeConnect(task_cast.m_nativeDevice.getAddress(), task_cast.isExplicit());
					}
					else if( state == PE_TaskState.REDUNDANT )
					{
						// nothing to do, but maybe should assert?
					}
					else if( state == PE_TaskState.FAILED_IMMEDIATELY )
					{
						final BleServer.ConnectionFailListener.Status status = task_cast.getStatus();

						if( status == BleServer.ConnectionFailListener.Status.SERVER_OPENING_FAILED || status == BleServer.ConnectionFailListener.Status.NATIVE_CONNECTION_FAILED_IMMEDIATELY )
						{
							m_server.onNativeConnectFail(task_cast.m_nativeDevice, status, task_cast.getGattStatus());
						}
						else
						{
							m_server.getManager().ASSERT(false, "Didn't expect server failed-immediately status to be something else.");

							m_server.onNativeConnectFail(task_cast.m_nativeDevice, BleServer.ConnectionFailListener.Status.NATIVE_CONNECTION_FAILED_IMMEDIATELY, task_cast.getGattStatus());
						}
					}
					else if( state == PE_TaskState.FAILED )
					{
						m_server.onNativeConnectFail(task_cast.m_nativeDevice, BleServer.ConnectionFailListener.Status.NATIVE_CONNECTION_FAILED_EVENTUALLY, task_cast.getGattStatus());
					}
					else if( state == PE_TaskState.TIMED_OUT )
					{
						m_server.onNativeConnectFail(task_cast.m_nativeDevice, BleServer.ConnectionFailListener.Status.TIMED_OUT, task_cast.getGattStatus());
					}
					else if( state == PE_TaskState.SOFTLY_CANCELLED )
					{
						// do nothing...this was handled upstream back in time
					}
					else
					{
						m_server.getManager().ASSERT(false, "Did not expect ending state " + state + " for connect task failure.");

						m_server.onNativeConnectFail(task_cast.m_nativeDevice, BleServer.ConnectionFailListener.Status.NATIVE_CONNECTION_FAILED_EVENTUALLY, task_cast.getGattStatus());
					}
				}
			}
		}
	};

	public P_BleServer_Listeners( BleServer server )
	{
		m_server = server;
		m_logger = m_server.getManager().getLogger();
		m_queue = m_server.getManager().getTaskQueue();
	}

	private boolean hasCurrentDisconnectTaskFor(final BluetoothDevice device)
	{
		final P_Task_DisconnectServer disconnectTask = m_queue.getCurrent(P_Task_DisconnectServer.class, m_server);

		return disconnectTask != null && disconnectTask.isFor(m_server, device.getAddress());
	}

	private boolean hasCurrentConnectTaskFor(final BluetoothDevice device)
	{
		final P_Task_ConnectServer connectTask = m_queue.getCurrent(P_Task_ConnectServer.class, m_server);

		return connectTask != null && connectTask.isFor(m_server, device.getAddress());
	}

	private void failDisconnectTaskIfPossibleFor(final BluetoothDevice device)
	{
		final P_Task_DisconnectServer disconnectTask = m_queue.getCurrent(P_Task_DisconnectServer.class, m_server);

		if( disconnectTask != null && disconnectTask.isFor(m_server, device.getAddress()) )
		{
			m_queue.fail(P_Task_DisconnectServer.class, m_server);
		}
	}

	private boolean failConnectTaskIfPossibleFor(final BluetoothDevice device, final int gattStatus)
	{
		if( hasCurrentConnectTaskFor(device) )
		{
			final P_Task_ConnectServer connectTask = m_queue.getCurrent(P_Task_ConnectServer.class, m_server);

			connectTask.onNativeFail(gattStatus);

			return true;
		}
		else
		{
			return false;
		}
	}

	private void onNativeConnectFail(final BluetoothDevice nativeDevice, final int gattStatus)
	{
		//--- DRK > NOTE: Making an assumption that the underlying stack agrees that the connection state is STATE_DISCONNECTED.
		//---				This is backed up by basic testing, but even if the underlying stack uses a different value, it can probably
		//---				be assumed that it will eventually go to STATE_DISCONNECTED, so SweetBlue library logic is sounder "living under the lie" for a bit regardless.
		m_server.m_nativeWrapper.updateNativeConnectionState(nativeDevice.getAddress(), BluetoothProfile.STATE_DISCONNECTED);

		if( hasCurrentConnectTaskFor(nativeDevice) )
		{
			final P_Task_ConnectServer connectTask = m_queue.getCurrent(P_Task_ConnectServer.class, m_server);

			connectTask.onNativeFail(gattStatus);
		}
		else
		{
			m_server.onNativeConnectFail(nativeDevice, BleServer.ConnectionFailListener.Status.NATIVE_CONNECTION_FAILED_EVENTUALLY, gattStatus);
		}
	}

	@Override public void onConnectionStateChange(final BluetoothDevice device, final int gattStatus, final int newState)
	{
		m_server.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				onConnectionStateChange_updateThread(device, gattStatus, newState);
			}
		});
	}

    private void onConnectionStateChange_updateThread(final BluetoothDevice device, final int gattStatus, final int newState)
	{
		m_logger.log_conn_status(gattStatus, m_logger.gattConn(newState));

		if( newState == BluetoothProfile.STATE_DISCONNECTED )
		{
			m_server.m_nativeWrapper.updateNativeConnectionState(device.getAddress(), newState);

			final boolean wasConnecting = hasCurrentConnectTaskFor(device);

			if( !failConnectTaskIfPossibleFor(device, gattStatus) )
			{
				if( hasCurrentDisconnectTaskFor(device) )
				{
					final P_Task_DisconnectServer disconnectTask = m_queue.getCurrent(P_Task_DisconnectServer.class, m_server);

					disconnectTask.onNativeSuccess(gattStatus);
				}
				else
				{
					m_server.onNativeDisconnect(device.getAddress(), /*explicit=*/false, gattStatus);
				}
			}
		}
		else if( newState == BluetoothProfile.STATE_CONNECTING )
		{
			if( Utils.isSuccess(gattStatus) )
			{
				m_server.m_nativeWrapper.updateNativeConnectionState(device.getAddress(), newState);

//						m_device.onConnecting(/*definitelyExplicit=*/false, /*isReconnect=*/false, P_BondManager.OVERRIDE_EMPTY_STATES, /*bleConnect=*/true);

				failDisconnectTaskIfPossibleFor(device);

				if( !hasCurrentConnectTaskFor(device) )
				{
					final P_Task_ConnectServer task = new P_Task_ConnectServer(m_server, device, m_taskStateListener, /*explicit=*/false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING);

					m_queue.add(task);
				}
				else
				{
					m_server.onNativeConnecting_implicit(device.getAddress());
				}
			}
			else
			{
				onNativeConnectFail(device, gattStatus);
			}
		}
		else if( newState == BluetoothProfile.STATE_CONNECTED )
		{
			if( Utils.isSuccess(gattStatus) )
			{
				m_server.m_nativeWrapper.updateNativeConnectionState(device.getAddress(), newState);

				failDisconnectTaskIfPossibleFor(device);

				if( hasCurrentConnectTaskFor(device) )
				{
					m_queue.succeed(P_Task_ConnectServer.class, m_server);
				}
				else
				{
					m_server.onNativeConnect(device.getAddress(), /*explicit=*/false);
				}
			}
			else
			{
				onNativeConnectFail(device, gattStatus);
			}
		}
		//--- DRK > NOTE: never seen this case happen with BleDevice, we'll see if it happens with the server.
		else if( newState == BluetoothProfile.STATE_DISCONNECTING )
		{
			m_server.m_nativeWrapper.updateNativeConnectionState(device.getAddress(), newState);

			//--- DRK > error level just so it's noticeable...never seen this with client connections so we'll see if it hits with server ones.
			m_logger.e("Actually natively disconnecting server!");

			if( !hasCurrentDisconnectTaskFor(device) )
			{
				P_Task_DisconnectServer task = new P_Task_DisconnectServer(m_server, device, m_taskStateListener, /*explicit=*/false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING);

				m_queue.add(task);
			}

			failConnectTaskIfPossibleFor(device, gattStatus);
		}
		else
		{
			m_server.m_nativeWrapper.updateNativeConnectionState(device);
		}
    }

	@Override public void onServiceAdded(final int gattStatus, final BluetoothGattService service)
	{
		m_server.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				onServiceAdded_updateThread(gattStatus, service);
			}
		});
	}

	private void onServiceAdded_updateThread(final int gattStatus, final BluetoothGattService service)
	{
		final P_Task_AddService task = m_queue.getCurrent(P_Task_AddService.class, m_server);

		if( task != null && task.getService().equals(service) )
		{
			task.onServiceAdded(gattStatus, service);
		}
		else
		{
			final BleServer.ServiceAddListener.Status status = Utils.isSuccess(gattStatus) ? BleServer.ServiceAddListener.Status.SUCCESS : BleServer.ServiceAddListener.Status.FAILED_EVENTUALLY;
			final BleServer.ServiceAddListener.ServiceAddEvent e = new BleServer.ServiceAddListener.ServiceAddEvent
			(
				m_server, service, status, gattStatus, /*solicited=*/false
			);

			m_server.serviceMngr_server().invokeListeners(e, null);
		}
    }

	private OutgoingEvent newEarlyOutResponse_Read(final BluetoothDevice device, final UUID serviceUuid, final UUID charUuid, final UUID descUuid_nullable, final int requestId, final int offset, final BleServer.OutgoingListener.Status status)
	{
		final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

		final OutgoingEvent e = new OutgoingEvent
		(
			m_server, device, serviceUuid, charUuid, descUuid_nullable, Type.READ, target, P_Const.EMPTY_BYTE_ARRAY, P_Const.EMPTY_BYTE_ARRAY,
			requestId, offset, /*responseNeeded=*/true, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true
		);

		return e;
	}

	private void onReadRequest_updateThread(final BluetoothDevice device, final int requestId, final int offset, final UUID serviceUuid, final UUID charUuid, final UUID descUuid_nullable)
	{
		final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

		final IncomingListener listener = m_server.getListener_Incoming() != null ? m_server.getListener_Incoming() : m_server.getManager().m_defaultServerIncomingListener;

		if( listener == null )
		{
			m_server.invokeOutgoingListeners(newEarlyOutResponse_Read(device, serviceUuid, charUuid, /*descUuid=*/null, requestId, offset, Status.NO_REQUEST_LISTENER_SET), null);
		}
		else
		{
			final IncomingEvent requestEvent = new IncomingEvent
			(
				m_server, device, serviceUuid, charUuid, descUuid_nullable, Type.READ, target, P_Const.EMPTY_BYTE_ARRAY, requestId, offset, /*responseNeeded=*/true
			);

			final IncomingListener.Please please = listener.onEvent(requestEvent);

			if( please == null)
			{
				m_server.invokeOutgoingListeners(newEarlyOutResponse_Read(device, serviceUuid, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), null);
			}
			else
			{
				final boolean attemptResponse = please.m_respond;

				if( attemptResponse )
				{
					final P_Task_SendReadWriteResponse responseTask = new P_Task_SendReadWriteResponse(m_server, requestEvent, please);

					m_queue.add(responseTask);
				}
				else
				{
					m_server.invokeOutgoingListeners(newEarlyOutResponse_Read(device, serviceUuid, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), please.m_outgoingListener);
				}
			}
		}
	}

	@Override public void onCharacteristicReadRequest(final BluetoothDevice device, final int requestId, final int offset, final BluetoothGattCharacteristic characteristic)
	{
		m_server.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				onReadRequest_updateThread(device, requestId, offset, characteristic.getService().getUuid(), characteristic.getUuid(), /*descUuid=*/null);
			}
		});
    }

	@Override public void onDescriptorReadRequest(final BluetoothDevice device, final int requestId, final int offset, final BluetoothGattDescriptor descriptor)
	{
		m_server.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				onReadRequest_updateThread(device, requestId, offset, descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(), descriptor.getUuid());
			}
		});
	}

	private OutgoingEvent newEarlyOutResponse_Write(final BluetoothDevice device, final Type type, final UUID serviceUuid, final UUID charUuid, final UUID descUuid_nullable, final int requestId, final int offset, final BleServer.OutgoingListener.Status status)
	{
		final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

		final OutgoingEvent e = new OutgoingEvent
		(
			m_server, device, serviceUuid, charUuid, descUuid_nullable, type, target, P_Const.EMPTY_BYTE_ARRAY, P_Const.EMPTY_BYTE_ARRAY,
			requestId, offset, /*responseNeeded=*/true, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true
		);

		return e;
	}

	private void onWriteRequest_updateThread(final BluetoothDevice device, final byte[] data, final int requestId, final int offset, final boolean preparedWrite, final boolean responseNeeded, final UUID serviceUuid, final UUID charUuid, final UUID descUuid_nullable)
	{
		final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;
		final Type type = preparedWrite ? Type.PREPARED_WRITE : Type.WRITE;

		final IncomingListener listener = m_server.getListener_Incoming() != null ? m_server.getListener_Incoming() : m_server.getManager().m_defaultServerIncomingListener;

		if( listener == null )
		{
			m_server.invokeOutgoingListeners(newEarlyOutResponse_Write(device, type, serviceUuid, charUuid, /*descUuid=*/null, requestId, offset, Status.NO_REQUEST_LISTENER_SET), null);
		}
		else
		{
			final IncomingEvent requestEvent = new IncomingEvent
			(
				m_server, device, serviceUuid, charUuid, descUuid_nullable, type, target, data, requestId, offset, responseNeeded
			);

			final IncomingListener.Please please = listener.onEvent(requestEvent);

			if( please == null)
			{
				m_server.invokeOutgoingListeners(newEarlyOutResponse_Write(device, type, serviceUuid, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), null);
			}
			else
			{
				final boolean attemptResponse = please.m_respond;

				if( attemptResponse )
				{
					final P_Task_SendReadWriteResponse responseTask = new P_Task_SendReadWriteResponse(m_server, requestEvent, please);

					m_queue.add(responseTask);
				}
				else
				{
					m_server.invokeOutgoingListeners(newEarlyOutResponse_Write(device, type, serviceUuid, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), please.m_outgoingListener);
				}
			}
		}
	}

	@Override public void onCharacteristicWriteRequest(final BluetoothDevice device, final int requestId, final BluetoothGattCharacteristic characteristic, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value)
	{
		m_server.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				onWriteRequest_updateThread(device, value, requestId, offset, preparedWrite, responseNeeded, characteristic.getService().getUuid(), characteristic.getUuid(), /*descUuid=*/null);
			}
		});
    }

	@Override public void onDescriptorWriteRequest( final BluetoothDevice device, final int requestId, final BluetoothGattDescriptor descriptor, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value)
	{
		m_server.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				onWriteRequest_updateThread(device, value, requestId, offset, preparedWrite, responseNeeded, descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(), descriptor.getUuid());
			}
		});
    }

	@Override public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute)
	{
    }

	@Override public void onNotificationSent( final BluetoothDevice device, final int gattStatus )
	{
		m_server.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				onNotificationSent_updateThread(device, gattStatus);
			}
		});
	}

	private void onNotificationSent_updateThread(final BluetoothDevice device, final int gattStatus)
	{
		final P_Task_SendNotification task = m_queue.getCurrent(P_Task_SendNotification.class, m_server);

		if( task != null && task.m_macAddress.equals(device.getAddress()) )
		{
			task.onNotificationSent(device, gattStatus);
		}
		else
		{
			final BleServer.OutgoingListener.OutgoingEvent e = new BleServer.OutgoingListener.OutgoingEvent
			(
				m_server, device, Uuids.INVALID, Uuids.INVALID, BleServer.ExchangeListener.ExchangeEvent.NON_APPLICABLE_UUID, Type.NOTIFICATION,
				BleServer.ExchangeListener.Target.CHARACTERISTIC, P_Const.EMPTY_BYTE_ARRAY, P_Const.EMPTY_BYTE_ARRAY, BleServer.ExchangeListener.ExchangeEvent.NON_APPLICABLE_REQUEST_ID,
				/*offset=*/0, /*responseNeeded=*/false, BleServer.OutgoingListener.Status.SUCCESS, BleStatuses.GATT_STATUS_NOT_APPLICABLE, gattStatus, /*solicited=*/false
			);

			m_server.invokeOutgoingListeners(e, null);
		}
    }
}
