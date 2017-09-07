package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;


public class ReadWriteFailGatt extends UnitTestGatt
{

    private final FailType m_failType;


    public ReadWriteFailGatt(BleDevice device, GattDatabase gattDb)
    {
        this(device, gattDb, FailType.GATT_ERROR, Interval.DISABLED);
    }

    public ReadWriteFailGatt(BleDevice device, GattDatabase gattDb, FailType failType)
    {
        this(device, gattDb, failType, Interval.DISABLED);
    }

    public ReadWriteFailGatt(BleDevice device, GattDatabase gattDb, FailType failType, Interval delayTime)
    {
        super(device, gattDb);
        m_failType = failType;
        setDelayTime(delayTime);
    }

    @Override
    public void sendReadResponse(BluetoothGattCharacteristic characteristic, byte[] data)
    {
        if (m_failType == FailType.GATT_ERROR)
        {
            NativeUtil.readError(getBleDevice(), characteristic, BleStatuses.GATT_ERROR, getDelayTime());
        }
        else
        {
            // If it's a time out, just do nothing
        }
    }

    public enum FailType
    {
        GATT_ERROR,
        TIME_OUT
    }
}
