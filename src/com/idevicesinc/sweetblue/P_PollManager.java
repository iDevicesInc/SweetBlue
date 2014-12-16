package com.idevicesinc.sweetblue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * 
 * 
 */
class P_PollManager
{
	static enum E_NotifyState
	{
		NOT_ENABLED, ENABLING, ENABLED;
	}
	
	private static class PollingReadListener extends P_WrappingReadWriteListener
	{
		protected CallbackEntry m_entry;
		private ReadWriteListener m_overrideListener;
		
		PollingReadListener(ReadWriteListener readWriteListener, Handler handler, boolean postToMain)
		{
			super(null, handler, postToMain);
			
			addListener(readWriteListener);
		}
		
		private boolean hasListener(ReadWriteListener listener)
		{
			return listener == m_overrideListener;
		}
		
		private void addListener(ReadWriteListener listener)
		{
			m_overrideListener = listener;
		}
		
		private void init(CallbackEntry entry)
		{
			m_entry = entry;
		}
		
		@Override public void onReadOrWriteComplete(Result result)
		{
			m_entry.onSuccessOrFailure();

			super.onReadOrWriteComplete(m_overrideListener, result);
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
		private final PollingReadListener m_pollingReadListener;
		private final double m_interval;
		private final UUID m_uuid;
		private final boolean m_usingNotify;
		private E_NotifyState m_notifyState;
		
		private double m_timeTracker;
		private boolean m_waitingForResponse;
		
		public CallbackEntry(BleDevice device, UUID uuid, double interval, ReadWriteListener readWriteListener, boolean trackChanges, boolean usingNotify)
		{
			m_uuid = uuid;
			m_interval = interval;
			m_device = device;
			m_usingNotify = usingNotify;
			m_notifyState = E_NotifyState.NOT_ENABLED;
			
			if( trackChanges || m_usingNotify)
			{
				m_pollingReadListener = new TrackingWrappingReadListener(readWriteListener, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
			}
			else
			{
				m_pollingReadListener = new PollingReadListener(readWriteListener, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
			}
			
			m_pollingReadListener.init(this);
		}
		
		boolean trackingChanges()
		{
			return m_pollingReadListener instanceof TrackingWrappingReadListener;
		}
		
		boolean usingNotify()
		{
			return m_usingNotify;
		}
		
		boolean isFor(UUID uuid, Double interval_nullable, ReadWriteListener readWriteListener_nullable, boolean usingNotify)
		{
			return
				usingNotify == m_usingNotify																			&&
				uuid.equals(m_uuid)																						&&
				(interval_nullable == null || interval_nullable == m_interval)											&&
				(readWriteListener_nullable == null || m_pollingReadListener.hasListener(readWriteListener_nullable) )	;
		}
		
		boolean isFor(UUID uuid)
		{
			return uuid.equals(m_uuid);
		}
		
		void onCharacteristicChangedFromNativeNotify(byte[] value)
		{
			//--- DRK > The early-outs in this method are for when, for example, a native onNotify comes in on a random thread,
			//---		BleDevice#disconnect() is called on main thread before notify gets passed to main thread (to here).
			//---		Explicit disconnect clears all service/characteristic state and notify shouldn't get sent to app-land
			//---		regardless.
			if( m_device.is(BleDeviceState.DISCONNECTED) )  return;
			
			P_Characteristic characteristic = m_device.getServiceManager().getCharacteristic(m_uuid);
			
			if( characteristic == null )  return;
			
			BluetoothGattCharacteristic char_native = m_device.getServiceManager().getCharacteristic(m_uuid).getGuaranteedNative(); 
			Type type = m_device.getServiceManager().modifyResultType(char_native, Type.NOTIFICATION);
			
			if( value == null )
			{
				Result result = new Result(m_device, m_uuid, null, type, Target.CHARACTERISTIC, value, Status.NULL_VALUE_RETURNED, 0.0, 0.0);
				m_pollingReadListener.onReadOrWriteComplete(result);
			}
			else
			{
				if( value.length == 0 )
				{
					Result result = new Result(m_device, m_uuid, null, type, Target.CHARACTERISTIC, value, Status.EMPTY_VALUE_RETURNED, 0.0, 0.0);
					m_pollingReadListener.onReadOrWriteComplete(result);
				}
				else
				{
					Result result = new Result(m_device, m_uuid, null, type, Target.CHARACTERISTIC, value, Status.SUCCESS, 0.0, 0.0);
					m_pollingReadListener.onReadOrWriteComplete(result);
				}
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
						m_device.read_internal(m_uuid, type, m_pollingReadListener);
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
		if( !m_device.getManager().m_config.allowDuplicatePollEntries )
		{
			for( int i = m_entries.size()-1; i >= 0; i-- )
			{
				CallbackEntry ithEntry = m_entries.get(i);
				
				if( ithEntry.isFor(uuid, interval, /*listener=*/null, usingNotify) )
				{
					if( ithEntry.trackingChanges() == trackChanges)
					{
						ithEntry.m_pollingReadListener.addListener(listener);
						
						return;
					}
				}
			}
		}
		
		CallbackEntry newEntry = new CallbackEntry(m_device, uuid, interval, listener, trackChanges, usingNotify);
		
		if( usingNotify )
		{
			E_NotifyState state = getNotifyState(uuid);
			newEntry.m_notifyState = state;
		}

		m_entries.add(newEntry);
	}
	
	void stopPoll(UUID uuid, Double interval_nullable, ReadWriteListener listener, boolean usingNotify)
	{
		for( int i = m_entries.size()-1; i >= 0; i-- )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			if( ithEntry.isFor(uuid, interval_nullable, listener, usingNotify) )
			{
				m_entries.remove(i);
			}
		}
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
	
	E_NotifyState getNotifyState(UUID uuid)
	{
		E_NotifyState highestState = E_NotifyState.NOT_ENABLED;
		
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			if( ithEntry.isFor(uuid) )
			{
				if( ithEntry.m_notifyState.ordinal() > highestState.ordinal() )
				{
					highestState = ithEntry.m_notifyState;
				}
			}
		}
		
		return highestState;
	}
	
	void onNotifyStateChange(UUID uuid, E_NotifyState state)
	{
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			if( ithEntry.usingNotify() && ithEntry.isFor(uuid) )
			{
				ithEntry.m_notifyState = state;
			}
		}
	}
	
	void resetNotifyStates()
	{
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			ithEntry.m_notifyState = E_NotifyState.NOT_ENABLED;
		}
	}
	
	void enableNotifications()
	{
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			if( ithEntry.usingNotify() )
			{
				E_NotifyState notifyState = getNotifyState(ithEntry.m_uuid);
				
				P_Characteristic characteristic = m_device.getServiceManager().getCharacteristic(ithEntry.m_uuid);
				
				if( notifyState == E_NotifyState.NOT_ENABLED )
				{
					BleDevice.ReadWriteListener.Result earlyOutResult = m_device.getServiceManager().getEarlyOutResult(ithEntry.m_uuid, BleDevice.EMPTY_BYTE_ARRAY, BleDevice.ReadWriteListener.Type.ENABLING_NOTIFICATION);
					
					if( earlyOutResult != null )
					{
						ithEntry.m_pollingReadListener.onReadOrWriteComplete(earlyOutResult);
					}
					else
					{
						P_WrappingReadWriteListener wrappingListener = new P_WrappingReadWriteListener(ithEntry.m_pollingReadListener, m_device.getManager().m_mainThreadHandler, m_device.getManager().m_config.postCallbacksToMainThread);
						m_device.getManager().getTaskQueue().add(new P_Task_ToggleNotify(characteristic, /*enable=*/true, wrappingListener));
						
						notifyState = E_NotifyState.ENABLING;
					}
				}
				
				if( notifyState == E_NotifyState.ENABLED && ithEntry.m_notifyState != E_NotifyState.ENABLED )
				{
					Result result = newAlreadyEnabledResult(characteristic);
					ithEntry.m_pollingReadListener.onReadOrWriteComplete(result);
				}
				
				ithEntry.m_notifyState = notifyState;
			}
		}
	}
	
	Result newAlreadyEnabledResult(P_Characteristic characteristic)
	{
		//--- DRK > Just being anal with the null check here.
		byte[] writeValue = characteristic != null ? P_Task_ToggleNotify.getWriteValue(characteristic.getNative(), /*enable=*/true) : BleDevice.EMPTY_BYTE_ARRAY;
		Result result = new Result(m_device, characteristic.getUuid(), Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID, Type.ENABLING_NOTIFICATION, Target.DESCRIPTOR, writeValue, Status.SUCCESS, 0.0, 0.0);
		
		return result;
	}
}
