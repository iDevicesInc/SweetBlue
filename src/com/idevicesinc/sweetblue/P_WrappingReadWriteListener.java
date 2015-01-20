package com.idevicesinc.sweetblue;

import android.os.Handler;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;

/**
 * 
 * 
 */
class P_WrappingReadWriteListener extends PA_CallbackWrapper implements ReadWriteListener
{
	private final ReadWriteListener m_listener;
	
	P_WrappingReadWriteListener(ReadWriteListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
	}
	
	protected void onResult(final ReadWriteListener listener, final Result result)
	{
		if( listener == null )  return;
		
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					listener.onResult(result);
				}
			});
		}
		else
		{
			listener.onResult(result);
		}
	}
	
	@Override public void onResult(final Result result)
	{
		onResult(m_listener, result);
	}
}
