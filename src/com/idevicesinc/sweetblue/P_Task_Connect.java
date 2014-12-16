package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGatt;

/**
 * 
 * 
 *
 */
class P_Task_Connect extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;
	
	public P_Task_Connect(BleDevice device, I_StateListener listener)
	{
		this(device, listener, true, null);
	}
	
	public P_Task_Connect(BleDevice device, I_StateListener listener, boolean explicit, PE_TaskPriority priority)
	{
		super(device, listener, TIMEOUT_CONNECTION);
		
		m_explicit = explicit;
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
	}
	
	@Override public void execute()
	{
		if( getDevice().m_nativeWrapper.isNativelyConnected() )
		{
			m_logger.w("Already connected!");
			
			redundant();
			
			return;
		}
		
		if( getDevice().m_nativeWrapper./*already*/isNativelyConnecting() )
		{
			// nothing to do
			
			return;
		}
		
		getManager().ASSERT(!getDevice().m_nativeWrapper.isNativelyDisconnecting());
		
		if( m_explicit )
		{
			boolean autoConnect = getDevice().shouldUseAutoConnect();
			
			BluetoothGatt gatt = getDevice().getNative().connectGatt(getDevice().getManager().getApplication(), autoConnect, getDevice().getListeners());
			
			if( gatt == null )
			{
				fail();
				
				return;
			}
			else
			{
//				getDevice().m_nativeWrapper.updateNativeConnectionState(gatt);
			}
		}
		else
		{
			//--- DRK > Not sure why this fail() was here. Now commenting out.
			//---		Beforehand we would get here if we added an implicit
			//---		connection task in response to the "natively connecting"
			//---		gatt callback (which rarely or never gets called) and by the time 
			//---		this task is executed the connection has failed or we got disconnected.
			//---		In that case it doesn't make sense to fail again (even though listeners
			//---		should be and currently are defensive about that case).
//			this.fail();
			this.noOp();
		}
	}
	
	public boolean isExplicit()
	{
		return m_explicit;
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
	
	@Override public boolean isCancellableBy(PA_Task task)
	{
		if( task instanceof P_Task_Disconnect )
		{
			P_Task_Disconnect task_cast = (P_Task_Disconnect) task;
			
			//--- DRK > If an implicit disconnect comes in we have no choice but to bail.
			//---		Otherwise we let the connection task run its course then we'll
			//---		disconnect afterwards all nice and orderly-like.
			if( !task_cast.isExplicit() )
			{
				return true;
			}
		}
		else if( task instanceof P_Task_TurnBleOff )
		{
			return true;
		}
		
		return super.isCancellableBy(task);
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_Disconnect.class && this.getDevice().equals(task.getDevice()) )
		{
			if( this.m_explicit )
			{
				return true;
			}
		}
		
		return super.isSoftlyCancellableBy(task);
	}
}
