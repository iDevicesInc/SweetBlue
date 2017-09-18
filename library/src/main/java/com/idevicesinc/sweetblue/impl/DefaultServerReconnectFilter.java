package com.idevicesinc.sweetblue.impl;


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Default implementation of {@link ServerReconnectFilter} that attempts a certain number of retries. An instance of this class is set by default
 * for all new {@link BleServer} instances using {@link DefaultServerReconnectFilter#DEFAULT_CONNECTION_FAIL_RETRY_COUNT}.
 * Use {@link BleServer#setListener_ReconnectFilter(ServerReconnectFilter)} to override the default behavior.
 *
 * @see ServerReconnectFilter
 * @see BleServer#setListener_ReconnectFilter(ServerReconnectFilter)
 */
public class DefaultServerReconnectFilter implements ServerReconnectFilter
{

    /**
     * The default retry count provided to {@link DefaultServerReconnectFilter}.
     * So if you were to call {@link BleDevice#connect()} and all connections failed, in total the
     * library would try to connect {@value #DEFAULT_CONNECTION_FAIL_RETRY_COUNT}+1 times.
     *
     * @see DefaultServerReconnectFilter
     */
    public static final int DEFAULT_CONNECTION_FAIL_RETRY_COUNT = 2;

    /**
     * The default connection fail limit past which {@link DefaultServerReconnectFilter} will start returning {@link com.idevicesinc.sweetblue.ReconnectFilter.ConnectFailPlease#retryWithAutoConnectTrue()}.
     */
    public static final int DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT = 2;


    private final int m_retryCount;
    private final int m_failCountBeforeUsingAutoConnect;

    private final DefaultReconnectFilter m_defaultConnectionLostFilter;



    public DefaultServerReconnectFilter()
    {
        this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT);
    }

    public DefaultServerReconnectFilter(int retryCount, int failCountBeforeUsingAutoConnect)
    {
        this(retryCount, failCountBeforeUsingAutoConnect, DefaultReconnectFilter.SHORT_TERM_ATTEMPT_RATE, DefaultReconnectFilter.LONG_TERM_ATTEMPT_RATE,
                DefaultReconnectFilter.SHORT_TERM_TIMEOUT, DefaultReconnectFilter.LONG_TERM_TIMEOUT);
    }

    public DefaultServerReconnectFilter(final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
    {
        this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT, reconnectRate__SHORT_TERM, reconnectRate__LONG_TERM, timeout__SHORT_TERM, timeout__LONG_TERM);
    }

    public DefaultServerReconnectFilter(int retryCount, int failCountBeforeUsingAutoConnect, final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
    {
        m_retryCount = retryCount;
        m_failCountBeforeUsingAutoConnect = failCountBeforeUsingAutoConnect;

        m_defaultConnectionLostFilter = new DefaultReconnectFilter(reconnectRate__SHORT_TERM, reconnectRate__LONG_TERM, timeout__SHORT_TERM, timeout__LONG_TERM);
    }

    public final int getRetryCount()
    {
        return m_retryCount;
    }

    @Override public final ConnectFailPlease onConnectFailed(ConnectFailEvent e)
    {
        //--- DRK > Not necessary to check this ourselves, just being explicit.
        if (!e.status().allowsRetry() )
        {
            return ConnectFailPlease.doNotRetry();
        }
        else if (e.failureCountSoFar() <= m_retryCount)
        {
            return ConnectFailPlease.retry();
        }
        else
        {
            return ConnectFailPlease.doNotRetry();
        }
    }

    @Override
    public ConnectionLostPlease onConnectionLost(ConnectionLostEvent event)
    {
        return m_defaultConnectionLostFilter.onConnectionLost(event);
    }
}
