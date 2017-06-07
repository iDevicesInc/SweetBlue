package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;

import java.util.UUID;

final class P_Task_ReadDescriptor extends PA_Task_ReadOrWrite
{
	private final Type m_type;
	private final UUID m_descriptorUuid;

	public P_Task_ReadDescriptor(BleDevice device, BluetoothGattDescriptor descriptor, Type type, boolean requiresBonding, ReadWriteListener readListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, descriptor.getCharacteristic(), readListener, requiresBonding, txn, priority);
		
		m_type = type;
		m_descriptorUuid = descriptor.getUuid();
	}
	
	@Override protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, UUID serviceUuid, UUID charUuid, UUID descUuid)
	{
		return new ReadWriteEvent(getDevice(), serviceUuid, charUuid, descUuid, m_descriptorFilter, m_type, target, null, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override protected UUID getDescUuid()
	{
		return m_descriptorUuid;
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

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getServiceUuid(), getCharUuid(), getDescUuid());
			
			getDevice().invokeReadWriteCallback(m_readWriteListener, event);
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getServiceUuid(), getCharUuid(), getDescUuid());
			getDevice().invokeReadWriteCallback(m_readWriteListener, event);
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.READ_DESCRIPTOR;
	}
}
