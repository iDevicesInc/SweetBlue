package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattService;

class P_Task_AddService extends PA_Task_RequiresBleOn implements PA_Task.I_StateListener
{
	private final BluetoothGattService m_service;

	public P_Task_AddService(BleServer server, final BluetoothGattService service)
	{
		super(server, null);

		m_service = service;
	}

	@Override void execute()
	{

	}

	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.ADD_SERVICE;
	}


	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{

	}
}
