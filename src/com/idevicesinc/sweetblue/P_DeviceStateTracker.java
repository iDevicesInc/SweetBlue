package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleDevice.StateListener;
import com.idevicesinc.sweetblue.utils.BitwiseEnum;

/**
 * 
 * 
 *
 */
class P_DeviceStateTracker extends PA_StateTracker
{
	private StateListener m_stateListener;
	private final BleDevice m_device;
	
	P_DeviceStateTracker(BleDevice device)
	{
		super(device.getManager().getLogger(), BleDeviceState.values());
		
		m_device = device;
		
		set(BleDeviceState.UNDISCOVERED, true, BleDeviceState.DISCONNECTED, true);
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

	@Override protected void onStateChange(int oldStateBits, int newStateBits)
	{
		if( m_stateListener != null )
		{
			m_stateListener.onStateChange(m_device, oldStateBits, newStateBits);
		}
		
		if( m_device.getManager().m_defaultDeviceStateListener != null )
		{
			m_device.getManager().m_defaultDeviceStateListener.onStateChange(m_device, oldStateBits, newStateBits);
		}
		
//		m_device.getManager().getLogger().e(this.toString());
	}

	@Override protected void append_assert(BitwiseEnum newState)
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
