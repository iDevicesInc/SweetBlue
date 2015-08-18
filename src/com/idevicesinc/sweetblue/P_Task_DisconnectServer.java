package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;

class P_Task_DisconnectServer extends PA_Task_RequiresBleOn
{
	final BluetoothDevice m_nativeDevice;
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;

	private int m_gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

	public P_Task_DisconnectServer(final BleServer server, final BluetoothDevice nativeDevice, final I_StateListener listener, final boolean explicit, PE_TaskPriority priority)
	{
		super( server, listener );

		m_nativeDevice = nativeDevice;
		m_priority = priority;
		m_explicit = explicit;
	}

	@Override void execute()
	{

	}

	public int getGattStatus()
	{
		return m_gattStatus;
	}

	@Override public boolean isExplicit()
	{
		return m_explicit;
	}

	public void onNativeSuccess(int gattStatus)
	{
		m_gattStatus = gattStatus;

		succeed();
	}

	public boolean isFor(final String macAddress)
	{
		return macAddress.equals(m_nativeDevice.getAddress());
	}

	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.DISCONNECT_SERVER;
	}

	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_ConnectServer.class && this.getServer().equals(task.getServer()) )
		{
			final P_Task_ConnectServer task_cast = (P_Task_ConnectServer) task;

			if( task_cast.m_nativeDevice.getAddress().equals(m_nativeDevice.getAddress()) )
			{
				//if( isCancellable() )
				{
					return true;
				}
			}
		}

		return super.isSoftlyCancellableBy(task);
	}
}
