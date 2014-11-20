package com.idevicesinc.sweetblue;

import android.os.Handler;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_WrappingDiscoveryListener extends PA_CallbackWrapper implements BleManager.DiscoveryListener_Full
{
	final BleManager.DiscoveryListener m_listener;
	final BleManager.DiscoveryListener_Full m_listener_full;
	
	P_WrappingDiscoveryListener(BleManager.DiscoveryListener listener, Handler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
		
		m_listener_full = listener instanceof BleManager.DiscoveryListener_Full ? (BleManager.DiscoveryListener_Full)listener : null;
	}
	
	@Override public void onDeviceDiscovered(final BleDevice device)
	{
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_listener.onDeviceDiscovered(device);
				}
			});
		}
		else
		{
			m_listener.onDeviceDiscovered(device);
		}
	}
	
	@Override public void onDeviceRediscovered(final BleDevice device)
	{
		if( m_listener_full == null )  return;
		
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_listener_full.onDeviceRediscovered(device);
				}
			});
		}
		else
		{
			m_listener_full.onDeviceRediscovered(device);
		}
	}

	@Override public void onDeviceUndiscovered(final BleDevice device)
	{
		if( m_listener_full == null )  return;
		
		if( postToMain() )
		{
			m_handler.post(new Runnable()
			{
				@Override public void run()
				{
					m_listener_full.onDeviceUndiscovered(device);
				}
			});
		}
		else
		{
			m_listener_full.onDeviceUndiscovered(device);
		}
	}
}
