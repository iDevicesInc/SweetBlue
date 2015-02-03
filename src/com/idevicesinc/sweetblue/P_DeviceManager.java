package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Reason;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;

/**
 * 
 * 
 *
 */
class P_DeviceManager
{
	private final HashMap<String, BleDevice> m_map = new HashMap<String, BleDevice>();
	private final ArrayList<BleDevice> m_list = new ArrayList<BleDevice>();
	
	private final P_Logger m_logger;
	private final BleManager m_mngr;
	
	private boolean m_updating = false;
	
	P_DeviceManager(BleManager mngr)
	{
		m_mngr = mngr;
		m_logger = m_mngr.getLogger();
	}
	
	public List<BleDevice> getList()
	{
		return m_list;
	}
	
	public boolean has(BleDevice device)
	{
		synchronized (m_list)
		{
			for( int i = 0; i < m_list.size(); i++ )
			{
				BleDevice device_ith = m_list.get(i);
				
				if( device_ith == device )  return true;
			}
		}
		
		return false;
	}
	
	public BleDevice get(int i)
	{
		synchronized (m_list)
		{
			return m_list.get(i);
		}
	}
	
	int getCount(Object[] query)
	{
		int count = 0;
		
		synchronized (m_list)
		{
			for( int i = 0; i < m_list.size(); i++ )
			{
				BleDevice device_ith = m_list.get(i);
				
				if( device_ith.is(query) )
				{
					count++;
				}
			}
		}
		
		return count;
	}
	
	int getCount(BleDeviceState state)
	{
		int count = 0;
		
		synchronized (m_list)
		{
			for( int i = 0; i < m_list.size(); i++ )
			{
				BleDevice device_ith = m_list.get(i);
				
				if( device_ith.is(state) )
				{
					count++;
				}
			}
		}
		
		return count;
	}
	
	int getCount()
	{
		synchronized (m_list)
		{
			return m_list.size();
		}
	}
	
	public BleDevice get(String uniqueId)
	{
		synchronized (m_list)
		{
			return m_map.get(uniqueId);
		}
	}
	
	synchronized void add(BleDevice device)
	{
		synchronized (m_list)
		{
			if( m_map.containsKey(device.getMacAddress()) )
			{
				m_logger.e("Already registered device " + device.getMacAddress());
				
				return;
			}
			
			m_list.add(device);
			m_map.put(device.getMacAddress(), device);
		}
	}
	
	synchronized void remove(BleDevice device)
	{
		synchronized (m_list)
		{
			m_mngr.ASSERT(!m_updating, "Removing device while updating!");
			
			m_list.remove(device);
			m_map.remove(device.getMacAddress());
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
				BleDevice ithDevice = m_list.get(i); 
				ithDevice.update(timeStep);
			}
			
			m_updating = false;
		}
	}
	
	void unbondAll(PE_TaskPriority priority)
	{
		synchronized (m_list)
		{
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleDevice device = get(i);
				
				if( device.m_nativeWrapper.isNativelyBonded() || device.m_nativeWrapper.isNativelyBonding() )
				{
					device.removeBond(priority);
				}
			}
		}
	}
	
	void disconnectAllForTurnOff(PE_TaskPriority priority)
	{
		synchronized (m_list)
		{
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleDevice device = get(i);

				//--- DRK > Just an early-out performance check here.
				if( device.is(BleDeviceState.CONNECTED) )
				{
					device.disconnectWithReason(priority, Reason.BLE_TURNING_OFF, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE);
				}
			}
		}
	}
	
	void rediscoverDevicesAfterBleTurningBackOn()
	{
		synchronized (m_list)
		{
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleDevice device = (BleDevice) m_list.get(i);
				
				if( !device.is(BleDeviceState.DISCOVERED) )
				{
					device.onNewlyDiscovered(null, device.getRssi(), null);
					
					if( m_mngr.m_discoveryListener != null )
		    		{
						DiscoveryEvent event = new DiscoveryEvent(device, LifeCycle.DISCOVERED);
						m_mngr.m_discoveryListener.onDiscoveryEvent(event);
		    		}
				}
			}
		}
	}
	
	void reconnectDevicesAfterBleTurningBackOn()
	{
		synchronized (m_list)
		{
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleDevice device = (BleDevice) m_list.get(i);
				
				boolean autoReconnectDeviceWhenBleTurnsBackOn = BleDeviceConfig.bool(device.conf_device().autoReconnectDeviceWhenBleTurnsBackOn, device.conf_mngr().autoReconnectDeviceWhenBleTurnsBackOn);
				
				if( autoReconnectDeviceWhenBleTurnsBackOn && device.lastDisconnectWasBecauseOfBleTurnOff() )
				{
					device.connect();
				}
			}
		}
	}

	void undiscoverAllForTurnOff(PA_StateTracker.E_Intent intent)
	{
		synchronized (m_list)
		{
			m_mngr.ASSERT(!m_updating, "Undiscovering devices while updating!");
	
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleDevice device_ith = m_list.get(i);
				
				boolean retainDeviceWhenBleTurnsOff = BleDeviceConfig.bool(device_ith.conf_device().retainDeviceWhenBleTurnsOff, device_ith.conf_mngr().retainDeviceWhenBleTurnsOff);
				
				if( !retainDeviceWhenBleTurnsOff )
				{
					undiscoverAndRemove(device_ith, m_mngr.m_discoveryListener, intent);
					
					continue;
				}
				
				boolean undiscoverDeviceWhenBleTurnsOff = BleDeviceConfig.bool(device_ith.conf_device().undiscoverDeviceWhenBleTurnsOff, device_ith.conf_mngr().undiscoverDeviceWhenBleTurnsOff);
				
				if( undiscoverDeviceWhenBleTurnsOff)
				{
					undiscoverDevice(device_ith, m_mngr.m_discoveryListener, intent);
				}
			}
		}
	}
	
	private static void undiscoverDevice(BleDevice device, BleManager.DiscoveryListener listener, PA_StateTracker.E_Intent intent)
	{
		if( !device.is(BleDeviceState.DISCOVERED) )  return;
		
		device.onUndiscovered(intent);
		
		if( listener != null )
		{
			DiscoveryEvent event = new DiscoveryEvent(device, LifeCycle.UNDISCOVERED);
			listener.onDiscoveryEvent(event);
		}
	}
	
	void undiscoverAndRemove(BleDevice device, BleManager.DiscoveryListener discoveryListener, E_Intent intent)
	{
		synchronized (m_list)
		{
			remove(device);
		
			undiscoverDevice(device, discoveryListener, intent);
		}
	}
	
	void purgeStaleDevices(final double scanTime, final BleManager.DiscoveryListener listener)
	{
		//--- DRK > Band-aid fix for a potential race condition where scan is stopped from main thread (e.g. by backgrounding).
		//---		Thus we can start going through this list but then still get some discovery callbacks at the same time.
		m_mngr.getUpdateLoop().forcePost(new Runnable()
		{
			@Override public void run()
			{				
				synchronized (m_list)
				{
					if( m_updating )
					{
						m_mngr.ASSERT(false, "Purging devices in middle of updating!");
						
						return;
					}
					
					for( int i = m_list.size()-1; i >= 0; i-- )
					{
						BleDevice device = get(i);
						
						Interval minScanTimeToInvokeUndiscovery = BleDeviceConfig.interval(device.conf_device().minScanTimeToInvokeUndiscovery, device.conf_mngr().minScanTimeToInvokeUndiscovery);
						if( Interval.isDisabled(minScanTimeToInvokeUndiscovery) )  continue;
						
						Interval scanKeepAlive_interval = BleDeviceConfig.interval(device.conf_device().scanKeepAlive, device.conf_mngr().scanKeepAlive);
						if( Interval.isDisabled(scanKeepAlive_interval) )  continue;

						if( scanTime < Interval.asDouble(minScanTimeToInvokeUndiscovery) )  continue;
						
						boolean purgeable = device.getOrigin() != BleDevice.Origin.EXPLICIT && ((device.getStateMask() & ~BleDeviceState.PURGEABLE_MASK) == 0x0);
						
						if( purgeable )
						{
							if( device.getTimeSinceLastDiscovery() > scanKeepAlive_interval.secs() )
							{
								undiscoverAndRemove(device, listener, E_Intent.IMPLICIT);
							}
						}
					}
				}
			}
		});
	}
	
	boolean hasDevice(BleDeviceState ... filter)
	{
		synchronized (m_list)
		{
			if( filter == null || filter.length == 0 )
			{
				return m_list.size() > 0;
			}
		
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleDevice device = get(i);
				
				if( device.isAny(filter) )
				{
					return true;
				}
			}
			
			return false;
		}
		
	}
}
