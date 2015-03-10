package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.idevicesinc.sweetblue.BleDevice.BondListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Status;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;

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
	
	public ArrayList<BleDevice> getList()
	{
		return m_list;
	}
	
	public BleDevice getDevice(final int mask_BleDeviceState)
	{
		for( int i = 0; i < getCount(); i++ )
		{
			BleDevice device = get(i);

			if( device.is(mask_BleDeviceState) )
			{
				return device;
			}
		}

		return BleDevice.NULL;
	}
	
	public List<BleDevice> getDevices_List(Object ... query)
	{
		final ArrayList<BleDevice> toReturn = new ArrayList<BleDevice>();
		
		for( int i = 0; i < this.getCount(); i++ )
		{
			final BleDevice device_ith = this.get(i);

			if( device_ith.is(query) )
			{
				toReturn.add(device_ith);
			}
		}
		
		return toReturn;
	}
	
	public List<BleDevice> getDevices_List(final BleDeviceState state)
	{
		final ArrayList<BleDevice> toReturn = new ArrayList<BleDevice>();
		
		for( int i = 0; i < this.getCount(); i++ )
		{
			final BleDevice device_ith = this.get(i);

			if( device_ith.is(state) )
			{
				toReturn.add(device_ith);
			}
		}
		
		return toReturn;
	}
	
	public List<BleDevice> getDevices_List(final int mask_BleDeviceState)
	{
		final ArrayList<BleDevice> toReturn = new ArrayList<BleDevice>();
		
		for( int i = 0; i < this.getCount(); i++ )
		{
			final BleDevice device_ith = this.get(i);

			if( device_ith.is(mask_BleDeviceState) )
			{
				toReturn.add(device_ith);
			}
		}
		
		return toReturn;
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
	
	synchronized void remove(BleDevice device, P_DeviceManager cache)
	{
		synchronized (m_list)
		{
			m_mngr.ASSERT(!m_updating, "Removing device while updating!");
			m_mngr.ASSERT(m_map.containsKey(device.getMacAddress()));
			
			m_list.remove(device);
			m_map.remove(device.getMacAddress());
			
			final boolean cacheDevice = BleDeviceConfig.bool(device.conf_device().cacheDeviceOnUndiscovery, device.conf_mngr().cacheDeviceOnUndiscovery);
			
			if( cacheDevice && cache != null )
			{
				cache.add(device);
			}
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
	
	void unbondAll(PE_TaskPriority priority, BondListener.Status status)
	{
		synchronized (m_list)
		{
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				BleDevice device = get(i);
				
				if( device.m_nativeWrapper.isNativelyBonded() || device.m_nativeWrapper.isNativelyBonding() )
				{
					device.unbond_internal(priority, status);
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
					device.disconnectWithReason(priority, Status.BLE_TURNING_OFF, ConnectionFailListener.Timing.NOT_APPLICABLE, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE, BleDeviceConfig.BOND_FAIL_REASON_NOT_APPLICABLE, device.NULL_READWRITE_RESULT());
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
						DiscoveryEvent event = new DiscoveryEvent(m_mngr, device, LifeCycle.DISCOVERED);
						m_mngr.m_discoveryListener.onEvent(event);
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
				final BleDevice device = (BleDevice) m_list.get(i);
				
				final boolean autoReconnectDeviceWhenBleTurnsBackOn = BleDeviceConfig.bool(device.conf_device().autoReconnectDeviceWhenBleTurnsBackOn, device.conf_mngr().autoReconnectDeviceWhenBleTurnsBackOn);
				
				if( autoReconnectDeviceWhenBleTurnsBackOn && device.lastDisconnectWasBecauseOfBleTurnOff() )
				{
					device.connect();
				}
			}
		}
	}

	void undiscoverAllForTurnOff(final P_DeviceManager cache, final PA_StateTracker.E_Intent intent)
	{
		synchronized (m_list)
		{
			m_mngr.ASSERT(!m_updating, "Undiscovering devices while updating!");
	
			for( int i = m_list.size()-1; i >= 0; i-- )
			{
				final BleDevice device_ith = m_list.get(i);
				
				final boolean retainDeviceWhenBleTurnsOff = BleDeviceConfig.bool(device_ith.conf_device().retainDeviceWhenBleTurnsOff, device_ith.conf_mngr().retainDeviceWhenBleTurnsOff);
				
				if( !retainDeviceWhenBleTurnsOff )
				{
					undiscoverAndRemove(device_ith, m_mngr.m_discoveryListener, cache, intent);
					
					continue;
				}
				
				final boolean undiscoverDeviceWhenBleTurnsOff = BleDeviceConfig.bool(device_ith.conf_device().undiscoverDeviceWhenBleTurnsOff, device_ith.conf_mngr().undiscoverDeviceWhenBleTurnsOff);
				
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
			DiscoveryEvent event = new DiscoveryEvent(device.getManager(), device, LifeCycle.UNDISCOVERED);
			listener.onEvent(event);
		}
	}
	
	void undiscoverAndRemove(BleDevice device, BleManager.DiscoveryListener discoveryListener, P_DeviceManager cache, E_Intent intent)
	{
		synchronized (m_list)
		{
			remove(device, cache);
		
			undiscoverDevice(device, discoveryListener, intent);
		}
	}
	
	void purgeStaleDevices(final double scanTime, final P_DeviceManager cache, final BleManager.DiscoveryListener listener)
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
						
						Interval minScanTimeToInvokeUndiscovery = BleDeviceConfig.interval(device.conf_device().minScanTimeNeededForUndiscovery, device.conf_mngr().minScanTimeNeededForUndiscovery);
						if( Interval.isDisabled(minScanTimeToInvokeUndiscovery) )  continue;
						
						Interval scanKeepAlive_interval = BleDeviceConfig.interval(device.conf_device().undiscoveryKeepAlive, device.conf_mngr().undiscoveryKeepAlive);
						if( Interval.isDisabled(scanKeepAlive_interval) )  continue;

						if( scanTime < Interval.secs(minScanTimeToInvokeUndiscovery) )  continue;
						
						boolean purgeable = device.getOrigin() != BleDeviceOrigin.EXPLICIT && ((device.getStateMask() & ~BleDeviceState.PURGEABLE_MASK) == 0x0);
						
						if( purgeable )
						{
							if( device.getTimeSinceLastDiscovery() > scanKeepAlive_interval.secs() )
							{
								undiscoverAndRemove(device, listener, cache, E_Intent.UNINTENTIONAL);
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
