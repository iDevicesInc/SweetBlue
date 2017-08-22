package com.idevicesinc.sweetblue;

import java.util.UUID;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;


final class P_Task_ToggleNotify extends PA_Task_ReadOrWrite implements PA_Task.I_StateListener
{
	private static int Type_NOTIFY = 0;
	private static int Type_INDICATE = 1;
	
	private final boolean m_enable;

	private byte[] m_writeValue = null;


	public P_Task_ToggleNotify(BleDevice device, BleNotify notify, boolean enable, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, notify, false, txn, priority);

		m_enable = enable;
	}
	
	private byte[] getWriteValue()
	{
		return m_writeValue != null ? m_writeValue : P_Const.EMPTY_BYTE_ARRAY;
	}
	
	static byte[] getWriteValue(BluetoothGattCharacteristic char_native, boolean enable)
	{
		final int type;
		
		if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0x0 )
		{
			type = Type_NOTIFY;
		}
		else if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0x0 )
		{
			type = Type_INDICATE;
		}
		else
		{
			type = Type_NOTIFY;
		}
		
		final byte[] enableValue = type == Type_NOTIFY ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
		final byte[] disableValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

		return enable ? enableValue : disableValue;
	}

	@Override protected void executeReadOrWrite()
	{
		final BluetoothGattCharacteristic char_native = getFilteredCharacteristic() != null ? getFilteredCharacteristic() : getDevice().getNativeCharacteristic(getServiceUuid(), getCharUuid());

		if( char_native == null )
		{
			this.fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
		}
		else if( false == getDevice().layerManager().getGattLayer().setCharacteristicNotification(char_native, m_enable) )
		{
			this.fail(Status.FAILED_TO_TOGGLE_NOTIFICATION, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
		}
		else
		{
			final BluetoothGattDescriptor descriptor = char_native.getDescriptor(m_bleOp.descriptorUuid);

			if( descriptor == null )
			{
				//--- DRK > Previously we were failing the task if the descriptor came up null. It was assumed that writing the descriptor
				//---		was a requirement. It turns out that, at least sometimes, simply calling setCharacteristicNotification(true) is enough.
				succeed();

				// this.fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, m_uuid, m_descUuid);
			}
			else
			{
				if (!m_bleOp.isServiceUuidValid())
					m_bleOp.serviceUuid = char_native.getService().getUuid();

				m_writeValue = getWriteValue(char_native, m_enable);

				if( false == descriptor.setValue(getWriteValue()) )
				{
					this.fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getCharUuid(), m_bleOp.descriptorUuid);
				}
				else if( false == getDevice().layerManager().getGattLayer().writeDescriptor(descriptor) )
				{
					this.fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getCharUuid(), m_bleOp.descriptorUuid);
				}
				else
				{
					// SUCCESS, so far...
				}
			}
		}
	}

	@Override protected void fail(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid)
	{
		super.fail(status, gattStatus, target, charUuid, descUuid);

		if( m_enable )
		{
			getDevice().getPollManager().onNotifyStateChange(getServiceUuid(), charUuid, P_PollManager.E_NotifyState__NOT_ENABLED);
		}
	}
	
	@Override protected void succeed()
	{
//		getDevice().addWriteTime(result.totalTime);
		
		if( m_enable )
		{
			getDevice().getPollManager().onNotifyStateChange(getServiceUuid(), getCharUuid(), P_PollManager.E_NotifyState__ENABLED);
		}
		else
		{
			getDevice().getPollManager().onNotifyStateChange(getServiceUuid(), getCharUuid(), P_PollManager.E_NotifyState__NOT_ENABLED);
		}

		super.succeed();

		final ReadWriteEvent event = newReadWriteEvent(Status.SUCCESS, BluetoothGatt.GATT_SUCCESS, Target.DESCRIPTOR, m_bleOp);
		getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
	}

	public void onDescriptorWrite(BluetoothGatt gatt, UUID descUuid, int status)
	{
		getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));

		if( !descUuid.equals(m_bleOp.descriptorUuid) ) return;

		final boolean isConnected = getDevice().is_internal(BleDeviceState.CONNECTED);

		if( isConnected && Utils.isSuccess(status))
		{
			succeed();
		}
		else
		{
			if( !isConnected && Utils.isSuccess(status) )
			{
				//--- DRK > Trying to catch a case that I currently can't explain any other way.
				//--- DRK > UPDATE: Nevermind, must have been tired when I wrote this assert, device can be
				//---			explicitly disconnected while the notify enable write is out and this can get tripped.
//				getManager().ASSERT(false, "Successfully enabled notification but device isn't connected.");

				fail(Status.CANCELLED_FROM_DISCONNECT, status, Target.DESCRIPTOR, getCharUuid(), descUuid);
			}
			else
			{
				fail(Status.REMOTE_GATT_FAILURE, status, Target.DESCRIPTOR, getCharUuid(), descUuid);
			}
		}
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().charName(getCharUuid()) + " descriptor write timed out!");

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, m_bleOp);

			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final Target target = this.getState() == PE_TaskState.EXECUTING ? Target.DESCRIPTOR : Target.CHARACTERISTIC;
			final UUID descUuid = target == Target.DESCRIPTOR ? m_bleOp.descriptorUuid : ReadWriteEvent.NON_APPLICABLE_UUID;
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, target, m_bleOp);

			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
		}
	}
	
	@Override protected UUID getDescUuid()
	{
		return m_bleOp.descriptorUuid;
	}
	
	@Override public boolean isMoreImportantThan(PA_Task task)
	{
		return isMoreImportantThan_default(task);
	}
	
	private ReadWriteListener.Type getReadWriteType()
	{
		return m_enable ? ReadWriteListener.Type.ENABLING_NOTIFICATION : ReadWriteListener.Type.DISABLING_NOTIFICATION;
	}

	@Override protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp)
	{
		BleNotify notify = new BleNotify(bleOp.serviceUuid, bleOp.charUuid).setDescriptorUUID(bleOp.descriptorUuid).setDescriptorFilter(bleOp.descriptorFilter);
		notify.m_data = new PresentData(getWriteValue());
		return new ReadWriteEvent(getDevice(), notify, getReadWriteType(), target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.TOGGLE_NOTIFY;
	}
}
