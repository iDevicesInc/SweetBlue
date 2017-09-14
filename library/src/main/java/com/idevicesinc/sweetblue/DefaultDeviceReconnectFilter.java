package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Interval;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;


public class DefaultDeviceReconnectFilter implements DeviceReconnectFilter
{

    /**
     * The default retry count provided to {@link DefaultDeviceReconnectFilter}.
     * So if you were to call {@link BleDevice#connect()} and all connections failed, in total the
     * library would try to connect {@value #DEFAULT_CONNECTION_FAIL_RETRY_COUNT}+1 times.
     *
     * @see DefaultDeviceReconnectFilter
     */
    public static final int DEFAULT_CONNECTION_FAIL_RETRY_COUNT = 2;

    /**
     * The default connection fail limit past which {@link DefaultDeviceReconnectFilter} will start returning {@link ReconnectFilter.ConnectFailPlease#retryWithAutoConnectTrue()}.
     */
    public static final int DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT = 2;

    /**
     * The maximum amount of time to keep trying if connection is failing due to (what usually are) transient bonding failures
     */
    public static final Interval MAX_RETRY_TIME_FOR_BOND_FAILURE = Interval.secs(120.0);

    private final int m_retryCount;
    private final int m_failCountBeforeUsingAutoConnect;

    public static final ConnectionLostPlease DEFAULT_INITIAL_RECONNECT_DELAY	= ConnectionLostPlease.retryInstantly();

    public static final Interval LONG_TERM_ATTEMPT_RATE			= Interval.secs(3.0);
    public static final Interval SHORT_TERM_ATTEMPT_RATE		= Interval.secs(1.0);

    public static final Interval SHORT_TERM_TIMEOUT				= Interval.FIVE_SECS;
    public static final Interval LONG_TERM_TIMEOUT				= Interval.mins(5);

    private final ConnectionLostPlease m_please__SHORT_TERM__SHOULD_TRY_AGAIN;
    private final ConnectionLostPlease m_please__LONG_TERM__SHOULD_TRY_AGAIN;

    private final Interval m_timeout__SHORT_TERM__SHOULD_CONTINUE;
    private final Interval m_timeout__LONG_TERM__SHOULD_CONTINUE;


    public DefaultDeviceReconnectFilter()
    {
        this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT);
    }

    public DefaultDeviceReconnectFilter(int retryCount, int failCountBeforeUsingAutoConnect)
    {
        this(retryCount, failCountBeforeUsingAutoConnect, SHORT_TERM_ATTEMPT_RATE, LONG_TERM_ATTEMPT_RATE, SHORT_TERM_TIMEOUT, LONG_TERM_TIMEOUT);
    }

    public DefaultDeviceReconnectFilter(final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
    {
        this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT, reconnectRate__SHORT_TERM, reconnectRate__LONG_TERM, timeout__SHORT_TERM, timeout__LONG_TERM);
    }

    public DefaultDeviceReconnectFilter(int retryCount, int failCountBeforeUsingAutoConnect, final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
    {
        m_retryCount = retryCount;
        m_failCountBeforeUsingAutoConnect = failCountBeforeUsingAutoConnect;

        m_please__SHORT_TERM__SHOULD_TRY_AGAIN = ConnectionLostPlease.retryIn(reconnectRate__SHORT_TERM);
        m_please__LONG_TERM__SHOULD_TRY_AGAIN = ConnectionLostPlease.retryIn(reconnectRate__LONG_TERM);

        m_timeout__SHORT_TERM__SHOULD_CONTINUE = timeout__SHORT_TERM;
        m_timeout__LONG_TERM__SHOULD_CONTINUE = timeout__LONG_TERM;
    }

    public final int getConnectFailRetryCount()
    {
        return m_retryCount;
    }


    @Override
    public ConnectFailPlease onConnectFailed(ConnectFailEvent e)
    {
        //--- DRK > Not necessary to check this ourselves, just being explicit.
        if (!e.status().allowsRetry() || e.device().is(RECONNECTING_LONG_TERM))
        {
            return ConnectFailPlease.doNotRetry();
        }

        if (e.failureCountSoFar() <= m_retryCount)
        {
            if (e.failureCountSoFar() >= m_failCountBeforeUsingAutoConnect)
            {
                return ConnectFailPlease.retryWithAutoConnectTrue();
            }
            else
            {
                if (e.status() == Status.NATIVE_CONNECTION_FAILED && e.timing() == Timing.TIMED_OUT)
                {
                    if (e.autoConnectUsage() == AutoConnectUsage.USED)
                    {
                        return ConnectFailPlease.retryWithAutoConnectFalse();
                    }
                    else if (e.autoConnectUsage() == AutoConnectUsage.NOT_USED)
                    {
                        return ConnectFailPlease.retryWithAutoConnectTrue();
                    }
                    else
                    {
                        return ConnectFailPlease.retry();
                    }
                }
                else
                {
                    return ConnectFailPlease.retry();
                }
            }
        }
        else
        {
            return ConnectFailPlease.doNotRetry();
        }
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
                    return m_please__LONG_TERM__SHOULD_TRY_AGAIN;
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
            return ConnectionLostPlease.persistIf(e.totalTimeReconnecting().lt(m_timeout__LONG_TERM__SHOULD_CONTINUE));
        }
    }

}
