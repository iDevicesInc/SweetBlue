package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;


final class P_ReliableWriteManager
{
	private final BleDevice m_device;

	private BleDevice.ReadWriteListener m_listener;

	P_ReliableWriteManager(final BleDevice device)
	{
		m_device = device;
	}

	public void onDisconnect()
	{
		m_listener = null;
	}

	BleDevice.ReadWriteListener.ReadWriteEvent newEvent(final BleDevice.ReadWriteListener.Status status, final int gattStatus, final boolean solicited)
	{
		return new BleDevice.ReadWriteListener.ReadWriteEvent(m_device, Uuids.INVALID, Uuids.INVALID, Uuids.INVALID, null, BleDevice.ReadWriteListener.Type.WRITE, BleDevice.ReadWriteListener.Target.RELIABLE_WRITE, P_Const.EMPTY_BYTE_ARRAY, status, gattStatus, 0.0, 0.0, solicited);
	}

	private BleDevice.ReadWriteListener.ReadWriteEvent getGeneralEarlyOutEvent()
	{
		final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

		if( m_device.isNull() )
		{
			return newEvent(BleDevice.ReadWriteListener.Status.NULL_DEVICE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
		}
		else
		{
			if( false == m_device.is(BleDeviceState.CONNECTED) )
			{
				return newEvent(BleDevice.ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
			}
			else if( true == m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )
			{
				return newEvent(BleDevice.ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
			}
			else
			{
				return null;
			}
		}
	}

	private BleDevice.ReadWriteListener.ReadWriteEvent getNeverBeganEarlyOutEvent()
	{
		if( m_listener == null )
		{
			final BleDevice.ReadWriteListener.ReadWriteEvent e_earlyOut = getGeneralEarlyOutEvent();

			if( e_earlyOut != null )
			{
				return e_earlyOut;
			}
			else
			{
				final BleDevice.ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(BleDevice.ReadWriteListener.Status.RELIABLE_WRITE_NEVER_BEGAN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

				return e_earlyOut_specific;
			}
		}
		else
		{
			return null;
		}
	}

	public BleDevice.ReadWriteListener.ReadWriteEvent begin(final BleDevice.ReadWriteListener listener)
	{
		final BleDevice.ReadWriteListener.ReadWriteEvent e_earlyOut = getGeneralEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			if( m_listener != null )
			{
				final BleDevice.ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(BleDevice.ReadWriteListener.Status.RELIABLE_WRITE_ALREADY_BEGAN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

				m_device.invokeReadWriteCallback(listener, e_earlyOut_specific);

				return e_earlyOut_specific;
			}
			else
			{
				if( false == m_device.layerManager().getGattLayer().beginReliableWrite() )
				{
					final BleDevice.ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(BleDevice.ReadWriteListener.Status.RELIABLE_WRITE_FAILED_TO_BEGIN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

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

	public BleDevice.ReadWriteListener.ReadWriteEvent abort()
	{
		final BleDevice.ReadWriteListener.ReadWriteEvent e_earlyOut = getNeverBeganEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(m_listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			final BleDevice.ReadWriteListener listener = m_listener;
			m_listener = null;

			abortReliableWrite();

			final BleDevice.ReadWriteListener.ReadWriteEvent e = newEvent(BleDevice.ReadWriteListener.Status.RELIABLE_WRITE_ABORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

			m_device.invokeReadWriteCallback(listener, e);

			return e;
		}
	}

	private void abortReliableWrite()
	{
		m_device.layerManager().getGattLayer().abortReliableWrite(m_device.getNative());
	}

	public BleDevice.ReadWriteListener.ReadWriteEvent execute()
	{
		final BleDevice.ReadWriteListener.ReadWriteEvent e_earlyOut = getNeverBeganEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(m_listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			final BleDevice.ReadWriteListener listener = m_listener;
			m_listener = null;

			final P_Task_ExecuteReliableWrite task = new P_Task_ExecuteReliableWrite(m_device, listener, m_device.getOverrideReadWritePriority());

			m_device.getTaskQueue().add(task);

			return m_device.NULL_READWRITE_EVENT();
		}
	}

	public void onReliableWriteCompleted_unsolicited(final BluetoothGatt gatt, final int gattStatus)
	{
		final BleDevice.ReadWriteListener listener = m_listener;
		m_listener = null;

		final BleDevice.ReadWriteListener.Status status = Utils.isSuccess(gattStatus) ? BleDevice.ReadWriteListener.Status.SUCCESS : BleDevice.ReadWriteListener.Status.REMOTE_GATT_FAILURE;
		final BleDevice.ReadWriteListener.ReadWriteEvent e = newEvent(status, gattStatus, /*solicited=*/false);

		m_device.invokeReadWriteCallback(listener, e);
	}
}
