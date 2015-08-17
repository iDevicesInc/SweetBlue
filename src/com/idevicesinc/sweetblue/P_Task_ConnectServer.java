package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;

class P_Task_ConnectServer extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;
	private final BluetoothDevice m_nativeDevice;

	public P_Task_ConnectServer(BleServer server, BluetoothDevice nativeDevice, I_StateListener listener, boolean explicit, PE_TaskPriority priority)
	{
		super(server, listener);

		m_nativeDevice = nativeDevice;
		m_explicit = explicit;
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
	}

	@Override public void execute()
	{

	}

	@Override public boolean isCancellableBy(PA_Task task)
	{
		if( task instanceof P_Task_DisconnectServer )
		{
			if( task.getClass() == P_Task_DisconnectServer.class && this.getServer().equals(task.getServer()) )
			{
				final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

				if( task_cast.m_macAddress.equals(m_nativeDevice.getAddress()) )
				{

					//--- DRK > If an implicit disconnect comes in we have no choice but to bail.
					//---		Otherwise we let the connection task run its course then we'll
					//---		disconnect afterwards all nice and orderly-like.
					if( !task_cast.isExplicit() )
					{
						return true;
					}
				}
			}
		}
		else if( task instanceof P_Task_TurnBleOff )
		{
			return true;
		}

		return super.isCancellableBy(task);
	}

	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_DisconnectServer.class && this.getServer().equals(task.getServer()) )
		{
			final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

			if( task_cast.m_macAddress.equals(m_nativeDevice.getAddress()) )
			{
				if( this.isExplicit() )
				{
					return true;
				}
			}
		}

		return super.isSoftlyCancellableBy(task);
	}

	@Override public boolean isExplicit()
	{
		return m_explicit;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.CONNECT_SERVER;
	}
}
