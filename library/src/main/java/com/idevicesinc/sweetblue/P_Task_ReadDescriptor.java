package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;
import java.util.UUID;


final class P_Task_ReadDescriptor extends PA_Task_ReadOrWrite
{

	private final Type m_type;


	public P_Task_ReadDescriptor(BleDevice device, BleRead read, Type type, boolean requiresBonding, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, read, requiresBonding, txn, priority);

		m_type = type;
	}
	
	@Override protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp)
	{
		final BleRead read = new BleRead(bleOp.serviceUuid, bleOp.charUuid).setDescriptorUUID(bleOp.descriptorUuid).setDescriptorFilter(bleOp.descriptorFilter);
		return new ReadWriteEvent(getDevice(), read, m_type, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override protected UUID getDescUuid()
	{
		return m_bleOp.descriptorUuid;
	}

	@Override protected Target getDefaultTarget()
	{
		return Target.DESCRIPTOR;
	}

	@Override protected void executeReadOrWrite()
	{
		final BluetoothGattDescriptor desc_native = getDevice().getNativeDescriptor(getServiceUuid(), getCharUuid(), getDescUuid());

		if( desc_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getCharUuid(), getDescUuid());
		}
		else
		{
			if (!m_bleOp.isServiceUuidValid())
				m_bleOp.serviceUuid = desc_native.getCharacteristic().getService().getUuid();
			if (!m_bleOp.isCharUuidValid())
				m_bleOp.charUuid = desc_native.getCharacteristic().getUuid();

			if( false == getDevice().layerManager().readDescriptor(desc_native) )
			{
				fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getCharUuid(), getDescUuid());
			}
			else
			{
				// DRK > SUCCESS, for now...
			}
		}
	}

	@Override
	public void onDescriptorRead(BluetoothGatt gatt, UUID uuid, byte[] value, int gattStatus)
	{
		getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));

		onCharacteristicOrDescriptorRead(gatt, uuid, value, gattStatus, m_type);
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().descriptorName(getDescUuid()) + " read timed out!");

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, m_bleOp);
			
			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, m_bleOp);
			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.READ_DESCRIPTOR;
	}
}
