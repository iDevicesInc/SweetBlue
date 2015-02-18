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
				if( state == PE_TaskState.SUCCEEDED )
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
}
