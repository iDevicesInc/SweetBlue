package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * 
 * @author dougkoellmer
 */
class P_PollManager
{
	private static class PollingReadListener extends P_WrappingReadWriteListener
	{
		protected CallbackEntry m_entry;
		
		PollingReadListener(ReadWriteListener readWriteListener, Handler handler, boolean postToMain)
		{
			super(readWriteListener, handler, postToMain);
		}
		
		private void init(CallbackEntry entry)
		{
			m_entry = entry;
		}
		
		@Override public void onReadOrWriteComplete(Result result)
		{
			m_entry.onSuccessOrFailure();
			
			super.onReadOrWriteComplete(result);
		}
	}
	
	private static class TrackingWrappingReadListener extends PollingReadListener
	{
		private byte[] m_lastValue = null;
		
		TrackingWrappingReadListener(ReadWriteListener readWriteListener, Handler handler, boolean postToMain)
		{
			super(readWriteListener, handler, postToMain);
		}
		
		@Override public void onReadOrWriteComplete(Result result)
		{
			if( result.status == Status.SUCCESS )
			{
				if( m_lastValue == null || !Arrays.equals(m_lastValue, result.data) )
				{
					super.onReadOrWriteComplete(result);
				}
				else
				{
					m_entry.onSuccessOrFailure();
				}
				
				m_lastValue = result.data;
			}
			else
			{
				m_lastValue = null;
				
				super.onReadOrWriteComplete(result);
			}
		}
	}
	
	private static class CallbackEntry
	{
		private final BleDevice m_device;
		private final ReadWriteListener m_externalReadWriteListener;
		private final PollingReadListener m_internalPollingListener;
		private final double m_interval;
		private final UUID m_uuid;
		private final boolean m_usingNotify;
		
		private double m_timeTracker;
		private boolean m_waitingForResponse;
		
		public CallbackEntry(BleDevice device, UUID uuid, double interval, ReadWriteListener readWriteListener, boolean trackChanges, boolean usingNotify)
		{
			m_uuid = uuid;
			m_interval = interval;
			m_externalReadWriteListener = readWriteListener;
			m_device = device;
			m_usingNotify = usingNotify;
			
			if( trackChanges || m_usingNotify)
			{
				m_internalPollingListener = new TrackingWrappingReadListener(m_externalReadWriteListener, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
			}
			else
			{
				m_internalPollingListener = new PollingReadListener(m_externalReadWriteListener, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
			}
			
			m_internalPollingListener.init(this);
		}
		
		boolean trackingChanges()
		{
			return m_internalPollingListener instanceof TrackingWrappingReadListener;
		}
		
		boolean usingNotify()
		{
			return m_usingNotify;
		}
		
		boolean isFor(UUID uuid, Double interval_nullable, ReadWriteListener readWriteListener, boolean usingNotify)
		{
			return
				usingNotify == m_usingNotify												&&
				uuid.equals(m_uuid)															&&
				interval_nullable == null || interval_nullable == m_interval				&&
				readWriteListener.equals(m_externalReadWriteListener)						 ;
		}
		
		boolean isFor(UUID uuid)
		{
			return uuid.equals(m_uuid);
		}
		
		void onCharacteristicChangedFromNativeNotify(byte[] value)
		{
			BluetoothGattCharacteristic char_native = m_device.getServiceManager().getCharacteristic(m_uuid).getGuaranteedNative(); 
			Type type = m_device.getServiceManager().modifyResultType(char_native, Type.NOTIFICATION);
			
			if( value == null )
			{
				Result result = new Result(m_device, m_uuid, null, type, Target.CHARACTERISTIC, value, Status.NULL_VALUE_RETURNED, 0.0, 0.0);
				m_internalPollingListener.onReadOrWriteComplete(result);
			}
			else
			{
				Result result = new Result(m_device, m_uuid, null, type, Target.CHARACTERISTIC, value, Status.SUCCESS, 0.0, 0.0);
				m_internalPollingListener.onReadOrWriteComplete(result);
			}
			
			m_timeTracker = 0.0;
		}
		
		void onSuccessOrFailure()
		{
			m_waitingForResponse = false;
			m_timeTracker = 0.0;
		}
		
		void update(double timeStep)
		{
			if( m_interval <= 0.0 )  return;
			if( m_interval == Interval.INFINITE.seconds )  return;
			
			m_timeTracker += timeStep;
			
			if( m_timeTracker >= m_interval )
			{
				m_timeTracker = 0.0;
				
				if( m_device.is(BleDeviceState.INITIALIZED) )
				{					
					if( !m_waitingForResponse )
					{
						m_waitingForResponse = true;
						Type type = trackingChanges() ? Type.PSUEDO_NOTIFICATION : Type.POLL;
						m_device.read_internal(m_uuid, type, m_internalPollingListener);
					}
				}
			}
		}
	}
	
	private final BleDevice m_device;
	private final ArrayList<CallbackEntry> m_entries = new ArrayList<CallbackEntry>();
	

	P_PollManager(BleDevice device)
	{
		m_device = device;
	}
	
	void startPoll(UUID uuid, double interval, ReadWriteListener listener, boolean trackChanges, boolean usingNotify)
	{
		synchronized (m_device.m_threadLock){
		synchronized (m_entries)
		{
			if( !m_device.getManager().m_config.allowDuplicatePollEntries )
			{
				for( int i = m_entries.size()-1; i >= 0; i-- )
				{
					CallbackEntry ithEntry = m_entries.get(i);
					
					if( ithEntry.isFor(uuid, interval, listener, usingNotify) )
					{
						if( ithEntry.trackingChanges() == trackChanges)
						{
							return;
						}
					}
				}
			}

			m_entries.add(new CallbackEntry(m_device, uuid, interval, listener, trackChanges, usingNotify));
		}}
	}
	
	void stopPoll(UUID uuid, Double interval_nullable, ReadWriteListener listener, boolean usingNotify)
	{
		synchronized (m_device.m_threadLock){
		synchronized (m_entries)
		{
			for( int i = m_entries.size()-1; i >= 0; i-- )
			{
				CallbackEntry ithEntry = m_entries.get(i);
				
				if( ithEntry.isFor(uuid, interval_nullable, listener, usingNotify) )
				{
					m_entries.remove(i);
				}
			}
		}}
	}
	
	void update(double timeStep)
	{
		synchronized (m_entries)
		{
			for( int i = 0; i < m_entries.size(); i++ )
			{
				CallbackEntry ithEntry = m_entries.get(i);
				
				ithEntry.update(timeStep);
			}
		}
	}
	
	void onCharacteristicChangedFromNativeNotify(UUID uuid, byte[] value)
	{
		synchronized (m_entries)
		{
			for( int i = 0; i < m_entries.size(); i++ )
			{
				CallbackEntry ithEntry = m_entries.get(i);
				
				if( ithEntry.isFor(uuid) && ithEntry.usingNotify() )
				{
					ithEntry.onCharacteristicChangedFromNativeNotify(value);
				}
			}
		}
	}
	
	void enableNotifications()
	{
		synchronized (m_entries)
		{
			for( int i = 0; i < m_entries.size(); i++ )
			{
				CallbackEntry ithEntry = m_entries.get(i);
				
				if( ithEntry.usingNotify() )
				{
					BleDevice.ReadWriteListener.Result earlyOutResult = m_device.getServiceManager().getEarlyOutResult(ithEntry.m_uuid, BleDevice.EMPTY_BYTE_ARRAY, BleDevice.ReadWriteListener.Type.NOTIFICATION);
					
					if( earlyOutResult != null )
					{
						if( ithEntry.m_externalReadWriteListener != null )
						{
							ithEntry.m_externalReadWriteListener.onReadOrWriteComplete(earlyOutResult);
						}
						
						m_entries.remove(i);
						i--;
						
						continue;
					}
					
					P_Characteristic characteristic = m_device.getServiceManager().getCharacteristic(ithEntry.m_uuid);
					
					m_device.getManager().getTaskQueue().add(new P_Task_ToggleNotify(characteristic, /*enable=*/true));
				}
			}
		}
	}
}
