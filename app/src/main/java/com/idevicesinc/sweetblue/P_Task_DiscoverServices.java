package com.idevicesinc.sweetblue;

import java.lang.reflect.Method;

import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Utils;

class P_Task_DiscoverServices extends PA_Task_RequiresConnection
{
	private int m_gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	
	public P_Task_DiscoverServices(BleDevice bleDevice, I_StateListener listener)
	{
		super(bleDevice, listener);
	}

	@Override public void execute()
	{
//		if( !getDevice().getNativeGatt().getServices().isEmpty() )
		{
			final boolean useRefresh = BleDeviceConfig.bool(getDevice().conf_device().useGattRefresh, getDevice().conf_mngr().useGattRefresh);
			
			if( useRefresh )
			{
				getDevice().gattLayer().refreshGatt();
			}
		}
		
		if( !getDevice().gattLayer().discoverServices() )
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
