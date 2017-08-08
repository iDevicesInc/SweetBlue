package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;

import static com.idevicesinc.sweetblue.BleDeviceState.BONDED;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDING;
import static com.idevicesinc.sweetblue.BleDeviceState.UNBONDED;
import com.idevicesinc.sweetblue.BleDevice.BondListener.BondEvent;
import com.idevicesinc.sweetblue.BleDevice.BondListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.BondListener.Status;
import com.idevicesinc.sweetblue.BleDeviceConfig.BondFilter;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;
import java.util.UUID;


final class P_BondManager
{
	static final Object[] OVERRIDE_UNBONDED_STATES = {UNBONDED, true, BONDING, false, BONDED, false};
	static final Object[] OVERRIDE_BONDING_STATES = {UNBONDED, false, BONDING, true, BONDED, false};
	static final Object[] OVERRIDE_EMPTY_STATES = {};
	
	private final BleDevice m_device;

	private int m_bondRetries = 0;
	
	private BleDevice.BondListener m_listener;

	private boolean m_bondRequested;

	
	P_BondManager(BleDevice device)
	{
		m_device = device;
	}

	
	public final void setListener(BleDevice.BondListener listener_nullable)
	{
		m_listener = listener_nullable;
	}

	public final void resetBondRetryCount()
	{
		m_bondRetries = 0;
		m_bondRequested = false;
	}

	final void onBondTaskStateChange(final PA_Task task, final PE_TaskState state)
	{
		final E_Intent intent = task.isExplicit() ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
		
		if( task.getClass() == P_Task_Bond.class )
		{
			final P_Task_Bond bondTask = (P_Task_Bond) task;
			
			if( state.isEndingState() )
			{
				if( state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT )
				{
					this.onNativeBond(intent);
				}
				else if( state == PE_TaskState.SOFTLY_CANCELLED )
				{
					
				}
				else
				{
					final int failReason = bondTask.getFailReason();
					final boolean wasDirect = bondTask.isDirect();
					final BondListener.Status status;
					
					if( state == PE_TaskState.TIMED_OUT )
					{
						status = Status.TIMED_OUT;
					}
					else if( state == PE_TaskState.FAILED_IMMEDIATELY )
					{
						status = Status.FAILED_IMMEDIATELY;
					}
					else
					{
						status = Status.FAILED_EVENTUALLY;
					}
					
					this.onNativeBondFailed(intent, status, failReason, wasDirect);
				}
			}
		}
		else if( task.getClass() == P_Task_Unbond.class )
		{
			if( state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT )
			{
				this.onNativeUnbond(intent);
			}
			else
			{
				// not sure what to do here, if anything
			}
		}
	}

	final void onNativeUnbond(final E_Intent intent)
	{
		m_device.stateTracker_updateBoth(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, false, BONDING, false, UNBONDED, true);
	}

	final void onNativeBonding(final E_Intent intent)
	{
		m_device.stateTracker_updateBoth(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, false, BONDING, true, UNBONDED, false);
	}

	final void onNativeBond(final E_Intent intent)
	{
		final boolean wasAlreadyBonded = m_device.is(BONDED);
		
		m_device.stateTracker_updateBoth(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, true, BONDING, false, UNBONDED, false);
		
		if( !wasAlreadyBonded )
		{
			invokeCallback(Status.SUCCESS, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, intent.convert());
		}
	}
	
	private boolean failConnection(final BondListener.Status status)
	{
		if( status.canFailConnection() )
		{
			if( m_device.is_internal(BleDeviceState.CONNECTING_OVERALL) )
			{
				final boolean bondingFailFailsConnection = BleDeviceConfig.bool(m_device.conf_device().bondingFailFailsConnection, m_device.conf_mngr().bondingFailFailsConnection);
				
				if( bondingFailFailsConnection )
				{
					return true;
				}
			}
		}
		
		return false;
	}

	final Object[] getOverrideBondStatesForDisconnect(ConnectionFailListener.Status connectionFailReasonIfConnecting)
	{
		final Object[] overrideBondingStates;
		
		if( connectionFailReasonIfConnecting == ConnectionFailListener.Status.BONDING_FAILED )
		{
			overrideBondingStates = OVERRIDE_UNBONDED_STATES;
		}
		else
		{
			overrideBondingStates = OVERRIDE_EMPTY_STATES;
		}
		
		return overrideBondingStates;
	}

	final void onNativeBondRequest()
	{
		m_bondRequested = true;
	}

	final void onNativeBondFailed(final E_Intent intent, final BondListener.Status status, final int failReason, final boolean wasDirect)
	{
		if( isNativelyBondingOrBonded() )
		{
			//--- DRK > This is for cases where the bond task has timed out,
			//--- or otherwise failed without actually resetting internal bond state.
			m_device.unbond_justAddTheTask();
		}

		// Determine if we need to retry the bond.
		if (getFilter() != null)
		{
			final BondRetryFilter.RetryEvent event = new BondRetryFilter.RetryEvent(m_device, failReason, m_bondRetries, wasDirect, m_bondRequested);
			final BondRetryFilter.Please please = m_device.getManager().m_config.bondRetryFilter.onEvent(event);
			if (please.shouldRetry())
			{
				m_device.getManager().getLogger().w("Bond failed with failReason of " + m_device.getManager().getLogger().gattUnbondReason(failReason) + ". Retrying bond...");
				m_bondRetries++;
				m_device.stateTracker_updateBoth(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDING, false, UNBONDED, true);
				m_device.bond_private(wasDirect, false, m_listener);
				return;
			}
		}
		resetBondRetryCount();

		
		if( m_device.is_internal(BleDeviceState.CONNECTED) || m_device.is_internal(BleDeviceState.CONNECTING) )
		{
			saveNeedsBondingIfDesired();
		}
		
		if( failConnection(status) )
		{
			final boolean doingReconnect_shortTerm = m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM);
			
			m_device.disconnectWithReason(BleDevice.ConnectionFailListener.Status.BONDING_FAILED, status.timing(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, failReason, m_device.NULL_READWRITE_EVENT());
		}
		else
		{
			onNativeBondFailed_common(intent);
		}
		
		invokeCallback(status, failReason, intent.convert());
		
		if( status == Status.TIMED_OUT )
		{
			m_device.getManager().uhOh(UhOh.BOND_TIMED_OUT);
		}
	}

	private BondRetryFilter getFilter()
	{
		if (m_device.conf_device().bondRetryFilter != null)
		{
			return m_device.conf_device().bondRetryFilter;
		}
		else
		{
			return m_device.conf_mngr().bondRetryFilter;
		}
	}

	final void saveNeedsBondingIfDesired()
	{
		final boolean tryBondingWhileDisconnected = BleDeviceConfig.bool(m_device.conf_device().tryBondingWhileDisconnected, m_device.conf_mngr().tryBondingWhileDisconnected);
		
		if( tryBondingWhileDisconnected )
		{
			final boolean tryBondingWhileDisconnected_manageOnDisk = BleDeviceConfig.bool(m_device.conf_device().tryBondingWhileDisconnected_manageOnDisk, m_device.conf_mngr().tryBondingWhileDisconnected_manageOnDisk);
			
			m_device.getManager().m_diskOptionsMngr.saveNeedsBonding(m_device.getMacAddress(), tryBondingWhileDisconnected_manageOnDisk);
		}
	}
	
	private void onNativeBondFailed_common(final E_Intent intent)
	{
		m_device.stateTracker_updateBoth(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, false, BONDING, false, UNBONDED, true);
	}

	final boolean bondIfNeeded(final UUID charUuid, final BondFilter.CharacteristicEventType type)
	{
		final BleDeviceConfig.BondFilter bondFilter = m_device.conf_device().bondFilter != null ? m_device.conf_device().bondFilter : m_device.conf_mngr().bondFilter;
		
		if( bondFilter == null )  return false;
		
		final BondFilter.CharacteristicEvent event = new BleDeviceConfig.BondFilter.CharacteristicEvent(m_device, charUuid, type);
		
		final BondFilter.Please please = bondFilter.onEvent(event);
		
		return applyPlease_BondFilter(please);
	}

	final boolean applyPlease_BondFilter(BondFilter.Please please_nullable)
	{
		if( please_nullable == null )
		{
			return false;
		}

		if (!Utils.isKitKat())
		{
			return false;
		}

		final Boolean bond = please_nullable.bond_private();
		
		if( bond == null )
		{
			return false;
		}
		
		if( bond )
		{
			m_device.bond_private(/*isDirect=*/false, false, please_nullable.listener());
		}
		else
		{
            if (m_device.isAny(BONDING, BONDED))
			{
				m_device.unbond();
			}
		}
		
		return bond;
	}

	final BondEvent invokeCallback(Status status, int failReason, State.ChangeIntent intent)
	{
		final BondEvent event = new BondEvent(m_device, status, failReason, intent);
		
		invokeCallback(event);
		
		return event;
	}

	final void invokeCallback(final BondEvent event)
	{		
		if( m_listener != null )
		{
			m_listener.onEvent(event);
		}
		
		if( m_device.getManager().m_defaultBondListener != null )
		{
			m_device.getManager().m_defaultBondListener.onEvent(event);
		}
	}

	final Object[] getNativeBondingStateOverrides()
	{
		// Cut the 3 calls to jni layer down to one for efficiency (getting the native bond state calls a jni function)
		int bondState = m_device.m_nativeWrapper.getNativeBondState();
		return new Object[]{BONDING, m_device.m_nativeWrapper.isNativelyBonding(bondState), BONDED, m_device.m_nativeWrapper.isNativelyBonded(bondState), UNBONDED, m_device.m_nativeWrapper.isNativelyUnbonded(bondState)};
	}
	
	boolean isNativelyBondingOrBonded()
	{
		//--- DRK > These asserts are here because, as far as I could discern from logs, the abstracted
		//---		state for bonding/bonded was true, but when we did an encrypted write, it kicked
		//---		off a bonding operation, implying that natively the bonding state silently changed
		//---		since we discovered the device. I really don't know.
		//---		UPDATE: Nevermind, the reason bonding wasn't happening after connection was because
		//---				it was using the default config option of false. Leaving asserts here anywway
		//---				cause they can't hurt.
		//---		UPDATE AGAIN: Actually, these asserts can hit if you're connected to a device, you go
		//---		into OS settings, unbond, which kicks off an implicit disconnect which then kicks off
		//---		an implicit reconnect...race condition makes it so that you can query the bond state
		//---		and get its updated value before the bond state callback gets sent
		//---		UPDATE AGAIN AGAIN: Nevermind, it seems getBondState *can* actually lie, so original comment sorta stands...wow.
//		m_mngr.ASSERT(m_stateTracker.checkBitMatch(BONDED, isNativelyBonded()));
//		m_mngr.ASSERT(m_stateTracker.checkBitMatch(BONDING, isNativelyBonding()));
		int bondState = m_device.m_nativeWrapper.getNativeBondState();
		
		return bondState == BluetoothDevice.BOND_BONDED || bondState == BluetoothDevice.BOND_BONDING;
	}
}
