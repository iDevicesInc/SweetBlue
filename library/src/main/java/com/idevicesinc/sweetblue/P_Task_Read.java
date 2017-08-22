package com.idevicesinc.sweetblue;

import java.util.UUID;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;


final class P_Task_Read extends PA_Task_ReadOrWrite
{
	private final Type m_type;
	

	public P_Task_Read(BleDevice device, BleRead read, Type type, boolean requiresBonding, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, read, requiresBonding, txn, priority);
		m_type = type;
	}
	
	@Override protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp)
	{
		BleRead read = new BleRead(bleOp.serviceUuid, bleOp.charUuid).setDescriptorFilter(bleOp.descriptorFilter).setDescriptorUUID(bleOp.descriptorUuid);
		return new ReadWriteEvent(getDevice(), read, m_type, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
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
			if (!m_bleOp.isServiceUuidValid())
				m_bleOp.serviceUuid = char_native.getService().getUuid();

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

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);
			
			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);
			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.READ;
	}
}
