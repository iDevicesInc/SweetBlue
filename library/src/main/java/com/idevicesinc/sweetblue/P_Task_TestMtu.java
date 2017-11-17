package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Utils;
import java.util.UUID;


class P_Task_TestMtu extends PA_Task_ReadOrWrite
{

    private final byte[] m_data;
    private final BleDevice.ReadWriteListener.Type m_writeType;


    P_Task_TestMtu(BleDevice device, UUID serviceUuid, UUID charUuid, DescriptorFilter filter, final FutureData futureData, boolean requiresBonding, BleDevice.ReadWriteListener.Type writeType, BleDevice.ReadWriteListener writeListener, BleTransaction txn, PE_TaskPriority priority)
    {
        super(device, serviceUuid, charUuid, requiresBonding, txn, priority, filter, writeListener);

        m_data = futureData.getData();

        m_writeType = writeType;
    }

    @Override
    protected final BleDevice.ReadWriteListener.ReadWriteEvent newReadWriteEvent(BleDevice.ReadWriteListener.Status status, int gattStatus, BleDevice.ReadWriteListener.Target target, UUID serviceUuid, UUID charUuid, UUID descUuid)
    {
        final BleCharacteristicWrapper char_native = getDevice().getNativeBleCharacteristic(serviceUuid, charUuid);
        final BleDevice.ReadWriteListener.Type type = P_DeviceServiceManager.modifyResultType(char_native, BleDevice.ReadWriteListener.Type.WRITE);
        final UUID actualDescUuid = getActualDescUuid(descUuid);

        return new BleDevice.ReadWriteListener.ReadWriteEvent(getDevice(), serviceUuid, charUuid, actualDescUuid, m_descriptorFilter, type, target, m_data, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
    }


    @Override protected void executeReadOrWrite()
    {
        if( false == write_earlyOut(m_data) )
        {
            final BluetoothGattCharacteristic char_native = getFilteredCharacteristic() != null ? getFilteredCharacteristic() : getDevice().getNativeCharacteristic(getServiceUuid(), getCharUuid());

            if( char_native == null )
            {
                fail(BleDevice.ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
            }
            else
            {
                // Set the write type now, if it is not null
                if (m_writeType != null)
                {
                    if (m_writeType == BleDevice.ReadWriteListener.Type.WRITE_NO_RESPONSE)
                    {
                        char_native.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    }
                    else if (m_writeType == BleDevice.ReadWriteListener.Type.WRITE_SIGNED)
                    {
                        char_native.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_SIGNED);
                    }
                    else if (char_native.getWriteType() != BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    {
                        char_native.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    }
                }

                if( false == getDevice().layerManager().setCharValue(char_native, m_data) )
                {
                    fail(BleDevice.ReadWriteListener.Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
                }
                else
                {
                    if( false == getDevice().layerManager().writeCharacteristic(char_native) )
                    {
                        fail(BleDevice.ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
                    }
                    else
                    {
                        // SUCCESS, for now...
                    }
                }
            }
        }
    }

    public void onCharacteristicWrite(final BluetoothGatt gatt, final UUID uuid, final int gattStatus)
    {
        getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));

        if( false == this.isForCharacteristic(uuid) )  return;

        if( false == acknowledgeCallback(gattStatus) )  return;

        if( Utils.isSuccess(gattStatus) )
        {
            succeedWrite();
        }
        else
        {
            fail(BleDevice.ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), uuid, BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
        }
    }

    @Override public void onStateChange(final PA_Task task, final PE_TaskState state)
    {
        super.onStateChange(task, state);

        if( state == PE_TaskState.TIMED_OUT )
        {
            getLogger().w(getLogger().charName(getCharUuid()) + " write timed out!");

            getDevice().invokeReadWriteCallback(m_readWriteListener, newReadWriteEvent(BleDevice.ReadWriteListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getServiceUuid(), getCharUuid(), BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID));

            getManager().uhOh(BleManager.UhOhListener.UhOh.WRITE_MTU_TEST_TIMED_OUT);
        }
        else if( state == PE_TaskState.SOFTLY_CANCELLED )
        {
            getDevice().invokeReadWriteCallback(m_readWriteListener, newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getServiceUuid(), getCharUuid(), BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID));
        }
    }

    @Override protected BleTask getTaskType()
    {
        return BleTask.WRITE;
    }

    @Override
    protected BleDevice.ReadWriteListener.Target getDefaultTarget()
    {
        return BleDevice.ReadWriteListener.Target.CHARACTERISTIC_TEST_MTU;
    }
}
