package com.idevicesinc.sweetblue;

import java.util.UUID;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;


final class P_Task_Read extends PA_Task_ReadOrWrite
{
	private final Type m_type;
	
	public P_Task_Read(BleDevice device, BluetoothGattCharacteristic characteristic, Type type, boolean requiresBonding, ReadWriteListener readListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, characteristic, readListener, requiresBonding, txn, priority);
		
		m_type = type;
	}

	public P_Task_Read(BleDevice device, UUID serviceUuid, UUID charUuid, Type type, boolean requiresBonding, DescriptorFilter filter, ReadWriteListener readListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, serviceUuid, charUuid, requiresBonding, txn, priority, filter, readListener);
		m_type = type;
	}
	
	@Override protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, UUID serviceUuid, UUID charUuid, UUID descUuid)
	{
		return new ReadWriteEvent(getDevice(), serviceUuid, charUuid, descUuid, m_descriptorFilter, m_type, target, null, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override protected void executeReadOrWrite()
	{
		final BluetoothGattCharacteristic char_native = getFilteredCharacteristic() != null ? getFilteredCharacteristic() : getDevice().getNativeCharacteristic(getServiceUuid(), getCharUuid());

		if( char_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
		}
		else
		{
			if( false == getDevice().layerManager().readCharacteristic(char_native) )
			{
				fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			}
			else
			{
				// DRK > SUCCESS, for now...
			}
		}
	}

	public void onCharacteristicRead(BluetoothGatt gatt, UUID uuid, byte[] value, int gattStatus)
	{
		getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));
		 
		if( false == this.isForCharacteristic(uuid) )  return;

		onCharacteristicOrDescriptorRead(gatt, uuid, value, gattStatus, m_type);
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().charName(getCharUuid()) + " read timed out!");

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getServiceUuid(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			
			getDevice().invokeReadWriteCallback(m_readWriteListener, event);
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getServiceUuid(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			getDevice().invokeReadWriteCallback(m_readWriteListener, event);
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.READ;
	}
}
