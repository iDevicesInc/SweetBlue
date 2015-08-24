package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleManagerState.SCANNING;

import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Utils;

import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class P_BleManager_Listeners
{
	public static final String BluetoothDevice_EXTRA_REASON = "android.bluetooth.device.extra.REASON";
	public static final String BluetoothDevice_ACTION_DISAPPEARED = "android.bluetooth.device.action.DISAPPEARED";
	
	final BluetoothAdapter.LeScanCallback m_scanCallback_preLollipop = new BluetoothAdapter.LeScanCallback()
	{
		@Override public void onLeScan(final BluetoothDevice device_native, final int rssi, final byte[] scanRecord)
        {
			m_mngr.getCrashResolver().notifyScannedDevice(device_native, this);
			
			m_mngr.getUpdateLoop().postIfNeeded(new Runnable()
			{
				@Override public void run()
				{
					m_mngr.onDiscovered(device_native, rssi, scanRecord);
				}
			});
        }
    };
    
	private final PA_Task.I_StateListener m_scanTaskListener = new PA_Task.I_StateListener()
	{
		@Override public void onStateChange(PA_Task task, PE_TaskState state)
		{
			if( task.getState().ordinal() <= PE_TaskState.QUEUED.ordinal() )  return;
			
			//--- DRK > Got this assert to trip by putting a breakpoint in constructor of NativeDeviceWrapper
			//---		and waiting, but now can't reproduce.
			if( !m_mngr.ASSERT(task.getClass() == P_Task_Scan.class && m_mngr.is(SCANNING)) )  return;
			
			if( state.isEndingState() )
			{
				P_Task_Scan scanTask = (P_Task_Scan) task;

				if( state == PE_TaskState.INTERRUPTED || state == PE_TaskState.TIMED_OUT || state == PE_TaskState.SUCCEEDED )
				{
					m_mngr.tryPurgingStaleDevices(scanTask.getTotalTimeExecuting());
				}

				m_mngr.stopNativeScan(scanTask);
				
				if( state == PE_TaskState.INTERRUPTED )
				{
					// task will be put back onto the queue presently...nothing to do here
				}
				else
				{
					m_mngr.clearScanningRelatedMembers(scanTask.isExplicit() ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL);
				}
			}
		}
	};
	
	private final BroadcastReceiver m_receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final String action = intent.getAction();
			
			if ( action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) )
			{
				onNativeBleStateChange(context, intent);
			}
			else if( action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED) )
			{
				onNativeBondStateChanged(context, intent);
			}
			else if( action.equals(BluetoothDevice.ACTION_FOUND) )
			{
				onDeviceFound(context, intent);
			}
			else if( action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) )
			{
				onClassicDiscoveryFinished();
			}
			
			//--- DRK > This block doesn't do anything...just wrote it to see how these other events work and if they're useful.
			//---		They don't seem to be but leaving it here for the future if needed anyway.
			else if( action.contains("ACL") || action.equals(BluetoothDevice.ACTION_UUID) || action.equals(BluetoothDevice_ACTION_DISAPPEARED) )
			{
				final BluetoothDevice device_native = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				if( action.equals(BluetoothDevice.ACTION_FOUND) )
				{
//					device_native.fetchUuidsWithSdp();
				}
				else if( action.equals(BluetoothDevice.ACTION_UUID) )
				{
					m_logger.e("");
				}
				
				BleDevice device = m_mngr.getDevice(device_native.getAddress());
				
				if( device != null )
				{
//					m_logger.e("Known device " + device.getDebugName() + " " + action);
				}
				else
				{
//					m_logger.e("Mystery device " + device_native.getName() + " " + device_native.getAddress() + " " + action);
				}
			}
		}
	};
	
	private final BleManager m_mngr;
	private final P_TaskQueue m_taskQueue;
	private final P_Logger m_logger;
	
	
	P_BleManager_Listeners(BleManager bleMngr)
	{
		m_mngr = bleMngr;
		m_taskQueue = m_mngr.getTaskQueue();
		m_logger = m_mngr.getLogger();
		
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		intentFilter.addAction(BluetoothDevice.ACTION_UUID);
		intentFilter.addAction(BluetoothDevice_ACTION_DISAPPEARED);
		
		m_mngr.getApplicationContext().registerReceiver(m_receiver, intentFilter);
	}
	
	void onDestroy()
	{
		m_mngr.getApplicationContext().unregisterReceiver(m_receiver);
	}
	
	PA_Task.I_StateListener getScanTaskListener()
	{
		return m_scanTaskListener;
	}
	
	private void onDeviceFound(Context context, Intent intent)
	{
		final BluetoothDevice device_native = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		final int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
		
		m_mngr.getUpdateLoop().postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				m_mngr.onDiscovered(device_native, rssi, null);
			}
		});
	}
	
	private final Runnable m_classicDiscoveryFinished = new Runnable()
	{
		@Override public void run()
		{
			m_taskQueue.interrupt(P_Task_Scan.class, m_mngr);
		}
	};
	
	private void onClassicDiscoveryFinished()
	{
		m_mngr.getUpdateLoop().postIfNeeded(m_classicDiscoveryFinished);
	}
	
	private void onNativeBleStateChange(Context context, Intent intent)
	{
		final int previousNativeState = intent.getExtras().getInt(BluetoothAdapter.EXTRA_PREVIOUS_STATE);
		final int newNativeState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
		
		int logLevel = newNativeState == BluetoothAdapter.ERROR || previousNativeState == BluetoothAdapter.ERROR ? Log.WARN : Log.INFO;
		m_logger.log(logLevel, "previous=" + m_logger.gattBleState(previousNativeState) + " new=" + m_logger.gattBleState(newNativeState));
		
		m_mngr.getUpdateLoop().postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				onNativeBleStateChange(previousNativeState, newNativeState);
			}
		});
	}
	
	private void onNativeBleStateChange(int previousNativeState, int newNativeState)
	{
		//--- DRK > Checking for inconsistent state at this point (instead of at bottom of function),
		//---		simply because where this is where it was first observed. Checking at the bottom
		//---		may not work because maybe this bug relied on a race condition.
		//---		UPDATE: Not checking for inconsistent state anymore cause it can be legitimate due to native 
		//---		state changing while call to this method is sitting on the main thread queue.
		BluetoothAdapter bluetoothAdapter = m_mngr.getNative().getAdapter();
		final int adapterState = bluetoothAdapter.getState();
//		boolean inconsistentState = adapterState != newNativeState;
		PA_StateTracker.E_Intent intent = E_Intent.INTENTIONAL;
		final boolean hitErrorState = newNativeState == BluetoothAdapter.ERROR;
		
		if( hitErrorState )
		{
			newNativeState = adapterState;
			
			if( newNativeState /*still*/ == BluetoothAdapter.ERROR )
			{
				return; // really not sure what we can do better here besides bailing out.
			}
		}
		else if( newNativeState == BluetoothAdapter.STATE_OFF )
		{
			m_mngr.m_wakeLockMngr.clear();
			
			m_taskQueue.fail(P_Task_TurnBleOn.class, m_mngr);
			P_Task_TurnBleOff turnOffTask = m_taskQueue.getCurrent(P_Task_TurnBleOff.class, m_mngr);
			intent = turnOffTask == null || turnOffTask.isImplicit() ? E_Intent.UNINTENTIONAL : intent;
			m_taskQueue.succeed(P_Task_TurnBleOff.class, m_mngr);
			
			//--- DRK > Should have already been handled by the "turning off" event, but this is just to be 
			//---		sure all devices are cleared in case something weird happens and we go straight
			//---		from ON to OFF or something.
			m_mngr.m_deviceMngr.undiscoverAllForTurnOff(m_mngr.m_deviceMngr_cache, intent);
		}
		else if( newNativeState == BluetoothAdapter.STATE_TURNING_ON )
		{
			if( !m_taskQueue.isCurrent(P_Task_TurnBleOn.class, m_mngr) )
			{
				m_taskQueue.add(new P_Task_TurnBleOn(m_mngr, /*implicit=*/true));
				intent = E_Intent.UNINTENTIONAL;
			}
			
			m_taskQueue.fail(P_Task_TurnBleOff.class, m_mngr);
		}
		else if( newNativeState == BluetoothAdapter.STATE_ON )
		{
			m_taskQueue.fail(P_Task_TurnBleOff.class, m_mngr);
			P_Task_TurnBleOn turnOnTask = m_taskQueue.getCurrent(P_Task_TurnBleOn.class, m_mngr);
			intent = turnOnTask == null || turnOnTask.isImplicit() ? E_Intent.UNINTENTIONAL : intent;
			m_taskQueue.succeed(P_Task_TurnBleOn.class, m_mngr);
		}
		else if( newNativeState == BluetoothAdapter.STATE_TURNING_OFF )
		{
			if( !m_taskQueue.isCurrent(P_Task_TurnBleOff.class, m_mngr) )
			{
				m_mngr.m_deviceMngr.undiscoverAllForTurnOff(m_mngr.m_deviceMngr_cache, E_Intent.UNINTENTIONAL);
				m_taskQueue.add(new P_Task_TurnBleOff(m_mngr, /*implicit=*/true));
				intent = E_Intent.UNINTENTIONAL;
			}
			
			m_taskQueue.fail(P_Task_TurnBleOn.class, m_mngr);
		}
		
		//--- DRK > Can happen I suppose if newNativeState is an error and we revert to using the queried state and it's the same as previous state.
		//----		Below logic should still be resilient to this, but early-outing just in case.
		if( previousNativeState == newNativeState )
		{
			return;			
		}
		
		BleManagerState previousState = BleManagerState.get(previousNativeState);
		BleManagerState newState = BleManagerState.get(newNativeState);
		
		m_mngr.getNativeStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, previousState, false, newState, true);
		m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, previousState, false, newState, true);
		
		if( previousNativeState != BluetoothAdapter.STATE_ON && newNativeState == BluetoothAdapter.STATE_ON )
		{
			m_mngr.m_deviceMngr.rediscoverDevicesAfterBleTurningBackOn();
			m_mngr.m_deviceMngr.reconnectDevicesAfterBleTurningBackOn();
		}
		
		if( hitErrorState )
		{
			m_mngr.uhOh(UhOh.UNKNOWN_BLE_ERROR);
		}
		
		if( previousNativeState == BluetoothAdapter.STATE_TURNING_OFF && newNativeState == BluetoothAdapter.STATE_ON )
		{
			m_mngr.uhOh(UhOh.CANNOT_DISABLE_BLUETOOTH);
		}
		else if( previousNativeState == BluetoothAdapter.STATE_TURNING_ON && newNativeState == BluetoothAdapter.STATE_OFF )
		{
			m_mngr.uhOh(UhOh.CANNOT_ENABLE_BLUETOOTH);
		}
//		else if( inconsistentState )
//		{
//			m_mngr.uhOh(UhOh.INCONSISTENT_NATIVE_BLE_STATE);
//			m_logger.w("adapterState=" + m_logger.gattBleState(adapterState) + " newState=" + m_logger.gattBleState(newNativeState));
//		}
	}
	
	private void onNativeBondStateChanged(Context context, Intent intent)
	{
		final int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
		final int newState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
		int logLevel = newState == BluetoothDevice.ERROR || previousState == BluetoothDevice.ERROR ? Log.WARN : Log.INFO;
		m_logger.log(logLevel, "previous=" + m_logger.gattBondState(previousState) + " new=" + m_logger.gattBondState(newState));
		
		final int failReason;
		
		if( newState == BluetoothDevice.BOND_NONE )
		{
			//--- DRK > Can't access BluetoothDevice.EXTRA_REASON cause of stupid @hide annotation, so hardcoding string here.
			failReason = intent.getIntExtra(BluetoothDevice_EXTRA_REASON, BluetoothDevice.ERROR);
			if( failReason != BleStatuses.BOND_SUCCESS )
			{
				m_logger.w(m_logger.gattUnbondReason(failReason));
			}
		}
		else
		{
			failReason = BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE;
		}
		
		final BluetoothDevice device_native = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		
		m_mngr.getUpdateLoop().postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				onNativeBondStateChanged(device_native, previousState, newState, failReason);
			}
		});
	}
	
	private void onNativeBondStateChanged(BluetoothDevice device_native, int previousState, int newState, int failReason)
	{
		BleDevice device = m_mngr.getDevice(device_native.getAddress());
			
		if( device == null )
		{
			P_Task_Bond bondTask = m_taskQueue.getCurrent(P_Task_Bond.class, m_mngr);
			
			if( bondTask != null )
			{
				if( bondTask.getDevice().getMacAddress().equals(device_native.getAddress()) )
				{
					device = bondTask.getDevice();
				}
			}
		}
		
		if( device /*still*/== null )
		{
			P_Task_Unbond unbondTask = m_taskQueue.getCurrent(P_Task_Unbond.class, m_mngr);
			
			if( unbondTask != null )
			{
				if( unbondTask.getDevice().getMacAddress().equals(device_native.getAddress()) )
				{
					device = unbondTask.getDevice();
				}
			}
		}
		
		if( device != null )
		{
			//--- DRK > Got an NPE here when restarting the app through the debugger. Pretty sure it's an impossible case
			//---		for actual app usage cause the listeners member of the device is final. So some memory corruption issue
			//---		associated with debugging most likely...still gating it for the hell of it.
			if( device.getListeners() != null )
			{
				device.getListeners().onNativeBondStateChanged(previousState, newState, failReason);
			}
		}
		
//		if( previousState == BluetoothDevice.BOND_BONDING && newState == BluetoothDevice.BOND_NONE )
//		{
//			m_mngr.uhOh(UhOh.WENT_FROM_BONDING_TO_UNBONDED);
//		}
	}
}
