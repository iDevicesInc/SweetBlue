package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;

import java.util.List;

import static com.idevicesinc.sweetblue.BleServerState.CONNECTED;
import static com.idevicesinc.sweetblue.BleServerState.CONNECTING;

final class P_ServerStateTracker
{
	private BleServer.StateListener m_stateListener;
	private final BleServer m_server;
	
	P_ServerStateTracker(BleServer server)
	{
		m_server = server;
	}
	
	public void setListener(BleServer.StateListener listener)
	{
		m_stateListener = listener;
	}

	BleServerState getOldConnectionState(final String macAddress)
	{
		final int stateMask = getStateMask(macAddress);

		if( BleServerState.CONNECTING.overlaps(stateMask) )
		{
			return CONNECTING;
		}
		else if( BleServerState.CONNECTED.overlaps(stateMask) )
		{
			return CONNECTED;
		}
		else
		{
			m_server.getManager().ASSERT(false, "Expected to be connecting or connected for an explicit disconnect.");

			return BleServerState.NULL;
		}
	}

	void doStateTransition(final String macAddress, final BleServerState oldState, final BleServerState newState, final State.ChangeIntent intent, final int gattStatus)
	{
		final int currentBits = m_server.getStateMask(macAddress);

		final int oldState_bit = false == oldState.isNull() ? oldState.bit() : 0x0;
		final int newState_bit = newState.bit();
		final int intentBits = intent == State.ChangeIntent.INTENTIONAL ? 0xFFFFFFFF : 0x00000000;

		final int oldBits = (currentBits | oldState_bit) & ~newState_bit;
		final int newBits = (currentBits | newState_bit) & ~oldState_bit;
		final int intentMask = (oldBits | newBits) & intentBits;

		final BleServer.StateListener.StateEvent e = new BleServer.StateListener.StateEvent(m_server, macAddress, oldBits, newBits, intentMask, gattStatus);

		fireEvent(e);
	}

	private void fireEvent(final BleServer.StateListener.StateEvent e)
	{
		if( m_stateListener != null )
		{
			m_stateListener.onEvent(e);
		}

		if( m_server.getManager().m_defaultServerStateListener != null )
		{
			m_server.getManager().m_defaultServerStateListener.onEvent(e);
		}
	}

	public int getStateMask(final String macAddress)
	{
		final P_TaskQueue queue = m_server.getManager().getTaskQueue();
		final List<PA_Task> queue_raw = queue.getRaw();
		final int bitForUnknownState = BleServerState.DISCONNECTED.bit();
		final PA_Task current = queue.getCurrent();

		if( m_server.m_nativeWrapper.isConnectingOrConnected(macAddress) )
		{
			for( int i = queue_raw.size()-1; i >= 0; i-- )
			{
				final PA_Task ith = queue_raw.get(i);

				if( ith.isFor(P_Task_ConnectServer.class, m_server, macAddress) )
				{
					return BleServerState.CONNECTING.bit();
				}

				if( ith.isFor(P_Task_DisconnectServer.class, m_server, macAddress) )
				{
					return BleServerState.DISCONNECTED.bit();
				}
			}

			if( current != null && current.isFor(P_Task_DisconnectServer.class, m_server, macAddress) )
			{
				return BleServerState.DISCONNECTED.bit();
			}
			else if( m_server.m_nativeWrapper.isConnected(macAddress) )
			{
				return BleServerState.CONNECTED.bit();
			}
			else if( m_server.m_nativeWrapper.isConnecting(macAddress) )
			{
				return BleServerState.CONNECTING.bit();
			}
			else
			{
				m_server.getManager().ASSERT(false, "Expected to be connecting or connected when getting state mask for server.");

				return bitForUnknownState;
			}
		}
		else if( m_server.m_nativeWrapper.isDisconnectingOrDisconnected(macAddress) )
		{
			for( int i = queue_raw.size()-1; i >= 0; i-- )
			{
				final PA_Task ith = queue_raw.get(i);

				if( ith.isFor(P_Task_DisconnectServer.class, m_server, macAddress) )
				{
					return BleServerState.DISCONNECTED.bit();
				}

				if( ith.isFor(P_Task_ConnectServer.class, m_server, macAddress) )
				{
					return BleServerState.CONNECTING.bit();
				}
			}

			if( current != null && current.isFor(P_Task_ConnectServer.class, m_server, macAddress) )
			{
				return BleServerState.CONNECTING.bit();
			}
			else
			{
				return BleServerState.DISCONNECTED.bit();
			}
		}
		else
		{
			m_server.getManager().ASSERT(false, "Native server is in an unknown state.");

			return bitForUnknownState;
		}
	}
}
