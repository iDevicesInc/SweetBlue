package com.idevicesinc.sweetblue;

import java.util.List;
import java.util.UUID;

import android.os.Handler;

/**
 * 
 * @author dougkoellmer
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

	@Override public void onBleStateChange(final BleManager manager, final int oldStateBits, final int newStateBits)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_listener.onBleStateChange(manager, oldStateBits, newStateBits);
				}
			});
		}
		else
		{
			m_listener.onBleStateChange(manager, oldStateBits, newStateBits);
		}
	}

	@Override public void onNativeBleStateChange(final BleManager manager, final int oldStateBits, final int newStateBits)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_nativeListener.onNativeBleStateChange(manager, oldStateBits, newStateBits);
				}
			});
		}
		else
		{
			m_nativeListener.onNativeBleStateChange(manager, oldStateBits, newStateBits);
		}
	}
}
