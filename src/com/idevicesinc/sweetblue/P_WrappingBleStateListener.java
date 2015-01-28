package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleManager.NativeStateListener;
import com.idevicesinc.sweetblue.BleManager.StateListener;

import android.os.Handler;

/**
 * 
 * 
 *
 */
class P_WrappingBleStateListener extends PA_CallbackWrapper implements BleManager.StateListener, BleManager.NativeStateListener
{
	private final BleManager.StateListener m_listener;
	private final BleManager.NativeStateListener m_nativeListener;
	
	P_WrappingBleStateListener(BleManager.StateListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
		m_nativeListener = null;
	}
	
	P_WrappingBleStateListener(BleManager.NativeStateListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = null;
		m_nativeListener = listener;
	}

	@Override public void onStateChange(final StateListener.ChangeEvent event)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_listener.onStateChange(event);
				}
			});
		}
		else
		{
			m_listener.onStateChange(event);
		}
	}

	@Override public void onNativeStateChange(final NativeStateListener.ChangeEvent event)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_nativeListener.onNativeStateChange(event);
				}
			});
		}
		else
		{
			m_nativeListener.onNativeStateChange(event);
		}
	}
}
