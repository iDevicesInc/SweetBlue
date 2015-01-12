package com.idevicesinc.sweetblue;

import android.os.Handler;

/**
 * 
 * 
 *
 */
class P_WrappingDeviceStateListener extends PA_CallbackWrapper implements BleDevice.StateListener, BleDevice.ConnectionFailListener
{
	private final BleDevice.StateListener m_stateListener;
	private final BleDevice.ConnectionFailListener m_connectionFailListener;
	
	P_WrappingDeviceStateListener(BleDevice.StateListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_stateListener = listener;
		m_connectionFailListener = null;
	}
	
	P_WrappingDeviceStateListener(BleDevice.ConnectionFailListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_stateListener = null;
		m_connectionFailListener = listener;
	}
	
	@Override public void onStateChange(final BleDevice device, final int oldStateBits, final int newStateBits, final int explicitnessMask)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_stateListener.onStateChange(device, oldStateBits, newStateBits, explicitnessMask);
				}
			});
		}
		else
		{
			m_stateListener.onStateChange(device, oldStateBits, newStateBits, explicitnessMask);
		}
	}

	@Override public Please onConnectionFail(final BleDevice device, final Reason cause, final int failureCount)
	{
//		if( postToMain() )
//		{
//			m_handler.post(new Runnable()
//			{
//				@Override public void run()
//				{
//					return m_connectionFailListener.onConnectionFail(device, cause, failureCount);
//				}
//			});
//		}
//		else
//		{
			return m_connectionFailListener.onConnectionFail(device, cause, failureCount);
//		}
	}
}
