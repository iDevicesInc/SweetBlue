package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;

class P_Task_DiscoverServices extends PA_Task_RequiresConnection
{
	private int m_gattStatus = BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE;
	
	public P_Task_DiscoverServices(BleDevice bleDevice, I_StateListener listener)
	{
		super(bleDevice, listener);
	}

	@Override public void execute()
	{
//		getDevice().getNativeGatt().refresh();
		
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
	
	public void onNativeFail(int gattStatus)
	{
		m_gattStatus = gattStatus;
		
		this.fail();
	}
	
	public int getGattStatus()
	{
		return m_gattStatus;
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.DISCOVER_SERVICES;
	}
}
