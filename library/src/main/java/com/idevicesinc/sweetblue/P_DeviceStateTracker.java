package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleDevice.StateListener.StateEvent;
import com.idevicesinc.sweetblue.BleDeviceConfig.BondFilter;
import com.idevicesinc.sweetblue.utils.State;


final class P_DeviceStateTracker extends PA_StateTracker
{
	private DeviceStateListener m_stateListener;
	private final BleDevice m_device;
	private final boolean m_forShortTermReconnect;
	
	private boolean m_syncing = false;
	
	P_DeviceStateTracker(BleDevice device, final boolean forShortTermReconnect)
	{
		super(BleDeviceState.VALUES(), /*trackTimes=*/forShortTermReconnect==false);
		
		m_device = device;
		m_forShortTermReconnect = forShortTermReconnect;
	}
	
	public void setListener(DeviceStateListener listener)
	{
		m_stateListener = listener;
	}

	public DeviceStateListener getListener()
	{
		return m_stateListener;
	}
	
	void sync(P_DeviceStateTracker otherTracker)
	{
		m_syncing = true;
		
		this.copy(otherTracker);
		
		m_syncing = false;
	}

	@Override protected void onStateChange(final int oldStateBits, final int newStateBits, final int intentMask, final int gattStatus)
	{
		if( m_device.isNull() )		return;
		if( m_syncing )				return;

		if( m_stateListener != null )
		{
			final StateEvent event = new StateEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
			m_device.postEventAsCallback(m_stateListener, event);
		}
		
		if( !m_forShortTermReconnect && m_device.getManager().m_defaultDeviceStateListener != null )
		{
			final StateEvent event = new StateEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
			m_device.postEventAsCallback(m_device.getManager().m_defaultDeviceStateListener, event);
		}

		final BleDeviceConfig.BondFilter bondFilter = BleDeviceConfig.filter(m_device.conf_device().bondFilter, m_device.conf_mngr().bondFilter);
		
		if( bondFilter == null )  return;
		
		final BondFilter.StateChangeEvent bondStateChangeEvent = new BondFilter.StateChangeEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
		
		final BondFilter.Please please = bondFilter.onEvent(bondStateChangeEvent);
		
		m_device.getManager().getLogger().checkPlease(please, BondFilter.Please.class);
		
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
		
		if( newState == BleDeviceState.BONDING )
		{
			int a;
		}
	}
	
	@Override public String toString()
	{
		return super.toString(BleDeviceState.VALUES());
	}
}
