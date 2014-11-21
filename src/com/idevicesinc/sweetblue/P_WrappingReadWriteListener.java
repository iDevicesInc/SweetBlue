package com.idevicesinc.sweetblue;

import android.os.Handler;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;

/**
 * 
 * @author dougkoellmer
 */
class P_WrappingReadWriteListener extends PA_CallbackWrapper implements ReadWriteListener
{
	private final ReadWriteListener m_listener;
	
	P_WrappingReadWriteListener(ReadWriteListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
	}
	
	protected void onReadOrWriteComplete(final ReadWriteListener listener, final Result result)
	{
		if( listener == null )  return;
		
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					listener.onReadOrWriteComplete(result);
				}
			});
		}
		else
		{
			listener.onReadOrWriteComplete(result);
		}
	}
	
	@Override public void onReadOrWriteComplete(final Result result)
	{
		onReadOrWriteComplete(m_listener, result);
	}
}
