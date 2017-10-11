package com.idevicesinc.sweetblue;

import java.util.UUID;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import com.idevicesinc.sweetblue.BleDevice.BondListener.Status;
import com.idevicesinc.sweetblue.BleNode.ConnectionFailListener.AutoConnectUsage;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.P_Task_Bond.E_TransactionLockBehavior;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_BleDevice_Listeners extends BluetoothGattCallback
{
	private final BleDevice m_device;
	private final P_Logger m_logger;
	private final P_TaskQueue m_queue;

	final PA_Task.I_StateListener m_taskStateListener = new PA_Task.I_StateListener()
	{
		@Override
		public final void onStateChange(PA_Task task, PE_TaskState state)
		{
			if (task.getClass() == P_Task_Connect.class)
			{
				final P_Task_Connect connectTask = (P_Task_Connect) task;

				if (state.isEndingState())
				{
					if (state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT)
					{
						if (state == PE_TaskState.SUCCEEDED)
						{
							m_device.setToAlwaysUseAutoConnectIfItWorked();
						}

						if (state == PE_TaskState.REDUNDANT)
						{
//							Log.e("", "redundant");
						}

						m_device.onNativeConnect(connectTask.isExplicit());
					}
					else
					{
						m_device.onNativeConnectFail(state, connectTask.getGattStatus(), connectTask.getAutoConnectUsage());
					}
				}
			}
			else if (task.getClass() == P_Task_Disconnect.class)
			{
				// Only add the disconnect task if it's not already in the queue
				if ((state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT) && !m_device.queue().isInQueue(P_Task_Disconnect.class, m_device))
				{
					P_Task_Disconnect task_cast = (P_Task_Disconnect) task;

					m_device.onNativeDisconnect(task_cast.isExplicit(), task_cast.getGattStatus(), /*doShortTermReconnect=*/true, /*saveLastDisconnect=*/task_cast.shouldSaveLastDisconnect());
				}
			}
			else if (task.getClass() == P_Task_DiscoverServices.class)
			{
				final P_Task_DiscoverServices discoverTask = (P_Task_DiscoverServices) task;

				if (state == PE_TaskState.EXECUTING)
				{
					// m_stateTracker.append(GETTING_SERVICES);
				}
				else if (state == PE_TaskState.SUCCEEDED)
				{
					final boolean simulateServiceDiscoveryFailure = false;

					if (simulateServiceDiscoveryFailure && m_device.getConnectionRetryCount() == 0)
					{
						m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.EVENTUALLY, discoverTask.getGattStatus(), BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_EVENT());
					}
					else
					{
						m_device.onServicesDiscovered();
					}
				}
				else if (state.isEndingState())
				{
					if (state == PE_TaskState.SOFTLY_CANCELLED)
					{
						// pretty sure doing nothing is correct.
					}
					else if (state == PE_TaskState.FAILED_IMMEDIATELY)
					{
						m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.IMMEDIATELY, discoverTask.getGattStatus(), BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_EVENT());
					}
					else if (state == PE_TaskState.TIMED_OUT)
					{
						m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.TIMED_OUT, discoverTask.getGattStatus(), BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_EVENT());
					}
					else
					{
						// If an explicit disconnect() was called while discovering services, we do NOT want to throw another disconnectWithReason (the task will do it when it executes)
						if (!m_device.queue().isInQueue(P_Task_Disconnect.class, m_device))
						{
							m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.EVENTUALLY, discoverTask.getGattStatus(), BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_EVENT());
						}
					}
				}
			}
			else if (task.getClass() == P_Task_Bond.class)
			{
				m_device.m_bondMngr.onBondTaskStateChange(task, state);
			}
		}
	};



	public P_BleDevice_Listeners(BleDevice device)
	{
		m_device = device;
		m_logger = m_device.getManager().getLogger();
		m_queue = m_device.getTaskQueue();
	}

	@Override
	public final void onConnectionStateChange(final BluetoothGatt gatt, final int gattStatus, final int newState)
	{
		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onConnectionStateChange_updateThread(gatt, gattStatus, newState);
			}
		});
	}

	private void onConnectionStateChange_updateThread(final BluetoothGatt gatt, final int gattStatus, final int newState)
	{
		//--- DRK > NOTE: For some devices disconnecting by turning off the peripheral comes back with a status of 8, which is BluetoothGatt.GATT_SERVER.
		//---				For that same device disconnecting from the app the status is 0. Just an FYI to future developers in case they want to distinguish
		//---				between the two as far as user intent or something.

		//--- RB > NOTE: Regarding the above comment, 8 is actually BleStatuses.CONN_TIMEOUT -- it seems connection status codes have different variables
		//---				associated to them. Some of them share the same value as BluetoothGatt status codes. This is fixed now with the new log_conn_status
		//---				logger method (and the values are in BleStatuses)
		m_logger.log_conn_status(gattStatus, m_logger.gattConn(newState));

		if (newState == BluetoothProfile.STATE_DISCONNECTED)
		{
			m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);

			final P_Task_Connect connectTask = m_queue.getCurrent(P_Task_Connect.class, m_device);

			if (connectTask != null)
			{
				connectTask.onNativeFail(gattStatus);
			}
			else
			{
				final P_Task_Disconnect disconnectTask = m_queue.getCurrent(P_Task_Disconnect.class, m_device);

				m_device.m_nativeWrapper.closeGattIfNeeded(/*disconnectAlso=*/false);

				final BleDeviceConfig.RefreshOption option = m_device.conf_device().gattRefreshOption != null ? m_device.conf_device().gattRefreshOption : m_device.conf_mngr().gattRefreshOption;

				if (option == BleDeviceConfig.RefreshOption.AFTER_DISCONNECTING)
				{
					Utils.refreshGatt(m_device.getNativeGatt());
				}

				if (disconnectTask != null)
				{
					disconnectTask.onNativeSuccess(gattStatus);
				}
				else
				{
					final boolean doShortTermReconnect = m_device.getManager().is(BleManagerState.ON);
					m_device.onNativeDisconnect(/*explicit=*/false, gattStatus, doShortTermReconnect, /*saveLastDisconnect=*/true);
				}
			}
		}
		else if (newState == BluetoothProfile.STATE_CONNECTING)
		{
			if (Utils.isSuccess(gattStatus))
			{
				m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);

				m_device.onConnecting(/*definitelyExplicit=*/false, /*isReconnect=*/false, P_BondManager.OVERRIDE_EMPTY_STATES, /*bleConnect=*/true);

				if (!m_queue.isCurrent(P_Task_Connect.class, m_device))
				{
					P_Task_Connect task = new P_Task_Connect(m_device, m_taskStateListener, /*explicit=*/false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING);
					m_queue.add(task);
				}

				m_queue.fail(P_Task_Disconnect.class, m_device);
			}
			else
			{
				onNativeConnectFail(gatt, gattStatus);
			}
		}
		else if (newState == BluetoothProfile.STATE_CONNECTED)
		{
			if (Utils.isSuccess(gattStatus))
			{
				m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);

				m_queue.fail(P_Task_Disconnect.class, m_device);

				if (!m_queue.succeed(P_Task_Connect.class, m_device))
				{
					m_device.onNativeConnect(/*explicit=*/false);
				}
			}
			else
			{
				onNativeConnectFail(gatt, gattStatus);
			}
		}
		//--- DRK > NOTE: never seen this case happen.
		else if (newState == BluetoothProfile.STATE_DISCONNECTING)
		{
			m_logger.e("Actually natively disconnecting!"); // DRK > error level just so it's noticeable...never seen this.

			m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);

//			m_device.onDisconnecting();

			if (!m_queue.isCurrent(P_Task_Disconnect.class, m_device))
			{
				P_Task_Disconnect task = new P_Task_Disconnect(m_device, m_taskStateListener, /*explicit=*/false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING, /*cancellable=*/true);
				m_queue.add(task);
			}

			m_queue.fail(P_Task_Connect.class, m_device);
		}
		else
		{
			m_device.m_nativeWrapper.updateNativeConnectionState(gatt);
		}
	}

	private void onNativeConnectFail(final BluetoothGatt gatt, final int gattStatus)
	{
		//--- DRK > NOTE: Making an assumption that the underlying stack agrees that the connection state is STATE_DISCONNECTED.
		//---				This is backed up by basic testing, but even if the underlying stack uses a different value, it can probably
		//---				be assumed that it will eventually go to STATE_DISCONNECTED, so SweetBlue library logic is sounder "living under the lie" for a bit regardless.
		m_device.m_nativeWrapper.updateNativeConnectionState(gatt, BluetoothProfile.STATE_DISCONNECTED);

		final P_Task_Connect connectTask = m_queue.getCurrent(P_Task_Connect.class, m_device);

		if (connectTask != null)
		{
			connectTask.onNativeFail(gattStatus);
		}
		else
		{
			m_device.onNativeConnectFail((PE_TaskState) null, gattStatus, AutoConnectUsage.UNKNOWN);
		}
	}

	@Override
	public final void onServicesDiscovered(final BluetoothGatt gatt, final int gattStatus)
	{
		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onServicesDiscovered_updateThread(gatt, gattStatus);
			}
		});
	}

	private void onServicesDiscovered_updateThread(final BluetoothGatt gatt, final int gattStatus)
	{
		m_logger.log_status(gattStatus);

		if (Utils.isSuccess(gattStatus))
		{
			m_queue.succeed(P_Task_DiscoverServices.class, m_device);
		}
		else
		{
			final P_Task_DiscoverServices task = m_queue.getCurrent(P_Task_DiscoverServices.class, m_device);

			if (task != null)
			{
				task.onNativeFail(gattStatus);
			}
		}
	}

	@Override
	public final void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int gattStatus)
	{
		final byte[] value = characteristic.getValue() == null ? null : characteristic.getValue().clone();

		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onCharacteristicRead_updateThread(gatt, characteristic, gattStatus, value);
			}
		});
	}

	private void onCharacteristicRead_updateThread(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int gattStatus, final byte[] value)
	{
		final UUID uuid = characteristic.getUuid();
		m_logger.i(m_logger.charName(uuid));
		m_logger.log_status(gattStatus);

		final P_Task_Read readTask = m_queue.getCurrent(P_Task_Read.class, m_device);

		if (readTask != null && readTask.isFor(characteristic))
		{
			readTask.onCharacteristicRead(gatt, characteristic.getUuid(), value, gattStatus);
		}
		else
		{
			final P_Task_BatteryLevel batteryTask = m_queue.getCurrent(P_Task_BatteryLevel.class, m_device);
			if (batteryTask != null)
			{
				batteryTask.onCharacteristicRead(gatt, characteristic.getUuid(), value, gattStatus);
			}
			else
			{
				fireUnsolicitedEvent(new BleCharacteristicWrapper(characteristic), BleDescriptorWrapper.NULL, BleDevice.ReadWriteListener.Type.READ, BleDevice.ReadWriteListener.Target.CHARACTERISTIC, value, gattStatus);
			}
		}
	}

	@Override
	public final void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int gattStatus)
	{
		final byte[] data = characteristic.getValue();

		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onCharacteristicWrite_updateThread(gatt, characteristic, data, gattStatus);
			}
		});
	}

	private void onCharacteristicWrite_updateThread(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] data, final int gattStatus)
	{
		final UUID uuid = characteristic.getUuid();
		m_logger.i(m_logger.charName(uuid));
		m_logger.log_status(gattStatus);

		final P_Task_Write task = m_queue.getCurrent(P_Task_Write.class, m_device);

		if (task != null && task.isFor(characteristic))
		{
			task.onCharacteristicWrite(gatt, characteristic.getUuid(), gattStatus);
		}
		else
		{
			fireUnsolicitedEvent(new BleCharacteristicWrapper(characteristic), BleDescriptorWrapper.NULL, BleDevice.ReadWriteListener.Type.WRITE, BleDevice.ReadWriteListener.Target.CHARACTERISTIC, data, gattStatus);
		}
	}

	private void fireUnsolicitedEvent(final BleCharacteristicWrapper characteristic_nullable, final BleDescriptorWrapper descriptor_nullable, BleDevice.ReadWriteListener.Type type, final BleDevice.ReadWriteListener.Target target, final byte[] data, final int gattStatus)
	{
		final BleDevice.ReadWriteListener.Type type_modified = characteristic_nullable != null ? P_DeviceServiceManager.modifyResultType(characteristic_nullable, type) : type;
		final BleDevice.ReadWriteListener.Status status = Utils.isSuccess(gattStatus) ? BleDevice.ReadWriteListener.Status.SUCCESS : BleDevice.ReadWriteListener.Status.REMOTE_GATT_FAILURE;

		final UUID serviceUuid			= !characteristic_nullable.isNull()	? characteristic_nullable.getCharacteristic().getService().getUuid()	: BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID;
		final UUID characteristicUuid	= !characteristic_nullable.isNull()	? characteristic_nullable.getCharacteristic().getUuid()					: BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID;
		final UUID descriptorUuid		= !descriptor_nullable.isNull()		? descriptor_nullable.getDescriptor().getUuid()							: BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID;

		final double time = Interval.DISABLED.secs();
		final boolean solicited = false;

		final BleDevice.ReadWriteListener.ReadWriteEvent e;

		if (target == BleDevice.ReadWriteListener.Target.CHARACTERISTIC || target == BleDevice.ReadWriteListener.Target.DESCRIPTOR)
		{
			e = new BleDevice.ReadWriteListener.ReadWriteEvent
			(
				m_device, serviceUuid, characteristicUuid, descriptorUuid, null, type_modified,
				target, data, status, gattStatus, time, time, solicited
			);
		}
		else if (target == BleDevice.ReadWriteListener.Target.RSSI)
		{
			e = new BleDevice.ReadWriteListener.ReadWriteEvent
					(
							m_device, type, m_device.getRssi(), status, gattStatus, time, time, solicited
					);
		}
		else if (target == BleDevice.ReadWriteListener.Target.MTU)
		{
			e = new BleDevice.ReadWriteListener.ReadWriteEvent
					(
							m_device, m_device.getMtu(), status, gattStatus, time, time, solicited
					);
		}
		else
		{
			return;
		}

		m_device.invokeReadWriteCallback(null, e);
	}

	@Override
	public final void onReliableWriteCompleted(final BluetoothGatt gatt, final int gattStatus)
	{
		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onReliableWriteCompleted_updateThread(gatt, gattStatus);
			}
		});
	}

	private void onReliableWriteCompleted_updateThread(final BluetoothGatt gatt, final int gattStatus)
	{
		m_logger.log_status(gattStatus);

		final P_Task_ExecuteReliableWrite task = m_queue.getCurrent(P_Task_ExecuteReliableWrite.class, m_device);

		if (task != null)
		{
			task.onReliableWriteCompleted(gatt, gattStatus);
		}
		else
		{
			m_device.m_reliableWriteMngr.onReliableWriteCompleted_unsolicited(gatt, gattStatus);
		}
	}

	@Override
	public final void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int gattStatus)
	{
		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onReadRemoteRssi_updateThread(gatt, rssi, gattStatus);
			}
		});
	}

	private void onReadRemoteRssi_updateThread(final BluetoothGatt gatt, final int rssi, final int gattStatus)
	{
		if (Utils.isSuccess(gattStatus))
		{
			m_device.updateRssi(rssi);
		}

		final P_Task_ReadRssi task = m_queue.getCurrent(P_Task_ReadRssi.class, m_device);

		if (task != null)
		{
			task.onReadRemoteRssi(gatt, rssi, gattStatus);
		}
		else
		{
			fireUnsolicitedEvent(null, null, BleDevice.ReadWriteListener.Type.READ, BleDevice.ReadWriteListener.Target.RSSI, P_Const.EMPTY_BYTE_ARRAY, gattStatus);
		}
	}

	@Override
	public final void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int gattStatus)
	{
		final byte[] data = descriptor.getValue();

		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onDescriptorWrite_updateThread(gatt, descriptor, data, gattStatus);
			}
		});
	}

	private void onDescriptorWrite_updateThread(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final byte[] data, final int gattStatus)
	{
		final UUID uuid = descriptor.getUuid();
		m_logger.i(m_logger.descriptorName(uuid));
		m_logger.log_status(gattStatus);

		final P_Task_WriteDescriptor task_write = m_queue.getCurrent(P_Task_WriteDescriptor.class, m_device);

		if (task_write != null && task_write.isFor(descriptor))
		{
			task_write.onDescriptorWrite(gatt, descriptor.getUuid(), gattStatus);
		}
		else
		{
			final P_Task_ToggleNotify task_toggleNotify = m_queue.getCurrent(P_Task_ToggleNotify.class, m_device);

			if (task_toggleNotify != null && task_toggleNotify.isFor(descriptor))
			{
				task_toggleNotify.onDescriptorWrite(gatt, uuid, gattStatus);
			}
			else
			{
				fireUnsolicitedEvent(new BleCharacteristicWrapper(descriptor.getCharacteristic()), new BleDescriptorWrapper(descriptor), BleDevice.ReadWriteListener.Type.WRITE, BleDevice.ReadWriteListener.Target.DESCRIPTOR, data, gattStatus);
			}
		}
	}

	@Override
	public final void onDescriptorRead(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int gattStatus)
	{
		final byte[] data = descriptor.getValue();

		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onDescriptorRead_updateThread(gatt, descriptor, data, gattStatus);
			}
		});
	}

	private void onDescriptorRead_updateThread(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final byte[] data, final int gattStatus)
	{
		final PA_Task_ReadOrWrite task_readOrWrite = m_queue.getCurrent(PA_Task_ReadOrWrite.class, m_device);

		if (task_readOrWrite != null && task_readOrWrite.descriptorMatches(descriptor))
		{
			task_readOrWrite.onDescriptorReadCallback(gatt, new BleDescriptorWrapper(descriptor), data, gattStatus);
		}
		else
		{
			fireUnsolicitedEvent(new BleCharacteristicWrapper(descriptor.getCharacteristic()), new BleDescriptorWrapper(descriptor), BleDevice.ReadWriteListener.Type.READ, BleDevice.ReadWriteListener.Target.DESCRIPTOR, data, gattStatus);
		}

//		final P_Task_ReadDescriptor task_read = m_queue.getCurrent(P_Task_ReadDescriptor.class, m_device);
//
//		if( task_read != null && task_read.isFor(descriptor) )
//		{
//			task_read.onDescriptorRead(gatt, descriptor.getUuid(), data, gattStatus);
//		}
//		else
//		{
//			final P_Task_BatteryLevel battery = m_queue.getCurrent(P_Task_BatteryLevel.class, m_device);
//			if (battery != null)
//			{
//				battery.onDescriptorRead(descriptor, data, gattStatus);
//			}
//			else
//			{
//				fireUnsolicitedEvent(descriptor.getCharacteristic(), descriptor, BleDevice.ReadWriteListener.Type.READ, BleDevice.ReadWriteListener.Target.DESCRIPTOR, data, gattStatus);
//			}
//		}
	}

	@Override
	public final void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic)
	{
		final byte[] value = characteristic.getValue() == null ? null : characteristic.getValue().clone();

		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				onCharacteristicChanged_updateThread(gatt, characteristic, value);
			}
		});
	}

	private void onCharacteristicChanged_updateThread(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] value)
	{
		final UUID characteristicUuid = characteristic.getUuid();
		final UUID serviceUuid = characteristic.getService().getUuid();

		m_logger.d("characteristic=" + characteristicUuid.toString());

		m_device.getPollManager().onCharacteristicChangedFromNativeNotify(serviceUuid, characteristicUuid, value);
	}

	public final void onNativeBoneRequest_updateThread(BleDevice device)
	{
		m_logger.i("Bond request served for device with mac " + device.getMacAddress());
		device.m_bondMngr.onNativeBondRequest();
	}

	public final void onNativeBondStateChanged_updateThread(int previousState, int newState, int failReason)
	{
		if (newState == BluetoothDevice.ERROR)
		{
			P_TaskQueue queue = m_device.getTaskQueue();
			queue.fail(P_Task_Bond.class, m_device);
			queue.fail(P_Task_Unbond.class, m_device);
			
			m_logger.e("newState for bond is BluetoothDevice.ERROR!(?)");
		}
		else if (newState == BluetoothDevice.BOND_NONE)
		{
			final P_Task_Bond bondTask = m_queue.getCurrent(P_Task_Bond.class, m_device);
			
			if( bondTask != null )
			{
				bondTask.onNativeFail(failReason);
			}
			else if (!m_queue.succeed(P_Task_Unbond.class, m_device))
			{
				if( previousState == BluetoothDevice.BOND_BONDING || previousState == BluetoothDevice.BOND_NONE )
				{
					m_device.m_bondMngr.onNativeBondFailed(E_Intent.UNINTENTIONAL, Status.FAILED_EVENTUALLY, failReason, false);
				}
				else
				{
					m_device.m_bondMngr.onNativeUnbond(E_Intent.UNINTENTIONAL);
				}
			}
		}
		else if (newState == BluetoothDevice.BOND_BONDING)
		{
			final P_Task_Bond task = m_queue.getCurrent(P_Task_Bond.class, m_device);
			E_Intent intent = task != null && task.isExplicit() ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
			boolean isCurrent = task != null; // avoiding erroneous dead code warning from putting this directly in if-clause below.
			m_device.m_bondMngr.onNativeBonding(intent);

			if ( !isCurrent )
			{
				m_queue.add(new P_Task_Bond(m_device, /*explicit=*/false, /*isDirect=*/false, /*partOfConnection=*/false, m_taskStateListener, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING, E_TransactionLockBehavior.PASSES));
			}

			m_queue.fail(P_Task_Unbond.class, m_device);
		}
		else if (newState == BluetoothDevice.BOND_BONDED)
		{
			m_queue.fail(P_Task_Unbond.class, m_device);

			final P_Task_Bond task = m_queue.getCurrent(P_Task_Bond.class, m_device);

			if (task != null)
			{
				task.onNativeSuccess();
			}
			else
			{
				m_device.m_bondMngr.onNativeBond(E_Intent.UNINTENTIONAL);
			}
		}
	}

	@Override public final void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int gattStatus)
	{
		m_device.getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				onMtuChanged_updateThread(gatt, mtu, gattStatus);
			}
		});
	}

	private void onMtuChanged_updateThread(BluetoothGatt gatt, int mtu, int gattStatus)
	{
		if( Utils.isSuccess(gattStatus) )
		{
			m_device.updateMtu(mtu);
		}

		final P_Task_RequestMtu task = m_queue.getCurrent(P_Task_RequestMtu.class, m_device);

		if( task != null )
		{
			task.onMtuChanged(gatt, mtu, gattStatus);
		}
		else
		{
			fireUnsolicitedEvent(null, null, BleDevice.ReadWriteListener.Type.WRITE, BleDevice.ReadWriteListener.Target.MTU, P_Const.EMPTY_BYTE_ARRAY, gattStatus);
		}
	}
}
