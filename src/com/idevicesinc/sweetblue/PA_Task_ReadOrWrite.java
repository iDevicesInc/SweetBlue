package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;

abstract class PA_Task_ReadOrWrite extends PA_Task_Transactionable
{
	protected final P_Characteristic m_characteristic;
	protected final P_WrappingReadWriteListener m_readWriteListener;
	
	PA_Task_ReadOrWrite(P_Characteristic characteristic, P_WrappingReadWriteListener readWriteListener, boolean requiresBonding, BleTransaction txn_nullable, PE_TaskPriority priority)
	{
		super(characteristic.getDevice(), txn_nullable, requiresBonding, priority);
		
		m_characteristic = characteristic;
		m_readWriteListener = readWriteListener;
	}
	
	protected abstract ReadWriteEvent newResult(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid);
	
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
		getDevice().invokeReadWriteCallback(m_readWriteListener, newResult(status, gattStatus, target, charUuid, descUuid));
		
		this.fail();
	}
	
	@Override protected boolean isExecutable()
	{
		boolean super_isExecutable = super.isExecutable();
		
		if( !super_isExecutable )
		{
			getDevice().invokeReadWriteCallback(m_readWriteListener, newResult(Status.NOT_CONNECTED, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_characteristic.getUuid(), getDescriptorUuid()));
		}
		
		return super_isExecutable;
	}
	
	protected boolean acknowledgeCallback(int status)
	{
		 //--- DRK > As of now, on the nexus 7, if a write requires authentication, it kicks off a bonding process
		 //---		 and we don't get a callback for the write (android bug), so we let this write task be interruptible
		 //---		 by an implicit bond task. If on other devices we *do* get a callback, we ignore it so that this
		 //---		 library's logic always follows the lowest common denominator that is the nexus 7.
		 if( status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION || status == PS_GattStatus.UNKNOWN_STATUS_AFTER_GATT_INSUFFICIENT_AUTHENTICATION )
		 {
			 return false;
		 }
		 
		 return true;
	}
	
	@Override protected UUID getCharUuid()
	{
		return m_characteristic.getUuid();
	}
	
	protected boolean isFor(UUID uuid)
	{
		return uuid.equals(m_characteristic.getUuid());
	}
	
	@Override protected String getToStringAddition()
	{
		return getManager().getLogger().uuidName(m_characteristic.getUuid());
	}
}
