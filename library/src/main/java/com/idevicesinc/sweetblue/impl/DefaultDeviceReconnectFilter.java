package com.idevicesinc.sweetblue.impl;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.utils.Interval;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;


/**
 * Default implementation of {@link DeviceReconnectFilter}, which handles reconnect logic.
 */
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
     * The default connection fail limit past which {@link DefaultDeviceReconnectFilter} will start returning {@link ConnectFailPlease#retryWithAutoConnectTrue()}.
     */
    public static final int DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT = 2;


    private final int m_retryCount;
    private final int m_failCountBeforeUsingAutoConnect;

    // The DefaultReconnectFilter is shared between this class, and DefaultServerReconnectFilter class.
    private final DefaultReconnectFilter m_defaultConnectionLostFilter;


    public DefaultDeviceReconnectFilter()
    {
        this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT);
    }

    public DefaultDeviceReconnectFilter(int retryCount, int failCountBeforeUsingAutoConnect)
    {
        this(retryCount, failCountBeforeUsingAutoConnect, DefaultReconnectFilter.SHORT_TERM_ATTEMPT_RATE, DefaultReconnectFilter.LONG_TERM_ATTEMPT_RATE,
                DefaultReconnectFilter.SHORT_TERM_TIMEOUT, DefaultReconnectFilter.LONG_TERM_TIMEOUT);
    }

    public DefaultDeviceReconnectFilter(final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
    {
        this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT, reconnectRate__SHORT_TERM, reconnectRate__LONG_TERM, timeout__SHORT_TERM, timeout__LONG_TERM);
    }

    public DefaultDeviceReconnectFilter(int retryCount, int failCountBeforeUsingAutoConnect, final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
    {
        m_retryCount = retryCount;
        m_failCountBeforeUsingAutoConnect = failCountBeforeUsingAutoConnect;

        m_defaultConnectionLostFilter = new DefaultReconnectFilter(reconnectRate__SHORT_TERM, reconnectRate__LONG_TERM, timeout__SHORT_TERM, timeout__LONG_TERM);
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
        return m_defaultConnectionLostFilter.onConnectionLost(e);
    }

}
