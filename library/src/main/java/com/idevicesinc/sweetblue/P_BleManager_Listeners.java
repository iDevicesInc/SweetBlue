package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleManagerState.BLE_SCAN_READY;
import static com.idevicesinc.sweetblue.BleManagerState.BOOST_SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.IDLE;
import static com.idevicesinc.sweetblue.BleManagerState.OFF;
import static com.idevicesinc.sweetblue.BleManagerState.ON;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING_PAUSED;
import static com.idevicesinc.sweetblue.BleManagerState.STARTING_SCAN;
import static com.idevicesinc.sweetblue.BleManagerState.TURNING_OFF;
import static com.idevicesinc.sweetblue.BleManagerState.TURNING_ON;

import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


final class P_BleManager_Listeners
{
    private static final String BluetoothDevice_EXTRA_REASON = "android.bluetooth.device.extra.REASON";
    private static final String BluetoothDevice_ACTION_DISAPPEARED = "android.bluetooth.device.action.DISAPPEARED";


    private Interval m_pollRate;
    private double m_timeSinceLastPoll;
    private boolean m_checkingState;


    private final PA_Task.I_StateListener m_scanTaskListener = new PA_Task.I_StateListener()
    {
        @Override public void onStateChange(PA_Task task, PE_TaskState state)
        {
            if (task.getState().ordinal() <= PE_TaskState.QUEUED.ordinal()) return;

            //--- DRK > Got this assert to trip by putting a breakpoint in constructor of NativeDeviceWrapper
            //---		and waiting, but now can't reproduce.
            if (!m_mngr.ASSERT(task.getClass() == P_Task_Scan.class && state != PE_TaskState.CLEARED_FROM_QUEUE && m_mngr.isAny(SCANNING, BOOST_SCANNING, STARTING_SCAN, SCANNING_PAUSED)))
                return;

            if (state.isEndingState())
            {
                final P_Task_Scan scanTask = (P_Task_Scan) task;
                final double totalTimeExecuting = scanTask.getTotalTimeExecuting();

                if (state == PE_TaskState.INTERRUPTED || state == PE_TaskState.TIMED_OUT || state == PE_TaskState.SUCCEEDED)
                {
                    if (state == PE_TaskState.INTERRUPTED)
                    {
                        m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
                        {
                            @Override public void run()
                            {
                                m_mngr.tryPurgingStaleDevices(totalTimeExecuting);
                            }
                        });
                    }
                    else
                    {
                        m_mngr.tryPurgingStaleDevices(totalTimeExecuting);
                    }
                }

                m_mngr.getScanManager().stopNativeScan(scanTask);

                if (state == PE_TaskState.INTERRUPTED)
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
        @Override public void onReceive(final Context context, final Intent intent)
        {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                onNativeBleStateChangeFromBroadcastReceiver(context, intent);
            }
            else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                onNativeBondStateChanged(context, intent);
            }
            else if (action.equals(BluetoothDevice.ACTION_FOUND))
            {
                onDeviceFound_classic(context, intent);
            }
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
            {
                onClassicDiscoveryFinished();
            }
            else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST))
            {
                onBondRequest(intent);
            }

            //--- DRK > This block doesn't do anything...just wrote it to see how these other events work and if they're useful.
            //---		They don't seem to be but leaving it here for the future if needed anyway.
            else if (action.contains("ACL") || action.equals(BluetoothDevice.ACTION_UUID) || action.equals(BluetoothDevice_ACTION_DISAPPEARED))
            {
                final BluetoothDevice device_native = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (action.equals(BluetoothDevice.ACTION_FOUND))
                {
//					device_native.fetchUuidsWithSdp();
                }
                else if (action.equals(BluetoothDevice.ACTION_UUID))
                {
                    m_mngr.getLogger().d("");
                }

                BleDevice device = m_mngr.getDevice(device_native.getAddress());

                if (device != null)
                {
//					m_mngr.getLogger().e("Known device " + device.getDebugName() + " " + action);
                }
                else
                {
//					m_mngr.getLogger().e("Mystery device " + device_native.getName() + " " + device_native.getAddress() + " " + action);
                }
            }
        }
    };

    private void onBondRequest(Intent intent)
    {
        final P_NativeDeviceLayer layer = m_mngr.m_config.newDeviceLayer(BleDevice.NULL);

        final BluetoothDevice device_native = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        layer.setNativeDevice(device_native);

        onNativeBondRequest(layer);
    }

    private void onNativeBondRequest(final P_NativeDeviceLayer device_native)
    {
        m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
        {
            @Override
            public void run()
            {
                final BleDevice device = getDeviceFromNativeDevice(device_native);

                if (device != null)
                {
                    if (device.getListeners() != null)
                    {
                        device.getListeners().onNativeBoneRequest_updateThread(device);
                    }
                }
            }
        });
    }

    private final BleManager m_mngr;

    private int m_nativeState;

    P_BleManager_Listeners(BleManager bleMngr)
    {
        m_mngr = bleMngr;

        m_mngr.getApplicationContext().registerReceiver(m_receiver, newIntentFilter());

        m_pollRate = m_mngr.m_config.defaultStatePollRate;
    }

    public void updatePollRate(Interval rate)
    {
        m_pollRate = rate;
    }

    private static IntentFilter newIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        intentFilter.addAction(BluetoothDevice_ACTION_DISAPPEARED);

        return intentFilter;
    }

    void onDestroy()
    {
        try
        {
            m_mngr.getApplicationContext().unregisterReceiver(m_receiver);
        } catch (Exception e)
        {
            m_mngr.getLogger().w("Tried to unregister ble listeners failed with message: " + e.getMessage());
        }
    }

    PA_Task.I_StateListener getScanTaskListener()
    {
        return m_scanTaskListener;
    }

    private void onDeviceFound_classic(Context context, Intent intent)
    {
        // If this was discovered via the hack to show the bond popup, then do not propogate this
        // any further, as this scan is JUST to get the dialog to pop up (rather than show in the notification area)
        P_Task_BondPopupHack hack = m_mngr.getTaskQueue().getCurrent(P_Task_BondPopupHack.class, m_mngr);

        // Only pipe discovery event if the scan task is running, and the manager says we're doing a classic scan
        P_Task_Scan scan = m_mngr.getTaskQueue().getCurrent(P_Task_Scan.class, m_mngr);
        if (hack == null && scan != null && m_mngr.m_config.scanApi == BleScanApi.CLASSIC)
        {
            final BluetoothDevice device_native = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

            final P_NativeDeviceLayer layer = m_mngr.m_config.newDeviceLayer(BleDevice.NULL);
            layer.setNativeDevice(device_native);

            final List<P_ScanManager.DiscoveryEntry> entries = new ArrayList<>(1);
            entries.add(new P_ScanManager.DiscoveryEntry(layer, rssi, null));

            m_mngr.onDiscoveredFromNativeStack(entries);
        }
    }

    void onClassicDiscoveryFinished()
    {
        P_Task_Scan scanTask = m_mngr.getTaskQueue().getCurrent(P_Task_Scan.class, m_mngr);
        boolean interrupt = false;
        if (scanTask != null)
        {
            if (scanTask.isClassicBoosted())
            {
                return;
            }
            if (m_mngr.getScanManager().isPeriodicScan())
                interrupt = true;

        }
        if (interrupt)
            m_mngr.getTaskQueue().interrupt(P_Task_Scan.class, m_mngr);
        else
            // Try to succeed the task, if that fails, it means it's no longer current, so we'll clear the queue of the scan task, just to be sure it doesn't
            // start up again
            if (!m_mngr.getTaskQueue().succeed(P_Task_Scan.class, m_mngr))
            {
                m_mngr.getTaskQueue().clearQueueOf(P_Task_Scan.class, m_mngr);
            }
    }

    final void onNativeBleStateChangeFromBroadcastReceiver(Context context, Intent intent)
    {
        final int previousNativeState = intent.getExtras().getInt(BluetoothAdapter.EXTRA_PREVIOUS_STATE);
        final int newNativeState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

        int logLevel = newNativeState == BluetoothAdapter.ERROR || previousNativeState == BluetoothAdapter.ERROR ? Log.WARN : Log.INFO;
        m_mngr.getLogger().log(logLevel, "previous=" + m_mngr.getLogger().gattBleState(previousNativeState) + " new=" + m_mngr.getLogger().gattBleState(newNativeState));

        if (Utils.isMarshmallow())
        {
            //--- > RB Commenting all this logic out. It's my opinion that we should never ignore native callbacks. Granted, we also poll the native
            //---       state on devices running Marshmallow or higher, but there are times when polling is insufficient (we could potentially get several
            //---       broadcasts between update ticks). So we'll just check if we're already checking the state, if not, we filter this broadcast through
            //---       to the system.
            // If in the IDLE state, we will pass the state change through as if pre 6.0, and bump out of the IDLE state
            // to catch any other changes that may happen in polling.
//            if (m_mngr.is(IDLE))
//            {
                // If we're checking the state in the update method, don't bother posting this here. We'll fall out of IDLE state, so any further changes
                // will be caught soon in polling.
                if (!m_checkingState)
                {
                    onNativeBleStateChange(previousNativeState, newNativeState);
                }
//                m_mngr.m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, IDLE, false);
//                m_mngr.checkIdleStatus();
//            }

            //--- > RB The below is commented out because above we are handling all states from the native callback, unless polling is currently checking
            //---       the state.
//            /*else*/ if (previousNativeState == BleStatuses.STATE_ON && newNativeState == BleStatuses.STATE_TURNING_OFF)
//            {
//                if (m_nativeState == BleStatuses.STATE_ON)
//                {
//                    m_nativeState = BleStatuses.STATE_TURNING_OFF;
//
//                    //--- DRK > We allow this code path in this particular case in marshmallow because STATE_TURNING_OFF is only active
//                    //---		for a very short time, so polling might miss it. If polling detects it before this, fine, because we
//                    //---		early-out above and never call this method. If afterwards, it skips it because m_nativeState is identical
//                    //---		to what's reported from the native stack.
//                    onNativeBleStateChange(previousNativeState, newNativeState);
//                }
//            }
        }
        else
        {
            onNativeBleStateChange(previousNativeState, newNativeState);
        }
    }

    private void onNativeBleStateChange(int previousNativeState, int newNativeState)
    {
        //--- DRK > Checking for inconsistent state at this point (instead of at bottom of function),
        //---		simply because where this is where it was first observed. Checking at the bottom
        //---		may not work because maybe this bug relied on a race condition.
        //---		UPDATE: Not checking for inconsistent state anymore cause it can be legitimate due to native
        //---		state changing while call to this method is sitting on the update thread queue.
        final int adapterState = m_mngr.managerLayer().getState();

//		boolean inconsistentState = adapterState != newNativeState;
        PA_StateTracker.E_Intent intent = E_Intent.INTENTIONAL;
        final boolean hitErrorState = newNativeState == BluetoothAdapter.ERROR;

        if (hitErrorState)
        {
            newNativeState = adapterState;

            if (newNativeState /*still*/ == BluetoothAdapter.ERROR)
            {
                return; // really not sure what we can do better here besides bailing out.
            }
        }
        else if (newNativeState == BluetoothAdapter.STATE_OFF)
        {
            m_mngr.m_wakeLockMngr.clear();

            if (m_mngr.getTaskQueue().isCurrent(P_Task_TurnBleOn.class, m_mngr))
            {
                return;
            }

            m_mngr.getTaskQueue().fail(P_Task_TurnBleOn.class, m_mngr);
            P_Task_TurnBleOff turnOffTask = m_mngr.getTaskQueue().getCurrent(P_Task_TurnBleOff.class, m_mngr);
            intent = turnOffTask == null || turnOffTask.isImplicit() ? E_Intent.UNINTENTIONAL : intent;
            m_mngr.getTaskQueue().succeed(P_Task_TurnBleOff.class, m_mngr);

            //--- DRK > Should have already been handled by the "turning off" event, but this is just to be
            //---		sure all devices are cleared in case something weird happens and we go straight
            //---		from ON to OFF or something.
            m_mngr.m_deviceMngr.undiscoverAllForTurnOff(m_mngr.m_deviceMngr_cache, intent);

            // We need to make sure to remove the transitory states, in case they were missed, and to enforce the ON/OFF state to get propagated app-side
            if (m_mngr.isAny(TURNING_OFF, TURNING_ON))
            {
                m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_OFF, false, TURNING_ON, false, OFF, true, ON, false, BLE_SCAN_READY, false);
            }
        }
        else if (newNativeState == BluetoothAdapter.STATE_TURNING_ON)
        {
            if (!m_mngr.getTaskQueue().isCurrent(P_Task_TurnBleOn.class, m_mngr))
            {
                m_mngr.getTaskQueue().add(new P_Task_TurnBleOn(m_mngr, /*implicit=*/true));
                intent = E_Intent.UNINTENTIONAL;
            }

            m_mngr.getTaskQueue().fail(P_Task_TurnBleOff.class, m_mngr);
        }
        else if (newNativeState == BluetoothAdapter.STATE_ON)
        {
            m_mngr.getTaskQueue().fail(P_Task_TurnBleOff.class, m_mngr);
            P_Task_TurnBleOn turnOnTask = m_mngr.getTaskQueue().getCurrent(P_Task_TurnBleOn.class, m_mngr);
            intent = turnOnTask == null || turnOnTask.isImplicit() ? E_Intent.UNINTENTIONAL : intent;
            m_mngr.getTaskQueue().succeed(P_Task_TurnBleOn.class, m_mngr);

            // We need to make sure to remove the transitory states, in case they were missed, and to enforce the ON/OFF state to get propagated app-side
            if (m_mngr.isAny(TURNING_OFF, TURNING_ON))
            {
                m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_OFF, false, TURNING_ON, false, ON, true, OFF, false);
            }
        }
        else if (newNativeState == BluetoothAdapter.STATE_TURNING_OFF)
        {
            if (!m_mngr.getTaskQueue().isCurrent(P_Task_TurnBleOff.class, m_mngr))
            {
                m_mngr.m_deviceMngr.disconnectAllForTurnOff(PE_TaskPriority.CRITICAL);

//				m_mngr.m_deviceMngr.undiscoverAllForTurnOff(m_mngr.m_deviceMngr_cache, E_Intent.UNINTENTIONAL);
                m_mngr.getTaskQueue().add(new P_Task_TurnBleOff(m_mngr, /*implicit=*/true));

                if (m_mngr.m_server != null)
                {
                    m_mngr.m_server.disconnect_internal(BleServer.ServiceAddListener.Status.CANCELLED_FROM_BLE_TURNING_OFF, BleServer.ConnectionFailListener.Status.CANCELLED_FROM_BLE_TURNING_OFF, State.ChangeIntent.UNINTENTIONAL);
                }

                intent = E_Intent.UNINTENTIONAL;
            }

            m_mngr.getTaskQueue().fail(P_Task_TurnBleOn.class, m_mngr);
        }

        //--- DRK > Can happen I suppose if newNativeState is an error and we revert to using the queried state and it's the same as previous state.
        //----		Below logic should still be resilient to this, but early-outing just in case.
        if (previousNativeState == newNativeState)
        {
            return;
        }

        BleManagerState previousState = BleManagerState.get(previousNativeState);
        BleManagerState newState = BleManagerState.get(newNativeState);

        m_mngr.getLogger().e(previousNativeState + " " + newNativeState + " " + previousState + " " + newState);

        m_mngr.getNativeStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, previousState, false, newState, true);
        m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, previousState, false, newState, true);

        // If BT is now off, and the manager thinks it's still in the scan ready state, then remove the scan ready state.
        if (newNativeState == BluetoothAdapter.STATE_OFF && m_mngr.is(BLE_SCAN_READY))
        {
            m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BLE_SCAN_READY, false);
        }

        if (previousNativeState != BluetoothAdapter.STATE_ON && newNativeState == BluetoothAdapter.STATE_ON)
        {
            m_mngr.m_deviceMngr.rediscoverDevicesAfterBleTurningBackOn();
            m_mngr.m_deviceMngr.reconnectDevicesAfterBleTurningBackOn();
        }

        if (hitErrorState)
        {
            m_mngr.uhOh(UhOh.UNKNOWN_BLE_ERROR);
        }

        if (previousNativeState == BluetoothAdapter.STATE_TURNING_OFF && newNativeState == BluetoothAdapter.STATE_ON)
        {
            m_mngr.uhOh(UhOh.CANNOT_DISABLE_BLUETOOTH);
        }
        else if (previousNativeState == BluetoothAdapter.STATE_TURNING_ON && newNativeState == BluetoothAdapter.STATE_OFF)
        {
            m_mngr.uhOh(UhOh.CANNOT_ENABLE_BLUETOOTH);
        }
//		else if( inconsistentState )
//		{
//			m_mngr.uhOh(UhOh.INCONSISTENT_NATIVE_BLE_STATE);
//			m_mngr.getLogger().w("adapterState=" + m_mngr.getLogger().gattBleState(adapterState) + " newState=" + m_mngr.getLogger().gattBleState(newNativeState));
//		}
    }

    private void onNativeBondStateChanged(Context context, Intent intent)
    {
        final int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
        final int newState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
        int logLevel = newState == BluetoothDevice.ERROR || previousState == BluetoothDevice.ERROR ? Log.WARN : Log.INFO;
        m_mngr.getLogger().log(logLevel, "previous=" + m_mngr.getLogger().gattBondState(previousState) + " new=" + m_mngr.getLogger().gattBondState(newState));

        final int failReason;

        if (newState == BluetoothDevice.BOND_NONE)
        {
            //--- DRK > Can't access BluetoothDevice.EXTRA_REASON cause of stupid @hide annotation, so hardcoding string here.
            failReason = intent.getIntExtra(BluetoothDevice_EXTRA_REASON, BluetoothDevice.ERROR);

            if (failReason != BleStatuses.BOND_SUCCESS)
            {
                m_mngr.getLogger().w(m_mngr.getLogger().gattUnbondReason(failReason));
            }
        }
        else
        {
            failReason = BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE;
        }

        final P_NativeDeviceLayer layer = m_mngr.m_config.newDeviceLayer(BleDevice.NULL);

        final BluetoothDevice device_native = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        layer.setNativeDevice(device_native);

        onNativeBondStateChanged(layer, previousState, newState, failReason);
    }

    private BleDevice getDeviceFromNativeDevice(final P_NativeDeviceLayer device_native)
    {
        BleDevice device = m_mngr.getDevice(device_native.getAddress());

        if (device == null)
        {
            final P_Task_Bond bondTask = m_mngr.getTaskQueue().getCurrent(P_Task_Bond.class, m_mngr);

            if (bondTask != null)
            {
                if (bondTask.getDevice().getMacAddress().equals(device_native.getAddress()))
                {
                    device = bondTask.getDevice();
                }
            }
        }

        if (device /*still*/ == null)
        {
            final P_Task_Unbond unbondTask = m_mngr.getTaskQueue().getCurrent(P_Task_Unbond.class, m_mngr);

            if (unbondTask != null)
            {
                if (unbondTask.getDevice().getMacAddress().equals(device_native.getAddress()))
                {
                    device = unbondTask.getDevice();
                }
            }
        }

        return device;
    }

    void onNativeBondStateChanged(final P_NativeDeviceLayer device_native, final int previousState, final int newState, final int failReason)
    {
        m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
        {
            @Override public void run()
            {
                final BleDevice device = getDeviceFromNativeDevice(device_native);

                if (device != null)
                {
                    //--- DRK > Got an NPE here when restarting the app through the debugger. Pretty sure it's an impossible case
                    //---		for actual app usage cause the listeners member of the device is final. So some memory corruption issue
                    //---		associated with debugging most likely...still gating it for the hell of it.
                    if (device.getListeners() != null)
                    {
                        device.getListeners().onNativeBondStateChanged_updateThread(previousState, newState, failReason);
                    }
                }

//		if( previousState == BluetoothDevice.BOND_BONDING && newState == BluetoothDevice.BOND_NONE )
//		{
//			m_mngr.uhOh(UhOh.WENT_FROM_BONDING_TO_UNBONDED);
//		}
             }
        });
    }

    private static boolean isBleStateFromPreM(final int state)
    {
        return
                state == BleStatuses.STATE_ON ||
                        state == BleStatuses.STATE_TURNING_OFF ||
                        state == BleStatuses.STATE_OFF ||
                        state == BleStatuses.STATE_TURNING_ON;
    }

    private void assertOnWeirdStateChange(final int oldState, final int newState)
    {
        //--- DRK > Note this is not an assert SweetBlue-logic-wise...just want to call out attention to state changes that I assumed were impossible.
        //---		That said I will not be surprised if this trips.
        m_mngr.ASSERT(false, "Weird BLE state change detected from polling: " + m_mngr.getLogger().gattBleState(oldState) + " -> " + m_mngr.getLogger().gattBleState(newState));

        //--- RB > We still want to update the native state here, even if it's a state change we weren't expecting to get. This will probably happen more often now that the IDLE
        //---       state is in, which means it's possible we miss more state changes as a result
        onNativeBleStateChange_fromPolling(oldState, newState);
    }

    private void onNativeBleStateChange_fromPolling(final int oldState, final int newState)
    {
        if (false == isBleStateFromPreM(oldState) || false == isBleStateFromPreM(newState))
        {
            m_mngr.ASSERT(false, "Either " + m_mngr.getLogger().gattBleState(oldState) + " or " + m_mngr.getLogger().gattBleState(newState) + " are not valid pre-M BLE states!");
        }
        else
        {
            onNativeBleStateChange(oldState, newState);
        }
    }

    /**
     * See the copy/pasted log statements in {@link BleStatuses} for an example of how the state changes
     * occur over the course of a few seconds in Android M.
     */
    public void update(double time_Step)
    {
//		m_mngr.getLogger().e("*********************" + m_mngr.getLogger().gattBleState(getBleState()));

        if (Utils.isMarshmallow() && m_mngr.m_config.allowManagerStatePolling && Interval.isEnabled(m_pollRate) && m_timeSinceLastPoll >= m_pollRate.secs())
        {
            m_checkingState = true;

            m_timeSinceLastPoll = 0.0;

            final int oldState = m_nativeState;
            final int newState = getBleState();

            if (oldState != newState)
            {
                m_nativeState = newState;

                if (oldState == BleStatuses.STATE_ON)
                {
                    if (newState == BleStatuses.STATE_TURNING_OFF || newState == BleStatuses.STATE_BLE_TURNING_OFF)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_ON, BleStatuses.STATE_TURNING_OFF);
                    }
                    else if (newState == BleStatuses.STATE_OFF)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_ON, BleStatuses.STATE_OFF);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_TURNING_OFF)
                {
                    if (newState == BleStatuses.STATE_ON)
                    {
                        //--- DRK > This is a "valid" case observed in pre-Android-M BroadcastReceiver callbacks.
                        //---		Down the line this will result in an UhOh and log errors and whatnot but we
                        //---		let it pass just because we did previously.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_ON);
                    }
                    else if (newState == BleStatuses.STATE_OFF)
                    {
                        //--- DRK > Based on limited testing, we *should* get STATE_TURNING_OFF->STATE_BLE_TURNING_OFF->STATE_OFF
                        //---		but it's possible we missed STATE_BLE_TURNING_OFF so no problem, behaves just like pre-M.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_OFF);
                    }
                    else if (newState == BleStatuses.STATE_BLE_TURNING_OFF)
                    {
                        //--- DRK > We skip this case cause we consider STATE_TURNING_OFF to be the "start"
                        //---		of the turning off process, and STATE_TURNING_OFF->STATE_BLE_TURNING_OFF to just be the "continuation".
                    }
                    else if (newState == BleStatuses.STATE_BLE_ON)
                    {
                        //--- DRK > Ignoring this because even though oddly enough it's an observed state transition, it doesn't make
                        //---		sense from the perspective of onNativeBleStateChange(). Note that it happens pretty fast so sometimes we miss it, but no big deal.
                    }
                    else if (newState == BleStatuses.STATE_BLE_TURNING_ON)
                    {
                        //--- RB > Adding this missing state. Due to the IDLE state, it's possible we miss other states in between update loops, so we must make sure
                        //--- 		to account for all states in this check. This case is a bit more odd, as we went from TURNING_OFF, to BLE_TURNING_ON. Will treat this as
                        //---		STATE_TURNING_ON (usually, that state comes before this one)

                        // Fail the Turn off task, as we're now turning on

                        m_mngr.getTaskQueue().fail(P_Task_TurnBleOff.class, m_mngr);

                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_TURNING_ON);
                    }
                    else if (newState == BleStatuses.STATE_TURNING_ON)
                    {
                        //--- RB > Adding this missing state. Due to the IDLE state, it's possible we miss other states in between update loops, so we must make sure
                        //--- 		to account for all states in this check.

                        // Fail the Turn off task, as we're now turning on

                        m_mngr.getTaskQueue().fail(P_Task_TurnBleOff.class, m_mngr);

                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_TURNING_ON);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_BLE_TURNING_OFF)
                {
                    if (newState == BleStatuses.STATE_OFF)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_OFF);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_OFF)
                {
                    if (newState == BleStatuses.STATE_ON)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_OFF, BleStatuses.STATE_ON);
                    }
                    else if (newState == BleStatuses.STATE_BLE_TURNING_ON || newState == BleStatuses.STATE_TURNING_ON)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_OFF, BleStatuses.STATE_TURNING_ON);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_BLE_TURNING_ON)
                {
                    if (newState == BleStatuses.STATE_ON)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_ON);
                    }
                    else if (newState == BleStatuses.STATE_OFF)
                    {
                        //--- DRK > Have never seen this case directly but *have* seen STATE_TURNING_ON->STATE_OFF so have UhOh/logging-logic
                        //---		in place to handle it.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_OFF);
                    }
                    else if (newState == BleStatuses.STATE_TURNING_ON)
                    {
                        //--- DRK > We skip this case cause we consider STATE_BLE_TURNING_ON to be the "start"
                        //---		of the turning on process, and STATE_BLE_TURNING_ON->STATE_TURNING_ON to just be the "continuation".
                    }
                    else if (newState == BleStatuses.STATE_BLE_ON)
                    {
                        //--- DRK > Also skipping this transition because we consider it the continuation of bluetooth turning on.
                        //---		Next state should be STATE_TURNING_ON.
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_TURNING_ON)
                {
                    if (newState == BleStatuses.STATE_ON)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_ON);
                    }
                    else if (newState == BleStatuses.STATE_OFF)
                    {
                        //--- DRK > "Valid" case seen in the wild pre-M. UhOhs/logging are in place to catch it.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_OFF);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }


                //--- DRK > I've put line breaks before this else-if case to emphasize how it doesn't
                //---		fit in nicely with the rest and should be looked down upon and even ridiculed.
                else if (oldState == BleStatuses.STATE_BLE_ON)
                {
                    if (newState == BleStatuses.STATE_OFF)
                    {
                        //--- DRK > This is to cover the case of STATE_ON->STATE_TURNING_OFF->STATE_BLE_ON->STATE_BLE_TURNING_OFF->STATE_OFF (see logcat in BleStatuses)
                        //---		but STATE_BLE_TURNING_OFF gets skipped for whatever reason because the timestep is large.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_OFF);
                    }
                    else if (newState == BleStatuses.STATE_BLE_TURNING_OFF)
                    {
                        //--- DRK > Skipping because this is just the continuation of the turning off process that should have been caught earlier.
                    }
                    else if (newState == BleStatuses.STATE_TURNING_ON)
                    {
                        //--- DRK > Skipping because this is just the continuation of the bluetooth turning on process.
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
            }
            m_checkingState = false;
        }
        else if (Interval.isEnabled(m_pollRate) && m_timeSinceLastPoll < m_pollRate.secs())
        {
            m_timeSinceLastPoll += time_Step;
        }
    }

    private int getBleState()
    {
        if (Utils.isMarshmallow() && m_mngr.getNativeAdapter() != null)
        {
            return m_mngr.managerLayer().getBleState();
        }
        else
        {
            return m_mngr.managerLayer().getState();
        }
    }
}
