package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;

final class P_Task_Write extends PA_Task_ReadOrWrite
{
	public static final int MTU_LIMIT = 20;
	
	private byte[] m_data = null;

	private final FutureData m_futureData;


	public P_Task_Write(BleDevice device, BluetoothGattCharacteristic characteristic, final FutureData futureData, boolean requiresBonding, BleDevice.ReadWriteListener writeListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, characteristic, writeListener, requiresBonding, txn, priority);

		m_futureData = futureData;
	}

	public P_Task_Write(BleDevice device, UUID serviceUuid, UUID charUuid, DescriptorFilter filter, final FutureData futureData, boolean requiresBonding, BleDevice.ReadWriteListener writeListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, serviceUuid, charUuid, requiresBonding, txn, priority, filter, writeListener);

		m_futureData = futureData;
	}
	
	@Override protected ReadWriteEvent newReadWriteEvent(final Status status, final int gattStatus, final Target target, final UUID serviceUuid, final UUID charUuid, final UUID descUuid)
	{
		final BluetoothGattCharacteristic char_native = getDevice().getNativeCharacteristic(serviceUuid, charUuid);
		final Type type = P_DeviceServiceManager.modifyResultType(char_native, Type.WRITE);
		final UUID actualDescUuid = getActualDescUuid(descUuid);
		
		return new ReadWriteEvent(getDevice(), serviceUuid, charUuid, actualDescUuid, type, target, m_data, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override protected void executeReadOrWrite()
	{
		m_data = m_futureData.getData();

		if( false == write_earlyOut(m_data) )
		{
			final BluetoothGattCharacteristic char_native = getDevice().getNativeCharacteristic(getServiceUuid(), getCharUuid());

			if( char_native == null )
			{
				fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			}
			else
			{
				if( false == getDevice().layerManager().setCharValue(char_native, m_data) )
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
			
			getDevice().invokeReadWriteCallback(m_readWriteListener, newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getServiceUuid(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID));
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_readWriteListener, newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getServiceUuid(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID));
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.WRITE;
	}
}
