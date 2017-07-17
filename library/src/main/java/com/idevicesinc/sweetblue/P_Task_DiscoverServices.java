package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Interval;


final class P_Task_DiscoverServices extends PA_Task_RequiresConnection
{
	private int m_gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	private boolean m_gattRefresh;
	private double m_curDiscoverDelay;
	private double m_discoverDelayTarget;
	private boolean m_discoverAttempted;

	
	public P_Task_DiscoverServices(BleDevice bleDevice, I_StateListener listener, boolean gattRefresh, Interval discoverDelay)
	{
		super(bleDevice, listener);
		m_gattRefresh = gattRefresh;
		m_discoverDelayTarget = Interval.isDisabled(discoverDelay) || discoverDelay == Interval.INFINITE ? 0.0 : discoverDelay.secs();
	}

	@Override public void execute()
	{
		if( m_gattRefresh )
		{
			getDevice().layerManager().refreshGatt();
			return;
		}

		if (m_discoverDelayTarget == 0.0) {
			m_discoverAttempted = true;
			if (!getDevice().layerManager().discoverServices() )
			{
				failImmediately();

				getManager().uhOh(UhOh.SERVICE_DISCOVERY_IMMEDIATELY_FAILED);
			}
		}
	}

	@Override protected void update(double timeStep)
	{
		if ((m_gattRefresh || m_discoverDelayTarget > 0.0) && !m_discoverAttempted)
		{
			m_curDiscoverDelay += timeStep;
			if (m_curDiscoverDelay >= m_discoverDelayTarget)
			{
				m_discoverAttempted = true;
				if( !getDevice().layerManager().discoverServices() )
				{
					failImmediately();

					getManager().uhOh(UhOh.SERVICE_DISCOVERY_IMMEDIATELY_FAILED);
				}
			}
		}
	}

	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.MEDIUM;
	}
	
	public void onNativeFail(int gattStatus)
	{
		m_gattStatus = gattStatus;

//		if (getDevice().is(BleDeviceState.CONNECTED))
//		{
//			getDevice().disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.EVENTUALLY, gattStatus, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, BleDevice.ReadWriteListener.ReadWriteEvent.NULL(getDevice()));
//		}
		
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
