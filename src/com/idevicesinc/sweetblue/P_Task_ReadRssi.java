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
class P_Task_ReadRssi extends PA_Task_Transactionable implements PA_Task.I_StateListener
{	
	protected final P_WrappingReadWriteListener m_readWriteListener;
	private final Type m_type;
	
	public P_Task_ReadRssi(BleDevice device, P_WrappingReadWriteListener readListener, BleTransaction txn_nullable, PE_TaskPriority priority, Type type)
	{
		super(device, txn_nullable, false, priority);
		
		m_readWriteListener = readListener;
		m_type = type;
	}
	
	private Result newResult(Status status, int gattStatus, int rssi)
	{
		return new Result(getDevice(), m_type, /*rssi=*/rssi, status, gattStatus, getTotalTime(), getTotalTimeExecuting());
	}
	
	@Override protected boolean isExecutable()
	{
		boolean super_isExecutable = super.isExecutable();
		
		if( !super_isExecutable )
		{
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onResult(newResult(Status.NOT_CONNECTED, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE, 0));
			}
		}
		
		return super_isExecutable;
	}
	
	private void fail(Status status, int gattStatus)
	{
		if( m_readWriteListener != null )
		{
			m_readWriteListener.onResult(newResult(status, gattStatus, 0));
		}
		
		this.fail();
	}

	@Override public void execute()
	{
		if( !getDevice().getNativeGatt().readRemoteRssi() )
		{
			fail(Status.FAILED_TO_SEND_OUT, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE);
		}
	}
	
	private void succeed(int gattStatus, int rssi)
	{
		Result result = newResult(Status.SUCCESS, gattStatus, rssi);
		
		if( m_readWriteListener != null )
		{
			m_readWriteListener.onResult(result);
		}
		 
		super.succeed();
	}
	
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
	{
		getManager().ASSERT(gatt == getDevice().getNativeGatt());
		
		if( Utils.isSuccess(status) )
		{
			succeed(status, rssi);
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, status);
		}
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onResult(newResult(Status.TIMED_OUT, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE, 0));
			}
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			if( m_readWriteListener != null )
			{
				m_readWriteListener.onResult(newResult(getCancelType(), BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE, 0));
			}
		}
	}
}
