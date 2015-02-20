package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.BONDED;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDING;
import static com.idevicesinc.sweetblue.BleDeviceState.UNBONDED;

import com.idevicesinc.sweetblue.BleDevice.BondListener.BondEvent;
import com.idevicesinc.sweetblue.BleDevice.BondListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.BondListener.Status;
import com.idevicesinc.sweetblue.BleDeviceConfig.BondFilter;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.State;

class P_BondManager
{
	private final BleDevice m_device;
	
	private BleDevice.BondListener m_listener;
	
	P_BondManager(BleDevice device)
	{
		m_device = device;
	}
	
	public void setListener(BleDevice.BondListener listener_nullable)
	{
		synchronized (m_device.m_threadLock)
		{
			if( listener_nullable != null )
			{
				m_listener = new P_WrappingBondListener(listener_nullable, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
			}
			else
			{
				m_listener = null;
			}
		}
	}
	
	void onBondTaskStateChange(final PA_Task task, final PE_TaskState state)
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
					
					this.onNativeBondFailed(intent, status, failReason);
				}
			}
		}
		else if( task.getClass() == P_Task_Unbond.class )
		{
			if( state == PE_TaskState.SUCCEEDED )
			{
				this.onNativeUnbond(intent);
			}
			else
			{
				// not sure what to do here, if anything
			}
		}
	}
	
	void onNativeUnbond(final E_Intent intent)
	{
		m_device.getStateTracker().update(intent, BONDED, false, BONDING, false, UNBONDED, true);
	}
	
	void onNativeBonding(final E_Intent intent)
	{
		m_device.getStateTracker().update(intent, BONDED, false, BONDING, true, UNBONDED, false);
	}
	
	void onNativeBond(final E_Intent intent)
	{
		final boolean wasAlreadyBonded = m_device.is(BONDED);
		
		m_device.getStateTracker().update(intent, BONDED, true, BONDING, false, UNBONDED, false);
		
		if( !wasAlreadyBonded )
		{
			invokeCallback(Status.SUCCESS, BleDeviceConfig.BOND_FAIL_REASON_NOT_APPLICABLE, intent.convert());
		}
	}
	
	void onNativeBondFailed(final E_Intent intent, final BondListener.Status status, final int failReason)
	{
		m_device.getStateTracker().update(intent, BONDED, false, BONDING, false, UNBONDED, true);
		
		invokeCallback(status, failReason, intent.convert());
	}
	
	boolean bondIfNeeded(final P_Characteristic characteristic, final BondFilter.CharacteristicEventType type)
	{
		final BleDeviceConfig.BondFilter bondFilter = m_device.conf_device().bondFilter != null ? m_device.conf_device().bondFilter : m_device.conf_mngr().bondFilter;
		
		if( bondFilter == null )  return false;
		
		final BondFilter.CharacteristicEvent event = new BleDeviceConfig.BondFilter.CharacteristicEvent(m_device, characteristic.getUuid(), type);
		
		final BondFilter.Please please = bondFilter.onCharacteristicEvent(event);
		
		return applyPlease_BondFilter(please);
	}
	
	boolean applyPlease_BondFilter(BondFilter.Please please_nullable)
	{
		if( please_nullable == null )
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
			m_device.bond(please_nullable.listener());
		}
		else if( !bond )
		{
			m_device.unbond();
		}
		
		return bond;
	}
	
	void invokeCallback(Status status, int failReason, State.ChangeIntent intent)
	{
		if( m_listener == null )  return;
		
		final BondEvent event = new BondEvent(m_device, status, failReason, intent);
		m_listener.onBondEvent(event);
	}
	
	private boolean isBondingOrBonded()
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
		
		return m_device.m_nativeWrapper.isNativelyBonded() || m_device.m_nativeWrapper.isNativelyBonding();
	}
}
