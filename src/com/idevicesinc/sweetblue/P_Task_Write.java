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
class P_Task_Write extends PA_Task_ReadOrWrite implements PA_Task.I_StateListener
{
	private final byte[] m_data;
	
	public P_Task_Write(P_Characteristic characteristic, byte[] data, boolean requiresBonding, P_WrappingReadWriteListener writeListener)
	{
		this(characteristic, data, requiresBonding, writeListener, null, null);
	}
	
	public P_Task_Write(P_Characteristic characteristic, byte[] data, boolean requiresBonding, P_WrappingReadWriteListener writeListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(characteristic, writeListener, requiresBonding, txn, priority);
		
		m_data = data;
	}
	
	@Override protected Result newResult(Status status, Target target, UUID charUuid, UUID descUuid)
	{
		return new Result(getDevice(), charUuid, descUuid, Type.WRITE, target, m_data, status, getTotalTime(), getTotalTimeExecuting());
	}

	@Override public void execute()
	{
		BluetoothGattCharacteristic char_native = m_characteristic.getGuaranteedNative();
		
		if( char_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);  return;
		}
		
		if( !char_native.setValue(m_data) )
		{
			fail(Status.FAILED_TO_WRITE_VALUE_TO_TARGET, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
		}
		
		if( !getDevice().getGatt().writeCharacteristic(char_native) )
		{
			fail(Status.FAILED_TO_SEND_OUT, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
		}
	}
	
	@Override protected void succeed()
	{
		Result result = newResult(Status.SUCCESS, getDefaultTarget(), m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID); 
		getDevice().addWriteTime(result.totalTime.seconds);
		m_readWriteListener.onReadOrWriteComplete(result);
		 
		super.succeed();
	}
	
	public void onCharacteristicWrite(BluetoothGatt gatt, UUID uuid, int status)
	{
		 getManager().ASSERT(gatt == getDevice().getGatt());
		 
		 if( !this.isFor(uuid) )  return;
		 
		 if( !acknowledgeCallback(status) )  return;
				 
		 if( Utils.isSuccess(status) )
		 {
			succeed();
		 }
		 else
		 {
			 fail(Status.REMOTE_GATT_FAILURE, Target.CHARACTERISTIC, uuid, Result.NON_APPLICABLE_UUID);
		 }
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(m_characteristic.getUuid()) + " write timed out!");
			
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.TIMED_OUT, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID));
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.CANCELLED, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID));
		}
	}
}
