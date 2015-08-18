package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;

class P_Task_ConnectServer extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;
	final BluetoothDevice m_nativeDevice;

	private int m_gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

	public P_Task_ConnectServer(BleServer server, BluetoothDevice nativeDevice, I_StateListener listener, boolean explicit, PE_TaskPriority priority)
	{
		super(server, listener);

		m_nativeDevice = nativeDevice;
		m_explicit = explicit;
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
	}

	@Override public void execute()
	{
		final BluetoothGattServer server_native_nullable = getServer().getNative();

		if( server_native_nullable == null )
		{
			if( !getServer().m_nativeWrapper.openServer() )
			{
				failImmediately();

				return;
			}
		}

		final BluetoothGattServer server_native = getServer().getNative();

		if( server_native /*still*/ == null )
		{
			getManager().ASSERT(false, "Server should not be null after successfully opening!");

			failImmediately();
		}
		else
		{
			if( getServer().m_nativeWrapper.isDisconnected(m_nativeDevice.getAddress()) )
			{
				if( !server_native.connect(m_nativeDevice, false) )
				{
					failImmediately();
				}
				else
				{
					// SUCCESS! At least, we will wait and see.
				}
			}
			else
			{
				if( getServer().m_nativeWrapper.isDisconnecting(m_nativeDevice.getAddress()) )
				{
					getManager().ASSERT(false, "Server is currently disconnecting a client when we're trying to connect.");

					failImmediately();
				}
				else if( getServer().m_nativeWrapper.isConnecting(m_nativeDevice.getAddress()) )
				{
					//--- DRK > We don't fail out, but this is a good sign that something's amiss upstream.
					getManager().ASSERT(false, "Server is already connecting to the given client.");
				}
				else if( getServer().m_nativeWrapper.isConnected(m_nativeDevice.getAddress()) )
				{
					redundant();
				}
			}
		}
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

	public void onNativeFail(final int gattStatus)
	{
		m_gattStatus = gattStatus;

		fail();
	}

	public boolean isFor(final String macAddress)
	{
		return macAddress.equals(m_nativeDevice.getAddress());
	}

	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
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
