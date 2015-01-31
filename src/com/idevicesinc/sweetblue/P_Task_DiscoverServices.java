package com.idevicesinc.sweetblue;

/**
 * 
 * 
 *
 */
class P_Task_DiscoverServices extends PA_Task_RequiresConnection
{
	private int m_gattStatus = BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE;
	
	public P_Task_DiscoverServices(BleDevice bleDevice, double timeout, I_StateListener listener)
	{
		super(bleDevice, timeout, listener);
	}

	@Override public void execute()
	{
		if( !getDevice().getNativeGatt().discoverServices() )
		{
			failImmediately();
			
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
	
	public void onNativeFail(int gattStatus)
	{
		m_gattStatus = gattStatus;
		
		this.fail();
	}
	
	public int getGattStatus()
	{
		return m_gattStatus;
	}
}
