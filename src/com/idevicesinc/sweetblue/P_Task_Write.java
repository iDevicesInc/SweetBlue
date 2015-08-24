package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh;

class P_Task_Write extends PA_Task_ReadOrWrite
{
	public static final int MTU_LIMIT = 20;
	
	private byte[] m_allDataToSend;

	private final FutureData m_futureData;
	
	private int m_offset = 0;
	private byte[] m_maxChunkBuffer;
	private final int m_maxChunkSize = MTU_LIMIT;
	
	private byte[] m_lastChunkBufferSent;
	
	public P_Task_Write(BleDevice device, P_Characteristic characteristic, final FutureData futureData, boolean requiresBonding, P_WrappingReadWriteListener writeListener, BleTransaction txn, PE_TaskPriority priority)
	{
		super(device, characteristic, writeListener, requiresBonding, txn, priority);

		m_futureData = futureData;
	}
	
	@Override protected ReadWriteEvent newReadWriteEvent(final Status status, final int gattStatus, final Target target, final UUID serviceUuid, final UUID charUuid, final UUID descUuid)
	{
		final BluetoothGattCharacteristic char_native = getDevice().getNativeCharacteristic(serviceUuid, charUuid);
		final Type type = P_ServiceManager.modifyResultType(char_native, Type.WRITE);
		
		return new ReadWriteEvent(getDevice(), serviceUuid, charUuid, descUuid, type, target, m_allDataToSend, status, gattStatus, getTotalTime(), getTotalTimeExecuting());
	}
	
	private boolean weBeChunkin()
	{
		return m_allDataToSend != null && m_allDataToSend.length > m_maxChunkSize;
	}

	@Override public void execute()
	{
		super.execute();

		m_allDataToSend = m_futureData.getData();

		if( m_allDataToSend == null )
		{
			fail(Status.NULL_DATA, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);

			return;
		}
		else if( m_allDataToSend.length == 0 )
		{
			fail(Status.EMPTY_DATA, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);

			return;
		}
		
		final BluetoothGattCharacteristic char_native = getDevice().getNativeCharacteristic(getServiceUuid(), getCharUuid());
		
		if( char_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			
			return;
		}
		
		if( !weBeChunkin() )
		{
			write(m_allDataToSend, char_native);
		}
		else
		{
			if( !getDevice().getNativeGatt().beginReliableWrite() )
			{
				fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
				
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
		final BluetoothGattCharacteristic char_native = getDevice().getNativeCharacteristic(getServiceUuid(), getCharUuid());
		
		int copySize = m_allDataToSend.length - m_offset;
		copySize = copySize > m_maxChunkSize ? m_maxChunkSize : copySize;
		m_lastChunkBufferSent = copySize == m_maxChunkSize ? getMaxChunkBuffer() : new byte[copySize];
		Utils.memcpy(m_lastChunkBufferSent, m_allDataToSend, copySize, 0, m_offset);
		
		m_offset += copySize;
		
		write(m_lastChunkBufferSent, char_native);
	}
	
	private void write(final byte[] data, final BluetoothGattCharacteristic char_native)
	{
		if( !char_native.setValue(data) )
		{
			fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			
			return;
		}
		
		if( !getDevice().getNativeGatt().writeCharacteristic(char_native) )
		{
			fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			
			return;
		}
	}
	
	@Override protected void succeed()
	{
		super.succeed();

		final ReadWriteEvent event = newReadWriteEvent(Status.SUCCESS, BluetoothGatt.GATT_SUCCESS, getDefaultTarget(), getServiceUuid(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
		getDevice().addWriteTime(event.time_total().secs());
		getDevice().invokeReadWriteCallback(m_readWriteListener, event);
	}
	
	public void onCharacteristicWrite(final BluetoothGatt gatt, final UUID uuid, final int gattStatus)
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
						 fail(Status.REMOTE_GATT_FAILURE, gattStatus, Target.CHARACTERISTIC, uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
						 
						 return;
					 }
				 }
				 else
				 {
					 writeNextChunk();
					 
					 resetTimeout(getTimeout());
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
			 
			 fail(Status.REMOTE_GATT_FAILURE, gattStatus, Target.CHARACTERISTIC, uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
		 }
	}
	
	public void onReliableWriteCompleted(final BluetoothGatt gatt, final int gattStatus)
	{
		if( Utils.isSuccess(gattStatus) )
		{
			succeed();
		}
		else
		{
			//--- DRK > Not sure if this is implicitly handled or not...hopefully not a problem to call more than once.
			abortReliableWriteIfNeeded();
			
			fail(Status.REMOTE_GATT_FAILURE, gattStatus, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
		}
	}
	
	private boolean canAbortReliableWrite()
	{
		return getDevice().getNativeGatt() != null && weBeChunkin();
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void abortReliableWrite(final BluetoothGatt gatt)
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
	
	@Override public void onStateChange(final PA_Task task, final PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(getCharUuid()) + " write timed out!");
			
			abortReliableWriteIfNeeded();
			
			getDevice().invokeReadWriteCallback(m_readWriteListener, newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getServiceUuid(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID));
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			abortReliableWriteIfNeeded();
			
			getDevice().invokeReadWriteCallback(m_readWriteListener, newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getServiceUuid(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID));
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.WRITE;
	}
}
