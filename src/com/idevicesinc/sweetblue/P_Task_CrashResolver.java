package com.idevicesinc.sweetblue;


class P_Task_CrashResolver extends PA_Task_RequiresBleOn 
{
	private final P_BluetoothCrashResolver m_resolver;

	private volatile boolean m_startedRecovery = false;
	private final boolean m_partOfReset;

	private final Runnable m_updateRunnable = new Runnable()
	{
		@Override public void run()
		{
			if( getState() == PE_TaskState.EXECUTING && true == m_startedRecovery )
			{
				if( false == m_resolver.isRecoveryInProgress() )
				{
					succeed();
				}
			}
		}
	};
	
	public P_Task_CrashResolver(BleManager manager, P_BluetoothCrashResolver resolver, final boolean partOfReset)
	{
		super(manager, null);
		
		m_resolver = resolver;
		m_partOfReset = partOfReset;
	}
	
	@Override public void execute()
	{
		if( true == m_resolver.isRecoveryInProgress() )
		{
			getManager().ASSERT(false, "CrashResolver recovery already in progress!");
		}
		else
		{
			getQueue().getExecuteHandler().post(new Runnable()
			{
				@Override public void run()
				{
					m_resolver.forceFlush();

					m_startedRecovery = true;
				}
			});
		}
	}

	@Override public boolean isCancellableBy(PA_Task task)
	{
		if( task instanceof P_Task_TurnBleOff )
		{
			final P_Task_TurnBleOff task_cast = (P_Task_TurnBleOff) task;

			if( task_cast.isImplicit() || false == this.m_partOfReset )
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return super.isCancellableBy(task);
		}
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.CRITICAL;
	}
	
	@Override protected void update(double timeStep)
	{
		getQueue().getExecuteHandler().post(m_updateRunnable);
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.RESOLVE_CRASHES;
	}
}
