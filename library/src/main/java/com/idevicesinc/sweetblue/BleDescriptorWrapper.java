package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattDescriptor;


/**
 * Wrapper class which holds an instance of {@link BluetoothGattDescriptor}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattDescriptor} returned from {@link #getDescriptor()}.
 */
public final class BleDescriptorWrapper extends P_NativeGattObject<BluetoothGattDescriptor>
{


    private BleDescriptorWrapper()
    {
        super(null, null);
    }

    BleDescriptorWrapper(BluetoothGattDescriptor descriptor)
    {
        super(descriptor);
    }

    BleDescriptorWrapper(BleManager.UhOhListener.UhOh uhOh)
    {
        super(uhOh);
    }

    BleDescriptorWrapper(BluetoothGattDescriptor descriptor, BleManager.UhOhListener.UhOh uhoh)
    {
        super(descriptor, uhoh);
    }

    /**
     * Returns the instance of {@link BluetoothGattDescriptor} held in this class.
     */
    public final BluetoothGattDescriptor getDescriptor()
    {
        return getGattObject();
    }

    public final static BleDescriptorWrapper NULL = new BleDescriptorWrapper();

}
