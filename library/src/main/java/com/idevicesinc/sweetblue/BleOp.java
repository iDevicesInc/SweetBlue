package com.idevicesinc.sweetblue;


import java.util.ArrayList;
import java.util.List;
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

    private List<T> opList = new ArrayList<>();

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

    abstract T createDuplicate();

    abstract T createNewOp();



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

    /**
     * Adds the current Ble operation to a list, and returns a new copy (with all the values the same as the current). This is helpful if you are performing
     * a bunch of reads/writes/notifies where you only need to change one thing for each (for instance, the characteristic UUID).
     */
    public final T next()
    {
        opList.add((T) this);
        return createDuplicate();
    }

    /**
     * Adds the current Ble operation to a list, and returns a new instance for chaining multiple together into a list.
     */
    public final T nextNew()
    {
        opList.add((T) this);
        return createNewOp();
    }

    /**
     * Returns the list of operations. This will be empty if {@link #next()}, or {@link #nextNew()} is never called.
     */
    public final List<T> list()
    {
        return opList;
    }

    /**
     * Same as {@link #list()}, only returns an array instead.
     */
    public final T[] array()
    {
        return (T[]) opList.toArray();
    }

    final T getDuplicateOp()
    {
        BleOp op = createNewOp();
        op.charUuid = charUuid;
        op.serviceUuid = serviceUuid;
        op.readWriteListener = readWriteListener;
        op.descriptorUuid = descriptorUuid;
        op.descriptorFilter = descriptorFilter;
        return (T) op;
    }


}
