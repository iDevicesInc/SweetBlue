package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_Task_AddService extends PA_Task_RequiresBleOn implements PA_Task.I_StateListener
{
	private final BluetoothGattService m_service;
	private final BleServer.ServiceAddListener m_addListener;

	private boolean m_cancelledInTheMiddleOfExecuting = false;


	public P_Task_AddService(BleServer server, final BluetoothGattService service, final BleServer.ServiceAddListener addListener)
	{
		super(server, null);

		m_service = service;
		m_addListener = addListener;
	}

	@Override void execute()
	{
		final P_NativeServerLayer server_native_nullable = getServer().getNativeLayer();

		if( server_native_nullable.isServerNull() )
		{
			if( !getServer().m_nativeWrapper.openServer() )
			{
				failImmediately(BleServer.ServiceAddListener.Status.SERVER_OPENING_FAILED);

				return;
			}
		}

		final P_NativeServerLayer server_native = getServer().getNativeLayer();

		if( server_native.isServerNull() )
		{
			failImmediately(BleServer.ServiceAddListener.Status.SERVER_OPENING_FAILED);

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
				failImmediately(BleServer.ServiceAddListener.Status.FAILED_IMMEDIATELY);
			}
		}
	}

	@Override protected void onNotExecutable()
	{
		getManager().ASSERT(false, "Should never have gotten into the queue, and if BLE goes OFF in the mean time should be removed from queue.");

		super.onNotExecutable();

		invokeFailCallback(BleServer.ServiceAddListener.Status.BLE_NOT_ON, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	public BluetoothGattService getService()
	{
		return m_service;
	}

	private void fail(final BleServer.ServiceAddListener.Status status, final int gattStatus)
	{
		super.fail();

		invokeFailCallback(status, gattStatus);
	}

	private void failImmediately(final BleServer.ServiceAddListener.Status status)
	{
		super.failImmediately();

		invokeFailCallback(status, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	private void invokeFailCallback(final BleServer.ServiceAddListener.Status status, final int gattStatus)
	{
		final BleServer.ServiceAddListener.ServiceAddEvent e = new BleServer.ServiceAddListener.ServiceAddEvent
		(
			getServer(), m_service, status, gattStatus, /*solicited=*/true
		);

		getServer().serviceMngr_server().invokeListeners(e, m_addListener);
	}

	protected void succeed(final int gattStatus)
	{
		super.succeed();

		final BleServer.ServiceAddListener.ServiceAddEvent e = new BleServer.ServiceAddListener.ServiceAddEvent
		(
			getServer(), m_service, BleServer.ServiceAddListener.Status.SUCCESS, BleStatuses.GATT_SUCCESS, /*solicited=*/true
		);

		getServer().serviceMngr_server().invokeListeners(e, m_addListener);
	}

	public boolean cancelledInTheMiddleOfExecuting()
	{
		return m_cancelledInTheMiddleOfExecuting;
	}

	public void onServiceAdded(final int gattStatus, final BluetoothGattService service)
	{
		if( m_cancelledInTheMiddleOfExecuting )
		{
			final P_NativeServerLayer server_native_nullable = getServer().getNativeLayer();

			if( !server_native_nullable.isServerNull() )
			{
				server_native_nullable.removeService(service);
			}

			//--- DRK > Not invoking appland callback because it was already send in call to cancel() back in time.
			fail();
		}
		else
		{
			if( Utils.isSuccess(gattStatus) )
			{
				succeed(gattStatus);
			}
			else
			{
				fail(BleServer.ServiceAddListener.Status.FAILED_EVENTUALLY, gattStatus);
			}
		}
	}

	public void cancel(final BleServer.ServiceAddListener.Status status)
	{
		if( this.getState() == PE_TaskState.ARMED )
		{
			fail();
		}
		else if( this.getState() == PE_TaskState.EXECUTING )
		{
			//--- DRK > We don't actually fail the task here because we let it run
			//--- 		its course until we get a callback from the native stack.
			m_cancelledInTheMiddleOfExecuting = true;
		}
		else
		{
			clearFromQueue();
		}

		invokeFailCallback(status, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	protected final BleServer.ServiceAddListener.Status getCancelStatusType()
	{
		BleManager mngr = this.getManager();

		if( mngr.isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF) )
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
			invokeFailCallback(getCancelStatusType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else if( state == PE_TaskState.TIMED_OUT )
		{
			invokeFailCallback(BleServer.ServiceAddListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
	}
}
