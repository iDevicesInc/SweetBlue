package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Wrapper class which holds an instance of {@link BluetoothGattCharacteristic}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattCharacteristic} returned from {@link #getCharacteristic()}.
 */
public final class NativeBleCharacteristic extends P_NativeGattObject<BluetoothGattCharacteristic>
{

    private NativeBleCharacteristic()
    {
        super();
    }

    NativeBleCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        super(characteristic);
    }

    NativeBleCharacteristic(BleManager.UhOhListener.UhOh uhoh)
    {
        super(uhoh);
    }

    NativeBleCharacteristic(BluetoothGattCharacteristic characteristic, BleManager.UhOhListener.UhOh uhoh)
    {
        super(characteristic, uhoh);
    }

    /**
     * Returns the instance of {@link BluetoothGattCharacteristic} held in this class.
     */
    public BluetoothGattCharacteristic getCharacteristic()
    {
        return getGattObject();
    }

    public final static NativeBleCharacteristic NULL = new NativeBleCharacteristic();

}
