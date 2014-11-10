package com.idevicesinc.sweetblue.utils;

import java.util.UUID;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;

/**
 * A convenience wrapper for reading standard battery level characteristics 
 * from any device conforming to the BLE specification.
 * 
 * @author dougkoellmer
 */
public class BatteryService
{
	/**
	 * Callback to be implemented to know when interesting things happen to the battery.
	 */
	public static interface Listener
	{
		/**
		 * Called when the battery level changes, usually when it goes down I would imagine.
		 */
		void onBatteryLevelChanged(int level);
		
		/**
		 * Called when the battery level has reached the threshold passed into {@link BatteryService#BatteryMonitor(BleDevice, int, Listener)}.
		 */
		void onBatteryLow(int level);
	}
	
	private final BleDevice.ReadWriteListener m_readListener = new BleDevice.ReadWriteListener()
	{
		@Override public void onReadOrWriteComplete(Result result)
		{
			if( result.status != Status.SUCCESS )  return;
			
			m_level = result.data[0];
			m_level = ((m_level + 5) / 10)*10;
			m_hasReceivedReading = true;
			
			if( m_listener != null )  m_listener.onBatteryLevelChanged(m_level);
			
			fireBatteryWarningIfNeeded();
		}
	};
	
	private int m_level = 0;
	private final Listener m_listener;
	private final BleDevice m_device;
	private final int m_warningLevel;
	private boolean m_hasShownBatteryWarning = false;
	private boolean m_hasReceivedReading = false;
	
	public BatteryService(BleDevice device, int warningLevel, Listener listener)
	{
		m_device = device;
		m_listener = listener;
		m_warningLevel = warningLevel;
	}
	
	/**
	 * Returns whether we've received any reading so far. If {@link Boolean#FALSE} then
	 * {@link #getLevel()} is meaningless.
	 */
	public boolean hasReceivedReading()
	{
		return m_hasReceivedReading;
	}
	
	/**
	 * Returns the latest known battery level out of 100. This may not be accurate if {@link #read()}
	 * hasn't been called in a while, or you never called {@link #startPoll(Interval)}, or
	 * you called {@link #startPoll(Interval)} with a very long {@link Interval}, or 
	 * {@link #hasReceivedReading()} returns {@link Boolean#FALSE}.
	 */
	public int getLevel()
	{
		return m_level;
	}
	
	/**
	 * Invokes {@link BleDevice#read(UUID, BleDevice.ReadWriteListener)} with {@link StandardUuids#BATTERY_LEVEL}.
	 */
	public void read()
	{
		m_device.read(StandardUuids.BATTERY_LEVEL, m_readListener);
	}
	
	/**
	 * Calls {@link BleDevice#startChangeTrackingPoll(UUID, Interval, BleDevice.ReadWriteListener)} with
	 * {@link StandardUuids#BATTERY_LEVEL}.
	 */
	public void startPoll(Interval interval)
	{
		m_device.startChangeTrackingPoll(StandardUuids.BATTERY_LEVEL, interval, m_readListener);
	}
	
	/**
	 * Stops a poll started with {@link #startPoll(Interval)}.
	 */
	public void stopPoll()
	{
		m_device.stopPoll(StandardUuids.BATTERY_LEVEL, m_readListener);
	}
	
	private void fireBatteryWarningIfNeeded()
	{
		if( !m_hasShownBatteryWarning )
		{
			if( m_level < m_warningLevel )
			{
				m_hasShownBatteryWarning = true;

				if( m_listener != null )  m_listener.onBatteryLow(m_level);
			}
		}
	}
}
