package com.idevicesinc.sweetblue;

import java.util.UUID;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.utils.Utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 
 * @author dougkoellmer
 */
class P_Task_Read extends PA_Task_ReadOrWrite implements PA_Task.I_StateListener
{
	private final Type m_type;
	
	public P_Task_Read(P_Characteristic characteristic, Type type, boolean requiresBonding, P_WrappingReadWriteListener readListener)
	{
		this(characteristic, type, requiresBonding, readListener, null, null);
	}
	
	public P_Task_Read(P_Characteristic characteristic, Type type, boolean requiresBonding, P_WrappingReadWriteListener readListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(characteristic, readListener, requiresBonding, txn, priority);
		
		m_type = type;
	}
	
	private Result newResult(byte[] data, Target target, UUID charUuid, UUID descUuid)
	{
		return new Result(getDevice(), charUuid, charUuid, m_type, target, data, Status.SUCCESS, getTotalTime(), getTotalTimeExecuting());
	}
	
	@Override protected Result newResult(Status status, Target target, UUID charUuid, UUID descUuid)
	{
		return new Result(getDevice(), charUuid, charUuid, m_type, target, null, status, getTotalTime(), getTotalTimeExecuting());
	}

	@Override public void execute()
	{
		BluetoothGattCharacteristic char_native = m_characteristic.getGuaranteedNative();
		
		if( char_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);  return;
		}
		
		if( !getDevice().getGatt().readCharacteristic(char_native) )
		{
			fail(Status.FAILED_TO_SEND_OUT, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
		}
	}
	
	private void succeed(byte[] value, Target target)
	{
		Result result = newResult(value, target, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID); 
		getDevice().addReadTime(result.totalTime.seconds);
		
		if( m_readWriteListener != null )
		{
			m_readWriteListener.onReadOrWriteComplete(result);
		}
		 
		super.succeed();
	}
	
	public void onCharacteristicRead(BluetoothGatt gatt, UUID uuid, byte[] value, int status)
	{
		getManager().ASSERT(gatt == getDevice().getGatt());
		 
		 if( !this.isFor(uuid) )  return;
		 
		 if( !acknowledgeCallback(status) )  return;

		 if( Utils.isSuccess(status) )
		 {
			 if( value != null )
			 {				 
				 succeed(value, Target.CHARACTERISTIC);
			 }
			 else
			 {
				 fail(Status.NULL_VALUE_RETURNED, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
				 
				 getManager().uhOh(UhOh.READ_RETURNED_NULL);
			 }
		 }
		 else
		 {
			 fail(Status.REMOTE_GATT_FAILURE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
		 }
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(m_characteristic.getUuid()) + " read timed out!");
			
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.TIMED_OUT, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID));
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.CANCELLED, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID));
		}
	}
}
