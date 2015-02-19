package com.idevicesinc.sweetblue;

/**
 * 
 * 
 *
 */
abstract class PA_Task_RequiresBleOn extends PA_Task
{
	public PA_Task_RequiresBleOn(BleServer server, I_StateListener listener)
	{
		super(server, listener);
	}
	
	public PA_Task_RequiresBleOn(BleManager manager, I_StateListener listener)
	{
		super(manager, listener);
	}
	
	public PA_Task_RequiresBleOn(BleDevice device, I_StateListener listener)
	{
		super(device, listener);
	}

	@Override protected boolean isExecutable()
	{
		return super.isExecutable() && BleManagerState.ON.overlaps(getManager().getNativeStateMask());
	}
	
	@Override public boolean isCancellableBy(PA_Task task)
	{
		if( task instanceof P_Task_TurnBleOff )
		{
			return true;
		}
		
		return super.isCancellableBy(task);
	}
}
