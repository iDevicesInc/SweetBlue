package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattDescriptor;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;


/**
 * Wrapper class which holds an instance of {@link BluetoothGattDescriptor}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattDescriptor} returned from {@link #getDescriptor()}.
 */
final class NativeBleDescriptor implements UsesCustomNull
{

    final BluetoothGattDescriptor m_descriptor;
    final BleManager.UhOhListener.UhOh m_uhOh;


    NativeBleDescriptor()
    {
        this(null, null);
    }

    NativeBleDescriptor(BluetoothGattDescriptor descriptor)
    {
        this(descriptor, null);
    }

    NativeBleDescriptor(BleManager.UhOhListener.UhOh uhOh)
    {
        this(null, uhOh);
    }

    NativeBleDescriptor(BluetoothGattDescriptor descriptor, BleManager.UhOhListener.UhOh uhoh)
    {
        m_descriptor = descriptor;
        m_uhOh = uhoh;
    }

    /**
     * Returns the instance of {@link BluetoothGattDescriptor} held in this class.
     */
    public BluetoothGattDescriptor getDescriptor()
    {
        return m_descriptor;
    }

    /**
     * Mostly used internally, but if there was a particular issue when retrieving a descriptor, it will have an {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh}
     * with a status of what went wrong.
     */
    public BleManager.UhOhListener.UhOh getUhOh()
    {
        return m_uhOh;
    }

    /**
     * Returns <code>true</code> if the {@link BluetoothGattDescriptor} held in this class is <code>null</code> or not.
     */
    @Override
    public boolean isNull()
    {
        return m_descriptor == null;
    }
}
