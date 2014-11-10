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
	
	@Override protected Result newResult(Status status)
	{
		return new Result(getDevice(), m_characteristic.getUuid(), Type.WRITE, m_data, status, getTotalTime(), getTotalTimeExecuting());
	}

	@Override public void execute()
	{
		BluetoothGattCharacteristic char_native = m_characteristic.getGuaranteedNative();
		
		if( char_native == null )
		{
			fail(Status.NO_MATCHING_CHARACTERISTIC);  return;
		}
		
		char_native.setValue(m_data);
		
		if( !getDevice().getGatt().writeCharacteristic(char_native) )
		{
			fail(Status.FAILED_IMMEDIATELY);
		}
	}
	
	@Override protected void succeed()
	{
		Result result = newResult(Status.SUCCESS); 
		getDevice().addWriteTime(result.totalTime);
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
			 fail(Status.FAILED_EVENTUALLY);
		 }
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(m_characteristic.getUuid()) + " write timed out!");
			
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.TIMED_OUT));
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.CANCELLED));
		}
	}
}
