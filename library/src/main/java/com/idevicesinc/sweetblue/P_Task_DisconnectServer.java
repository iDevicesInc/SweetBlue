package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;

final class P_Task_DisconnectServer extends PA_Task_ConnectOrDisconnectServer
{
	public P_Task_DisconnectServer(final BleServer server, final BluetoothDevice nativeDevice, final I_StateListener listener, final boolean explicit, PE_TaskPriority priority)
	{
		super( server, nativeDevice, listener, explicit, priority );
	}

	@Override void execute()
	{
		final P_NativeServerLayer server_native = getServer().getNativeLayer();

		if( server_native.isServerNull() )
		{
			failImmediately();

			getManager().ASSERT(false, "Tried to disconnect client from server but native server is null.");
		}
		else
		{
			if( getServer().m_nativeWrapper.isDisconnected(m_nativeDevice.getAddress()) )
			{
				redundant();
			}
			else if( getServer().m_nativeWrapper.isConnecting(m_nativeDevice.getAddress()) )
			{
				failImmediately();

				getManager().ASSERT(false, "Server is currently connecting a client when we're trying to disconnect.");
			}
			else if( getServer().m_nativeWrapper.isDisconnecting(m_nativeDevice.getAddress()) )
			{
				//--- DRK > We don't fail out, but this is a good sign that something's amiss upstream.
				getManager().ASSERT(false, "Server is already disconnecting from the given client.");
			}
			else if( getServer().m_nativeWrapper.isConnected(m_nativeDevice.getAddress()) )
			{
				server_native.cancelConnection(m_nativeDevice);

				// SUCCESS!
			}
			else
			{
				failImmediately();

				getManager().ASSERT(false, "Native server state didn't match any expected values.");
			}
		}
	}

	public void onNativeSuccess(int gattStatus)
	{
		m_gattStatus = gattStatus;

		succeed();
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
