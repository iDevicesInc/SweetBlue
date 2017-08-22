package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Uuids;

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
    FutureData m_data = P_Const.EMPTY_FUTURE_DATA;



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



    final boolean isRead()
    {
        return this instanceof BleRead;
    }

    final boolean isWrite()
    {
        return this instanceof BleWrite;
    }

    final boolean isNotify()
    {
        return this instanceof BleNotify;
    }

    final boolean isServiceUuidValid()
    {
        return serviceUuid != null && !serviceUuid.equals(Uuids.INVALID);
    }

    final boolean isCharUuidValid()
    {
        return charUuid != null && !charUuid.equals(Uuids.INVALID);
    }


    static BleOp createOp(UUID serviceUuid, UUID charUuid, UUID descUuid, DescriptorFilter filter, byte[] data, ReadWriteListener.Type type)
    {
        BleOp op;
        switch (type)
        {
            case WRITE:
            case WRITE_NO_RESPONSE:
            case WRITE_SIGNED:
                op = new BleWrite(serviceUuid, charUuid).setWriteType(type);
                break;
            case NOTIFICATION:
            case DISABLING_NOTIFICATION:
            case ENABLING_NOTIFICATION:
            case PSUEDO_NOTIFICATION:
                op = new BleNotify(serviceUuid, charUuid);
                break;
            default:
                op = new BleRead(serviceUuid, charUuid);
        }
        op.m_data = new PresentData(data);
        return op.setDescriptorFilter(filter).setDescriptorUUID(descUuid);
    }


    static class Builder<B extends Builder, T extends BleOp>
    {
        private List<T> readList = new ArrayList<>();

        T currentOp;
        private T lastRead = null;


        /**
         * Set the service UUID for this operation. This is only needed when you have characteristics with identical uuids under different services.
         */
        public final B setServiceUUID(UUID uuid)
        {
            currentOp.setServiceUUID(uuid);
            return (B) this;
        }

        /**
         * Set the characteristic UUID.
         */
        public final B setCharacteristicUUID(UUID uuid)
        {
            currentOp.setCharacteristicUUID(uuid);
            return (B) this;
        }

        /**
         * Set the descriptor UUID (if operating with a descriptor).
         */
        public final B setDescriptorUUID(UUID uuid)
        {
            currentOp.setDescriptorUUID(uuid);
            return (B) this;
        }

        /**
         * Set the {@link ReadWriteListener} for listening to the callback of the operation you wish to perform.
         */
        public final B setReadWriteListener(final ReadWriteListener listener)
        {
            currentOp.setReadWriteListener(listener);
            return (B) this;
        }

        /**
         * Set the {@link DescriptorFilter} to determine which characteristic to operate on, if there are multiple with the same {@link UUID} in the same
         * {@link android.bluetooth.BluetoothGattService}.
         */
        public final B setDescriptorFilter(DescriptorFilter filter)
        {
            currentOp.setDescriptorFilter(filter);
            return (B) this;
        }

        /**
         * Move on to another {@link BleRead} instance, based on the last one (so if you only need to change the char UUID, you don't have to set all
         * other fields again).
         */
        public final B next()
        {
            if (currentOp != lastRead)
            {
                readList.add(currentOp);
                lastRead = currentOp;
                currentOp = (T) currentOp.createDuplicate();
            }
            return (B) this;
        }

        /**
         * Move on to another {@link BleRead} instance, with the new instance having no fields set.
         */
        public final B nextNew()
        {
            if (currentOp != lastRead)
            {
                readList.add(currentOp);
                lastRead = currentOp;
                currentOp = (T) currentOp.createNewOp();
            }
            return (B) this;
        }

        /**
         * Builds, and returns the list of {@link BleRead}s.
         */
        public final List<T> build()
        {
            if (currentOp != lastRead)
            {
                readList.add(currentOp);
                lastRead = currentOp;
            }
            return readList;
        }

        /**
         * Same as {@link #build()}, only returns an array instead of a list.
         */
        public final T[] buildArray()
        {
            return (T[]) build().toArray();
        }
    }

    final T getDuplicateOp()
    {
        BleOp op = createNewOp();
        op.charUuid = charUuid;
        op.serviceUuid = serviceUuid;
        op.readWriteListener = readWriteListener;
        op.descriptorUuid = descriptorUuid;
        op.descriptorFilter = descriptorFilter;
        op.opList = opList;
        return (T) op;
    }


}
