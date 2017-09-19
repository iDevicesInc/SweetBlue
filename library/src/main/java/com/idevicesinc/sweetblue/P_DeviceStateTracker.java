package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.DeviceStateListener.StateEvent;
import com.idevicesinc.sweetblue.utils.State;
import java.util.Stack;


final class P_DeviceStateTracker extends PA_StateTracker
{
	private final Stack<DeviceStateListener> m_stateListenerStack;
	private final BleDevice m_device;
	private final boolean m_forShortTermReconnect;
	
	private boolean m_syncing = false;

	
	P_DeviceStateTracker(BleDevice device, final boolean forShortTermReconnect)
	{
		super(BleDeviceState.VALUES(), /*trackTimes=*/forShortTermReconnect==false);

		m_stateListenerStack = new Stack<>();

		m_device = device;
		m_forShortTermReconnect = forShortTermReconnect;
	}
	
	public final void setListener(DeviceStateListener listener)
	{
		m_stateListenerStack.clear();
		m_stateListenerStack.push(listener);
	}

	public final void pushListener(DeviceStateListener listener)
	{
		m_stateListenerStack.push(listener);
	}

	public final boolean popListener()
	{
		if (!m_stateListenerStack.empty())
        {
            m_stateListenerStack.pop();
            return true;
        }
        return false;
	}

	public final void clearListenerStack()
    {
        m_stateListenerStack.clear();
    }

	public final DeviceStateListener getListener()
	{
		if (m_stateListenerStack.empty())
			return null;
		return m_stateListenerStack.peek();
	}

    final void sync(P_DeviceStateTracker otherTracker)
	{
		m_syncing = true;
		
		this.copy(otherTracker);
		
		m_syncing = false;
	}

	@Override protected final void onStateChange(final int oldStateBits, final int newStateBits, final int intentMask, final int gattStatus)
	{
		if( m_device.isNull() )		return;
		if( m_syncing )				return;

		if( getListener() != null )
		{
			final StateEvent event = new StateEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
			m_device.postEventAsCallback(getListener(), event);
		}
		
		if( !m_forShortTermReconnect && m_device.getManager().m_defaultDeviceStateListener != null )
		{
			final StateEvent event = new StateEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
			m_device.postEventAsCallback(m_device.getManager().m_defaultDeviceStateListener, event);
		}

		final BondFilter bondFilter = BleDeviceConfig.filter(m_device.conf_device().bondFilter, m_device.conf_mngr().bondFilter);
		
		if( bondFilter == null )  return;
		
		final BondFilter.StateChangeEvent bondStateChangeEvent = new BondFilter.StateChangeEvent(m_device, oldStateBits, newStateBits, intentMask, gattStatus);
		
		final BondFilter.Please please = bondFilter.onEvent(bondStateChangeEvent);
		
		m_device.getManager().getLogger().checkPlease(please, BondFilter.Please.class);
		
		m_device.m_bondMngr.applyPlease_BondFilter(please);
		
//		m_device.getManager().getLogger().e(this.toString());
	}

	@Override protected final void append_assert(State newState)
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
	
	@Override public final String toString()
	{
		return super.toString(BleDeviceState.VALUES());
	}
}
