package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.UUID;

/**
 * Provide an implementation of this callback to {@link BleServer#setListener_ServiceAdd(AddServiceListener)}.
 *
 * @see BleServer#setListener_ServiceAdd(AddServiceListener)
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface AddServiceListener
{
    /**
     * Enumeration of the different ways that service addition can fail (and one way for it to succeed),
     * provided through {@link OutgoingListener.OutgoingEvent#status()}.
     */
    enum Status implements UsesCustomNull
    {
        /**
         * Fulfills the soft contract of {@link UsesCustomNull}.
         */
        NULL,

        /**
         * Service was added successfully.
         */
        SUCCESS,

        /**
         * Tried to add a service to {@link BleServer#NULL}.
         */
        NULL_SERVER,

        /**
         * Tried to add the same service reference twice.
         */
        DUPLICATE_SERVICE,

        /**
         * Adding this service required that a native {@link BluetoothGattServer} to be created,
         * but it could not be created for some reason.
         */
        SERVER_OPENING_FAILED,

        /**
         * The call to {@link BluetoothGattServer#addService(BluetoothGattService)} returned <code>false</code>.
         */
        FAILED_IMMEDIATELY,

        /**
         * {@link BluetoothGattServerCallback#onServiceAdded(int, BluetoothGattService)} reported a bad gatt status
         * for the service addition, which is provided through {@link OutgoingListener.OutgoingEvent#gattStatus_received()}.
         */
        FAILED_EVENTUALLY,

        /**
         * Couldn't add the service because the operation took longer than the time dictated by {@link BleNodeConfig#taskTimeoutRequestFilter}.
         */
        TIMED_OUT,

        /**
         * {@link BleServer#removeService(UUID)} or {@link BleServer#removeAllServices()} was called before the service could be fully added.
         */
        CANCELLED_FROM_REMOVAL,

        /**
         * The operation was cancelled because {@link BleServer#disconnect()} was called before the operation completed.
         */
        CANCELLED_FROM_DISCONNECT,

        /**
         * The operation was cancelled because {@link BleManager} went {@link BleManagerState#TURNING_OFF} and/or
         * {@link BleManagerState#OFF}. Note that if the user turns off BLE from their OS settings (airplane mode, etc.) then
         * {@link ServiceAddEvent#status()} could potentially be {@link #CANCELLED_FROM_DISCONNECT} because SweetBlue might get
         * the disconnect callback before the turning off callback. Basic testing has revealed that this is *not* the case, but you never know.
         * <br><br>
         * Either way, the device was or will be disconnected.
         */
        CANCELLED_FROM_BLE_TURNING_OFF,

        /**
         * {@link BleManager} is not {@link BleManagerState#ON} so we can't add a service.
         */
        BLE_NOT_ON;

        /**
         * Returns true if <code>this</code> equals {@link #SUCCESS}.
         */
        public final boolean wasSuccess()
        {
            return this == Status.SUCCESS;
        }

        @Override public final boolean isNull()
        {
            return this == NULL;
        }
    }

    /**
     * Event struct passed to {@link #onEvent(ServiceAddEvent)} to give you information about the success
     * of a service addition or the reason(s) for its failure.
     */
    @Immutable
    class ServiceAddEvent extends Event
    {
        /**
         * The server to which the service is being added.
         */
        public final BleServer server()  {  return m_server;  }
        private final BleServer m_server;

        /**
         * The service being added to {@link #server()}.
         */
        public final BluetoothGattService service()  {  return m_service;  }
        private final BluetoothGattService m_service;

        /**
         * Convenience to return the {@link UUID} of {@link #service()}.
         */
        public final UUID serviceUuid() {  return service().getUuid();  }

        /**
         * Should only be relevant if {@link #status()} is {@link Status#FAILED_EVENTUALLY}.
         */
        public final int gattStatus()  {  return m_gattStatus;  }
        private final int m_gattStatus;

        /**
         * Indicates the success or reason for failure for adding the service.
         */
        public final Status status()  {  return m_status;  }
        private final Status m_status;

        /**
         * This returns <code>true</code> if this event was the result of an explicit call through SweetBlue, e.g. through
         * {@link BleServer#addService(BleService, AddServiceListener)}. It will return <code>false</code> otherwise,
         * which can happen if for example you use {@link BleServer#getNativeLayer()} to bypass SweetBlue for whatever reason.
         * Another theoretical case is if you make an explicit call through SweetBlue, then you get {@link Status#TIMED_OUT},
         * but then the native stack eventually *does* come back with something - this has never been observed, but it is possible.
         */
        public final boolean solicited()  {  return m_solicited;  }
        private final boolean m_solicited;

        /*package*/ ServiceAddEvent(final BleServer server, final BluetoothGattService service, final Status status, final int gattStatus, final boolean solicited)
        {
            m_server = server;
            m_service = service;
            m_status = status;
            m_gattStatus = gattStatus;
            m_solicited = solicited;
        }

        /**
         * Convenience forwarding of {@link Status#wasSuccess()}.
         */
        public final boolean wasSuccess()
        {
            return status().wasSuccess();
        }

        /*package*/ static ServiceAddEvent NULL(BleServer server, BluetoothGattService service)
        {
            return EARLY_OUT(server, service, Status.NULL);
        }

        /*package*/ static ServiceAddEvent EARLY_OUT(BleServer server, BluetoothGattService service, Status status)
        {
            return new ServiceAddEvent(server, service, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
        }

        public final String toString()
        {
            return Utils_String.toString
            (
                this.getClass(),
                "status",			status(),
                "service",			server().getManager().getLogger().serviceName(service().getUuid()),
                "gattStatus",		server().getManager().getLogger().gattStatus(gattStatus())
            );
        }
    }

    /**
     * Called when a service has finished being added or failed to be added.
     */
    void onEvent(final ServiceAddEvent e);
}
