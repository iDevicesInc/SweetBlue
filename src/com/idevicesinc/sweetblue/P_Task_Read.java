package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;

class P_Task_Read extends PA_Task_ReadOrWrite
{
	private final Type m_type;
	
	public P_Task_Read(BleDevice device, UUID uuid, Type type, boolean requiresBonding, ReadWriteListener readListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, uuid, readListener, requiresBonding, txn, priority);
		
		m_type = type;
	}
	
	private ReadWriteEvent newResult(byte[] data, Target target, UUID charUuid, UUID descUuid)
	{
		return new ReadWriteEvent(getDevice(), charUuid, charUuid, m_type, target, data, Status.SUCCESS, BluetoothGatt.GATT_SUCCESS, getTotalTime(), getTotalTimeExecuting());
	}
	
	@Override protected ReadWriteEvent newResult(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid)
	{
		return new ReadWriteEvent(getDevice(), charUuid, charUuid, m_type, target, null, status, gattStatus, getTotalTime(), getTotalTimeExecuting());
	}

	@Override public void execute()
	{
		super.execute();
		
		BluetoothGattCharacteristic char_native = getDevice().getNativeCharacteristic(m_uuid);
		
		if( char_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
			
			return;
		}
		
		if( !getDevice().getNativeGatt().readCharacteristic(char_native) )
		{
			fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
			
			return;
		}
	}
	
	private void succeed(byte[] value, Target target)
	{
		ReadWriteEvent result = newResult(value, target, m_uuid, ReadWriteEvent.NON_APPLICABLE_UUID); 
		getDevice().addReadTime(result.time_total().secs());
		
		getDevice().invokeReadWriteCallback(m_readWriteListener, result);
		 
		super.succeed();
	}
	
	public void onCharacteristicRead(BluetoothGatt gatt, UUID uuid, byte[] value, int gattStatus)
	{
		getManager().ASSERT(gatt == getDevice().getNativeGatt());
		 
		if( !this.isFor(uuid) )  return;
		 
		if( !acknowledgeCallback(gattStatus) )  return;
		
		if( Utils.isSuccess(gattStatus) )
		{
			if( value != null )
			{
				if( value.length == 0 )
				{
					 fail(Status.EMPTY_DATA, gattStatus, Target.CHARACTERISTIC, m_uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
				}
				else
				{
					succeed(value, Target.CHARACTERISTIC);
				}
			}
			else
			{
				fail(Status.NULL_DATA, gattStatus, Target.CHARACTERISTIC, m_uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
				 
				getManager().uhOh(UhOh.READ_RETURNED_NULL);
			}
		 }
		 else
		 {
			 fail(Status.REMOTE_GATT_FAILURE, gattStatus, Target.CHARACTERISTIC, m_uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
		 }
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(m_uuid) + " read timed out!");
			
			getDevice().invokeReadWriteCallback(m_readWriteListener, newResult(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_uuid, ReadWriteEvent.NON_APPLICABLE_UUID));
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_readWriteListener, newResult(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_uuid, ReadWriteEvent.NON_APPLICABLE_UUID));
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.READ;
	}
}
