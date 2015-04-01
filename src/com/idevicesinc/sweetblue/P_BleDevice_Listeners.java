package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import com.idevicesinc.sweetblue.BleDevice.BondListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.AutoConnectUsage;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.P_Task_Bond.E_TransactionLockBehavior;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UpdateLoop;
import com.idevicesinc.sweetblue.utils.Utils;

class P_BleDevice_Listeners extends BluetoothGattCallback
{
	private final BleDevice m_device;
	private final P_Logger m_logger;
	private final P_TaskQueue m_queue;
	
	abstract class SynchronizedRunnable implements Runnable
	{
		@Override public void run()
		{
			synchronized (m_device.m_threadLock)
			{
				run_nested();
			}
		}
		
		public abstract void run_nested();
	}

	final PA_Task.I_StateListener m_taskStateListener = new PA_Task.I_StateListener()
	{
		@Override public void onStateChange(PA_Task task, PE_TaskState state)
		{
			synchronized (m_device.m_threadLock)
			{
				onStateChange_synchronized(task, state);
			}
		}
		
		private void onStateChange_synchronized(PA_Task task, PE_TaskState state)
		{
			if (task.getClass() == P_Task_Connect.class)
			{
				P_Task_Connect connectTask = (P_Task_Connect) task;
				
				if (state.isEndingState())
				{					
					if (state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT )
					{
						if( state == PE_TaskState.SUCCEEDED )
						{
							m_device.setToAlwaysUseAutoConnectIfItWorked();
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
				if (state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT || state == PE_TaskState.NO_OP)
				{
					P_Task_Disconnect task_cast = (P_Task_Disconnect) task;

					m_device.onNativeDisconnect(task_cast.isExplicit(), task_cast.getGattStatus(), /*doShortTermReconnect=*/true);
				}
			}
			else if (task.getClass() == P_Task_DiscoverServices.class)
			{
				P_Task_DiscoverServices discoverTask = (P_Task_DiscoverServices) task;
				if (state == PE_TaskState.EXECUTING)
				{
					// m_stateTracker.append(GETTING_SERVICES);
				}
				else if (state == PE_TaskState.SUCCEEDED)
				{
					final boolean simulateServiceDiscoveryFailure = false;
					
					if( simulateServiceDiscoveryFailure && m_device.getConnectionRetryCount() == 0 )
					{
						m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.EVENTUALLY, discoverTask.getGattStatus(), BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_EVENT());
					}
					else
					{
						m_device.onServicesDiscovered();
					}
				}
				else if (state.isEndingState() )
				{
					if( state == PE_TaskState.SOFTLY_CANCELLED )
					{
						// pretty sure doing nothing is correct.
					}
					else if( state == PE_TaskState.FAILED_IMMEDIATELY )
					{
						m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.IMMEDIATELY, discoverTask.getGattStatus(), BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_EVENT());
					}
					else if( state == PE_TaskState.TIMED_OUT )
					{
						m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.TIMED_OUT, discoverTask.getGattStatus(), BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_EVENT());
					}
					else
					{
						m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.EVENTUALLY, discoverTask.getGattStatus(), BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, m_device.NULL_READWRITE_EVENT());
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

	@Override public void onConnectionStateChange(final BluetoothGatt gatt, final int gattStatus, final int newState)
	{
		//--- DRK > NOTE: For some devices disconnecting by turning off the peripheral comes back with a status of 8, which is BluetoothGatt.GATT_SERVER.
		//---				For that same device disconnecting from the app the status is 0. Just an FYI to future developers in case they want to distinguish
		//---				between the two as far as user intent or something.
		m_logger.log_status(gattStatus, m_logger.gattConn(newState));
		
		UpdateLoop updater = m_device.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				onConnectionStateChange_synchronized(gatt, gattStatus, newState);
			}
		});
	}
	
	private void onConnectionStateChange_synchronized(final BluetoothGatt gatt, final int gattStatus, final int newState)
	{
		if (newState == BluetoothProfile.STATE_DISCONNECTED )
		{
			m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);
			
			final P_Task_Connect connectTask = m_queue.getCurrent(P_Task_Connect.class, m_device);
			
			if( connectTask != null )
			{
				connectTask.onNativeFail(gattStatus);
			}
			else
			{
				final P_Task_Disconnect disconnectTask = m_queue.getCurrent(P_Task_Disconnect.class, m_device);
				
				if( disconnectTask != null )
				{
					disconnectTask.onNativeSuccess(gattStatus);
				}
				else
				{
					m_device.onNativeDisconnect(/*explicit=*/false, gattStatus, /*doShortTermReconnect=*/true);
				}
			}

			//--- DRK > The following situation gives rise to the need to make sure gatt is closed here.
			//---		Before this line was added the library still recovered but it was/is an assert case in native device wrapper.
//			02-27 16:22:30.963: I/P_TaskQueue(29156): AMY(29156) print() - no current task [DiscoverServices(QUEUED igrill_v2_17D8)]
//			02-27 16:22:30.963: D/BluetoothManager(29156): getConnectionState()
//			02-27 16:22:30.963: D/BluetoothManager(29156): getConnectedDevices
//			02-27 16:22:30.983: I/P_TaskQueue(29156): AMY(29156) print() - no current task [DiscoverServices(QUEUED igrill_v2_17D8)]
//			02-27 16:22:30.983: I/P_TaskQueue(29156): AMY(29156) print() - DiscoverServices(ARMED igrill_v2_17D8) [queue empty]
//			02-27 16:22:31.013: D/BluetoothManager(29156): getConnectionState()
//			02-27 16:22:31.013: D/BluetoothManager(29156): getConnectedDevices
//			02-27 16:22:31.023: D/BluetoothGatt(29156): discoverServices() - device: D4:81:CA:20:17:D8
//			02-27 16:22:31.283: D/BluetoothGatt(29156): onSearchComplete() = Device=D4:81:CA:20:17:D8 Status=129
//			02-27 16:22:31.283: W/P_BleDevice_Listeners(29156): DON(29211) onServicesDiscovered() - GATT_INTERNAL_ERROR(129) 
//			02-27 16:22:31.293: D/BluetoothGatt(29156): onClientConnectionState() - status=133 clientIf=5 device=D4:81:CA:20:17:D8
//			02-27 16:22:31.303: I/PA_Task(29156): AMY(29156) setState() - DiscoverServices(FAILED igrill_v2_17D8) - 8009
//			02-27 16:22:31.303: I/P_TaskQueue(29156): AMY(29156) print() - no current task [Disconnect(QUEUED igrill_v2_17D8)]
//			02-27 16:22:31.303: W/P_BleDevice_Listeners(29156): BEN(29233) onConnectionStateChange() - GATT_ERROR(133) STATE_DISCONNECTED(0)
//			02-27 16:22:31.323: W/P_ConnectionFailManager(29156): AMY(29156) onConnectionFailed() - DISCOVERING_SERVICES_FAILED
//			02-27 16:22:31.333: I/P_TaskQueue(29156): AMY(29156) print() - no current task [Disconnect(QUEUED igrill_v2_17D8), Connect(QUEUED igrill_v2_17D8)]
//			02-27 16:22:31.343: I/P_TaskQueue(29156): AMY(29156) print() - no current task [Disconnect(QUEUED igrill_v2_17D8), Connect(QUEUED igrill_v2_17D8)]
//			02-27 16:22:31.353: I/P_TaskQueue(29156): AMY(29156) print() - Disconnect(ARMED igrill_v2_17D8) [Connect(QUEUED igrill_v2_17D8)]
//			02-27 16:22:31.353: I/P_NativeDeviceWrapper(29156): AMY(29156) updateNativeConnectionState() - STATE_DISCONNECTED(0)
//			02-27 16:22:31.363: I/PA_Task(29156): AMY(29156) setState() - Disconnect(SOFTLY_CANCELLED igrill_v2_17D8) - 8010
//			02-27 16:22:31.363: I/P_TaskQueue(29156): AMY(29156) print() - no current task [Connect(QUEUED igrill_v2_17D8)]
//			02-27 16:22:31.393: I/P_TaskQueue(29156): AMY(29156) print() - Connect(ARMED igrill_v2_17D8) [queue empty]
//			02-27 16:22:31.433: D/BluetoothManager(29156): getConnectionState()
//			02-27 16:22:31.433: D/BluetoothManager(29156): getConnectedDevices
//			02-27 16:22:31.443: D/BluetoothManager(29156): getConnectionState()
//			02-27 16:22:31.443: D/BluetoothManager(29156): getConnectedDevices
//			02-27 16:22:31.453: D/BluetoothManager(29156): getConnectionState()
//			02-27 16:22:31.453: D/BluetoothManager(29156): getConnectedDevices
//			02-27 16:22:31.463: D/BluetoothGatt(29156): connect() - device: D4:81:CA:20:17:D8, auto: false
//			02-27 16:22:31.463: D/BluetoothGatt(29156): registerApp()
//			02-27 16:22:31.463: D/BluetoothGatt(29156): registerApp() - UUID=e669a333-eddc-4e16-a8a9-794982b3d99c
//			02-27 16:22:31.473: I/BluetoothGatt(29156): Client registered, waiting for callback
//			02-27 16:22:31.473: D/BluetoothGatt(29156): onClientRegistered() - status=0 clientIf=6
//			02-27 16:22:31.513: E/BleManager(29156): ASSERTION FAILED 
//			02-27 16:22:31.513: E/BleManager(29156): java.lang.Exception
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.BleManager.ASSERT(BleManager.java:1210)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.BleManager.ASSERT(BleManager.java:1194)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.P_NativeDeviceWrapper.setGatt(P_NativeDeviceWrapper.java:255)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.P_NativeDeviceWrapper.updateGattFromCallback(P_NativeDeviceWrapper.java:88)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.P_NativeDeviceWrapper.updateGattInstance(P_NativeDeviceWrapper.java:94)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.P_Task_Connect.execute(P_Task_Connect.java:70)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.PA_Task.execute_wrapper(PA_Task.java:256)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.PA_Task.update_internal(PA_Task.java:325)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.P_TaskQueue.update(P_TaskQueue.java:184)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.BleManager.update_synchronized(BleManager.java:2112)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.BleManager.update(BleManager.java:2106)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.BleManager$1.onUpdate(BleManager.java:611)
//			02-27 16:22:31.513: E/BleManager(29156): 	at com.idevicesinc.sweetblue.utils.UpdateLoop$1.run(UpdateLoop.java:32)
			m_device.m_nativeWrapper.closeGattIfNeeded(/*disconnectAlso=*/false);
		}
		else if (newState == BluetoothProfile.STATE_CONNECTING)
		{
			if (Utils.isSuccess(gattStatus))
			{
				m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);

				m_device.onConnecting(/*definitelyExplicit=*/false, /*isReconnect=*/false, P_BondManager.OVERRIDE_EMPTY_STATES);
				
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
		
		P_Task_Connect connectTask = m_queue.getCurrent(P_Task_Connect.class, m_device);
		
		if( connectTask != null )
		{
			connectTask.onNativeFail(gattStatus);
		}
		else
		{
			m_device.onNativeConnectFail( (PE_TaskState)null, gattStatus, AutoConnectUsage.UNKNOWN);
		}
	}
	
	private final Runnable m_servicesDiscoveredSuccessRunnable = new SynchronizedRunnable()
	{
		@Override public void run_nested()
		{
			m_queue.succeed(P_Task_DiscoverServices.class, m_device);
		}
	};

	@Override public void onServicesDiscovered(BluetoothGatt gatt, final int gattStatus)
	{
		m_logger.log_status(gattStatus);
		
		UpdateLoop updater = m_device.getManager().getUpdateLoop();

		if( Utils.isSuccess(gattStatus) )
		{
			updater.postIfNeeded(m_servicesDiscoveredSuccessRunnable);
		}
		else
		{
			updater.postIfNeeded(new Runnable()
			{
				@Override public void run()
				{
					synchronized (m_device.m_threadLock)
					{
						P_Task_DiscoverServices task = m_queue.getCurrent(P_Task_DiscoverServices.class, m_device);
						
						if( task != null )
						{
							task.onNativeFail(gattStatus);
						}
					}
				}
			});
		}
	}
	
	@Override public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status)
	{
		final UUID uuid = characteristic.getUuid();
		final byte[] value = characteristic.getValue() == null ? null : characteristic.getValue().clone();
		m_logger.i(m_logger.charName(uuid));
		m_logger.log_status(status);
		
		UpdateLoop updater = m_device.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				P_Task_Read readTask = m_queue.getCurrent(P_Task_Read.class, m_device);
		
				if (readTask == null)  return;
		
				readTask.onCharacteristicRead(gatt, uuid, value, status);
			}
		});
	}

	@Override public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status)
	{
		final UUID uuid = characteristic.getUuid();
		m_logger.i(m_logger.charName(uuid));
		m_logger.log_status(status);
		
		UpdateLoop updater = m_device.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				P_Task_Write task = m_queue.getCurrent(P_Task_Write.class, m_device);
		
				if (task == null)  return;
		
				task.onCharacteristicWrite(gatt, uuid, status);
			}
		});
	}
	
	@Override public void onReliableWriteCompleted(final BluetoothGatt gatt, final int status)
	{
		m_logger.log_status(status);
		
		UpdateLoop updater = m_device.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				P_Task_Write task = m_queue.getCurrent(P_Task_Write.class, m_device);
		
				if (task == null)  return;
		
				task.onReliableWriteCompleted(gatt, status);
			}
		});
    }
	
	@Override public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int gattStatus)
	{
		UpdateLoop updater = m_device.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				if( Utils.isSuccess(gattStatus) )
				{
					m_device.updateRssi(rssi);
				}
				
				P_Task_ReadRssi task = m_queue.getCurrent(P_Task_ReadRssi.class, m_device);
				
				if (task == null)  return;
		
				task.onReadRemoteRssi(gatt, rssi, gattStatus);
			}
		});
	}
	
	@Override public void onDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, final int status)
	{
		final UUID uuid = descriptor.getUuid();
		m_logger.i(m_logger.descriptorName(uuid));
		m_logger.log_status(status);
		
		UpdateLoop updater = m_device.getManager().getUpdateLoop();
		
		updater.postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				P_Task_ToggleNotify task = m_queue.getCurrent(P_Task_ToggleNotify.class, m_device);
		
				if (task == null)  return;
		
				task.onDescriptorWrite(gatt, uuid, status);
			}
		});
	}
	
	@Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		final UUID uuid = characteristic.getUuid();
		final byte[] value = characteristic.getValue() == null ? null : characteristic.getValue().clone();
		
		m_device.getManager().getUpdateLoop().postIfNeeded(new SynchronizedRunnable()
		{
			@Override public void run_nested()
			{
				m_device.getPollManager().onCharacteristicChangedFromNativeNotify(uuid, value);
			}
		});
	}

	void onNativeBondStateChanged(int previousState, int newState, int failReason)
	{
		onNativeBondStateChanged_private(previousState, newState, failReason);
	}
	
	private void onNativeBondStateChanged_private(final int previousState, final int newState, final int failReason)
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
					m_device.m_bondMngr.onNativeBondFailed(E_Intent.UNINTENTIONAL, Status.FAILED_EVENTUALLY, failReason);
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
				m_queue.add(new P_Task_Bond(m_device, /*explicit=*/false, /*partOfConnection=*/false, m_taskStateListener, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING, E_TransactionLockBehavior.PASSES));
			}

			m_queue.fail(P_Task_Unbond.class, m_device);
		}
		else if (newState == BluetoothDevice.BOND_BONDED)
		{
			m_queue.fail(P_Task_Unbond.class, m_device);

			if (!m_queue.succeed(P_Task_Bond.class, m_device))
			{
				m_device.m_bondMngr.onNativeBond(E_Intent.UNINTENTIONAL);
			}
		}
	}
}
