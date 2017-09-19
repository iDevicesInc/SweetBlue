package com.idevicesinc.sweetblue.impl;


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Default implementation of {@link ReconnectFilter} that uses {@link ReconnectFilter.ConnectionLostPlease#retryInstantly()} for the
 * first reconnect attempt, and from then on uses the {@link Interval} rate passed to the constructor. An instance of this class is held
 * in {@link DefaultDeviceReconnectFilter}, and {@link DefaultServerReconnectFilter}, to handle connection lost events ONLY. The logic is the same
 * between the two, hence this class exists for this purpose, and to adhere to the DRY principle. This class is not meant for public consumption.
 *
 */
public class DefaultReconnectFilter implements ReconnectFilter
{

    public static final ConnectionLostPlease DEFAULT_INITIAL_RECONNECT_DELAY	= ConnectionLostPlease.retryInstantly();

    public static final Interval LONG_TERM_ATTEMPT_RATE			= Interval.secs(3.0);
    public static final Interval SHORT_TERM_ATTEMPT_RATE		= Interval.secs(1.0);

    public static final Interval SHORT_TERM_TIMEOUT				= Interval.FIVE_SECS;
    public static final Interval LONG_TERM_TIMEOUT				= Interval.mins(5);

    private final ConnectionLostPlease m_please__SHORT_TERM__SHOULD_TRY_AGAIN;
    private final ConnectionLostPlease m_please__LONG_TERM__SHOULD_TRY_AGAIN;

    private final Interval m_timeout__SHORT_TERM__SHOULD_CONTINUE;
    private final Interval m_timeout__LONG_TERM__SHOULD_CONTINUE;


    public DefaultReconnectFilter()
    {
        this
                (
                        DefaultReconnectFilter.SHORT_TERM_ATTEMPT_RATE,
                        DefaultReconnectFilter.LONG_TERM_ATTEMPT_RATE,
                        DefaultReconnectFilter.SHORT_TERM_TIMEOUT,
                        DefaultReconnectFilter.LONG_TERM_TIMEOUT
                );
    }

    public DefaultReconnectFilter(final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
    {
        m_please__SHORT_TERM__SHOULD_TRY_AGAIN = ConnectionLostPlease.retryIn(reconnectRate__SHORT_TERM);
        m_please__LONG_TERM__SHOULD_TRY_AGAIN = ConnectionLostPlease.retryIn(reconnectRate__LONG_TERM);

        m_timeout__SHORT_TERM__SHOULD_CONTINUE = timeout__SHORT_TERM;
        m_timeout__LONG_TERM__SHOULD_CONTINUE = timeout__LONG_TERM;
    }


    @Override
    public ConnectFailPlease onConnectFailed(ConnectFailEvent event)
    {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ConnectionLostPlease onConnectionLost(ConnectionLostEvent e)
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
            if( e.node() instanceof BleDevice)
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
