package com.idevicesinc.sweetblue;

import android.bluetooth.le.AdvertiseCallback;

import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Provide an implementation to {@link BleServer#setListener_Advertising(AdvertisingListener)}, and
 * {@link BleManager#setListener_Advertising(AdvertisingListener)} to receive a callback
 * when using {@link BleServer#startAdvertising(BleAdvertisingPacket)}.
 */
public interface AdvertisingListener
{

    /**
     * Enumeration describing the m_status of calling {@link BleServer#startAdvertising(BleAdvertisingPacket)}.
     */
    enum Status implements UsesCustomNull
    {
        SUCCESS(BleStatuses.ADVERTISE_SUCCESS),
        DATA_TOO_LARGE(AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE),
        TOO_MANY_ADVERTISERS(AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS),
        ALREADY_STARTED(AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED),
        INTERNAL_ERROR(AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR),
        ANDROID_VERSION_NOT_SUPPORTED(BleStatuses.ADVERTISE_ANDROID_VERSION_NOT_SUPPORTED),
        CHIPSET_NOT_SUPPORTED(AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED),
        BLE_NOT_ON(-1),
        NULL_SERVER(-2),
        NULL(-3);

        private final int m_nativeStatus;

        Status(int nativeStatus)
        {
            m_nativeStatus = nativeStatus;
        }

        public final int getNativeStatus()
        {
            return m_nativeStatus;
        }

        public static Status fromNativeStatus(int bit)
        {
            for (Status res : values())
            {
                if (res.m_nativeStatus == bit)
                {
                    return res;
                }
            }
            return SUCCESS;
        }

        @Override
        public final boolean isNull() {
            return this == NULL;
        }
    }

    /**
     * Sub class representing the Advertising Event
     */
    class AdvertisingEvent extends Event implements UsesCustomNull
    {
        private final BleServer m_server;
        private final Status m_status;

        AdvertisingEvent(BleServer server, Status m_status)
        {
            m_server = server;
            this.m_status = m_status;
        }

        /**
         * The backing {@link BleManager} which is attempting to start advertising.
         */
        public final BleServer server()
        {
            return m_server;
        }

        /**
         * Whether or not {@link BleServer#startAdvertising(BleAdvertisingPacket)} was successful or not. If false,
         * then call {@link #m_status} to get the error code.
         */
        public final boolean wasSuccess()
        {
            return m_status == Status.SUCCESS;
        }

        /**
         * Returns {@link Status} describing
         * the m_status of calling {@link BleServer#startAdvertising(BleAdvertisingPacket)}
         */
        public final Status status()
        {
            return m_status;
        }

        @Override
        public final boolean isNull() {
            return status() == Status.NULL;
        }

        @Override
        public final String toString() {
            return Utils_String.toString(this.getClass(),
                    "server", server().getClass().getSimpleName(),
                    "status", status());
        }
    }

    /**
     * Called upon the m_status of calling {@link BleServer#startAdvertising(BleAdvertisingPacket)}
     */
    void onEvent(AdvertisingEvent e);

}
