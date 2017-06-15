package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


final class P_Task_BatteryLevel extends PA_Task_ReadOrWrite
{

    private List<BluetoothGattCharacteristic> batteryChars;


    P_Task_BatteryLevel(BleDevice device, final byte[] nameSpaceAndDescription, final UUID descriptorUuid, BleDevice.ReadWriteListener readWriteListener, boolean requiresBonding, BleTransaction txn_nullable, PE_TaskPriority priority)
    {
        super(device, Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, requiresBonding, txn_nullable, priority, new DescriptorFilter()
        {
            @Override public Please onEvent(DescriptorEvent event)
            {
                return Please.acceptIf(Arrays.equals(event.value(), nameSpaceAndDescription));
            }

            @Override public UUID descriptorUuid()
            {
                return descriptorUuid;
            }
        }, readWriteListener);
    }

    @Override protected BleDevice.ReadWriteListener.ReadWriteEvent newReadWriteEvent(BleDevice.ReadWriteListener.Status status, int gattStatus, BleDevice.ReadWriteListener.Target target, UUID serviceUuid, UUID charUuid, UUID descUuid)
    {
        final UUID actualDescUuid = getActualDescUuid(descUuid);
        return new BleDevice.ReadWriteListener.ReadWriteEvent(getDevice(), serviceUuid, charUuid, actualDescUuid, m_descriptorFilter, BleDevice.ReadWriteListener.Type.READ, target, P_Const.EMPTY_BYTE_ARRAY, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), true);
    }

    @Override protected BleTask getTaskType()
    {
        return BleTask.READ;
    }

    @Override public void update(double timeStep)
    {
        // We don't let the kick off bond event happen here, as it's not needed for this
    }

    @Override protected void executeReadOrWrite()
    {
        BluetoothGattCharacteristic characteristic = getFilteredCharacteristic();
        if (characteristic != null)
        {
            if (false == getDevice().layerManager().readCharacteristic(characteristic))
            {
                fail(BleDevice.ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDevice.ReadWriteListener.Target.DESCRIPTOR, characteristic.getUuid(), BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
            }
            else
            {
                // SUCCESS for now, must wait for callback to return
            }
        }
    }

    public void onCharacteristicRead(BluetoothGatt gatt, UUID uuid, byte[] value, int gattStatus)
    {
        getManager().ASSERT(getDevice().layerManager().gattEquals(gatt));

        if( false == this.isForCharacteristic(uuid) )  return;

        onCharacteristicOrDescriptorRead(gatt, uuid, value, gattStatus, BleDevice.ReadWriteListener.Type.READ);
    }

}
