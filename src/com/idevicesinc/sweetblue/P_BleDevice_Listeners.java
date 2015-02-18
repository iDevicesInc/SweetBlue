package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.AutoConnectUsage;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Reason;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UpdateLoop;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * 
 * 
 * 
 */
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
					BluetoothGatt gatt = connectTask.getGatt();
					if( gatt != null )
					{
						m_device.m_nativeWrapper.updateGattInstance(gatt);
					}
					
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

					m_device.onNativeDisconnect(task_cast.isExplicit());
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
					//--- DRK > Just some debug code for testing connection failures.
//					if( m_device.getConnectionRetryCount() == 0 )
//					{
//						m_device.disconnectWithReason(E_Reason.GETTING_SERVICES_FAILED);
//					}
//					else
//					{
						m_device.onServicesDiscovered();
//					}
				}
				else if (state.isEndingState() )
				{
					if( state == PE_TaskState.SOFTLY_CANCELLED )
					{
						// pretty sure doing nothing is correct.
					}
					else if( state == PE_TaskState.FAILED_IMMEDIATELY )
					{
						m_device.disconnectWithReason(Reason.GETTING_SERVICES_FAILED_IMMEDIATELY, discoverTask.getGattStatus());
					}
					else if( state == PE_TaskState.TIMED_OUT )
					{
						m_device.disconnectWithReason(Reason.GETTING_SERVICES_TIMED_OUT, discoverTask.getGattStatus());
					}
					else
					{
						m_device.disconnectWithReason(Reason.GETTING_SERVICES_FAILED_EVENTUALLY, discoverTask.getGattStatus());
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
	
	private void onConnectionStateChange_synchronized(BluetoothGatt gatt, int gattStatus, int newState)
	{
		if (newState == BluetoothProfile.STATE_DISCONNECTED )
		{
			m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);
			
			if( !m_queue.fail(P_Task_Connect.class, m_device) )
			{
				// --- DRK > This assert can hit and is out of our control so commenting out for now.
				// --- Not sure how to handle disconnect "failures" so just assuming success for now.
				// U_Bt.ASSERT(U_Bt.isSuccess(gattStatus));
	
				if (!m_queue.succeed(P_Task_Disconnect.class, m_device))
				{
					m_device.onNativeDisconnect(/*explicit=*/false);
				}
			}
		}
		else if (newState == BluetoothProfile.STATE_CONNECTING)
		{
			if (Utils.isSuccess(gattStatus))
			{
				m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);

				m_device.onConnecting(/*definitelyExplicit=*/false, /*isReconnect=*/false);
				
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
			m_logger.e("Actually natively disconnecting!"); // error level just so it's noticeable.
			
			m_device.m_nativeWrapper.updateNativeConnectionState(gatt, newState);
			
			m_device.onDisconnecting();
			
			if (!m_queue.isCurrent(P_Task_Disconnect.class, m_device))
			{
				P_Task_Disconnect task = new P_Task_Disconnect(m_device, m_taskStateListener, /*explicit=*/false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING);
				m_queue.add(task);
			}

			m_queue.fail(P_Task_Connect.class, m_device);
		}
		else
		{
			m_device.m_nativeWrapper.updateNativeConnectionState(gatt);
		}
	}
	
	private void onNativeConnectFail(BluetoothGatt gatt, int gattStatus)
	{
		//--- DRK > NOTE: Making an assumption that the underlying stack agrees that the connection state is STATE_DISCONNECTED.
		//---				This is backed up by basic testing, but even if the underlying stack uses a different value, it can probably
		//---				be assumed that it will eventually go to STATE_DISCONNECTED, so SweetBlue library logic is sounder "living under the lie" for a bit.
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
		
		if( gattStatus == PS_GattStatus.UNKNOWN_STATUS_FOR_IMMEDIATE_CONNECTION_FAILURE )
		{
//			m_device.getManager().uhOh(UhOhReason.UNKNOWN_CONNECTION_ERROR);
		}
	}
	
	private final Runnable m_servicesDiscoveredSuccessRunnable = new SynchronizedRunnable()
	{
		@Override public void run_nested()
		{
			m_queue.succeed(P_Task_DiscoverServices.class, m_device);
		}
	};
	
	private final Runnable m_servicesDiscoveredFailRunnable = new SynchronizedRunnable()
	{
		@Override public void run_nested()
		{
			
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
	
	private void onNativeBondStateChanged_private(int previousState, int newState, int failReason)
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
			if( !m_queue.fail(P_Task_Bond.class, m_device) )
			{
				
			}
			
			if (!m_queue.succeed(P_Task_Unbond.class, m_device))
			{
				m_device.m_bondMngr.onNativeUnbond(E_Intent.IMPLICIT);
			}
		}
		else if (newState == BluetoothDevice.BOND_BONDING)
		{
			P_Task_Bond task = m_queue.getCurrent(P_Task_Bond.class, m_device);
			E_Intent intent = task != null && task.isExplicit() ? E_Intent.EXPLICIT : E_Intent.IMPLICIT;
			boolean isCurrent = task != null; // avoiding erroneous dead code warning from putting this directly in if-clause below.
			m_device.m_bondMngr.onNativeBonding(intent);

			if ( !isCurrent )
			{
				m_queue.add(new P_Task_Bond(m_device, /*explicit=*/false, /*partOfConnection=*/false, m_taskStateListener, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING));
			}

			m_queue.fail(P_Task_Unbond.class, m_device);
		}
		else if (newState == BluetoothDevice.BOND_BONDED)
		{
			m_queue.fail(P_Task_Unbond.class, m_device);

			if (!m_queue.succeed(P_Task_Bond.class, m_device))
			{
				m_device.m_bondMngr.onNativeBond(E_Intent.IMPLICIT);
			}
		}
	}
}
