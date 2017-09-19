package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattServer;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.impl.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.impl.DefaultServerReconnectFilter;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * An optional interface you can implement on {@link BleNodeConfig#reconnectFilter} to control reconnection behavior.
 *
 * @see BleNodeConfig#reconnectFilter
 * @see DefaultDeviceReconnectFilter
 * @see DefaultServerReconnectFilter
 */
public interface ReconnectFilter<T extends ReconnectFilter.ConnectFailEvent>
{
    /**
     * An enum provided through {@link ConnectionLostEvent#type()} that describes what reconnect stage we're at.
     */
    enum Type
    {
        /**
         * A small period of time has passed since we last asked about {@link #SHORT_TERM__SHOULD_TRY_AGAIN}, so just making sure you want to keep going.
         */
        SHORT_TERM__SHOULD_CONTINUE,

        /**
         * An attempt to reconnect in the short term failed, should we try again?.
         */
        SHORT_TERM__SHOULD_TRY_AGAIN,

        /**
         * A small period of time has passed since we last asked about {@link #LONG_TERM__SHOULD_TRY_AGAIN}, so just making sure you want to keep going.
         */
        LONG_TERM__SHOULD_CONTINUE,

        /**
         * An attempt to reconnect in the long term failed, should we try again?.
         */
        LONG_TERM__SHOULD_TRY_AGAIN;

        /**
         * Is this either {@link #SHORT_TERM__SHOULD_CONTINUE} or {@link #LONG_TERM__SHOULD_TRY_AGAIN}.
         */
        public boolean isShouldTryAgain()
        {
            return this == SHORT_TERM__SHOULD_TRY_AGAIN || this == LONG_TERM__SHOULD_TRY_AGAIN;
        }

        /**
         * Is this either {@link #SHORT_TERM__SHOULD_CONTINUE} or {@link #LONG_TERM__SHOULD_CONTINUE}.
         */
        public boolean isShouldContinue()
        {
            return this == SHORT_TERM__SHOULD_CONTINUE || this == LONG_TERM__SHOULD_CONTINUE;
        }

        /**
         * Is this either {@link #SHORT_TERM__SHOULD_TRY_AGAIN} or {@link #SHORT_TERM__SHOULD_CONTINUE}.
         */
        public boolean isShortTerm()
        {
            return this == SHORT_TERM__SHOULD_TRY_AGAIN || this == SHORT_TERM__SHOULD_CONTINUE;
        }

        /**
         * Is this either {@link #LONG_TERM__SHOULD_TRY_AGAIN} or {@link #LONG_TERM__SHOULD_CONTINUE}.
         */
        public boolean isLongTerm()
        {
            return this == LONG_TERM__SHOULD_TRY_AGAIN || this == LONG_TERM__SHOULD_CONTINUE;
        }
    }

    /**
     * Struct passed to {@link ReconnectFilter#onConnectionLost(ConnectionLostEvent)} to aid in making a decision.
     */
    @Immutable
    class ConnectionLostEvent extends Event
    {
        /**
         * The node that is currently trying to reconnect.
         */
        public BleNode node(){  return m_node;  }
        private BleNode m_node;

        /**
         * Tries to cast {@link #node()} to a {@link BleDevice}, otherwise returns {@link BleDevice#NULL}.
         */
        public BleDevice device(){  return node().cast(BleDevice.class);  }

        /**
         * Tries to cast {@link #node()} to a {@link BleServer}, otherwise returns {@link BleServer#NULL}.
         */
        public BleServer server(){  return node().cast(BleServer.class);  }

        /**
         * Convience to return the mac address of {@link #device()} or the client being reconnected to the {@link #server()}.
         */
        public String macAddress()  {  return m_macAddress;  }
        private String m_macAddress;

        /**
         * The number of times a reconnect attempt has failed so far.
         */
        public int failureCount(){  return m_failureCount;  }
        private int m_failureCount;

        /**
         * The total amount of time since the device disconnected and we started the reconnect process.
         */
        public Interval totalTimeReconnecting(){  return m_totalTimeReconnecting;  }
        private Interval m_totalTimeReconnecting;

        /**
         * The previous {@link Interval} returned through {@link ConnectionLostPlease#retryIn(Interval)},
         * or {@link Interval#ZERO} for the first invocation.
         */
        public Interval previousDelay(){  return m_previousDelay;  }
        private Interval m_previousDelay;

        /**
         * Returns the more detailed information about why the connection failed. This is passed to {@link DeviceReconnectFilter#onConnectFailed(ConnectFailEvent)}
         * before the call is made to {@link ReconnectFilter#onConnectionLost(ConnectionLostEvent)}. For the first call to {@link ReconnectFilter#onConnectionLost(ConnectionLostEvent)},
         * right after a spontaneous disconnect occurred, the connection didn't fail, so {@link ReconnectFilter.ConnectFailEvent#isNull()} will return <code>true</code>.
         */
        public ConnectFailEvent connectionFailEvent(){  return m_connectionFailEvent;  }
        private ConnectFailEvent m_connectionFailEvent;

        /**
         * See {@link Type} for more info.
         */
        public Type type(){  return m_type;  }
        private Type m_type;

        /*package*/ ConnectionLostEvent(BleNode node, final String macAddress, int failureCount, Interval totalTimeReconnecting, Interval previousDelay, ConnectFailEvent connectionFailEvent, final Type type)
        {
            this.init(node, macAddress, failureCount, totalTimeReconnecting, previousDelay, connectionFailEvent, type);
        }

        /*package*/ ConnectionLostEvent()
        {
        }

        /*package*/ void init(BleNode node, final String macAddress, int failureCount, Interval totalTimeReconnecting, Interval previousDelay, ConnectFailEvent connectionFailEvent, final Type type)
        {
            this.m_node						= node;
            this.m_macAddress				= macAddress;
            this.m_failureCount				= failureCount;
            this.m_totalTimeReconnecting	= totalTimeReconnecting;
            this.m_previousDelay			= previousDelay;
            this.m_connectionFailEvent		= connectionFailEvent;
            this.m_type						= type;
        }

        @Override public String toString()
        {
            return Utils_String.toString
            (
                this.getClass(),
                "node",						node(),
                "type",						type(),
                "failureCount",				failureCount(),
                "totalTimeReconnecting",	totalTimeReconnecting(),
                "previousDelay",			previousDelay()
            );
        }
    }

    /**
     * Return value for {@link ReconnectFilter#onConnectionLost(ConnectionLostEvent)}. Use static constructor methods to create instances.
     */
    @Immutable
    class ConnectionLostPlease
    {
        private static final Interval SHOULD_TRY_AGAIN__INSTANTLY	= Interval.ZERO;

        private static final ConnectionLostPlease SHOULD_CONTINUE__PERSIST		= new ConnectionLostPlease(true);
        private static final ConnectionLostPlease SHOULD_CONTINUE__STOP			= new ConnectionLostPlease(false);

        private final Interval m_interval__SHOULD_TRY_AGAIN;
        private final boolean m_persist;

        private ConnectionLostPlease(final Interval interval__SHOULD_TRY_AGAIN)
        {
            m_interval__SHOULD_TRY_AGAIN = interval__SHOULD_TRY_AGAIN;
            m_persist = true;
        }

        private ConnectionLostPlease(boolean persist)
        {
            m_persist = persist;
            m_interval__SHOULD_TRY_AGAIN = null;
        }

        /*package*/ Interval interval()
        {
            return m_interval__SHOULD_TRY_AGAIN;
        }

        /*package*/ boolean shouldPersist()
        {
            return m_persist;
        }

        /**
         * When {@link ConnectionLostEvent#type()} is either {@link Type#SHORT_TERM__SHOULD_TRY_AGAIN} or {@link Type#LONG_TERM__SHOULD_TRY_AGAIN},
         * return this from {@link ReconnectFilter#onConnectionLost(ConnectionLostEvent)} to instantly reconnect.
         */
        public static ConnectionLostPlease retryInstantly()
        {
            return new ConnectionLostPlease(SHOULD_TRY_AGAIN__INSTANTLY);
        }

        /**
         * Return this from {@link ReconnectFilter#onConnectionLost(ConnectionLostEvent)} to stop a reconnect attempt loop.
         * Note that {@link BleDevice#disconnect()} {@link BleServer#disconnect(String)} will also stop any ongoing reconnect loops.
         */
        public static ConnectionLostPlease stopRetrying()
        {
            return SHOULD_CONTINUE__STOP;
        }

        /**
         * Return this from {@link ReconnectFilter#onConnectionLost(ConnectionLostEvent)} to retry after the given amount of time.
         */
        public static ConnectionLostPlease retryIn(Interval interval)
        {
            return new ConnectionLostPlease(interval != null ? interval : SHOULD_TRY_AGAIN__INSTANTLY);
        }

        /**
         * Indicates that the {@link BleDevice} should keep {@link BleDeviceState#RECONNECTING_LONG_TERM} or
         * {@link BleDeviceState#RECONNECTING_SHORT_TERM}.
         */
        public static ConnectionLostPlease persist()
        {
            return SHOULD_CONTINUE__PERSIST;
        }

        /**
         * Returns {@link #persist()} if the condition holds, {@link #stopRetrying()} otherwise.
         */
        public static ConnectionLostPlease persistIf(final boolean condition)
        {
            return condition ? persist() : stopRetrying();
        }

        /**
         * Returns {@link #stopRetrying()} if the condition holds, {@link #persist()} otherwise.
         */
        public static ConnectionLostPlease stopRetryingIf(final boolean condition)
        {
            return condition ? stopRetrying() : persist();
        }
    }

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
     * Abstract base class for structures passed to {@link ServerReconnectFilter#onConnectFailed(ConnectFailEvent)}
     * and {@link DeviceReconnectFilter#onConnectFailed(ConnectFailEvent)} to provide more info about how/why a connection failed.
     */
    @Immutable
    abstract class ConnectFailEvent extends Event implements UsesCustomNull
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
         * If not applicable, for example if {@link DeviceReconnectFilter.ConnectFailEvent#status()} is {@link DeviceReconnectFilter.Status#EXPLICIT_DISCONNECT},
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

        ConnectFailEvent(int failureCountSoFar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, AutoConnectUsage autoConnectUsage)
        {
            this.m_failureCountSoFar = failureCountSoFar;
            this.m_latestAttemptTime = latestAttemptTime;
            this.m_totalAttemptTime = totalAttemptTime;
            this.m_gattStatus = gattStatus;
            this.m_autoConnectUsage = autoConnectUsage;
        }
    }

    /**
     * Return value for {@link DeviceReconnectFilter#onConnectFailed(ConnectFailEvent)}
     * and {@link ServerReconnectFilter#onConnectFailed(ConnectFailEvent)}.
     * Generally you will only return {@link #retry()} or {@link #doNotRetry()}, but there are more advanced options as well.
     */
    @Immutable
    class ConnectFailPlease
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

        private ConnectFailPlease(final int please__PE_Please)
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
        public static ConnectFailPlease retry()
        {
            return new ConnectFailPlease(PE_Please_RETRY);
        }

        /**
         * Returns {@link #retry()} if the given condition holds <code>true</code>, {@link #doNotRetry()} otherwise.
         */
        public static ConnectFailPlease retryIf(boolean condition)
        {
            return condition ? retry() : doNotRetry();
        }

        /**
         * Return this to stop the connection fail retry loop.
         */
        public static ConnectFailPlease doNotRetry()
        {
            return new ConnectFailPlease(PE_Please_DO_NOT_RETRY);
        }

        /**
         * Returns {@link #doNotRetry()} if the given condition holds <code>true</code>, {@link #retry()} otherwise.
         */
        public static ConnectFailPlease doNotRetryIf(boolean condition)
        {
            return condition ? doNotRetry() : retry();
        }

        /**
         * Same as {@link #retry()}, but <code>autoConnect=true</code> will be passed to
         * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}.
         * See more discussion at {@link BleDeviceConfig#alwaysUseAutoConnect}.
         */
        @com.idevicesinc.sweetblue.annotations.Advanced
        public static ConnectFailPlease retryWithAutoConnectTrue()
        {
            return new ConnectFailPlease(PE_Please_RETRY_WITH_AUTOCONNECT_TRUE);
        }

        /**
         * Opposite of{@link #retryWithAutoConnectTrue()}.
         */
        @com.idevicesinc.sweetblue.annotations.Advanced
        public static ConnectFailPlease retryWithAutoConnectFalse()
        {
            return new ConnectFailPlease(PE_Please_RETRY_WITH_AUTOCONNECT_FALSE);
        }

        /**
         * Returns <code>true</code> for everything except {@link #doNotRetry()}.
         */
        public boolean isRetry()
        {
            return isRetry(m_please__PE_Please);
        }
    }

    /**
     * This is called when connecting to a previously unconnected device fails. Use the static methods from {@link ConnectFailPlease} as return values to either
     * retry, or stop retrying to connect.
     */
    ConnectFailPlease onConnectFailed(T event);

    /**
     * This is called when a connected device loses it's connection, outside of you calling {@link BleDevice#disconnect()}
     */
    ConnectionLostPlease onConnectionLost(ConnectionLostEvent event);

    class DefaultNullReconnectFilter implements ReconnectFilter
    {
        public static final ConnectionLostPlease DEFAULT_INITIAL_RECONNECT_DELAY	= ConnectionLostPlease.retryInstantly();

        public static final Interval SHORT_TERM_ATTEMPT_RATE		= Interval.secs(1.0);

        public static final Interval SHORT_TERM_TIMEOUT				= Interval.FIVE_SECS;

        private final ConnectionLostPlease m_please__SHORT_TERM__SHOULD_TRY_AGAIN;
        private final Interval m_timeout__SHORT_TERM__SHOULD_CONTINUE;

        public DefaultNullReconnectFilter()
        {
            this
                    (
                            SHORT_TERM_ATTEMPT_RATE,
                            SHORT_TERM_TIMEOUT
                    );
        }

        public DefaultNullReconnectFilter(final Interval reconnectRate__SHORT_TERM, final Interval timeout__SHORT_TERM)
        {
            m_please__SHORT_TERM__SHOULD_TRY_AGAIN = ConnectionLostPlease.retryIn(reconnectRate__SHORT_TERM);

            m_timeout__SHORT_TERM__SHOULD_CONTINUE = timeout__SHORT_TERM;
        }

        @Override public ConnectionLostPlease onConnectionLost(final ConnectionLostEvent e)
        {
            if( e.type().isShouldTryAgain() )
            {
                if( e.failureCount() == 0 )
                {
                    return DEFAULT_INITIAL_RECONNECT_DELAY;
                }
                else
                {
                    if( e.type().isShortTerm() )
                    {
                        return m_please__SHORT_TERM__SHOULD_TRY_AGAIN;
                    }
                    else
                    {
                        return ConnectionLostPlease.stopRetrying();
                    }
                }
            }
            else if( e.type().isShouldContinue() )
            {
                if( e.node() instanceof BleDevice )
                {
                    final boolean definitelyPersist = BleDeviceState.CONNECTING_OVERALL.overlaps(e.device().getNativeStateMask()) &&
                            BleDeviceState.CONNECTED.overlaps(e.device().getNativeStateMask());

                    //--- DRK > We don't interrupt if we're in the middle of connecting
                    //---		but this will be the last attempt if it fails.
                    if( definitelyPersist )
                    {
                        return ConnectionLostPlease.persist();
                    }
                    else
                    {
                        return shouldContinue(e);
                    }
                }
                else
                {
                    return shouldContinue(e);
                }
            }
            else
            {
                return ConnectionLostPlease.stopRetrying();
            }
        }

        private ConnectionLostPlease shouldContinue(final ConnectionLostEvent e)
        {
            if( e.type().isShortTerm() )
            {
                return ConnectionLostPlease.persistIf(e.totalTimeReconnecting().lt(m_timeout__SHORT_TERM__SHOULD_CONTINUE));
            }
            else
            {
                return ConnectionLostPlease.stopRetrying();
            }
        }

        @Override
        public ConnectFailPlease onConnectFailed(ConnectFailEvent event)
        {
            return ConnectFailPlease.doNotRetry();
        }
    }
}
