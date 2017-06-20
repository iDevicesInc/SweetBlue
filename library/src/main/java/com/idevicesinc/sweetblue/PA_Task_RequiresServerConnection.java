package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattServer;

abstract class PA_Task_RequiresServerConnection extends PA_Task_RequiresBleOn
{
	final String m_macAddress;

	public PA_Task_RequiresServerConnection(BleServer server, final String macAddress)
	{
		super(server, null);

		m_macAddress = macAddress;
	}

	protected final BleServer.OutgoingListener.Status getCancelStatusType()
	{
		BleManager mngr = this.getManager();

		if( mngr.isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF) )
		{
			return BleServer.OutgoingListener.Status.CANCELLED_FROM_BLE_TURNING_OFF;
		}
		else
		{
			return BleServer.OutgoingListener.Status.CANCELLED_FROM_DISCONNECT;
		}
	}
	
	@Override protected boolean isExecutable()
	{
		boolean shouldBeExecutable = super.isExecutable() && getServer().m_nativeWrapper.getNativeState(m_macAddress) == BluetoothGattServer.STATE_CONNECTED;
		
		if( shouldBeExecutable )
		{
			return true;
		}
		
		return false;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_DisconnectServer.class && this.getServer().equals(task.getServer()) )
		{
			final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

			if( task_cast.m_nativeDevice != null && task_cast.m_nativeDevice.getAddress().equals(m_macAddress) )
			{
				if( task_cast.getOrdinal() > this.getOrdinal() )
				{
					return true;
				}
			}
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
		if( task.getClass() == P_Task_DisconnectServer.class && this.getServer().equals(task.getServer()) )
		{
			final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

			if( task_cast.m_nativeDevice != null && task_cast.m_nativeDevice.getAddress().equals(m_macAddress) )
			{
				if( !task_cast.isExplicit() )
				{
					//--- DRK > Not sure why the "not is connected" qualifier was there..
					//---		MAYBE something to do with onNativeDisconnect doing soft cancellation after state change.
					if( getState() == PE_TaskState.EXECUTING )//&& !getDevice().is(BleDeviceState.CONNECTED) )
					{
						softlyCancel();
					}
				}
			}
		}
	}
}
