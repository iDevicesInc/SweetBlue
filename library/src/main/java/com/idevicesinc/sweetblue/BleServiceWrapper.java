package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattService;


/**
 * Wrapper class which holds an instance of {@link BluetoothGattService}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattService} returned from {@link #getService()}.
 */
public final class BleServiceWrapper extends P_NativeGattObject<BluetoothGattService>
{

    private BleServiceWrapper()
    {
        super();
    }

    BleServiceWrapper(BleManager.UhOhListener.UhOh uhoh)
    {
        super(null, uhoh);
    }

    BleServiceWrapper(BluetoothGattService service)
    {
        super(service, null);
    }

    BleServiceWrapper(BluetoothGattService service, BleManager.UhOhListener.UhOh uhoh)
    {
        super(service, uhoh);
    }

    /**
     * Returns the instance of {@link BluetoothGattService} held in this class.
     */
    public final BluetoothGattService getService()
    {
        return getGattObject();
    }


    public final static BleServiceWrapper NULL = new BleServiceWrapper();

}
