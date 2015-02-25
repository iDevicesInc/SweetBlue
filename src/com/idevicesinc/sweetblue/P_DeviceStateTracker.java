package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleDevice.StateListener;
import com.idevicesinc.sweetblue.BleDevice.StateListener.ChangeEvent;
import com.idevicesinc.sweetblue.BleDeviceConfig.BondFilter;
import com.idevicesinc.sweetblue.utils.State;

/**
 */
class P_DeviceStateTracker extends PA_StateTracker
{
	private StateListener m_stateListener;
	private final BleDevice m_device;
	
	P_DeviceStateTracker(BleDevice device)
	{
		super(device.getManager().getLogger(), BleDeviceState.values());
		
		m_device = device;
	}
	
	public void setListener(StateListener listener)
	{
		if( listener != null )
		{
			m_stateListener = new P_WrappingDeviceStateListener(listener, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
		}
		else
		{
			m_stateListener = null;
		}
	}

	@Override protected void onStateChange(int oldStateBits, int newStateBits, int intentMask, int gattStatus)
	{
		ChangeEvent event = null;
		
		if( m_stateListener != null )
		{
			event = event != null ? event : new ChangeEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
			m_stateListener.onStateChange(event);
		}
		
		if( m_device.getManager().m_defaultDeviceStateListener != null )
		{
			event = event != null ? event : new ChangeEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
			m_device.getManager().m_defaultDeviceStateListener.onStateChange(event);
		}

		final BleDeviceConfig.BondFilter bondFilter = m_device.conf_device().bondFilter != null ? m_device.conf_device().bondFilter : m_device.conf_mngr().bondFilter;
		
		if( bondFilter == null )  return;
		
		final BondFilter.StateChangeEvent bondStateChangeEvent = new BondFilter.StateChangeEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
		
		final BondFilter.Please please = bondFilter.onStateChangeEvent(bondStateChangeEvent);
		
		m_device.m_bondMngr.applyPlease_BondFilter(please);
		
//		m_device.getManager().getLogger().e(this.toString());
	}

	@Override protected void append_assert(State newState)
	{
		if( newState.ordinal() > BleDeviceState.CONNECTING.ordinal() )
		{
			//--- DRK > No longer valid...during the connection flow a rogue disconnect can come in.
			//---		This immediately changes the native state of the device but the actual callback
			//---		for the disconnect is sent to the update thread so for a brief time we can be
			//---		abstractly connected/connecting but actually not natively connected. 
//			m_device.getManager().ASSERT(m_device.m_nativeWrapper.isNativelyConnected());
		}
	}
	
	@Override public String toString()
	{
		return super.toString(BleDeviceState.values());
	}
}
