package com.idevicesinc.sweetblue;


import java.util.UUID;


/**
 * Base class for basic BLE operations (read, write, notify).
 *
 * This class is parameterized so that the shared methods of this class get auto casted to the parent class (eg. BleWrite, BleRead, BleNotify), so when chaining,
 * you have access to the parent's methods that aren't contained in this one.
 */
public abstract class BleOp<T extends BleOp>
{

    final static String TAG = BleOp.class.getSimpleName();

    UUID serviceUuid = null;
    UUID charUuid = null;
    UUID descriptorUuid = null;
    ReadWriteListener readWriteListener = null;
    DescriptorFilter descriptorFilter = null;



    public BleOp()
    {
    }

    public BleOp(UUID serviceUuid, UUID characteristicUuid)
    {
        this.serviceUuid = serviceUuid;
        this.charUuid = characteristicUuid;
    }

    public BleOp(UUID characteristicUuid)
    {
        this(null, characteristicUuid);
    }


    /**
     * Returns <code>true</code> if the minimum values have been set for this operation
     */
    public abstract boolean isValid();



    /**
     * Set the service UUID for this operation. This is only needed when you have characteristics with identical uuids under different services.
     */
    public final T setServiceUUID(UUID uuid)
    {
        serviceUuid = uuid;
        return (T) this;
    }

    /**
     * Set the characteristic UUID.
     */
    public final T setCharacteristicUUID(UUID uuid)
    {
        charUuid = uuid;
        return (T) this;
    }

    /**
     * Set the descriptor UUID (if operating with a descriptor).
     */
    public final T setDescriptorUUID(UUID uuid)
    {
        descriptorUuid = uuid;
        return (T) this;
    }

    /**
     * Set the {@link ReadWriteListener} for listening to the callback of the operation you wish to perform.
     */
    public final T setReadWriteListener(final ReadWriteListener listener)
    {
        readWriteListener = listener;
        return (T) this;
    }

    /**
     * Set the {@link DescriptorFilter} to determine which characteristic to operate on, if there are multiple with the same {@link UUID} in the same
     * {@link android.bluetooth.BluetoothGattService}.
     */
    public final T setDescriptorFilter(DescriptorFilter filter)
    {
        descriptorFilter = filter;
        return (T) this;
    }


}
