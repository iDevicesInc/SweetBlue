package com.idevicesinc.sweetblue;

/**
 * 
 * 
 *
 */
class P_Task_Disconnect extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;
	
	public P_Task_Disconnect(BleDevice device, I_StateListener listener, boolean explicit, PE_TaskPriority priority)
	{
		super(device, listener);
		
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
		m_explicit = explicit;
	}
	
	public boolean isExplicit()
	{
		return m_explicit;
	}
	
	@Override public void execute()
	{
		if( !getDevice().m_nativeWrapper.isNativelyConnected() )
		{
			m_logger.w("Already disconnected!");
			
			redundant();
			
			return;
		}
		
		if( getDevice().getNativeGatt() == null )
		{
			m_logger.w("Already disconnected and gatt==null!");
			
			redundant();
			
			return;
		}
		
		if( getDevice().m_nativeWrapper./*already*/isNativelyDisconnecting() )
		{
			// nothing to do
			
			return;
		}
		
		if( m_explicit )
		{
			getDevice().getNativeGatt().disconnect();
		}
		else
		{
			this.noOp();
		}
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_Connect.class && this.getDevice().equals(task.getDevice()) )
		{
			if( this.m_explicit )
			{
				return true;
			}
		}
		
		return super.isSoftlyCancellableBy(task);
	}
}
