package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Pass an instance of this listener to {@link BleDevice#setListener_Bond(BondListener)} or {@link BleDevice#bond(BondListener)}.
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface BondListener
{

    /**
     * Used on {@link BondEvent#status()} to roughly enumerate success or failure.
     */
    public static enum Status implements UsesCustomNull
    {
        /**
         * Fulfills soft contract of {@link UsesCustomNull}.
         *
         * @see #isNull().
         */
        NULL,

        /**
         * The {@link BleDevice#bond()} call succeeded.
         */
        SUCCESS,

        /**
         * {@link BleDevice#bond(BondListener)} (or overloads) was called on {@link BleDevice#NULL}.
         */
        NULL_DEVICE,

        /**
         * Already {@link BleDeviceState#BONDED} or in the process of {@link BleDeviceState#BONDING}.
         */
        ALREADY_BONDING_OR_BONDED,

        /**
         * The call to {@link BluetoothDevice#createBond()} returned <code>false</code> and thus failed immediately.
         */
        FAILED_IMMEDIATELY,

        /**
         * We received a {@link BluetoothDevice#ACTION_BOND_STATE_CHANGED} through our internal {@link BroadcastReceiver} that we went from
         * {@link BleDeviceState#BONDING} back to {@link BleDeviceState#UNBONDED}, which means the attempt failed.
         * See {@link BondEvent#failReason()} for more information.
         */
        FAILED_EVENTUALLY,

        /**
         * The bond operation took longer than the time set in {@link BleDeviceConfig#taskTimeoutRequestFilter} so we cut it loose.
         */
        TIMED_OUT,

        /**
         * A call was made to {@link BleDevice#unbond()} at some point during the bonding process.
         */
        CANCELLED_FROM_UNBOND,

        /**
         * Cancelled from {@link BleManager} going {@link BleManagerState#TURNING_OFF} or
         * {@link BleManagerState#OFF}, probably from calling {@link BleManager#reset()}.
         */
        CANCELLED_FROM_BLE_TURNING_OFF;

        /**
         * @return <code>true</code> for {@link #CANCELLED_FROM_BLE_TURNING_OFF} or {@link #CANCELLED_FROM_UNBOND}.
         */
        public final boolean wasCancelled()
        {
            return this == CANCELLED_FROM_BLE_TURNING_OFF || this == CANCELLED_FROM_UNBOND;
        }

        final boolean canFailConnection()
        {
            return this == FAILED_IMMEDIATELY || this == FAILED_EVENTUALLY || this == TIMED_OUT;
        }

        final DeviceReconnectFilter.Timing timing()
        {
            switch (this)
            {
                case FAILED_IMMEDIATELY:
                    return DeviceReconnectFilter.Timing.IMMEDIATELY;
                case FAILED_EVENTUALLY:
                    return DeviceReconnectFilter.Timing.EVENTUALLY;
                case TIMED_OUT:
                    return DeviceReconnectFilter.Timing.TIMED_OUT;
                default:
                    return DeviceReconnectFilter.Timing.NOT_APPLICABLE;
            }
        }

        /**
         * @return <code>true</code> if <code>this</code> == {@link #NULL}.
         */
        @Override public final boolean isNull()
        {
            return this == NULL;
        }
    }

    /**
     * Struct passed to {@link BondListener#onEvent(BondEvent)} to provide more information about a {@link BleDevice#bond()} attempt.
     */
    @Immutable
    public static class BondEvent extends Event implements UsesCustomNull
    {
        /**
         * The {@link BleDevice} that attempted to {@link BleDevice#bond()}.
         */
        public final BleDevice device()
        {
            return m_device;
        }

        private final BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public final String macAddress()
        {
            return m_device.getMacAddress();
        }

        /**
         * The {@link Status} associated with this event.
         */
        public final Status status()
        {
            return m_status;
        }

        private final Status m_status;

        /**
         * If {@link #status()} is {@link BondListener.Status#FAILED_EVENTUALLY}, this integer will
         * be one of the values enumerated in {@link BluetoothDevice} that start with <code>UNBOND_REASON</code> such as
         * {@link BleStatuses#UNBOND_REASON_AUTH_FAILED}. Otherwise it will be equal to {@link BleStatuses#BOND_FAIL_REASON_NOT_APPLICABLE}.
         * See also a publically accessible list in {@link BleStatuses}.
         */
        public final int failReason()
        {
            return m_failReason;
        }

        private final int m_failReason;

        /**
         * Tells whether the bond was created through an explicit call through SweetBlue, or otherwise. If
         * {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#INTENTIONAL}, then {@link BleDevice#bond()} (or overloads) were called.
         * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL},
         * then the bond was created "spontaneously" as far as SweetBlue is concerned, whether through another app, the OS Bluetooth
         * settings, or maybe from a request by the remote BLE device itself.
         */
        public final State.ChangeIntent intent()
        {
            return m_intent;
        }

        private final State.ChangeIntent m_intent;

        BondEvent(BleDevice device, Status status, int failReason, State.ChangeIntent intent)
        {
            m_device = device;
            m_status = status;
            m_failReason = failReason;
            m_intent = intent;
        }

        static BondEvent NULL(final BleDevice device)
        {
            return new BondEvent(device, Status.NULL, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.NULL);
        }

        /**
         * Shortcut for checking if {@link #status()} == {@link BondListener.Status#SUCCESS}.
         */
        public final boolean wasSuccess()
        {
            return status() == Status.SUCCESS;
        }

        /**
         * Forwards {@link Status#wasCancelled()}.
         */
        public final boolean wasCancelled()
        {
            return status().wasCancelled();
        }

        @Override public final String toString()
        {
            if (isNull())
            {
                return BleDevice.NULL_STRING();
            }
            else
            {
                return Utils_String.toString
                        (
                                this.getClass(),
                                "device", device().getName_debug(),
                                "status", status(),
                                "failReason", device().getManager().getLogger().gattUnbondReason(failReason()),
                                "intent", intent()
                        );
            }
        }

        @Override public final boolean isNull()
        {
            return status().isNull();
        }
    }

    /**
     * Called after a call to {@link BleDevice#bond(BondListener)} (or overloads),
     * or when bonding through another app or the operating system settings.
     */
    void onEvent(BondEvent e);

}
