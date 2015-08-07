package com.idevicesinc.sweetblue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;



public class P_ServerManager
{
	private final HashMap<String, BleServer> m_map = new HashMap<String, BleServer>();
	private final ArrayList<BleServer> m_list = new ArrayList<BleServer>();
	
	private final P_Logger m_logger;
	private final BleManager m_mngr;
	
	private boolean m_updating = false;
	
	P_ServerManager(BleManager mngr)
	{
		m_mngr = mngr;
		m_logger = m_mngr.getLogger();
	}
	
	public List<BleServer> getList()
	{
		return m_list;
	}
	
	public BleServer get(int i)
	{
		synchronized (m_list)
		{
			return m_list.get(i);
		}
	}
	
	public int getCount()
	{
		synchronized (m_list)
		{
			return m_list.size();
		}
	}
	
	public BleServer get(UUID uniqueUuid)
	{
		synchronized (m_list)
		{
			return m_map.get(uniqueUuid.toString());
		}
	}
	
	synchronized void add(BleServer server)
	{
		synchronized (m_list)
		{
			if( m_map.containsKey( server.getUuid() ) )
			{
				m_logger.e("Already registered server " + server.getUuid());
				
				return;
			}
			
			m_list.add(server);
			m_map.put(server.getUuid().toString(), server);
		}
	}
	
	synchronized void remove(BleServer server)
	{
		synchronized (m_list)
		{
			m_mngr.ASSERT(!m_updating, "Removing server while updating!");
			
			m_list.remove(server);
			m_map.remove(server.getUuid().toString());
		}
	}
	
	void update(double timeStep)
	{
		synchronized (m_list)
		{
			//--- DRK > The asserts here and keeping track of "is updating" is because
			//---		once upon a time we iterated forward through the list with an end
			//---		condition based on the length assigned to a local variable before
			//---		looping (i.e. not checking the length of the array itself every iteration).
			//---		On the last iteration we got an out of bounds exception, so it seems somehow
			//---		that the array was modified up the call stack from this method, or from another
			//---		thread. After heavily auditing the code it's not clear how either situation could
			//---		happen. Note that we were using Collections.serializedList (or something, check SVN),
			//---		and not plain old ArrayList like we are now, if that has anything to do with it.
			
			if( m_updating )
			{
				m_mngr.ASSERT(false, "Already updating.");
				
				return;
			}
			
			m_updating = true;
			
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleServer ithDevice = m_list.get(i); 
				//ithDevice.update(timeStep);
			}
			
			m_updating = false;
		}
	}
	
	void disconnectAll(PE_TaskPriority priority)
	{
		synchronized (m_list)
		{
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleServer server = get(i);

				//--- DRK > Just an early-out performance check here.
				if( server.is(BleDeviceState.CONNECTED) )
				{
					server.disconnect(null);
				}
			}
		}
	}
	
	boolean hasServer(BleDeviceState ... filter)
	{
		synchronized (m_list)
		{
			if( filter == null || filter.length == 0 )
			{
				return m_list.size() > 0;
			}
		
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleServer server = get(i);
				
				if( server.isAny(filter) )
				{
					return true;
				}
			}
			
			return false;
		}		
	}
}
