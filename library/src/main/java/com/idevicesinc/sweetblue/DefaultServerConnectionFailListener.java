package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Default implementation of {@link ServerConnectionFailListener} that attempts a certain number of retries. An instance of this class is set by default
 * for all new {@link BleServer} instances using {@link DefaultServerConnectionFailListener#DEFAULT_CONNECTION_FAIL_RETRY_COUNT}.
 * Use {@link BleServer#setListener_ConnectionFail(ServerConnectionFailListener)} to override the default behavior.
 *
 * @see ServerConnectionFailListener
 * @see BleServer#setListener_ConnectionFail(ServerConnectionFailListener)
 */
@Immutable
public class DefaultServerConnectionFailListener implements ServerConnectionFailListener
{
    /**
     * The default retry count provided to {@link DefaultServerConnectionFailListener}.
     * So if you were to call {@link BleDevice#connect()} and all connections failed, in total the
     * library would try to connect {@value #DEFAULT_CONNECTION_FAIL_RETRY_COUNT}+1 times.
     *
     * @see DefaultServerConnectionFailListener
     */
    public static final int DEFAULT_CONNECTION_FAIL_RETRY_COUNT = 2;

    /**
     * The default connection fail limit past which {@link DefaultServerConnectionFailListener} will start returning {@link Please#retryWithAutoConnectTrue()}.
     */
    public static final int DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT = 2;


    private final int m_retryCount;
    private final int m_failCountBeforeUsingAutoConnect;

    public DefaultServerConnectionFailListener()
    {
        this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT);
    }

    public DefaultServerConnectionFailListener(int retryCount, int failCountBeforeUsingAutoConnect)
    {
        m_retryCount = retryCount;
        m_failCountBeforeUsingAutoConnect = failCountBeforeUsingAutoConnect;
    }

    public final int getRetryCount()
    {
        return m_retryCount;
    }

    @Override public final Please onEvent(ConnectionFailEvent e)
    {
        //--- DRK > Not necessary to check this ourselves, just being explicit.
        if (!e.status().allowsRetry() )
        {
            return Please.doNotRetry();
        }
        else if (e.failureCountSoFar() <= m_retryCount)
        {
            return Please.retry();
        }
        else
        {
            return Please.doNotRetry();
        }
    }
}
