package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.BONDED;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDING;
import static com.idevicesinc.sweetblue.BleDeviceState.UNBONDED;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDeviceConfig.BondFilter;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;

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
	
	void onBondTaskStateChange(PA_Task task, PE_TaskState state)
	{
		E_Intent intent = task.isExplicit() ? E_Intent.EXPLICIT : E_Intent.IMPLICIT;
		
		if( task.getClass() == P_Task_Bond.class )
		{
			if( state.isEndingState() )
			{
				if( state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT )
				{
					this.onNativeBond(intent);
				}
				else
				{
					this.onNativeBondFailed(intent);
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
	
	void onNativeUnbond(E_Intent intent)
	{
		m_device.getStateTracker().update(intent, BONDED, false, BONDING, false, UNBONDED, true);
	}
	
	void onNativeBonding(E_Intent intent)
	{
		m_device.getStateTracker().update(intent, BONDED, false, BONDING, true, UNBONDED, false);
	}
	
	void onNativeBond(E_Intent intent)
	{
		m_device.getStateTracker().update(intent, BONDED, true, BONDING, false, UNBONDED, false);
	}
	
	void onNativeBondFailed(E_Intent intent)
	{
		m_device.getStateTracker().update(intent, BONDED, false, BONDING, false, UNBONDED, true);
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
