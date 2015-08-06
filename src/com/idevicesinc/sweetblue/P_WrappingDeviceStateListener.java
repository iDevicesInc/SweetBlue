package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Interval;

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
	
	@Override public void onEvent(final StateEvent event)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_stateListener.onEvent(event);
				}
			});
		}
		else
		{
			m_stateListener.onEvent(event);
		}
	}

	@Override public Please onEvent(final ConnectionFailEvent moreInfo)
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
			return m_connectionFailListener.onEvent(moreInfo);
//		}
	}
}
