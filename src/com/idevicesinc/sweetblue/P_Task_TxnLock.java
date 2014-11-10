package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Interval;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter.LeScanCallback;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_Task_TxnLock extends PA_Task_RequiresBleOn
{
	private final BleTransaction m_txn;
	
	public P_Task_TxnLock(BleDevice device, BleTransaction txn)
	{
		super(device, null, Interval.INFINITE.seconds);
		
		m_txn = txn;
	}
	
	public BleTransaction getTxn()
	{
		return m_txn;
	}
	
	@Override public void execute()
	{
		//--- DRK > Nothing to do here...generally just spins infinitely until read/write comes in or txn ends.
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_EXPLICIT_BONDING_CONNECTING;
	}
	
	@Override public boolean isInterruptableBy(PA_Task task)
	{
		if( task instanceof PA_Task_ReadOrWrite )
		{
			PA_Task_ReadOrWrite task_cast = (PA_Task_ReadOrWrite) task;
			
			if( this.getDevice() == task_cast.getDevice() && this.getTxn() == task_cast.getTxn() )
			{
				return true;
			}
		}
		
		return super.isInterruptableBy(task);
	}
}
