package com.idevicesinc.sweetblue;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;

import com.idevicesinc.sweetblue.utils.Utils_Reflection;

class P_Task_Unbond extends PA_Task_RequiresBleOn
{
	private static final String METHOD_NAME__REMOVE_BOND			= "removeBond";
	private static final String METHOD_NAME__CANCEL_BOND_PROCESS	= "cancelBondProcess";

	
	private final PE_TaskPriority m_priority;
	
	public P_Task_Unbond(BleDevice device, I_StateListener listener, PE_TaskPriority priority)
	{
		super(device, listener);
		
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
	}
	
	public P_Task_Unbond(BleDevice device, I_StateListener listener)
	{
		this(device, listener, null);
	}
	
	@SuppressLint("NewApi")
	@Override public void execute()
	{
		if( getDevice().m_nativeWrapper.isNativelyUnbonded() )
		{
			//--- DRK > Commenting this out cause it's a little louder than need be...redundant ending state gets the point across.
//			m_logger.w("Already not bonded!");
			
			redundant();
		}
		else
		{
			if( getDevice().m_nativeWrapper.isNativelyBonding() )
			{
				if( false == cancelBondProcess() )
				{
					failImmediately();
				}
				else
				{
					// SUCCESS, so far...
				}
			}
			else if( getDevice().m_nativeWrapper.isNativelyBonded() )
			{
				if( false == removeBond() )
				{
					failImmediately();
				}
				else
				{
					// SUCCESS, so far...
				}
			}
			else
			{
				getManager().ASSERT(false, "Expected to be bonding or bonded only.");

				failImmediately();
			}
		}
	}
	
	private boolean removeBond()
	{
		return Utils_Reflection.callBooleanReturnMethod(getDevice().getNative(), METHOD_NAME__REMOVE_BOND, getManager().m_config.loggingEnabled);
	}
	
	private boolean cancelBondProcess()
	{
		return Utils_Reflection.callBooleanReturnMethod(getDevice().getNative(), METHOD_NAME__CANCEL_BOND_PROCESS, getManager().m_config.loggingEnabled);
	}
	
	@Override public boolean isExplicit()
	{
		return true; //TODO
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.UNBOND;
	}
}
