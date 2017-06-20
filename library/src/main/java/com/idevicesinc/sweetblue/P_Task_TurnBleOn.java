package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;

import com.idevicesinc.sweetblue.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 
 * 
 *
 */
final class P_Task_TurnBleOn extends PA_Task
{
	private final boolean m_implicit;
	
	public P_Task_TurnBleOn(BleManager manager, boolean implicit)
	{
		this(manager, implicit, null);
	}
	
	public P_Task_TurnBleOn(BleManager manager, boolean implicit, I_StateListener listener)
	{
		super(manager, listener);
		
		m_implicit = implicit;
	}
	
	public boolean isImplicit()
	{
		return m_implicit;
	}
	
	@Override public boolean isExplicit()
	{
		return !m_implicit;
	}

	@Override public void execute()
	{
		if( getManager().managerLayer().getState() == BluetoothAdapter.STATE_ON )
		{
			redundant();
		}
		else if( getManager().managerLayer().getState() == BluetoothAdapter.STATE_TURNING_ON )
		{
			// DRK > Nothing to do, already turning on.
		}
		else
		{
			if( m_implicit )
			{
				fail();
			}
			else if( false == getManager().managerLayer().enable() )
			{
				fail();
			}
			else
			{
				// SUCCESS, so far...
			}
		}
	}

	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.CRITICAL;
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.TURN_BLE_ON;
	}
}
