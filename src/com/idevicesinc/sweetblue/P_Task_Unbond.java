package com.idevicesinc.sweetblue;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;

/**
 * 
 * 
 *
 */
class P_Task_Unbond extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	
	public P_Task_Unbond(BleDevice device, I_StateListener listener, PE_TaskPriority priority)
	{
		super(device, BleDeviceConfig.DEFAULT_TASK_TIMEOUT, listener);
		
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
	}
	
	public P_Task_Unbond(BleDevice device, I_StateListener listener)
	{
		this(device, listener, null);
	}
	
	@SuppressLint("NewApi")
	@Override public void execute()
	{
		if( !getDevice().m_nativeWrapper.isNativelyBonded() )
		{
			m_logger.w("Already not bonded!");
			
			succeed();
			
			return;
		}

		try
		{
	        Method method = getDevice().getNative().getClass().getMethod("removeBond", (Class[]) null);
	        Boolean result = (Boolean) method.invoke(getDevice().getNative(), (Object[]) null);
	        
	        if( !result )
	        {
	        	fail();
	        }
	    }
		catch (Exception e)
		{
			fail();
	    }
	}
	
	@Override public boolean isExplicit()
	{
		return true; //TODO
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
}
