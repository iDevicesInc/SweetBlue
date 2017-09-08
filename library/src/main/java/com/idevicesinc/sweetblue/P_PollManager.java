package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.BleDeviceConfig.BondFilter.CharacteristicEventType;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Uuids;

final class P_PollManager
{
	static final int E_NotifyState__NOT_ENABLED		= 0;
	static final int E_NotifyState__ENABLING 		= 1;
	static final int E_NotifyState__ENABLED			= 2;
	
	private static class PollingReadListener extends P_WrappingReadWriteListener
	{
		protected CallbackEntry m_entry;
		private ReadWriteListener m_overrideListener;
		
		PollingReadListener(ReadWriteListener readWriteListener, P_SweetHandler handler, boolean postToMain)
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
		
		@Override public void onEvent(ReadWriteEvent result)
		{
			m_entry.onSuccessOrFailure();

			super.onEvent(m_overrideListener, result);
		}
	}
	
	private static class TrackingWrappingReadListener extends PollingReadListener
	{
		private byte[] m_lastValue = null;
		
		TrackingWrappingReadListener(ReadWriteListener readWriteListener, P_SweetHandler handler, boolean postToMain)
		{
			super(readWriteListener, handler, postToMain);
		}
		
		@Override public void onEvent(ReadWriteEvent event)
		{
			if( event.status() == Status.SUCCESS )
			{
				if( event.type().isNativeNotification() || m_lastValue == null || !Arrays.equals(m_lastValue, event.data()) )
				{
					super.onEvent(event);
				}
				else
				{
					m_entry.onSuccessOrFailure();
				}
				
				m_lastValue = event.data();
			}
			else
			{
				m_lastValue = null;
				
				super.onEvent(event);
			}
		}
	}
	
	private static class CallbackEntry
	{
		private final BleDevice m_device;
		private final PollingReadListener m_pollingReadListener;
		private double m_interval;
		private final UUID m_charUuid;
		private final UUID m_serviceUuid;
		private final DescriptorFilter m_descriptorFilter;
		private final boolean m_usingNotify;
		private int/*_E_NotifyState*/ m_notifyState;
		
		private double m_timeTracker;
		private boolean m_waitingForResponse;
		
		public CallbackEntry(BleDevice device, final UUID serviceUuid, UUID charUuid, DescriptorFilter descriptorFilter, double interval, ReadWriteListener readWriteListener, boolean trackChanges, boolean usingNotify)
		{
			m_serviceUuid = serviceUuid;
			m_charUuid = charUuid;
			m_descriptorFilter = descriptorFilter;
			m_interval = interval;
			m_device = device;
			m_usingNotify = usingNotify;
			m_notifyState = E_NotifyState__NOT_ENABLED;

			m_timeTracker = interval; // to get it to do a first read pretty much instantly.
			
			if( trackChanges || m_usingNotify)
			{
				m_pollingReadListener = new TrackingWrappingReadListener(readWriteListener, m_device.getManager().getPostManager().getUIHandler(), m_device.getManager().m_config.postCallbacksToMainThread);
			}
			else
			{
				m_pollingReadListener = new PollingReadListener(readWriteListener, m_device.getManager().getPostManager().getUIHandler(), m_device.getManager().m_config.postCallbacksToMainThread);
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
		
		boolean isFor(final UUID serviceUuid, final UUID charUuid, DescriptorFilter descriptorFilter, Double interval_nullable, ReadWriteListener readWriteListener_nullable, boolean usingNotify)
		{
			return
				usingNotify == m_usingNotify																			&&
				(m_serviceUuid == null || serviceUuid == null || m_serviceUuid.equals(serviceUuid))						&&
				descriptorMatches(m_descriptorFilter, descriptorFilter)													&&
				charUuid.equals(m_charUuid)																				&&
				(Interval.isDisabled(interval_nullable) || interval_nullable == m_interval)								&&
				(readWriteListener_nullable == null || m_pollingReadListener.hasListener(readWriteListener_nullable))	 ;
		}

		boolean descriptorMatches(DescriptorFilter curfilter, DescriptorFilter newFilter)
		{
			if (curfilter == null)
			{
				if (newFilter == null)
				{
					return true;
				}
				return false;
			}
			else
			{
				if (newFilter == null)
				{
					return false;
				}
				if (curfilter.equals(newFilter))
				{
					return true;
				}
			}
			return false;
		}
		
		boolean isFor(final UUID serviceUuid, final UUID charUuid)
		{
			if( serviceUuid == null || m_serviceUuid == null )
			{
				return charUuid.equals(m_charUuid);
			}
			else
			{
				return charUuid.equals(m_charUuid) && m_serviceUuid != null && m_serviceUuid.equals(serviceUuid);
			}
		}
		
		void onCharacteristicChangedFromNativeNotify(byte[] value)
		{
			//--- DRK > The early-outs in this method are for when, for example, a native onNotify comes in on a random thread,
			//---		BleDevice#disconnect() is called on main thread before notify gets passed to main thread (to here).
			//---		Explicit disconnect clears all service/characteristic state and notify shouldn't get sent to app-land
			//---		regardless.
			if( m_device.is(BleDeviceState.DISCONNECTED) )  return;
			
			BleCharacteristicWrapper characteristic = m_device.getNativeBleCharacteristic(m_serviceUuid, m_charUuid);
			
			if( characteristic.isNull() )  return;

			Type type = P_DeviceServiceManager.modifyResultType(characteristic, Type.NOTIFICATION);
			int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
			
			if( value == null )
			{
				ReadWriteEvent result = new ReadWriteEvent(m_device, m_serviceUuid, m_charUuid, null, m_descriptorFilter, type, Target.CHARACTERISTIC, value, Status.NULL_DATA, gattStatus, 0.0, 0.0, /*solicited=*/true);

				m_device.invokeReadWriteCallback(m_pollingReadListener, result);
			}
			else
			{
				if( value.length == 0 )
				{
					ReadWriteEvent result = new ReadWriteEvent(m_device, m_serviceUuid, m_charUuid, null, m_descriptorFilter, type, Target.CHARACTERISTIC, value, Status.EMPTY_DATA, gattStatus, 0.0, 0.0, /*solicited=*/true);
					m_device.invokeReadWriteCallback(m_pollingReadListener, result);
				}
				else
				{
					ReadWriteEvent result = new ReadWriteEvent(m_device, m_serviceUuid, m_charUuid, null, m_descriptorFilter, type, Target.CHARACTERISTIC, value, Status.SUCCESS, gattStatus, 0.0, 0.0, /*solicited=*/true);
					m_device.invokeReadWriteCallback(m_pollingReadListener, result);
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
			if( m_interval == Interval.INFINITE.secs() )  return;
			
			m_timeTracker += timeStep;
			
			if( m_timeTracker >= m_interval )
			{
				m_timeTracker = 0.0;
				
				if( m_device.is(BleDeviceState.INITIALIZED) && !m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )
				{					
					if( !m_waitingForResponse )
					{
						m_waitingForResponse = true;
						Type type = trackingChanges() ? Type.PSUEDO_NOTIFICATION : Type.POLL;
						m_device.read_internal(m_serviceUuid, m_charUuid, Uuids.INVALID, type, null, m_pollingReadListener);
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

	void clear()
	{
		m_entries.clear();
	}
	
	void startPoll(final UUID serviceUuid, final UUID charUuid, final DescriptorFilter decriptorFilter, double interval, ReadWriteListener listener, boolean trackChanges, boolean usingNotify)
	{
		if( m_device.isNull() )  return;
		
		boolean allowDuplicatePollEntries = BleDeviceConfig.bool(m_device.conf_device().allowDuplicatePollEntries, m_device.conf_mngr().allowDuplicatePollEntries);
		
		if( !allowDuplicatePollEntries )
		{
			for( int i = m_entries.size()-1; i >= 0; i-- )
			{
				CallbackEntry ithEntry = m_entries.get(i);

				if( ithEntry.m_charUuid.equals(charUuid) )
				{
					ithEntry.m_interval = interval;
				}
				
				if( ithEntry.isFor(serviceUuid, charUuid, decriptorFilter, interval, /*listener=*/null, usingNotify) )
				{
					if( ithEntry.trackingChanges() == trackChanges)
					{
						ithEntry.m_pollingReadListener.addListener(listener);
						
						return;
					}
				}
			}
		}
		
		CallbackEntry newEntry = new CallbackEntry(m_device, serviceUuid, charUuid, decriptorFilter, interval, listener, trackChanges, usingNotify);
		
		if( usingNotify )
		{
			final int/*_E_NotifyState*/ state = getNotifyState(serviceUuid, charUuid);
			newEntry.m_notifyState = state;
		}

		m_entries.add(newEntry);
	}
	
	void stopPoll(final UUID serviceUuid, final UUID characteristicUuid, DescriptorFilter descriptorFilter, Double interval_nullable, ReadWriteListener listener, boolean usingNotify)
	{
		if( m_device.isNull() )  return;
		
		for( int i = m_entries.size()-1; i >= 0; i-- )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			if( ithEntry.isFor(serviceUuid, characteristicUuid, descriptorFilter, interval_nullable, listener, usingNotify) )
			{
				m_entries.remove(i);
			}
		}
	}
	
	void update(double timeStep)
	{
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);

			ithEntry.update(timeStep);
		}
	}
	
	void onCharacteristicChangedFromNativeNotify(final UUID serviceUuid, final UUID charUuid, byte[] value)
	{
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);

			if( ithEntry.isFor(serviceUuid, charUuid) && ithEntry.usingNotify() )
			{
				ithEntry.onCharacteristicChangedFromNativeNotify(value);
			}
		}
	}
	
	int/*__E_NotifyState*/ getNotifyState(final UUID serviceUuid, final UUID charUuid)
	{
		int/*__E_NotifyState*/ highestState = E_NotifyState__NOT_ENABLED;
		
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			if( ithEntry.isFor(serviceUuid, charUuid) )
			{
				if( ithEntry.m_notifyState > highestState )
				{
					highestState = ithEntry.m_notifyState;
				}
			}
		}
		
		return highestState;
	}
	
	void onNotifyStateChange(final UUID serviceUuid, final UUID charUuid, int/*__E_NotifyState*/ state)
	{
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			if( ithEntry.usingNotify() && ithEntry.isFor(serviceUuid, charUuid) )
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
			
			ithEntry.m_notifyState = E_NotifyState__NOT_ENABLED;
		}
	}
	
	void enableNotifications_assumesWeAreConnected()
	{
		for( int i = 0; i < m_entries.size(); i++ )
		{
			CallbackEntry ithEntry = m_entries.get(i);
			
			if( ithEntry.usingNotify() )
			{
				int/*__E_NotifyState*/ notifyState = getNotifyState(ithEntry.m_serviceUuid, ithEntry.m_charUuid);
				
				BluetoothGattCharacteristic characteristic = m_device.getNativeCharacteristic(ithEntry.m_serviceUuid, ithEntry.m_charUuid);
				
				//--- DRK > This was observed to happen while doing iterative testing on a dev board that was changing
				//---		its gatt database again and again...I guess service discovery "succeeded" but the service
				//---		wasn't actually found, so downstream we got an NPE.
				if( characteristic == null )
				{
					continue;
				}
				
				if( notifyState == E_NotifyState__NOT_ENABLED )
				{
					BleDevice.ReadWriteListener.ReadWriteEvent earlyOutResult = m_device.serviceMngr_device().getEarlyOutEvent(ithEntry.m_serviceUuid, ithEntry.m_charUuid, Uuids.INVALID, ithEntry.m_descriptorFilter, P_Const.EMPTY_FUTURE_DATA, BleDevice.ReadWriteListener.Type.ENABLING_NOTIFICATION, Target.CHARACTERISTIC);
					
					if( earlyOutResult != null )
					{
						ithEntry.m_pollingReadListener.onEvent(earlyOutResult);
					}
					else
					{
						if (m_device.conf_device().autoEnableNotifiesOnReconnect)
						{
							m_device.m_bondMngr.bondIfNeeded(characteristic.getUuid(), CharacteristicEventType.ENABLE_NOTIFY);

							m_device.getManager().getTaskQueue().add(new P_Task_ToggleNotify(m_device, characteristic, /*enable=*/true, null, ithEntry.m_pollingReadListener, m_device.getOverrideReadWritePriority()));

							notifyState = E_NotifyState__ENABLING;
						}
					}
				}
				
				if( notifyState == E_NotifyState__ENABLED && ithEntry.m_notifyState != E_NotifyState__ENABLED )
				{
					ReadWriteEvent result = newAlreadyEnabledEvent(characteristic, ithEntry.m_serviceUuid, ithEntry.m_charUuid, ithEntry.m_descriptorFilter);
					ithEntry.m_pollingReadListener.onEvent(result);
				}
				
				ithEntry.m_notifyState = notifyState;
			}
		}
	}
	
	ReadWriteEvent newAlreadyEnabledEvent(BluetoothGattCharacteristic characteristic, final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter)
	{
		//--- DRK > Just being anal with the null check here.
		byte[] writeValue = characteristic != null ? P_Task_ToggleNotify.getWriteValue(characteristic, /*enable=*/true) : P_Const.EMPTY_BYTE_ARRAY;
		int gattStatus = BluetoothGatt.GATT_SUCCESS;
		ReadWriteEvent result = new ReadWriteEvent(m_device, serviceUuid, characteristicUuid, Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID, descriptorFilter, Type.ENABLING_NOTIFICATION, Target.DESCRIPTOR, writeValue, Status.SUCCESS, gattStatus, 0.0, 0.0, /*solicited=*/true);

		return result;
	}
}
