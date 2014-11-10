package com.idevicesinc.sweetblue;

import java.util.UUID;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 
 * @author dougkoellmer
 */
abstract class PA_Task_ReadOrWrite extends PA_Task_RequiresConnection
{
	protected final P_Characteristic m_characteristic;
	protected final P_WrappingReadWriteListener m_readWriteListener;
	protected final boolean m_requiresBonding;
	private final PE_TaskPriority m_priority;
	private final BleTransaction m_txn;
	
	PA_Task_ReadOrWrite(P_Characteristic characteristic, P_WrappingReadWriteListener readWriteListener, boolean requiresBonding, BleTransaction txn_nullable, PE_TaskPriority priority)
	{
		super(characteristic.getDevice(), null);
		
		m_characteristic = characteristic;
		m_readWriteListener = readWriteListener;
		m_requiresBonding = requiresBonding;
		m_txn = txn_nullable;
		m_priority = priority != null ? priority : PE_TaskPriority.FOR_NORMAL_READS_WRITES;
	}
	
	protected abstract Result newResult(Status status);
	
	protected void fail(Status status)
	{
		if( m_readWriteListener != null )
		{
			m_readWriteListener.onReadOrWriteComplete(newResult(status));
		}
		
		this.fail();
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
	
	public BleTransaction getTxn()
	{
		return m_txn;
	}
	
	protected boolean acknowledgeCallback(int status)
	{
		 //--- DRK > As of now, on the nexus 7, if a write requires authentication, it kicks off a bonding process
		 //---		 and we don't get a callback for the write (android bug), so we let this write task be interruptible
		 //---		 by an implicit bond task. If on other devices we *do* get a callback, we ignore it so that this
		 //---		 library's logic always follows the lowest common denominator that is the nexus 7.
		 if( status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION || status == PS_GattStatus.UNKNOWN_STATUS_AFTER_GATT_INSUFFICIENT_AUTHENTICATION )
		 {
			 return false;
		 }
		 
		 return true;
	}
	
	protected boolean isFor(UUID uuid)
	{
		return uuid.equals(m_characteristic.getUuid());
	}

	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_Disconnect.class && this.getDevice().equals(task.getDevice()) )
		{
			return true;
		}
		
		return super.isSoftlyCancellableBy(task);
	}
	
	@Override protected String getToStringAddition()
	{
		return getManager().getLogger().uuidName(m_characteristic.getUuid());
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
		
		return super.isMoreImportantThan(task);
	}
}
