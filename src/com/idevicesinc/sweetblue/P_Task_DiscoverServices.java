package com.idevicesinc.sweetblue;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_Task_DiscoverServices extends PA_Task_RequiresConnection
{
	public P_Task_DiscoverServices(BleDevice bleDevice, I_StateListener listener)
	{
		super(bleDevice, listener);
	}

	@Override public void execute()
	{
		if( !getDevice().getNativeGatt().discoverServices() )
		{
			fail();
			
			getManager().uhOh(UhOh.SERVICE_DISCOVERY_IMMEDIATELY_FAILED);
		}
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.MEDIUM;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_Disconnect.class && this.getDevice().equals(task.getDevice()) )
		{
			return true;
		}
		
		return super.isSoftlyCancellableBy(task);
	}
}
