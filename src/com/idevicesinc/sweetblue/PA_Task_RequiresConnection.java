package com.idevicesinc.sweetblue;

/**
 * 
 * 
 *
 */
abstract class PA_Task_RequiresConnection extends PA_Task_RequiresBleOn
{
	public PA_Task_RequiresConnection(BleDevice device, double timeout, I_StateListener listener)
	{
		super(device, timeout, listener);
	}
	
	@Override protected boolean isExecutable()
	{
		boolean shouldBeExecutable = super.isExecutable() && getDevice().m_nativeWrapper.isNativelyConnected(); 
		
		if( shouldBeExecutable )
		{
			if( getDevice().getNativeGatt() == null )
			{
				m_logger.e("Device says we're natively connected but gatt==null");
				getManager().ASSERT(false);
				
				return false;
			}
			else
			{
				return true;
			}
		}
		
		return false;
	}
}
