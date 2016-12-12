package com.idevicesinc.sweetblue;

import java.lang.reflect.Field;
import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.utils.Utils;

abstract class PA_Task_ReadOrWrite extends PA_Task_Transactionable implements PA_Task.I_StateListener
{
	private static final String FIELD_NAME_AUTH_RETRY = "mAuthRetry";
	
	private final UUID m_charUuid;
	private final UUID m_servUuid;

	protected final ReadWriteListener m_readWriteListener;
	
	private Boolean m_authRetryValue_onExecute = null;
	private boolean m_triedToKickOffBond = false;
	
	PA_Task_ReadOrWrite(BleDevice device, BluetoothGattCharacteristic nativeChar, ReadWriteListener readWriteListener, boolean requiresBonding, BleTransaction txn_nullable, PE_TaskPriority priority)
	{
		super(device, txn_nullable, requiresBonding, priority);

		m_charUuid = nativeChar.getUuid();
		m_servUuid = nativeChar.getService().getUuid();

		m_readWriteListener = readWriteListener;
	}

	PA_Task_ReadOrWrite(BleDevice device, UUID serviceUuid, UUID charUuid, ReadWriteListener readWriteListener, boolean requiresBonding, BleTransaction txn_nullable, PE_TaskPriority priority)
	{
		super(device, txn_nullable, requiresBonding, priority);

		m_charUuid = charUuid;
		m_servUuid = serviceUuid;

		m_readWriteListener = readWriteListener;
	}
	
	protected abstract ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, UUID serviceUuid, UUID charUuid, UUID descUuid);

	protected Target getDefaultTarget()
	{
		return Target.CHARACTERISTIC;
	}
	
	protected void fail(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid)
	{
		this.fail();

		getDevice().invokeReadWriteCallback(m_readWriteListener, newReadWriteEvent(status, gattStatus, target, getServiceUuid(), charUuid, descUuid));
	}

	@Override protected void onNotExecutable()
	{
		super.onNotExecutable();

		final ReadWriteEvent event = newReadWriteEvent(Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getServiceUuid(), getCharUuid(), getDescUuid());

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
		return m_charUuid;
	}

	protected UUID getServiceUuid()
	{
		return m_servUuid;
	}

	public boolean isFor(final BluetoothGattCharacteristic characteristic)
	{
		return
				characteristic.getUuid().equals(getCharUuid()) &&
				characteristic.getService().getUuid().equals(getServiceUuid());
	}

	public boolean isFor(final BluetoothGattDescriptor descriptor)
	{
		return descriptor.getUuid().equals(getDescUuid()) && isFor(descriptor.getCharacteristic());
	}
	
	protected boolean isForCharacteristic(UUID uuid)
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

	protected void onCharacteristicOrDescriptorRead(BluetoothGatt gatt, UUID uuid, byte[] value, int gattStatus, ReadWriteListener.Type type)
	{
		getManager().ASSERT(gatt == getDevice().getNativeGatt());

//		if( false == this.isForCharacteristic(uuid) )  return;

		if( false == acknowledgeCallback(gattStatus) )  return;

		if( Utils.isSuccess(gattStatus) )
		{
			if( value != null )
			{
				if( value.length == 0 )
				{
					fail(Status.EMPTY_DATA, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());
				}
				else
				{
					succeedRead(value, getDefaultTarget(), type);
				}
			}
			else
			{
				fail(Status.NULL_DATA, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());

				getManager().uhOh(BleManager.UhOhListener.UhOh.READ_RETURNED_NULL);
			}
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());
		}
	}

	private ReadWriteEvent newSuccessReadWriteEvent(byte[] data, Target target, ReadWriteListener.Type type, UUID charUuid, UUID descUuid)
	{
		return new ReadWriteEvent(getDevice(), getServiceUuid(), charUuid, descUuid, type, target, data, Status.SUCCESS, BluetoothGatt.GATT_SUCCESS, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	private void succeedRead(byte[] value, Target target, ReadWriteListener.Type type)
	{
		super.succeed();

		final ReadWriteEvent event = newSuccessReadWriteEvent(value, target, type, getCharUuid(), getDescUuid());
		getDevice().addReadTime(event.time_total().secs());

		getDevice().invokeReadWriteCallback(m_readWriteListener, event);
	}

	protected void succeedWrite()
	{
		super.succeed();

		final ReadWriteEvent event = newReadWriteEvent(Status.SUCCESS, BluetoothGatt.GATT_SUCCESS, getDefaultTarget(), getServiceUuid(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
		getDevice().addWriteTime(event.time_total().secs());
		getDevice().invokeReadWriteCallback(m_readWriteListener, event);
	}

	protected boolean write_earlyOut(final byte[] data_nullable)
	{
		if( data_nullable == null )
		{
			fail(Status.NULL_DATA, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), getDescUuid());

			return true;
		}
		else if( data_nullable.length == 0 )
		{
			fail(Status.EMPTY_DATA, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), getDescUuid());

			return true;
		}
		else
		{
			return false;
		}
	}
}
