package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;

/**
 * Wrapper class which holds an instance of {@link BluetoothGattCharacteristic}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattCharacteristic} returned from {@link #getCharacteristic()}.
 */
final class NativeBleCharacteristic implements UsesCustomNull
{

    final BluetoothGattCharacteristic m_characteristic;
    final BleManager.UhOhListener.UhOh m_uhOh;


    NativeBleCharacteristic()
    {
        this(null, null);
    }

    NativeBleCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        this(characteristic, null);
    }

    NativeBleCharacteristic(BleManager.UhOhListener.UhOh uhoh)
    {
        this(null, uhoh);
    }

    NativeBleCharacteristic(BluetoothGattCharacteristic characteristic, BleManager.UhOhListener.UhOh uhoh)
    {
        m_characteristic = characteristic;
        m_uhOh = uhoh;
    }

    /**
     * Returns the instance of {@link BluetoothGattCharacteristic} held in this class.
     */
    public BluetoothGattCharacteristic getCharacteristic()
    {
        return m_characteristic;
    }

    /**
     * Mostly used internally, but if there was a particular issue when retrieving a characteristic, it will have an {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh}
     * with a status of what went wrong.
     */
    public BleManager.UhOhListener.UhOh getUhOh()
    {
        return m_uhOh;
    }

    /**
     * Returns <code>true</code> if the {@link BluetoothGattCharacteristic} held in this class is <code>null</code> or not.
     */
    @Override
    public boolean isNull()
    {
        return m_characteristic == null;
    }
}
