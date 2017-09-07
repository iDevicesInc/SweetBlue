package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;

/**
 * 
 * 
 */
class P_WrappingReadWriteListener extends PA_CallbackWrapper implements ReadWriteListener
{
	private final ReadWriteListener m_listener;
	
	P_WrappingReadWriteListener(ReadWriteListener listener, P_SweetHandler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
	}
	
	protected void onEvent(final ReadWriteListener listener, final ReadWriteEvent result)
	{
		if( listener == null )  return;
		
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					listener.onEvent(result);
				}
			});
		}
		else
		{
			listener.onEvent(result);
		}
	}
	
	@Override public void onEvent(final ReadWriteEvent result)
	{
		onEvent(m_listener, result);
	}
}
