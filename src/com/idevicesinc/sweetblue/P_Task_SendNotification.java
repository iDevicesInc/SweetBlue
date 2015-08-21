package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.PA_Task.I_StateListener;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Utils;

import java.util.UUID;

class P_Task_SendNotification extends PA_Task_RequiresServerConnection implements I_StateListener
{
	private final BleServer m_server;
	private final BluetoothDevice m_nativeDevice;

	private final BleServer.OutgoingListener m_responseListener;
	private final FutureData m_futureData;

	private final UUID m_charUuid;
	private final UUID m_serviceUuid;

	private final boolean m_confirm;

	private byte[] m_data_sent = null;

	public P_Task_SendNotification(BleServer server, BluetoothDevice device, final UUID serviceUuid, final UUID charUuid, final FutureData futureData, boolean confirm, final BleServer.OutgoingListener responseListener)
	{
		super(server, device.getAddress());

		m_server = server;
		m_nativeDevice = device;
		m_futureData = futureData;
		m_responseListener = responseListener;
		m_charUuid = charUuid;
		m_serviceUuid = serviceUuid;
		m_confirm = confirm;
	}

	private byte[] data_sent()
	{
		if( m_data_sent == null )
		{
			m_data_sent = m_futureData.getData();
		}

		return m_data_sent;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.SEND_NOTIFICATION;
	}

	@Override void execute()
	{
		final BluetoothGattCharacteristic characteristic = getServer().getNativeCharacteristic(m_serviceUuid, m_charUuid);

		if( characteristic == null )
		{
			fail(BleServer.OutgoingListener.Status.NO_MATCHING_TARGET);
		}
		else
		{
			if( !characteristic.setValue(data_sent()) )
			{
				fail(BleServer.OutgoingListener.Status.FAILED_TO_SET_VALUE_ON_TARGET);
			}
			else
			{
				if( !getServer().getNative().notifyCharacteristicChanged(m_nativeDevice, characteristic, m_confirm) )
				{
					fail(BleServer.OutgoingListener.Status.FAILED_TO_SEND_OUT);
				}
				else
				{
					// SUCCESS, at least so far...we will see
				}
			}
		}
	}

	private BleServer.ExchangeListener.Type getType()
	{
		return m_confirm ? BleServer.ExchangeListener.Type.INDICATION : BleServer.ExchangeListener.Type.NOTIFICATION;
	}

	private void fail(final BleServer.OutgoingListener.Status status)
	{
		final BleServer.OutgoingListener.OutgoingEvent e = new BleServer.OutgoingListener.OutgoingEvent
		(
			getServer(), m_nativeDevice, m_serviceUuid, m_charUuid, BleServer.ExchangeListener.ExchangeEvent.NON_APPLICABLE_UUID, getType(),
			BleServer.ExchangeListener.Target.CHARACTERISTIC, BleServer.EMPTY_BYTE_ARRAY, data_sent(), BleServer.ExchangeListener.ExchangeEvent.NON_APPLICABLE_REQUEST_ID,
			/*offset=*/0, /*responseNeeded=*/false, status
		);

		getServer().invokeOutgoingListeners(e, m_responseListener);

		super.fail();
	}

	@Override protected void succeed()
	{
		final BleServer.OutgoingListener.OutgoingEvent e = new BleServer.OutgoingListener.OutgoingEvent
		(
			getServer(), m_nativeDevice, m_serviceUuid, m_charUuid, BleServer.ExchangeListener.ExchangeEvent.NON_APPLICABLE_UUID, getType(),
			BleServer.ExchangeListener.Target.CHARACTERISTIC, BleServer.EMPTY_BYTE_ARRAY, data_sent(), BleServer.ExchangeListener.ExchangeEvent.NON_APPLICABLE_REQUEST_ID,
			/*offset=*/0, /*responseNeeded=*/false, BleServer.OutgoingListener.Status.SUCCESS
		);

		getServer().invokeOutgoingListeners(e, m_responseListener);

		super.succeed();
	}

	void onNotificationSent(final BluetoothDevice device, final int gattStatus)
	{
		if( Utils.isSuccess(gattStatus) )
		{
			succeed();
		}
		else
		{
			fail(BleServer.OutgoingListener.Status.REMOTE_GATT_FAILURE);
		}
	}

	public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_NORMAL_READS_WRITES;
	}

	@Override public void onStateChange( PA_Task task, PE_TaskState state )
	{
		if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			fail(getCancelStatusType());
		}
	}
}
