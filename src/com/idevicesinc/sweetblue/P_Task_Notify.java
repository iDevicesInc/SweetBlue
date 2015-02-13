package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener;
import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Result;
import com.idevicesinc.sweetblue.PA_Task.I_StateListener;

public class P_Task_Notify extends PA_Task implements I_StateListener {

	private final BleServer m_server;
	private final BluetoothDevice m_device;
	
	private final ReadOrWriteRequestListener m_notifyListener;
	private final byte[] m_data;
	
	private final BluetoothGattCharacteristic m_characteristic;
	private final boolean m_confirm;
	public P_Task_Notify( BleServer server, BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] data, ReadOrWriteRequestListener statusListener, I_StateListener stateListener, boolean confirm ) {
		super( server, stateListener );
		m_server = server;
		m_device = device;
		m_notifyListener = statusListener;
		m_data = data;
		m_characteristic = characteristic;
		m_confirm = confirm;
	}

	@Override
	public void onStateChange( PA_Task task, PE_TaskState state ) {
		// TODO Auto-generated method stub

	}

	protected Result newResult( Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid ) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void execute() {
		if( !m_characteristic.setValue(m_data) )
		{
			fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, Result.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
			
			return;
		}
		if( !getServer().getNative().notifyCharacteristicChanged( m_device, m_characteristic, m_confirm ) )
		{
			fail(Status.FAILED_TO_SEND_OUT, Result.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);
			
			return;
		}
	}
	
	public PE_TaskPriority getPriority() {
		return PE_TaskPriority.FOR_NORMAL_READS_WRITES;
	}
	protected void fail(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid)
	{
		if( m_notifyListener != null )
		{
			m_notifyListener.onNotificationSent( newResult(status, gattStatus, target, charUuid, descUuid) );
		}
		
		this.fail();
	}
	public ReadOrWriteRequestListener getListener() {
		return m_notifyListener;
	}
}
