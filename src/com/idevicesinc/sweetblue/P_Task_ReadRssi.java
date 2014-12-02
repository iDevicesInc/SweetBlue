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
 * @author dougkoellmer
 */
class P_Task_ReadRssi extends PA_Task_Transactionable implements PA_Task.I_StateListener
{	
	protected final P_WrappingReadWriteListener m_readWriteListener;
	private final PE_TaskPriority m_priority;
	private final BleTransaction m_txn;
	
	public P_Task_ReadRssi(BleDevice device, P_WrappingReadWriteListener readListener, BleTransaction txn_nullable, PE_TaskPriority priority)
	{
		super(device, txn_nullable, false);
		
		m_readWriteListener = readListener;
		m_txn = txn_nullable;
		m_priority = priority;
	}
	
	private Result newResult(Status status)
	{
		return newResult(status, 0);
	}
	
	private Result newResult(Status status, int rssi)
	{
		return new Result(getDevice(), /*rssi=*/rssi, status, getTotalTime(), getTotalTimeExecuting());
	}
	
	@Override protected boolean isExecutable()
	{
		boolean super_isExecutable = super.isExecutable();
		
		if( !super_isExecutable )
		{
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onReadOrWriteComplete(newResult(Status.NOT_CONNECTED));
			}
		}
		
		return super_isExecutable;
	}
	
	protected void fail(Status status)
	{
		if( m_readWriteListener != null )
		{
			m_readWriteListener.onReadOrWriteComplete(newResult(status));
		}
		
		this.fail();
	}

	@Override public void execute()
	{
		if( !getDevice().getNativeGatt().readRemoteRssi() )
		{
			fail(Status.FAILED_TO_SEND_OUT);
		}
	}
	
	private void succeed(int rssi)
	{
		Result result = newResult(Status.SUCCESS, rssi);
		
		if( m_readWriteListener != null )
		{
			m_readWriteListener.onReadOrWriteComplete(result);
		}
		 
		super.succeed();
	}
	
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
	{
		getManager().ASSERT(gatt == getDevice().getNativeGatt());
		
		if( Utils.isSuccess(status) )
		{
			getDevice().updateRssi(rssi);
			
			succeed(rssi);
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE);
		}
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onReadOrWriteComplete(newResult(Status.TIMED_OUT));
			}
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onReadOrWriteComplete(newResult(Status.CANCELLED));
			}
		}
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
}
