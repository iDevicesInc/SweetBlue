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

        RetryEvent(BleDevice device, int failCode)
        {
            m_device = device;
            m_failCode = failCode;
        }

        /**
         * Returns the {@link BleDevice} that failed to bond.
         */
        public final BleDevice device()
        {
            return m_device;
        }

        /**
         * Returns the failure code the native stack returned as the reason that bonding failed.
         */
        public final int failCode()
        {
            return m_failCode;
        }

        /**
         * This is a best guess on our part. Basically, this will return <code>true</code> if the failure code does not equal
         * {@link BleStatuses#UNBOND_REASON_AUTH_FAILED}, or {@link BleStatuses#UNBOND_REASON_REPEATED_ATTEMPTS}.
         */
        public final boolean possibleRetry()
        {
            return m_failCode != BleStatuses.UNBOND_REASON_AUTH_FAILED && m_failCode != BleStatuses.UNBOND_REASON_REPEATED_ATTEMPTS;
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

    class DefaultBondRetryFilter implements BondRetryFilter
    {

        private final int m_maxAttempts;
        private int m_failCount;

        /**
         *
         */
        public DefaultBondRetryFilter(int maxRetries)
        {
            m_maxAttempts = maxRetries;
        }

        public DefaultBondRetryFilter()
        {
            this(5);
        }

        @Override
        public Please onEvent(RetryEvent e)
        {
            m_failCount++;
            return Please.retryIf(e.possibleRetry() && m_failCount < m_maxAttempts);
        }
    }

}
