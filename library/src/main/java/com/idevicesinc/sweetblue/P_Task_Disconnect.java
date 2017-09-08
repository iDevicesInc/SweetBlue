package com.idevicesinc.sweetblue;

final class P_Task_Disconnect extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;
	private int m_gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	private final boolean m_cancellableByConnect;
	private Integer m_overrideOrdinal = null;

	private final boolean m_saveLastDisconnect;
	
	public P_Task_Disconnect(BleDevice device, I_StateListener listener, boolean explicit, PE_TaskPriority priority, final boolean cancellableByConnect)
	{
		this(device, listener, explicit, priority, cancellableByConnect, false);
	}

	public P_Task_Disconnect(BleDevice device, I_StateListener listener, boolean explicit, PE_TaskPriority priority, final boolean cancellableByConnect, final boolean saveLastDisconnect)
	{
		super(device, listener);

		m_saveLastDisconnect = saveLastDisconnect;
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
		m_explicit = explicit;

		m_cancellableByConnect = cancellableByConnect;
	}
	
	@Override int getOrdinal()
	{
		if( m_overrideOrdinal != null )
		{
			return m_overrideOrdinal;
		}
		else
		{
			return super.getOrdinal();
		}
	}
	
	public void setOverrideOrdinal(int ordinal)
	{
		m_overrideOrdinal = ordinal;
	}
	
	@Override public boolean isExplicit()
	{
		return m_explicit;
	}

	public boolean shouldSaveLastDisconnect()
	{
		return m_saveLastDisconnect;
	}
	
	@Override public void execute()
	{
		if( !getDevice().m_nativeWrapper.isNativelyConnectingOrConnected() )
		{
			getLogger().w("Already disconnected!");
			
			redundant();
			
			return;
		}
		
		if( getDevice().layerManager().isGattNull() )
		{
			getLogger().w("Already disconnected and gatt==null!");
			
			redundant();
			
			return;
		}
		
		if( getDevice().m_nativeWrapper./*already*/isNativelyDisconnecting() )
		{
			// nothing to do
			
			return;
		}
		
//		if( m_explicit )
//		{
			getDevice().layerManager().disconnect();
//		}
//		else
//		{
//			// DRK > nothing to do...wait for implicit disconnect task to complete...note we're probably
//			// never going to get here cause I've never observed STATE_DISCONNECTING.
//		}
	}
	
	public int getGattStatus()
	{
		return m_gattStatus;
	}
	
	public void onNativeSuccess(int gattStatus)
	{
		m_gattStatus = gattStatus;
		
		succeed();
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
	
	boolean isCancellable()
	{
		return this.m_explicit && this.m_cancellableByConnect;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_Connect.class && this.getDevice().equals(task.getDevice()) )
		{
			if( isCancellable() )
			{
				return true;
			}
		}
		
		return super.isSoftlyCancellableBy(task);
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.DISCONNECT;
	}
}
