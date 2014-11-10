package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter.LeScanCallback;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_Task_TurnBleOff extends PA_Task
{
	private final boolean m_implicit;
	
	public P_Task_TurnBleOff(BleManager manager, boolean implicit)
	{
		this(manager, implicit, null);
	}
	
	public P_Task_TurnBleOff(BleManager manager, boolean implicit, I_StateListener listener)
	{
		super(manager, listener);
		
		m_implicit = implicit;
	}
	
	@Override public void execute()
	{
		if( getManager().getNative().getAdapter().getState() == BluetoothAdapter.STATE_OFF )
		{
			this.redundant();
			
			return;
		}
		
		if( getManager().getNative().getAdapter().getState() == BluetoothAdapter.STATE_TURNING_OFF )
		{
			return;
		}
		
		if( m_implicit )
		{
			this.fail();
		}
		else if( !getManager().getNative().getAdapter().disable() )
		{
			this.fail();
			
			return;
		}
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.CRITICAL;
	}
}
