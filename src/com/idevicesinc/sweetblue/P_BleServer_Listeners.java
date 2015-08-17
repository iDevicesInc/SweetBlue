package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import com.idevicesinc.sweetblue.BleServer.RequestListener;
import static com.idevicesinc.sweetblue.BleServer.RequestListener.*;
import static com.idevicesinc.sweetblue.BleServer.ResponseCompletionListener.*;
import com.idevicesinc.sweetblue.utils.UpdateLoop;

class P_BleServer_Listeners extends BluetoothGattServerCallback
{
	private final BleServer m_server;
	private final P_Logger m_logger;
	private final P_TaskQueue m_queue;

	final PA_Task.I_StateListener m_taskStateListener = new PA_Task.I_StateListener()
	{
		@Override public void onStateChange(PA_Task task, PE_TaskState state)
		{
			if (task.getClass() == P_Task_DisconnectServer.class)
			{
				if (state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT)
				{
					P_Task_Disconnect task_cast = (P_Task_Disconnect) task;

					m_server.onNativeDisconnect(task_cast.isExplicit());
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

    @Override public void onConnectionStateChange(final BluetoothDevice device, final int status, final int newState)
	{
		final UpdateLoop updateLoop = m_server.getManager().getUpdateLoop();

		updateLoop.postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				m_logger.log_status(status, m_logger.gattConn(newState));

				m_server.m_nativeWrapper.updateNativeConnectionState(device.getAddress(), newState);

				if (newState == BluetoothProfile.STATE_CONNECTED )
				{
				}
				else if (newState == BluetoothProfile.STATE_DISCONNECTED )
				{
				}
			}
		});
    }

	@Override public void onServiceAdded(int status, BluetoothGattService service)
	{
    }

	private BleServer.ResponseCompletionListener.ResponseCompletionEvent newEarlyOutResponse_Read(final BluetoothDevice device, final UUID charUuid, final UUID descUuid_nullable, final int requestId, final int offset, final BleServer.ResponseCompletionListener.Status status)
	{
		final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

		final ResponseCompletionEvent e = new ResponseCompletionEvent
		(
			m_server, device, charUuid, descUuid_nullable, Type.READ, target, BleServer.EMPTY_BYTE_ARRAY, BleServer.EMPTY_BYTE_ARRAY, requestId, offset, /*responseNeeded=*/true, status
		);

		return e;
	}

	private void onReadRequest(final BluetoothDevice device, final int requestId, final int offset, final UUID charUuid, final UUID descUuid_nullable)
	{
		final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

		final RequestListener listener = m_server.getListener_Request();

		if( listener == null )
		{
			m_server.invokeResponseListeners(newEarlyOutResponse_Read(device, charUuid, /*descUuid=*/null, requestId, offset, Status.NO_REQUEST_LISTENER_SET), null);
		}
		else
		{
			final RequestEvent requestEvent = new RequestEvent
			(
				m_server, device, charUuid, descUuid_nullable, Type.READ, target, BleServer.EMPTY_BYTE_ARRAY, requestId, offset, /*responseNeeded=*/true
			);

			final RequestListener.Please please = listener.onEvent(requestEvent);

			if( please == null)
			{
				m_server.invokeResponseListeners(newEarlyOutResponse_Read(device, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), null);
			}
			else
			{
				final boolean attemptResponse = please.m_respond;

				if( attemptResponse )
				{
					final P_Task_SendReadWriteResponse responseTask = new P_Task_SendReadWriteResponse(m_server, requestEvent, please);
				}
				else
				{
					m_server.invokeResponseListeners(newEarlyOutResponse_Read(device, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), please.m_responseListener);
				}
			}
		}
	}

	@Override public void onCharacteristicReadRequest(final BluetoothDevice device, final int requestId, final int offset, final BluetoothGattCharacteristic characteristic)
	{
		final UpdateLoop updateLoop = m_server.getManager().getUpdateLoop();

		updateLoop.postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				onReadRequest(device, requestId, offset, characteristic.getUuid(), /*descUuid=*/null);
			}
		});
    }

	@Override public void onDescriptorReadRequest(final BluetoothDevice device, final int requestId, final int offset, final BluetoothGattDescriptor descriptor)
	{
		final UpdateLoop updateLoop = m_server.getManager().getUpdateLoop();

		updateLoop.postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				onReadRequest(device, requestId, offset, descriptor.getCharacteristic().getUuid(), descriptor.getUuid());
			}
		});
	}

	private BleServer.ResponseCompletionListener.ResponseCompletionEvent newEarlyOutResponse_Write(final BluetoothDevice device, final Type type, final UUID charUuid, final UUID descUuid_nullable, final int requestId, final int offset, final BleServer.ResponseCompletionListener.Status status)
	{
		final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

		final ResponseCompletionEvent e = new ResponseCompletionEvent
		(
			m_server, device, charUuid, descUuid_nullable, type, target, BleServer.EMPTY_BYTE_ARRAY, BleServer.EMPTY_BYTE_ARRAY, requestId, offset, /*responseNeeded=*/true, status
		);

		return e;
	}


	private void onWriteRequest(final BluetoothDevice device, final int requestId, final int offset, final boolean preparedWrite, final boolean responseNeeded, final UUID charUuid, final UUID descUuid_nullable)
	{
		final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;
		final Type type = preparedWrite ? Type.PREPARED_WRITE : Type.WRITE;

		final RequestListener listener = m_server.getListener_Request();

		if( listener == null )
		{
			m_server.invokeResponseListeners(newEarlyOutResponse_Write(device, type, charUuid, /*descUuid=*/null, requestId, offset, Status.NO_REQUEST_LISTENER_SET), null);
		}
		else
		{
			final RequestEvent requestEvent = new RequestEvent
			(
				m_server, device, charUuid, descUuid_nullable, type, target, BleServer.EMPTY_BYTE_ARRAY, requestId, offset, responseNeeded
			);

			final RequestListener.Please please = listener.onEvent(requestEvent);

			if( please == null)
			{
				m_server.invokeResponseListeners(newEarlyOutResponse_Write(device, type, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), null);
			}
			else
			{
				final boolean attemptResponse = please.m_respond;

				if( attemptResponse )
				{
					final P_Task_SendReadWriteResponse responseTask = new P_Task_SendReadWriteResponse(m_server, requestEvent, please);
				}
				else
				{
					m_server.invokeResponseListeners(newEarlyOutResponse_Write(device, type, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), please.m_responseListener);
				}
			}
		}
	}

	@Override public void onCharacteristicWriteRequest(final BluetoothDevice device, final int requestId, final BluetoothGattCharacteristic characteristic, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value)
	{
		final UpdateLoop updateLoop = m_server.getManager().getUpdateLoop();

		updateLoop.postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				onWriteRequest(device, requestId, offset, preparedWrite, responseNeeded, characteristic.getUuid(), /*descUuid=*/null);
			}
		});
    }

	@Override public void onDescriptorWriteRequest( final BluetoothDevice device, final int requestId, final BluetoothGattDescriptor descriptor, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value)
	{
		final UpdateLoop updateLoop = m_server.getManager().getUpdateLoop();

		updateLoop.postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				onWriteRequest(device, requestId, offset, preparedWrite, responseNeeded, descriptor.getCharacteristic().getUuid(), descriptor.getUuid());
			}
		});
    }

	@Override public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute)
	{
    }

	@Override public void onNotificationSent( final BluetoothDevice device, final int status )
	{
		UpdateLoop updater = m_server.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
			}
		});
    }
}
