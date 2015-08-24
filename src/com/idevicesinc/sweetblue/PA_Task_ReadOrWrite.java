package com.idevicesinc.sweetblue;

import java.lang.reflect.Field;
import java.util.UUID;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;

abstract class PA_Task_ReadOrWrite extends PA_Task_Transactionable implements PA_Task.I_StateListener
{
	private static final String FIELD_NAME_AUTH_RETRY = "mAuthRetry";
	
	protected final P_Characteristic m_characteristic;
	protected final ReadWriteListener m_readWriteListener;
	
	private Boolean m_authRetryValue_onExecute = null;
	private boolean m_triedToKickOffBond = false;
	
	PA_Task_ReadOrWrite(BleDevice device, P_Characteristic characteristic, ReadWriteListener readWriteListener, boolean requiresBonding, BleTransaction txn_nullable, PE_TaskPriority priority)
	{
		super(device, txn_nullable, requiresBonding, priority);

		m_characteristic = characteristic;
		m_readWriteListener = readWriteListener;
	}
	
	protected abstract ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, UUID serviceUuid, UUID charUuid, UUID descUuid);
	
	//--- DRK > Will have to be overridden in the future if we decide to support descriptor reads/writes.
	protected Target getDefaultTarget()
	{
		return Target.CHARACTERISTIC;
	}
	
	//--- DRK > Will have to be overridden by read/write tasks if we ever support direct descriptor operations.
	protected UUID getDescriptorUuid()
	{
		return ReadWriteEvent.NON_APPLICABLE_UUID;
	}
	
	protected void fail(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid)
	{
		this.fail();

		getDevice().invokeReadWriteCallback(m_readWriteListener, newReadWriteEvent(status, gattStatus, target, getServiceUuid(), charUuid, descUuid));
	}

	@Override protected void onNotExecutable()
	{
		super.onNotExecutable();

		final ReadWriteEvent event = newReadWriteEvent(Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getServiceUuid(), getCharUuid(), getDescriptorUuid());

		getDevice().invokeReadWriteCallback(m_readWriteListener, event);
	}
	
	protected boolean acknowledgeCallback(int status)
	{
		 //--- DRK > As of now, on the nexus 7, if a write requires authentication, it kicks off a bonding process
		 //---		 and we don't get a callback for the write (android bug), so we let this write task be interruptible
		 //---		 by an implicit bond task. If on other devices we *do* get a callback, we ignore it so that this
		 //---		 library's logic always follows the lowest common denominator that is the nexus 7.
		//---		NOTE: Also happens with tab 4, same thing as nexus 7.
		 if( status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION || status == BleStatuses.GATT_AUTH_FAIL )
		 {
			 return false;
		 }
		 
		 return true;
	}
	
	private void checkIfBondingKickedOff()
	{
		if( getState() == PE_TaskState.EXECUTING )
		{
			if( m_triedToKickOffBond == false )
			{
				final Boolean authRetryValue_now = getAuthRetryValue();
				
				if( m_authRetryValue_onExecute != null && authRetryValue_now != null )
				{
					if( m_authRetryValue_onExecute == false && authRetryValue_now == true )
					{
						m_triedToKickOffBond = true;
						
						getManager().getLogger().i("Kicked off bond!");
					}
				}
			}
		}
	}
	
	private boolean triedToKickOffBond()
	{
		return m_triedToKickOffBond;
	}
	
	@Override public void execute()
	{
		m_authRetryValue_onExecute = getAuthRetryValue();
	}
	
	@Override public void update(double timeStep)
	{
		checkIfBondingKickedOff();
	}
	
	private Boolean getAuthRetryValue()
	{
		final BluetoothGatt gatt = getDevice().getNativeGatt();
		
		if( gatt != null )
		{
			try
			{
				final Field[] fields = gatt.getClass().getDeclaredFields();
		        Field field = gatt.getClass().getDeclaredField(FIELD_NAME_AUTH_RETRY);
		        final boolean isAccessible_saved = field.isAccessible();
		        field.setAccessible(true);
		        Boolean result = field.getBoolean(gatt);
		        field.setAccessible(isAccessible_saved);
		        
		        return result;
		    }
			catch (Exception e)
			{
				getManager().ASSERT(false, "Problem getting value of " + gatt.getClass().getSimpleName() + "." + FIELD_NAME_AUTH_RETRY);
		    }
		}
		else
		{
			getManager().ASSERT(false, "Expected gatt object to be not null");
		}
		
		return null;
	}
	
	@Override protected UUID getCharUuid()
	{
		return m_characteristic.getUuid();
	}

	protected UUID getServiceUuid()
	{
		return m_characteristic.getServiceUuid();
	}
	
	protected boolean isFor(UUID uuid)
	{
		return uuid.equals(getCharUuid());
	}
	
	@Override protected String getToStringAddition()
	{
		final String txn = getTxn() != null ? " txn!=null" : " txn==null";
		return getManager().getLogger().uuidName(getCharUuid()) + txn;
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			checkIfBondingKickedOff();
			
			if( triedToKickOffBond() )
			{
				getDevice().notifyOfPossibleImplicitBondingAttempt();
				getDevice().m_bondMngr.saveNeedsBondingIfDesired();
				
				getManager().getLogger().i("Kicked off bond and " + PE_TaskState.TIMED_OUT.name());
			}
		}
	}
}
