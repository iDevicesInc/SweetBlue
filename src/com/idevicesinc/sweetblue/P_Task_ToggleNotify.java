package com.idevicesinc.sweetblue;

import java.util.UUID;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.utils.Utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_Task_ToggleNotify extends PA_Task_ReadOrWrite implements PA_Task.I_StateListener
{
	private final boolean m_enable;
	
	public P_Task_ToggleNotify(P_Characteristic characteristic, boolean enable)
	{
		this(characteristic, enable, null);
	}
	
	public P_Task_ToggleNotify(P_Characteristic characteristic, boolean enable, PE_TaskPriority priority)
	{
		super(characteristic, null, false, null, priority);
		
		m_enable = enable;
	}

	@Override public void execute()
	{
		BluetoothGattCharacteristic char_native = m_characteristic.getGuaranteedNative();
		
		if( char_native == null )
		{
			this.fail();  return;
		}
		
		if( !getDevice().getGatt().setCharacteristicNotification(char_native, m_enable) )
		{
			this.fail();  return;
		}
		
		BluetoothGattDescriptor descriptor = char_native.getDescriptor(Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID);
		
		if( descriptor == null )
		{
			this.fail();  return;
		}
		
		if( !descriptor.setValue(m_enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) )
		{
			this.fail();  return;
		}
		
		if( !getDevice().getGatt().writeDescriptor(descriptor) )
		{
			this.fail();  return;
		}
	}
	
	public void onDescriptorWrite(BluetoothGatt gatt, UUID uuid, int status)
	{
		 getManager().ASSERT(gatt == getDevice().getGatt());
		 
		 if( !uuid.equals(Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID) )  return;

		 if( Utils.isSuccess(status) )
		 {
			succeed();
		 }
		 else
		 {
			 fail();
		 }
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
	}
	
	@Override public boolean isMoreImportantThan(PA_Task task)
	{
		return isMoreImportantThan_default(task);
	}

	@Override
	protected Result newResult(Status status)
	{
		return null;
	}
}
