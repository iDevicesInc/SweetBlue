package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * 
 * 
 */
class P_Task_Write extends PA_Task_ReadOrWrite implements PA_Task.I_StateListener
{
	private final byte[] m_data;
	
	private int m_offset = 0;
	private byte[] m_buffer;
	private final int m_chunkSize = PS_GattStatus.BYTE_LIMIT;
	
	private final BluetoothGattCharacteristic m_char_native;
	
	public P_Task_Write(P_Characteristic characteristic, byte[] data, boolean requiresBonding, P_WrappingReadWriteListener writeListener)
	{
		this(characteristic, data, requiresBonding, writeListener, null, null);
	}
	
	public P_Task_Write(P_Characteristic characteristic, byte[] data, boolean requiresBonding, P_WrappingReadWriteListener writeListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(characteristic, writeListener, requiresBonding, txn, priority);
		
		m_data = data;
		
		m_char_native = m_characteristic.getGuaranteedNative();
	}
	
	@Override protected Result newResult(Status status, Target target, UUID charUuid, UUID descUuid)
	{
		return new Result(getDevice(), charUuid, descUuid, Type.WRITE, target, m_data, status, getTotalTime(), getTotalTimeExecuting());
	}
	
	private boolean weBeChunkin()
	{
		return m_data.length > m_chunkSize;
	}

	@Override public void execute()
	{		
		if( m_char_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
			
			return;
		}
		
		if( !weBeChunkin() )
		{
			write(m_data);
		}
		else
		{
			if( !getDevice().getNativeGatt().beginReliableWrite() )
			{
				fail(Status.FAILED_TO_SEND_OUT, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
				
				return;
			}
			
			writeNextChunk();
		}
	}
	
	private void writeNextChunk()
	{		
		m_buffer = m_buffer != null ? m_buffer : new byte[m_chunkSize];
		Utils.memset(m_buffer, (byte) 0x0, m_buffer.length);
		int copySize = m_data.length - m_offset;
		copySize = copySize > m_chunkSize ? m_chunkSize : copySize;
		Utils.memcpy(m_buffer, m_data, copySize, 0, m_offset);
		
		m_offset += copySize;
		
		write(m_buffer);
	}
	
	private void write(byte[] data)
	{
		if( !m_char_native.setValue(data) )
		{
			fail(Status.FAILED_TO_WRITE_VALUE_TO_TARGET, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
			
			return;
		}
		
		if( !getDevice().getNativeGatt().writeCharacteristic(m_char_native) )
		{
			fail(Status.FAILED_TO_SEND_OUT, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
			
			return;
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
		 getManager().ASSERT(gatt == getDevice().getNativeGatt());
		 
		 if( !this.isFor(uuid) )  return;
		 
		 if( !acknowledgeCallback(status) )  return;

		 if( Utils.isSuccess(status) )
		 {
			 if( weBeChunkin() )
			 {
				 //TODO: Verify bytes got sent correctly, whatever that means.
				 
				 if( m_offset >= m_data.length )
				 {
					 if( !getDevice().getNativeGatt().executeReliableWrite() )
					 {
						 //TODO: Use new more accurate error status?
						 fail(Status.REMOTE_GATT_FAILURE, Target.CHARACTERISTIC, uuid, Result.NON_APPLICABLE_UUID);
						 
						 return;
					 }
				 }
				 else
				 {
					 writeNextChunk();
				 }
			 }
			 else
			 {
				 succeed();
			 }
		 }
		 else
		 {
			 fail(Status.REMOTE_GATT_FAILURE, Target.CHARACTERISTIC, uuid, Result.NON_APPLICABLE_UUID);
		 }
	}
	
	public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
	{
		if( Utils.isSuccess(status) )
		{
			succeed();
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, Target.CHARACTERISTIC, m_char_native.getUuid(), Result.NON_APPLICABLE_UUID);
		}
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(m_characteristic.getUuid()) + " write timed out!");
			
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onReadOrWriteComplete(newResult(Status.TIMED_OUT, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID));
			}
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onReadOrWriteComplete(newResult(Status.CANCELLED, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID));
			}
		}
	}
}
