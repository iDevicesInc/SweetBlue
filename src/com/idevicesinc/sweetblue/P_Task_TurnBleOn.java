package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_Task_TurnBleOn extends PA_Task
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

	@Override public void execute()
	{
		if( getManager().getNative().getAdapter().getState() == BluetoothAdapter.STATE_ON )
		{
			this.redundant();
			
			return;
		}
		
		if( getManager().getNative().getAdapter().getState() == BluetoothAdapter.STATE_TURNING_ON )
		{
			return;
		}
		
		if( m_implicit )
		{
			this.fail();
		}
		else if( !getManager().getNative().getAdapter().enable() )
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
