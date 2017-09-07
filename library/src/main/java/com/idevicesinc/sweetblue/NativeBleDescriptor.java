package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattDescriptor;


/**
 * Wrapper class which holds an instance of {@link BluetoothGattDescriptor}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattDescriptor} returned from {@link #getDescriptor()}.
 */
public final class NativeBleDescriptor extends P_NativeGattObject<BluetoothGattDescriptor>
{


    private NativeBleDescriptor()
    {
        super(null, null);
    }

    NativeBleDescriptor(BluetoothGattDescriptor descriptor)
    {
        super(descriptor);
    }

    NativeBleDescriptor(BleManager.UhOhListener.UhOh uhOh)
    {
        super(uhOh);
    }

    NativeBleDescriptor(BluetoothGattDescriptor descriptor, BleManager.UhOhListener.UhOh uhoh)
    {
        super(descriptor, uhoh);
    }

    /**
     * Returns the instance of {@link BluetoothGattDescriptor} held in this class.
     */
    public BluetoothGattDescriptor getDescriptor()
    {
        return getGattObject();
    }

    public final static NativeBleDescriptor NULL = new NativeBleDescriptor();

}
