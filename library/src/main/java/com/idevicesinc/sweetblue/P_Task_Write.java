package com.idevicesinc.sweetblue;

import java.util.UUID;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;


final class P_Task_Write extends PA_Task_ReadOrWrite
{


	public P_Task_Write(BleDevice device, BleWrite write, boolean requiresBonding, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, write, requiresBonding, txn, priority);

	}

	@Override
	protected ReadWriteEvent newReadWriteEvent(final Status status, final int gattStatus, final Target target, BleOp bleOp)
	{
		final BleCharacteristicWrapper char_native = getDevice().getNativeBleCharacteristic(bleOp.serviceUuid, bleOp.charUuid);
		final Type type = P_DeviceServiceManager.modifyResultType(char_native, Type.WRITE);
		final UUID actualDescUuid = getActualDescUuid(bleOp.descriptorUuid);
		final BleWrite write = new BleWrite(bleOp.serviceUuid, bleOp.charUuid).setDescriptorUUID(actualDescUuid).setDescriptorFilter(bleOp.descriptorFilter).setBytes(bleOp.m_data.getData());

		return new ReadWriteEvent(getDevice(), write, type, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	private BleWrite get()
	{
		return (BleWrite) m_bleOp;
	}

	@Override protected void executeReadOrWrite()
	{
		if( false == write_earlyOut(m_bleOp.m_data.getData()) )
		{
			final BluetoothGattCharacteristic char_native = getFilteredCharacteristic() != null ? getFilteredCharacteristic() : getDevice().getNativeCharacteristic(getServiceUuid(), getCharUuid());

			if( char_native == null )
			{
				fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			}
			else
			{
				if (!m_bleOp.isServiceUuidValid())
					m_bleOp.serviceUuid = char_native.getService().getUuid();

				// Set the write type now, if it is not null
				if (get().writeType != null)
				{
					if (get().writeType == Type.WRITE_NO_RESPONSE)
					{
						char_native.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
					}
					else if (get().writeType == Type.WRITE_SIGNED)
					{
						char_native.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_SIGNED);
					}
					else if (char_native.getWriteType() != BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
					{
						char_native.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
					}
				}

				if( false == getDevice().layerManager().setCharValue(char_native, m_bleOp.m_data.getData()) )
				{
					fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
				}
				else
				{
					if( false == getDevice().layerManager().writeCharacteristic(char_native) )
					{
						fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
					}
					else
					{
						// SUCCESS, for now...
					}
				}
			}
		}
	}

	public void onCharacteristicWrite(final BluetoothGatt gatt, final UUID uuid, final int gattStatus)
	{
		 getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));
		 
		 if( false == this.isForCharacteristic(uuid) )  return;
		 
		 if( false == acknowledgeCallback(gattStatus) )  return;

		 if( Utils.isSuccess(gattStatus) )
		 {
			 succeedWrite();
		 }
		 else
		 {
			 fail(Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
		 }
	}

	@Override public void onStateChange(final PA_Task task, final PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().charName(getCharUuid()) + " write timed out!");
			
			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp));
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp));
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.WRITE;
	}
}
