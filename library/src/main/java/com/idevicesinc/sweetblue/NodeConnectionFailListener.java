package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattServer;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;

/**
 * Base interface for {@link DeviceConnectionFailListener} and {@link ServerConnectionFailListener}.
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface NodeConnectionFailListener
{

    /**
     * Describes usage of the <code>autoConnect</code> parameter for either {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}
     * or {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    public static enum AutoConnectUsage
    {
        /**
         * Used when we didn't start the connection process, i.e. it came out of nowhere. Rare case but can happen, for example after
         * SweetBlue considers a connect timed out based on {@link BleNodeConfig#taskTimeoutRequestFilter} but then it somehow
         * does come in (shouldn't happen but who knows).
         */
        UNKNOWN,

        /**
         * Usage is not applicable.
         */
        NOT_APPLICABLE,

        /**
         * <code>autoConnect</code> was used.
         */
        USED,

        /**
         * <code>autoConnect</code> was not used.
         */
        NOT_USED;
    }

    /**
     * Abstract base class for structures passed to {@link ServerConnectionFailListener#onEvent(ServerConnectionFailListener.ConnectionFailEvent)}
     * and {@link DeviceConnectionFailListener#onEvent(DeviceConnectionFailListener.ConnectionFailEvent)} to provide more info about how/why a connection failed.
     */
    @Immutable
    public abstract static class ConnectionFailEvent extends Event implements UsesCustomNull
    {
        /**
         * The failure count so far. This will start at 1 and keep incrementing for more failures.
         */
        public int failureCountSoFar() {  return m_failureCountSoFar;  }
        private final int m_failureCountSoFar;

        /**
         * How long the last connection attempt took before failing.
         */
        public Interval attemptTime_latest() {  return m_latestAttemptTime;  }
        private final Interval m_latestAttemptTime;

        /**
         * How long it's been since {@link BleDevice#connect()} (or overloads) were initially called.
         */
        public Interval attemptTime_total() {  return m_totalAttemptTime;  }
        private final Interval m_totalAttemptTime;

        /**
         * The gattStatus returned, if applicable, from native callbacks like {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)}
         * or {@link BluetoothGattCallback#onServicesDiscovered(BluetoothGatt, int)}.
         * If not applicable, for example if {@link DeviceConnectionFailListener.ConnectionFailEvent#status()} is {@link DeviceConnectionFailListener.Status#EXPLICIT_DISCONNECT},
         * then this is set to {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
         * <br><br>
         * See {@link ReadWriteListener.ReadWriteEvent#gattStatus()} for more information about gatt status codes in general.
         *
         * @see ReadWriteListener.ReadWriteEvent#gattStatus()
         */
        public int gattStatus() {  return m_gattStatus;  }
        private final int m_gattStatus;

        /**
         * Whether <code>autoConnect=true</code> was passed to {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}.
         * See more discussion at {@link BleDeviceConfig#alwaysUseAutoConnect}.
         */
        @com.idevicesinc.sweetblue.annotations.Advanced
        public AutoConnectUsage autoConnectUsage() {  return m_autoConnectUsage;  }
        private final AutoConnectUsage m_autoConnectUsage;

        ConnectionFailEvent(int failureCountSoFar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, AutoConnectUsage autoConnectUsage)
        {
            this.m_failureCountSoFar = failureCountSoFar;
            this.m_latestAttemptTime = latestAttemptTime;
            this.m_totalAttemptTime = totalAttemptTime;
            this.m_gattStatus = gattStatus;
            this.m_autoConnectUsage = autoConnectUsage;
        }
    }

    /**
     * Return value for {@link DeviceConnectionFailListener#onEvent(DeviceConnectionFailListener.ConnectionFailEvent)}
     * and {@link ServerConnectionFailListener#onEvent(ServerConnectionFailListener.ConnectionFailEvent)}.
     * Generally you will only return {@link #retry()} or {@link #doNotRetry()}, but there are more advanced options as well.
     */
    @Immutable
    public static class Please
    {
        /*package*/ static final int PE_Please_NULL								= -1;
        /*package*/ static final int PE_Please_RETRY							=  0;
        /*package*/ static final int PE_Please_RETRY_WITH_AUTOCONNECT_TRUE		=  1;
        /*package*/ static final int PE_Please_RETRY_WITH_AUTOCONNECT_FALSE		=  2;
        /*package*/ static final int PE_Please_DO_NOT_RETRY						=  3;

        /*package*/ static final boolean isRetry(final int please__PE_Please)
        {
            return please__PE_Please != PE_Please_DO_NOT_RETRY && please__PE_Please != PE_Please_NULL;
        }

        private final int m_please__PE_Please;

        private Please(final int please__PE_Please)
        {
            m_please__PE_Please = please__PE_Please;
        }

        /*package*/ int/*__PE_Please*/ please()
        {
            return m_please__PE_Please;
        }

        /**
         * Return this to retry the connection, continuing the connection fail retry loop. <code>autoConnect</code> passed to
         * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}
         * will be false or true based on what has worked in the past, or on {@link BleDeviceConfig#alwaysUseAutoConnect}.
         */
        public static Please retry()
        {
            return new Please(PE_Please_RETRY);
        }

        /**
         * Returns {@link #retry()} if the given condition holds <code>true</code>, {@link #doNotRetry()} otherwise.
         */
        public static Please retryIf(boolean condition)
        {
            return condition ? retry() : doNotRetry();
        }

        /**
         * Return this to stop the connection fail retry loop.
         */
        public static Please doNotRetry()
        {
            return new Please(PE_Please_DO_NOT_RETRY);
        }

        /**
         * Returns {@link #doNotRetry()} if the given condition holds <code>true</code>, {@link #retry()} otherwise.
         */
        public static Please doNotRetryIf(boolean condition)
        {
            return condition ? doNotRetry() : retry();
        }

        /**
         * Same as {@link #retry()}, but <code>autoConnect=true</code> will be passed to
         * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}.
         * See more discussion at {@link BleDeviceConfig#alwaysUseAutoConnect}.
         */
        @com.idevicesinc.sweetblue.annotations.Advanced
        public static Please retryWithAutoConnectTrue()
        {
            return new Please(PE_Please_RETRY_WITH_AUTOCONNECT_TRUE);
        }

        /**
         * Opposite of{@link #retryWithAutoConnectTrue()}.
         */
        @com.idevicesinc.sweetblue.annotations.Advanced
        public static Please retryWithAutoConnectFalse()
        {
            return new Please(PE_Please_RETRY_WITH_AUTOCONNECT_FALSE);
        }

        /**
         * Returns <code>true</code> for everything except {@link #doNotRetry()}.
         */
        public boolean isRetry()
        {
            return isRetry(m_please__PE_Please);
        }
    }

}
