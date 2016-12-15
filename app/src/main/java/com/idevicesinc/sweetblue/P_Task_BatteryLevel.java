package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public final class P_Task_BatteryLevel extends PA_Task_ReadOrWrite
{

    private final byte[] mValueToMatch;
    private List<BluetoothGattCharacteristic> batteryChars;
    private UUID mDescriptorUuid;


    P_Task_BatteryLevel(BleDevice device, byte[] nameSpaceAndDescription, UUID descriptorUuid, BleDevice.ReadWriteListener readWriteListener, boolean requiresBonding, BleTransaction txn_nullable, PE_TaskPriority priority)
    {
        super(device, Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, readWriteListener, requiresBonding, txn_nullable, priority);
        mValueToMatch = nameSpaceAndDescription;
        mDescriptorUuid = descriptorUuid;
    }

    @Override protected BleDevice.ReadWriteListener.ReadWriteEvent newReadWriteEvent(BleDevice.ReadWriteListener.Status status, int gattStatus, BleDevice.ReadWriteListener.Target target, UUID serviceUuid, UUID charUuid, UUID descUuid)
    {
        return new BleDevice.ReadWriteListener.ReadWriteEvent(getDevice(), serviceUuid, charUuid, descUuid, BleDevice.ReadWriteListener.Type.READ, target, BleDevice.EMPTY_BYTE_ARRAY, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), true);
    }

    @Override protected BleTask getTaskType()
    {
        return BleTask.READ;
    }

    @Override public void update(double timeStep)
    {
        // We don't let the kick off bond event happen here, as it's not needed for this
    }

    @Override public void execute()
    {
        super.execute();

        // This task assumes there are multiple battery characteristics under the same battery service. In order to tell them apart, the bluetooth spec says to read
        // the descriptors of said characteristics, and compare the namespace and description. So, we first get all battery chars, and put them into a list. Then we get
        // the descriptor of the first item in the list, and try to read it. If the namespace and description match, we read that descriptor's characteristic for the battery level.
        // If not, we move on to the next char, and read it's descriptor and resume the same process until we either find it, or don't.

        List<BluetoothGattCharacteristic> charList = getDevice().getNativeCharacteristics_List(getServiceUuid());
        batteryChars = new ArrayList<>();
        if (charList != null)
        {
            int size = charList.size();
            for (int i = 0; i < size; i++)
            {
                BluetoothGattCharacteristic ch = charList.get(i);
                if (ch.getUuid().equals(getCharUuid()))
                {
                    batteryChars.add(ch);
                }
            }
            size = batteryChars.size();
            if (size == 0)
            {
                fail(BleDevice.ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDevice.ReadWriteListener.Target.CHARACTERISTIC, Uuids.BATTERY_LEVEL, mDescriptorUuid);
            }
            final BluetoothGattCharacteristic ch = batteryChars.get(0);
            final BluetoothGattDescriptor desc = ch.getDescriptor(mDescriptorUuid);
            if (desc == null)
            {
                fail(BleDevice.ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDevice.ReadWriteListener.Target.CHARACTERISTIC, Uuids.BATTERY_LEVEL, mDescriptorUuid);
            }
            else
            {
                if (!getDevice().getNativeGatt().readDescriptor(desc))
                {
                    fail(BleDevice.ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDevice.ReadWriteListener.Target.DESCRIPTOR, ch.getUuid(), desc.getUuid());
                }
                else
                {
                    // Wait for the descriptor read callback
                }
            }
        }
        else
        {
            fail(BleDevice.ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDevice.ReadWriteListener.Target.CHARACTERISTIC, Uuids.BATTERY_LEVEL, mDescriptorUuid);
        }
    }

    public void onCharacteristicRead(BluetoothGatt gatt, UUID uuid, byte[] value, int gattStatus)
    {
        getManager().ASSERT(gatt == getDevice().getNativeGatt());

        if( false == this.isForCharacteristic(uuid) )  return;

        onCharacteristicOrDescriptorRead(gatt, uuid, value, gattStatus, BleDevice.ReadWriteListener.Type.READ);
    }

    public void onDescriptorRead(BluetoothGattDescriptor desc, byte[] value, int gattStatus)
    {
        if (!batteryChars.contains(desc.getCharacteristic()))
        {
            return;
        }

        if( Utils.isSuccess(gattStatus))
        {
            boolean bothNull = value == null && mValueToMatch == null;
            if (bothNull || Arrays.equals(value, mValueToMatch))
            {
                if (false == getDevice().getNativeGatt().readCharacteristic(desc.getCharacteristic()))
                {
                    fail(BleDevice.ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDevice.ReadWriteListener.Target.DESCRIPTOR, desc.getCharacteristic().getUuid(), BleDevice.ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
                }
                else
                {
                    // SUCCESS for now, must wait for callback to return
                }
            }
            else
            {
                batteryChars.remove(desc.getCharacteristic());
                if (batteryChars.size() == 0)
                {
                    fail(BleDevice.ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDevice.ReadWriteListener.Target.DESCRIPTOR, desc.getCharacteristic().getUuid(), desc.getUuid());
                }
                else
                {
                    final BluetoothGattCharacteristic ch = batteryChars.get(0);
                    final BluetoothGattDescriptor descr = ch.getDescriptor(mDescriptorUuid);
                    if (!getDevice().getNativeGatt().readDescriptor(descr))
                    {
                        fail(BleDevice.ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDevice.ReadWriteListener.Target.DESCRIPTOR, ch.getUuid(), descr.getUuid());
                    }
                    else
                    {
                        // SUCCESS for now until the descriptor read comes back, and we can compare it to the given namespaceanddescription
                    }
                }
            }
        }
        else
        {
            fail(BleDevice.ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());
        }
    }

}
