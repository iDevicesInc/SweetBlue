package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;

abstract class PA_Task_Transactionable extends PA_Task_RequiresConnection
{
	protected final boolean m_requiresBonding;
	private final BleTransaction m_txn;
	private final PE_TaskPriority m_priority;
	
	PA_Task_Transactionable(BleDevice device, BleTransaction txn_nullable, boolean requiresBonding, PE_TaskPriority priority)
	{
		super(device, null);
		
		m_requiresBonding = requiresBonding;
		m_txn = txn_nullable;
		m_priority = priority != null ? priority : PE_TaskPriority.FOR_NORMAL_READS_WRITES;
	}
	
	protected final BleDevice.ReadWriteListener.Status getCancelType()
	{
		BleManager mngr = this.getManager();
		
		if( mngr.is(BleManagerState.TURNING_OFF) )
		{
			return BleDevice.ReadWriteListener.Status.CANCELLED_FROM_BLE_TURNING_OFF;
		}
		else
		{
			return BleDevice.ReadWriteListener.Status.CANCELLED_FROM_DISCONNECT;
		}
	}
	
	public BleTransaction getTxn()
	{
		return m_txn;
	}
	
	@Override public boolean isInterruptableBy(PA_Task task)
	{
		if( m_requiresBonding )
		{
			if( task instanceof P_Task_Bond )
			{
				P_Task_Bond bondTask = (P_Task_Bond) task;
				
				if( !bondTask.isExplicit() )
				{
					return true;
				}
			}
		}
		
		if( this.getState() == PE_TaskState.ARMED )
		{
			if( task instanceof P_Task_Connect )
			{
				if( task.getDevice().equals(this.getDevice()) )
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override public boolean isMoreImportantThan(PA_Task task)
	{
		if( task instanceof P_Task_TxnLock )
		{
			P_Task_TxnLock task_cast = (P_Task_TxnLock) task;
			
			if( this.getDevice() == task_cast.getDevice() && this.getTxn() == task_cast.getTxn() )
			{
				return true;
			}
		}
		else if( task instanceof P_Task_Scan )
		{
			if( this.getPriority().ordinal() <= PE_TaskPriority.FOR_NORMAL_READS_WRITES.ordinal() )
			{
				return false;
			}
		}
		
		//--- DRK > This allows the plain old reads/writes during auth/initialization to have
		//---		higher priority than notification enabling. Otherwise we don't care.
		else if( task instanceof P_Task_ToggleNotify )
		{
			if( !(this instanceof P_Task_ToggleNotify) )
			{
				if( this.getDevice().is(BleDeviceState.INITIALIZED) )
				{
					return true;
				}
			}
		}
		
		return super.isMoreImportantThan(task);
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
}
