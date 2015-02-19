package com.idevicesinc.sweetblue;

/**
 * 
 * 
 *
 */
abstract class PA_Task_RequiresConnection extends PA_Task_RequiresBleOn
{
	public PA_Task_RequiresConnection(BleDevice device, I_StateListener listener)
	{
		super(device, listener);
	}
	
	@Override protected boolean isExecutable()
	{
		boolean shouldBeExecutable = super.isExecutable() && getDevice().m_nativeWrapper.isNativelyConnected(); 
		
		if( shouldBeExecutable )
		{
			if( getDevice().getNativeGatt() == null )
			{
				m_logger.e("Device says we're natively connected but gatt==null");
				getManager().ASSERT(false);
				
				return false;
			}
			else
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_Disconnect.class && this.getDevice().equals(task.getDevice()) )
		{
			return true;
		}
		
		return super.isSoftlyCancellableBy(task);
	}
	
	@Override protected void attemptToSoftlyCancel(PA_Task task)
	{
		super.attemptToSoftlyCancel(task);
		
		//--- DRK > The following logic became necessary due to the following situation:
		//---		* device connected successfully.
		//---		* getting service task started execution, sent out get services call.
		//---		* something related to the get services call (probably, gatt status code 142/0x8E) made us disconnect, resulting in connection fail callback
		//---		* we get no error callback for getting services, thus...
		//---		* getting services task was still executing until it timed out, prompting another connection fail callback even though we already failed from the root cause.
		//---		NOTE that this was only directly observed for discovering services, but who knows, maybe it can happen for reads/writes/etc. as well. Normally, I'm pretty sure,
		//---		reads/writes fail first then you get the disconnect callback.
		if( task instanceof P_Task_Disconnect )
		{
			P_Task_Disconnect task_cast = (P_Task_Disconnect) task;
			
			if( !task_cast.isExplicit() )
			{
				if( getState() == PE_TaskState.EXECUTING && !getDevice().is(BleDeviceState.CONNECTED) )
				{
					softlyCancel();
				}
			}
		}
	}
}
