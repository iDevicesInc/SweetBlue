package com.idevicesinc.sweetblue;

import java.util.UUID;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
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
	
	private Result newResult(byte[] data)
	{
		return new Result(getDevice(), m_characteristic.getUuid(), m_type, data, Status.SUCCESS, getTotalTime(), getTotalTimeExecuting());
	}
	
	@Override protected Result newResult(Status status)
	{
		return new Result(getDevice(), m_characteristic.getUuid(), m_type, null, status, getTotalTime(), getTotalTimeExecuting());
	}

	@Override public void execute()
	{
		BluetoothGattCharacteristic char_native = m_characteristic.getGuaranteedNative();
		
		if( char_native == null )
		{
			fail(Status.NO_MATCHING_CHARACTERISTIC);  return;
		}
		
		if( !getDevice().getGatt().readCharacteristic(char_native) )
		{
			fail(Status.FAILED_IMMEDIATELY);
		}
	}
	
	private void succeed(byte[] value)
	{
		Result result = newResult(value); 
		getDevice().addReadTime(result.totalTime);
		m_readWriteListener.onReadOrWriteComplete(result);
		 
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
				 succeed(value);
			 }
			 else
			 {
				 fail(Status.NULL_CHARACTERISTIC_VALUE);
				 
				 getManager().uhOh(UhOh.READ_RETURNED_NULL);
			 }
		 }
		 else
		 {
			 fail(Status.FAILED_EVENTUALLY);
		 }
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(m_characteristic.getUuid()) + " read timed out!");
			
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.TIMED_OUT));
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.CANCELLED));
		}
	}
}
