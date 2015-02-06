package com.idevicesinc.sweetblue;

import android.os.Handler;

public class P_WrappingServerStateListener extends PA_CallbackWrapper implements BleStateListener {

	private final BleStateListener m_stateListener;
	
	public P_WrappingServerStateListener( BleStateListener listener, Handler handler, boolean postToMain ) {
		super( handler, postToMain );
		m_stateListener = listener;
		// TODO Auto-generated constructor stub
	}

	@Override public void onStateChange(final BleDevice device, final int oldStateBits, final int newStateBits)
	{

	}
	
	@Override public void onStateChange(final BleServer device, final int oldStateBits, final int newStateBits)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_stateListener.onStateChange(device, oldStateBits, newStateBits);
				}
			});
		}
		else
		{
			m_stateListener.onStateChange(device, oldStateBits, newStateBits);
		}
	}
}
