package com.idevicesinc.sweetblue;


/**
 * 
 * 
 *
 */
class P_Task_CrashResolver extends PA_Task_RequiresBleOn 
{
	private final P_BluetoothCrashResolver m_resolver;
	
	public P_Task_CrashResolver(BleManager manager, P_BluetoothCrashResolver resolver)
	{
		super(manager, null);
		
		m_resolver = resolver;
	}
	
//	public BtTask_CrashResolver(BleManager manager, BluetoothCrashResolver resolver, I_StateListener listener, double timeout)
//	{
//		super(manager, listener, timeout);
//		
//		m_resolver = resolver;
//	}
	
	@Override public void execute()
	{
		if( m_resolver.isRecoveryInProgress() )
		{
			succeed();
			
			return;
		}
		
		m_resolver.forceFlush();
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.CRITICAL;
	}
	
	@Override protected void update(double timeStep)
	{
		if( !m_resolver.isRecoveryInProgress() )
		{
			succeed();
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.RESOLVE_CRASHES;
	}
}
