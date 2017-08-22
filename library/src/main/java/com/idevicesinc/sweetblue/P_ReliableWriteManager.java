package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;


final class P_ReliableWriteManager
{
	private final BleDevice m_device;

	private ReadWriteListener m_listener;

	P_ReliableWriteManager(final BleDevice device)
	{
		m_device = device;
	}

	public void onDisconnect()
	{
		m_listener = null;
	}

	ReadWriteListener.ReadWriteEvent newEvent(final ReadWriteListener.Status status, final int gattStatus, final boolean solicited)
	{
		return new ReadWriteListener.ReadWriteEvent(m_device, BleWrite.INVALID, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.RELIABLE_WRITE, status, gattStatus, 0.0, 0.0, solicited);
	}

	private ReadWriteListener.ReadWriteEvent getGeneralEarlyOutEvent()
	{
		final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

		if( m_device.isNull() )
		{
			return newEvent(ReadWriteListener.Status.NULL_DEVICE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
		}
		else
		{
			if( false == m_device.is(BleDeviceState.CONNECTED) )
			{
				return newEvent(ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
			}
			else if( true == m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )
			{
				return newEvent(ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
			}
			else
			{
				return null;
			}
		}
	}

	private ReadWriteListener.ReadWriteEvent getNeverBeganEarlyOutEvent()
	{
		if( m_listener == null )
		{
			final ReadWriteListener.ReadWriteEvent e_earlyOut = getGeneralEarlyOutEvent();

			if( e_earlyOut != null )
			{
				return e_earlyOut;
			}
			else
			{
				final ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(ReadWriteListener.Status.RELIABLE_WRITE_NEVER_BEGAN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

				return e_earlyOut_specific;
			}
		}
		else
		{
			return null;
		}
	}

	public ReadWriteListener.ReadWriteEvent begin(final ReadWriteListener listener)
	{
		final ReadWriteListener.ReadWriteEvent e_earlyOut = getGeneralEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			if( m_listener != null )
			{
				final ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(ReadWriteListener.Status.RELIABLE_WRITE_ALREADY_BEGAN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

				m_device.invokeReadWriteCallback(listener, e_earlyOut_specific);

				return e_earlyOut_specific;
			}
			else
			{
				if( false == m_device.layerManager().getGattLayer().beginReliableWrite() )
				{
					final ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(ReadWriteListener.Status.RELIABLE_WRITE_FAILED_TO_BEGIN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

					m_device.invokeReadWriteCallback(listener, e_earlyOut_specific);

					return e_earlyOut_specific;
				}
				else
				{
					m_listener = listener;

					return m_device.NULL_READWRITE_EVENT();
				}
			}
		}
	}

	public ReadWriteListener.ReadWriteEvent abort()
	{
		final ReadWriteListener.ReadWriteEvent e_earlyOut = getNeverBeganEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(m_listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			final ReadWriteListener listener = m_listener;
			m_listener = null;

			abortReliableWrite();

			final ReadWriteListener.ReadWriteEvent e = newEvent(ReadWriteListener.Status.RELIABLE_WRITE_ABORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

			m_device.invokeReadWriteCallback(listener, e);

			return e;
		}
	}

	private void abortReliableWrite()
	{
		m_device.layerManager().getGattLayer().abortReliableWrite(m_device.getNative());
	}

	public ReadWriteListener.ReadWriteEvent execute()
	{
		final ReadWriteListener.ReadWriteEvent e_earlyOut = getNeverBeganEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(m_listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			final ReadWriteListener listener = m_listener;
			m_listener = null;

			final P_Task_ExecuteReliableWrite task = new P_Task_ExecuteReliableWrite(m_device, listener, m_device.getOverrideReadWritePriority());

			m_device.getTaskQueue().add(task);

			return m_device.NULL_READWRITE_EVENT();
		}
	}

	public void onReliableWriteCompleted_unsolicited(final BluetoothGatt gatt, final int gattStatus)
	{
		final ReadWriteListener listener = m_listener;
		m_listener = null;

		final ReadWriteListener.Status status = Utils.isSuccess(gattStatus) ? ReadWriteListener.Status.SUCCESS : ReadWriteListener.Status.REMOTE_GATT_FAILURE;
		final ReadWriteListener.ReadWriteEvent e = newEvent(status, gattStatus, /*solicited=*/false);

		m_device.invokeReadWriteCallback(listener, e);
	}
}
