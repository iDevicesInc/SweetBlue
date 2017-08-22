package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Utils;
import java.util.UUID;


final class P_Task_WriteDescriptor extends PA_Task_ReadOrWrite
{

	public P_Task_WriteDescriptor(BleDevice device, BleWrite write, boolean requiresBonding, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, write, requiresBonding, txn, priority);
	}

	@Override
	protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp)
	{
		final BleOp op = BleOp.createOp(bleOp.serviceUuid, bleOp.charUuid, bleOp.descriptorUuid, bleOp.descriptorFilter, bleOp.m_data.getData(), Type.WRITE);
		return new ReadWriteEvent(getDevice(), op, Type.WRITE, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override
	protected UUID getDescUuid()
	{
		return m_bleOp.descriptorUuid;
	}

	@Override
	protected Target getDefaultTarget()
	{
		return Target.DESCRIPTOR;
	}

	private byte[] getData()
	{
		return m_bleOp.m_data.getData();
	}

	@Override protected void executeReadOrWrite()
	{

		if( false == write_earlyOut(getData()) )
		{
			final BluetoothGattDescriptor desc_native = getDevice().getNativeDescriptor(getServiceUuid(), getCharUuid(), getDescUuid());

			if( desc_native == null )
			{
				fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), getDescUuid());
			}
			else
			{
				if (!m_bleOp.isServiceUuidValid())
					m_bleOp.serviceUuid = desc_native.getCharacteristic().getService().getUuid();
				if (!m_bleOp.isCharUuidValid())
					m_bleOp.charUuid = desc_native.getCharacteristic().getUuid();

				if( false == getDevice().layerManager().setDescValue(desc_native, getData()) )
				{
					fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), getDescUuid());
				}
				else
				{
					if( false == getDevice().layerManager().writeDescriptor(desc_native) )
					{
						fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), getDescUuid());
					}
					else
					{
						// DRK > SUCCESS, for now...
					}
				}
			}
		}
	}
	
	public void onDescriptorWrite(BluetoothGatt gatt, UUID uuid, int gattStatus)
	{
		getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));

//		if( !this.isForCharacteristic(uuid) )  return;

		if( false == acknowledgeCallback(gattStatus) )  return;

		if( Utils.isSuccess(gattStatus) )
		{
			succeedWrite();
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());
		}
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().descriptorName(getDescUuid()) + " read timed out!");

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);
			
			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);
			getDevice().invokeReadWriteCallback(m_bleOp.readWriteListener, event);
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.WRITE_DESCRIPTOR;
	}
}
