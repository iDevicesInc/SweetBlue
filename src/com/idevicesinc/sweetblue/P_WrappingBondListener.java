package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Interval;

import android.os.Handler;

class P_WrappingBondListener extends PA_CallbackWrapper implements BleDevice.BondListener
{
	private final BleDevice.BondListener m_listener;
	
	P_WrappingBondListener(BleDevice.BondListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
	}
	
	@Override public void onEvent(final BleDevice.BondListener.BondEvent event)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_listener.onEvent(event);
				}
			});
		}
		else
		{
			m_listener.onEvent(event);
		}
	}
}
