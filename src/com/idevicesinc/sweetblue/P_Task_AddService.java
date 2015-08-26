package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.Utils;

class P_Task_AddService extends PA_Task_RequiresBleOn implements PA_Task.I_StateListener
{
	private final BluetoothGattService m_service;
	private BleServer.ServiceAddListener.Status m_status = BleServer.ServiceAddListener.Status.NULL;
	private final BleServer.ServiceAddListener m_addListener;

	public P_Task_AddService(BleServer server, final BluetoothGattService service, final BleServer.ServiceAddListener addListener)
	{
		super(server, null);

		m_service = service;
		m_addListener = addListener;
	}

	@Override void execute()
	{
		final BluetoothGattServer server_native_nullable = getServer().getNative();

		if( server_native_nullable == null )
		{
			if( !getServer().m_nativeWrapper.openServer() )
			{
				m_status = BleServer.ServiceAddListener.Status.SERVER_OPENING_FAILED;

				failImmediately();

				return;
			}
		}

		final BluetoothGattServer server_native = getServer().getNative();

		if( server_native /*still*/ == null )
		{
			m_status = BleServer.ServiceAddListener.Status.SERVER_OPENING_FAILED;

			failImmediately();

			getManager().ASSERT(false, "Server should not be null after successfully opening!");
		}
		else
		{
			if( server_native.addService(m_service) )
			{
				// SUCCESS, so far...
			}
			else
			{
				m_status = BleServer.ServiceAddListener.Status.FAILED_IMMEDIATELY;

				failImmediately();
			}
		}
	}

	public BluetoothGattService getService()
	{
		return m_service;
	}

	private void fail(final BleServer.ServiceAddListener.Status status, final int gattStatus)
	{
		super.fail();

		final BleServer.ServiceAddListener.ServiceAddEvent e = new BleServer.ServiceAddListener.ServiceAddEvent
		(
			getServer(), m_service, status, gattStatus
		);

		getServer().m_serviceMngr.invokeListeners(e, m_addListener);
	}

	protected void succeed(final int gattStatus)
	{
		super.succeed();

		final BleServer.ServiceAddListener.ServiceAddEvent e = new BleServer.ServiceAddListener.ServiceAddEvent
		(
				getServer(), m_service, BleServer.ServiceAddListener.Status.SUCCESS, BleStatuses.GATT_SUCCESS
		);

		getServer().m_serviceMngr.invokeListeners(e, m_addListener);
	}

	public void onServiceAdded(final int gattStatus, final BluetoothGattService service)
	{
		if( Utils.isSuccess(gattStatus) )
		{
			succeed();
		}
		else
		{
			fail(BleServer.ServiceAddListener.Status.FAILED_EVENTUALLY, gattStatus);
		}
	}

	protected final BleServer.ServiceAddListener.Status getCancelStatusType()
	{
		BleManager mngr = this.getManager();

		if( mngr.is(BleManagerState.TURNING_OFF) )
		{
			return BleServer.ServiceAddListener.Status.CANCELLED_FROM_BLE_TURNING_OFF;
		}
		else
		{
			return BleServer.ServiceAddListener.Status.CANCELLED_FROM_DISCONNECT;
		}
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
		if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			fail(getCancelStatusType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else if( state == PE_TaskState.TIMED_OUT )
		{
			fail(BleServer.ServiceAddListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
	}
}
