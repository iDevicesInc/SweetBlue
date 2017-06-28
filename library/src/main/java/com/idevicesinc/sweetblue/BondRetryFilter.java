package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_T;

/**
 * Interface used to tell SweetBlue how to behave when a bond attempt fails.
 */
public interface BondRetryFilter extends GenericListener_T<BondRetryFilter.RetryEvent, BondRetryFilter.Please>
{

    /**
     * Event class which holds an instance of the device that a bond has failed on, and the failure code.
     */
    final class RetryEvent extends Event
    {

        private final BleDevice m_device;
        private final int m_failCode;
        private final boolean m_direct;
        private final int m_retryAttempts;
        private final Status m_status;
        private final boolean m_userPrompted;


        RetryEvent(BleDevice device, int failCode, int retryAttempts, boolean direct, boolean userPrompted)
        {
            m_device = device;
            m_failCode = failCode;
            m_direct = direct;
            m_retryAttempts = retryAttempts;
            m_status = Status.fromNativeBit(failCode);
            m_userPrompted = userPrompted;
        }

        /**
         * Returns the {@link BleDevice} that failed to bond.
         */
        public final BleDevice device()
        {
            return m_device;
        }

        /**
         * Returns the {@link Status} of the bond attempt. While {@link Status#SUCCESS} exists, if the {@link BondRetryFilter} is invoked, it is implied that
         * the attempt was not successful. This is just an enum containing the bond failure codes contained in {@link BleStatuses} to make the errors more
         * readable.
         */
        public final Status status()
        {
            return m_status;
        }

        /**
         * Returns the failure code the native stack returned as the reason that bonding failed.
         */
        public final int failCode()
        {
            return m_failCode;
        }

        /**
         * Returns how many times SweetBlue has retried bonding on the {@link #device()}.
         */
        public final int retryAttempts()
        {
            return m_retryAttempts;
        }

        /**
         * Returns whether this bond attempt was from a direct bond call, from calling {@link BleDevice#bond(BleDevice.BondListener)}, or
         * {@link BleDevice#bond()}.
         */
        public final boolean isDirect()
        {
            return m_direct;
        }

        /**
         * Returns whether this bond request attempt prompted the user or not. NOTE: This only means the OS has "prompted" the user for bonding, in the case
         * that the device requires a pin/password. This can either mean a notification was put into the drawer, OR the dialog was shown to the user. However, if you
         * set {@link BleDeviceConfig#forceBondDialog}, then there's a very high chance that the dialog was shown.
         */
        public final boolean wasUserPrompted()
        {
            return m_userPrompted;
        }

        /**
         * This is a best guess on our part. Basically, this will return <code>true</code> if {@link #wasUserPrompted()} returns <code>false</code>, or the {@link Status}
         * does not equal {@link Status#AUTH_FAILED}, {@link Status#AUTH_REJECTED}, {@link Status#REPEATED_ATTEMPTS}, or {@link Status#SUCCESS} (which should never
         * happen here, but it's left just in case).
         */
        public final boolean possibleRetry()
        {
            switch (m_status)
            {
                case AUTH_FAILED:
                case REPEATED_ATTEMPTS:
                case AUTH_REJECTED:
                case SUCCESS:
                    return false;
                default:
                    return !wasUserPrompted();
            }
        }

    }

    /**
     * Please class to tell the library if it should retry bonding to the device again or not.
     */
    final class Please
    {
        private final boolean m_retry;

        private Please(boolean retry)
        {
            m_retry = retry;
        }

        boolean shouldRetry()
        {
            return m_retry;
        }

        /**
         * Tell SweetBlue to retry bonding to the device.
         */
        public static Please retry()
        {
            return new Please(true);
        }

        /**
         * Tell SweetBlue to retry bonding to the device, if the provided condition is <code>true</code>.
         */
        public static Please retryIf(boolean condition)
        {
            return new Please(condition);
        }

        /**
         * Tells SweetBlue not to retry bonding to the device.
         */
        public static Please stop()
        {
            return new Please(false);
        }

        /**
         * Tells SweetBlue not to retry bonding to the device, if the provided condition is <code>true</code>.
         */
        public static Please stopIf(boolean condition)
        {
            return new Please(!condition);
        }
    }


    enum Status
    {
        /**
         * The bond attempt was successful.
         */
        SUCCESS(BleStatuses.BOND_SUCCESS),

        /**
         * A bond attempt failed because pins did not match, or remote device did not respond to pin request in time.
         */
        AUTH_FAILED(BleStatuses.UNBOND_REASON_AUTH_FAILED),

        /**
         * A bond attempt failed because the other side explicitly rejected bonding.
         */
        AUTH_REJECTED(BleStatuses.UNBOND_REASON_AUTH_REJECTED),

        /**
         * A bond attempt failed because we canceled the bonding process.
         */
        AUTH_CANCELED(BleStatuses.UNBOND_REASON_AUTH_CANCELED),

        /**
         * A bond attempt failed because we could not contact the remote device.
         */
        REMOTE_DEVICE_DOWN(BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN),

        /**
         * A bond attempt failed because a discovery is in progress.
         */
        DISCOVERY_IN_PROGRESS(BleStatuses.UNBOND_REASON_DISCOVERY_IN_PROGRESS),

        /**
         * A bond attempt failed because of authentication timeout.
         */
        AUTH_TIMEOUT(BleStatuses.UNBOND_REASON_AUTH_TIMEOUT),

        /**
         * A bond attempt failed because of repeated attempts.
         */
        REPEATED_ATTEMPTS(BleStatuses.UNBOND_REASON_REPEATED_ATTEMPTS),

        /**
         * A bond attempt failed because we received an Authentication Cancel by remote end.
         */
        REMOTE_AUTH_CANCELED(BleStatuses.UNBOND_REASON_REMOTE_AUTH_CANCELED),

        /**
         * An existing bond was explicitly revoked.
         */
        REMOVED(BleStatuses.UNBOND_REASON_REMOVED),

        /**
         * Catchall for when we get a bond failure, and don't know the appropriate error (this should in theory never happen)
         */
        UNKNOWN_ERROR(-1);


        private int m_nativeBit;
        private static Status[] VALUES;


        Status(int nativeBit)
        {
            m_nativeBit = nativeBit;
        }

        public static Status fromNativeBit(int bit)
        {
            for (Status s : VALUES())
            {
                if (s.m_nativeBit == bit)
                {
                    return s;
                }
            }
            return UNKNOWN_ERROR;
        }

        public static Status[] VALUES()
        {
            if (VALUES == null)
            {
                VALUES = values();
            }
            return VALUES;
        }
    }


    /**
     * The default {@link BondRetryFilter} that SweetBlue uses. Feel free to instantiate this with your own number of retry attempts,
     * See {@link BondRetryFilter.DefaultBondRetryFilter#DefaultBondRetryFilter(int)}, or to provide custom logic on when to retry the bond attempt, in which case you should override
     * the {@link BondRetryFilter.DefaultBondRetryFilter#onEvent(RetryEvent)} method. This will retry the bond only if the bond attempt was direct, in that
     * {@link BleDevice#bond(BleDevice.BondListener)}, or {@link BleDevice#bond()} was called, and if we think it's a possible retry situation.
     * See {@link RetryEvent#possibleRetry()}.
     */
    class DefaultBondRetryFilter implements BondRetryFilter
    {

        private final int m_maxAttempts;

        /**
         * Constructor which allows you to specify the maximum number of bond retry attempts before giving up.
         */
        public DefaultBondRetryFilter(int maxRetries)
        {
            m_maxAttempts = maxRetries;
        }

        /**
         * Constructor which sets the max bond retries to {@link BleDeviceConfig#DEFAULT_MAX_BOND_RETRIES} before giving up.
         */
        public DefaultBondRetryFilter()
        {
            this(BleDeviceConfig.DEFAULT_MAX_BOND_RETRIES);
        }

        @Override
        public Please onEvent(RetryEvent e)
        {
            return Please.retryIf(e.isDirect() && e.possibleRetry() && e.retryAttempts() < m_maxAttempts);
        }
    }

}
