package com.idevicesinc.sweetblue;

import android.annotation.SuppressLint;

/**
 * 
 * 
 *
 */
class P_Task_Bond extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;
	private final boolean m_partOfConnection;
	
	public P_Task_Bond(BleDevice device, boolean explicit, boolean partOfConnection, I_StateListener listener, PE_TaskPriority priority)
	{
		super(device, listener);
		
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
		m_explicit = explicit;
		m_partOfConnection = partOfConnection;
	}
	
	public P_Task_Bond(BleDevice device, boolean explicit, boolean partOfConnection, I_StateListener listener)
	{
		this(device, explicit, partOfConnection, listener, null);
	}
	
	@Override public boolean isExplicit()
	{
		return m_explicit;
	}
	
	@SuppressLint("NewApi")
	@Override public void execute()
	{
		//--- DRK > Commenting out this block for now because Android can lie and tell us we're bonded when we're actually not,
		//---		so therefore we always try to force a bond regardless. Not sure if that actually forces
		//---		Android to "come clean" about its actual bond status or not, but worth a try.
		//---		UPDATE: No, it doesn't appear this works...Android lies even to itself, so commenting this back in.
		if( getDevice().m_nativeWrapper.isNativelyBonded() )
		{
			m_logger.w("Already bonded!");
			
			succeed();
			
			return;
		}
		
		if( getDevice().m_nativeWrapper./*already*/isNativelyBonding() )
		{
			// nothing to do
			
			return;
		}

		if( !m_explicit )
		{
			fail();
		}
		else if( !getDevice().getNative().createBond() )
		{
			fail();
			
			m_logger.w("Bond failed immediately.");
		}
	}
	
	@Override public boolean isMoreImportantThan(PA_Task task)
	{
		if( task instanceof P_Task_TxnLock )
		{
			P_Task_TxnLock task_cast = (P_Task_TxnLock) task;
			
			if( this.getDevice() == task_cast.getDevice() )
			{
				return true;
			}
		}
		
		return super.isMoreImportantThan(task);
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_Disconnect.class && this.getDevice().equals(task.getDevice()) )
		{
			if( this.m_partOfConnection && this.getState() == PE_TaskState.EXECUTING )
			{
				return true;
			}
		}
		
		return super.isSoftlyCancellableBy(task);
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.BOND;
	}
}
