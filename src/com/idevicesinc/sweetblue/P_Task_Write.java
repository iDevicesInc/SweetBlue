package com.idevicesinc.sweetblue;

import java.util.UUID;
import android.annotation.SuppressLint;
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
	private final byte[] m_allDataToSend;
	
	private int m_offset = 0;
	private byte[] m_maxChunkBuffer;
	private final int m_maxChunkSize = PS_GattStatus.BYTE_LIMIT;
	
	private byte[] m_lastChunkBufferSent;
	
	private final BluetoothGattCharacteristic m_char_native;
	
	public P_Task_Write(P_Characteristic characteristic, byte[] data, boolean requiresBonding, P_WrappingReadWriteListener writeListener)
	{
		this(characteristic, data, requiresBonding, writeListener, null, null);
	}
	
	public P_Task_Write(P_Characteristic characteristic, byte[] data, boolean requiresBonding, P_WrappingReadWriteListener writeListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(characteristic, writeListener, requiresBonding, txn, priority);
		
		m_allDataToSend = data;
		
		m_char_native = m_characteristic.getGuaranteedNative();
	}
	
	@Override protected Result newResult(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid)
	{
		return new Result(getDevice(), charUuid, descUuid, Type.WRITE, target, m_allDataToSend, status, gattStatus, getTotalTime(), getTotalTimeExecuting());
	}
	
	private boolean weBeChunkin()
	{
		return m_allDataToSend.length > m_maxChunkSize;
	}

	@Override public void execute()
	{		
		if( m_char_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, Result.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
			
			return;
		}
		
		if( !weBeChunkin() )
		{
			write(m_allDataToSend);
		}
		else
		{
			if( !getDevice().getNativeGatt().beginReliableWrite() )
			{
				fail(Status.FAILED_TO_SEND_OUT, Result.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
				
				return;
			}
			
			writeNextChunk();
		}
	}
	
	private byte[] getMaxChunkBuffer()
	{
		m_maxChunkBuffer = m_maxChunkBuffer != null ? m_maxChunkBuffer : new byte[m_maxChunkSize];
		Utils.memset(m_maxChunkBuffer, (byte) 0x0, m_maxChunkBuffer.length);
		
		return m_maxChunkBuffer;
	}
	
	private void writeNextChunk()
	{
		int copySize = m_allDataToSend.length - m_offset;
		copySize = copySize > m_maxChunkSize ? m_maxChunkSize : copySize;
		m_lastChunkBufferSent = copySize == m_maxChunkSize ? getMaxChunkBuffer() : new byte[copySize];
		Utils.memcpy(m_lastChunkBufferSent, m_allDataToSend, copySize, 0, m_offset);
		
		m_offset += copySize;
		
		write(m_lastChunkBufferSent);
	}
	
	private void write(byte[] data)
	{
		if( !m_char_native.setValue(data) )
		{
			fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, Result.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
			
			return;
		}
		
		if( !getDevice().getNativeGatt().writeCharacteristic(m_char_native) )
		{
			fail(Status.FAILED_TO_SEND_OUT, Result.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
			
			return;
		}
	}
	
	@Override protected void succeed()
	{
		Result result = newResult(Status.SUCCESS, BluetoothGatt.GATT_SUCCESS, getDefaultTarget(), m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID); 
		getDevice().addWriteTime(result.totalTime.seconds);
		m_readWriteListener.onReadOrWriteComplete(result);
		 
		super.succeed();
	}
	
	public void onCharacteristicWrite(BluetoothGatt gatt, UUID uuid, int gattStatus)
	{
		 getManager().ASSERT(gatt == getDevice().getNativeGatt());
		 
		 if( !this.isFor(uuid) )  return;
		 
		 if( !acknowledgeCallback(gattStatus) )  return;

		 if( Utils.isSuccess(gattStatus) )
		 {
			 if( weBeChunkin() )
			 {
				 //TODO: Verify bytes got sent correctly, whatever that means.
				 
				 if( m_offset >= m_allDataToSend.length )
				 {
					 if( !gatt.executeReliableWrite() )
					 {
						 //TODO: Use new more accurate error status?
						 fail(Status.REMOTE_GATT_FAILURE, gattStatus, Target.CHARACTERISTIC, uuid, Result.NON_APPLICABLE_UUID);
						 
						 return;
					 }
				 }
				 else
				 {
					 writeNextChunk();
					 
					 resetTimeout(TIMEOUT_DEFAULT);
				 }
			 }
			 else
			 {
				 succeed();
			 }
		 }
		 else
		 {
			 if( weBeChunkin() )
			 {
				 abortReliableWrite(getDevice().getNativeGatt());
			 }
			 
			 fail(Status.REMOTE_GATT_FAILURE, gattStatus, Target.CHARACTERISTIC, uuid, Result.NON_APPLICABLE_UUID);
		 }
	}
	
	public void onReliableWriteCompleted(BluetoothGatt gatt, int gattStatus)
	{
		if( Utils.isSuccess(gattStatus) )
		{
			succeed();
		}
		else
		{
			//--- DRK > Not sure if this is implicitly handled or not...hopefully not a problem to call more than once.
			abortReliableWriteIfNeeded();
			
			fail(Status.REMOTE_GATT_FAILURE, gattStatus, Target.CHARACTERISTIC, m_char_native.getUuid(), Result.NON_APPLICABLE_UUID);
		}
	}
	
	private boolean canAbortReliableWrite()
	{
		return getDevice().getNativeGatt() != null && weBeChunkin();
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void abortReliableWrite(BluetoothGatt gatt)
	{
		if( android.os.Build.VERSION.SDK_INT < 19 )
		{
			gatt.abortReliableWrite(getDevice().getNative());
		}
		else
		{
			gatt.abortReliableWrite();	
		}
	}
	
	private void abortReliableWriteIfNeeded()
	{
		if( canAbortReliableWrite() )
		{
			abortReliableWrite(getDevice().getNativeGatt());
		}
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(m_characteristic.getUuid()) + " write timed out!");
			
			abortReliableWriteIfNeeded();
			
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onReadOrWriteComplete(newResult(Status.TIMED_OUT, Result.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID));
			}
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			abortReliableWriteIfNeeded();
			
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onReadOrWriteComplete(newResult(Status.CANCELLED, Result.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID));
			}
		}
	}
}
