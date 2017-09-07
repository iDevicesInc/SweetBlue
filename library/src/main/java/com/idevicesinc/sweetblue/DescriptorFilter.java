package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.UUID;


/**
 * Interface used when the Bluetooth device you're trying to connect to has multiple
 * {@link BluetoothGattCharacteristic}s with the same {@link java.util.UUID}, in the same
 * {@link BluetoothGattService}. You use this interface to decide which {@link BluetoothGattCharacteristic}
 * you want to perform an operation on using any of the read(), or write() methods which accept this
 * Interface as an argument.
 */
public interface DescriptorFilter
{

    /**
     * Event class which is an argument in the {@link #onEvent(DescriptorEvent)} method of the
     * {@link DescriptorFilter} interface. This holds the service, characteristic, and current
     * descriptor, and descriptor value.
     */
    @Immutable
    final class DescriptorEvent extends Event
    {

        private final BluetoothGattService m_service;
        private final BluetoothGattCharacteristic m_char;
        private final BluetoothGattDescriptor m_desc;
        private final FutureData m_value;


        DescriptorEvent(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, FutureData value)
        {
            m_service = service;
            m_char = characteristic;
            m_desc = descriptor;
            m_value = value;
        }

        /**
         * Convenience method to return the value stored in the {@link BluetoothGattDescriptor}. This is what you will check to determine if the
         * current {@link BluetoothGattCharacteristic} is the one you want to operate on.
         */
        public final byte[] value()
        {
            return m_value.getData();
        }

        /**
         * Convenience method which returns the {@link BluetoothGattService}.
         */
        public final @Nullable(Nullable.Prevalence.NEVER) BluetoothGattService service()
        {
            return m_service;
        }

        /**
         * Convenience method which returns the current {@link BluetoothGattCharacteristic}.
         *
         * @deprecated - Method name is misspelled, but left in here so as to not break any current implementations.
         */
        @Deprecated
        public final @Nullable(Nullable.Prevalence.NEVER) BluetoothGattCharacteristic characteristc()
        {
            return m_char;
        }

        /**
         * Convenience method which returns the current {@link BluetoothGattCharacteristic}.
         */
        public final @Nullable(Nullable.Prevalence.NEVER) BluetoothGattCharacteristic characteristic()
        {
            return m_char;
        }

        /**
         * Convenience method which returns the {@link BluetoothGattDescriptor}. If {@link #descriptorUuid()} is returning <code>null</code>,
         * then this will always be <code>null</code> as well.
         */
        public final @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor descriptor()
        {
            return m_desc;
        }

    }

    /**
     * Class used to dictate if the current {@link BluetoothGattCharacteristic} is the correct one to operate on
     * by looking at it's {@link BluetoothGattDescriptor} value.
     */
    final class Please
    {

        private final boolean m_accept;

        private Please(boolean accept)
        {
            m_accept = accept;
        }


        boolean isAccepted()
        {
            return m_accept;
        }

        /**
         * Accept the current {@link BluetoothGattCharacteristic} as the one to perform the operation on.
         */
        public static Please accept()
        {
            return new Please(true);
        }

        /**
         * Similar to {@link #accept()}, allowing you to pass in a boolean conditional to specify whether
         * to accept the current {@link BluetoothGattCharacteristic}.
         */
        public static Please acceptIf(boolean condition)
        {
            return new Please(condition);
        }

        /**
         * Opposite of {@link #accept()}.
         */
        public static Please deny()
        {
            return new Please(false);
        }

        /**
         * Opposite of {@link #acceptIf(boolean)}.
         */
        public static Please denyIf(boolean condition)
        {
            return new Please(!condition);
        }

    }

    /**
     * Method called when trying to determine which {@link BluetoothGattCharacteristic} to perform the operation on.
     */
    Please onEvent(DescriptorEvent event);

    /**
     * Return the {@link UUID} of the descriptor you want to read to distinguish which {@link BluetoothGattCharacteristic} you would
     * like to perform an op on.
     */
    UUID descriptorUuid();

}
