package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Wrapper class which holds an instance of {@link BluetoothGattCharacteristic}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattCharacteristic} returned from {@link #getCharacteristic()}.
 */
public final class BleCharacteristicWrapper extends P_NativeGattObject<BluetoothGattCharacteristic>
{

    private BleCharacteristicWrapper()
    {
        super();
    }

    BleCharacteristicWrapper(BluetoothGattCharacteristic characteristic)
    {
        super(characteristic);
    }

    BleCharacteristicWrapper(BleManager.UhOhListener.UhOh uhoh)
    {
        super(uhoh);
    }

    BleCharacteristicWrapper(BluetoothGattCharacteristic characteristic, BleManager.UhOhListener.UhOh uhoh)
    {
        super(characteristic, uhoh);
    }

    /**
     * Returns the instance of {@link BluetoothGattCharacteristic} held in this class.
     */
    public final BluetoothGattCharacteristic getCharacteristic()
    {
        return getGattObject();
    }

    public final static BleCharacteristicWrapper NULL = new BleCharacteristicWrapper();

}
