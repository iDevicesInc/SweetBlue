package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattServer;

import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

final class P_ClientManager
{
	private static final int[] CONNECTING_OR_CONNECTED = {BluetoothGattServer.STATE_CONNECTING, BluetoothGattServer.STATE_CONNECTED};

	private final BleServer m_server;
	private final HashSet<String> m_allConnectingOrConnectedClients = new HashSet<String>();


	P_ClientManager(final BleServer server)
	{
		m_server = server;
	}

	public void onConnecting(final String macAddress)
	{
		m_allConnectingOrConnectedClients.add(macAddress);
	}

	public void onConnected(final String macAddress)
	{
		m_allConnectingOrConnectedClients.add(macAddress);
	}

//	public void onDisconnected(final String macAddress)
//	{
//		m_allConnectingOrConnectedClients.remove(macAddress);
//	}


	public void getClients(final ForEach_Void<String> forEach, final int stateMask)
	{
		getClients_private(forEach, getClients(stateMask));
	}

	private void getClients_private(final ForEach_Void<String> forEach, final Iterator<String> iterator)
	{
		while( iterator.hasNext() )
		{
			final String client = iterator.next();

			forEach.next(client);
		}
	}

	public void getClients(final ForEach_Breakable<String> forEach, final int stateMask)
	{
		getClients_private(forEach, getClients(stateMask));
	}

	private void getClients_private(final ForEach_Breakable<String> forEach, final Iterator<String> iterator)
	{
		while( iterator.hasNext() )
		{
			final String client = iterator.next();

			final ForEach_Breakable.Please please = forEach.next(client);

			if( please.shouldContinue() == false )
			{
				break;
			}
		}
	}

	public Iterator<String> getClients(final int stateMask)
	{
		return new ClientIterator(stateMask);
	}

	public List<String> getClients_List(final int stateMask)
	{
		if( getClientCount() == 0 )
		{
			return newEmptyList();
		}
		else
		{
			final Iterator<String> iterator = getClients(stateMask);
			final ArrayList<String> toReturn = new ArrayList<String>();

			while( iterator.hasNext() )
			{
				toReturn.add(iterator.next());
			}

			return toReturn;
		}
	}

	public int getClientCount()
	{
		return m_allConnectingOrConnectedClients.size();
	}

	public int getClientCount(final int stateMask)
	{
		final Iterator<String> iterator = getClients(stateMask);
		int count = 0;

		while( iterator.hasNext() )
		{
			final String client = iterator.next();

			count++;
		}

		return count;
	}

	private List<String> newEmptyList()
	{
		return new ArrayList<String>();
	}

	private class ClientIterator implements Iterator<String>
	{
		private final int m_stateMask;

		private String m_next = null;
		private String m_returned = null;

		private final Iterator<String> m_all = m_allConnectingOrConnectedClients.iterator();

		ClientIterator(final int stateMask)
		{
			m_stateMask = stateMask;

			findNext();
		}

		private void findNext()
		{
			while( m_all.hasNext() )
			{
				final String client = m_all.next();

				if( m_stateMask != 0x0 && m_server.isAny(client, m_stateMask) )
				{
					m_next = client;

					break;
				}
				else if( m_stateMask == 0x0 )
				{
					m_next = client;

					break;
				}
			}
		}

		@Override public boolean hasNext()
		{
			return m_next != null;
		}

		@Override public String next()
		{
			m_returned = m_next;

			if( m_next == null )
			{
				throw new NoSuchElementException("No more clients associated with this server.");
			}

			m_next = null;
			findNext();

			return m_returned;
		}

		@Override public void remove()
		{
			if( m_returned == null )
			{
				throw new IllegalStateException("remove() was already called.");
			}

			final String toRemove = m_returned;
			m_returned = null;
			m_all.remove();
			m_server.disconnect(toRemove);
		}
	}
}
