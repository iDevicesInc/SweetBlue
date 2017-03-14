package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Interval;

import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;

/**
 * Default implementation of {@link DeviceConnectionFailListener} that attempts a certain number of retries. An instance of this class is set by default
 * for all new {@link BleDevice} instances using {@link DefaultDeviceConnectionFailListener#DEFAULT_CONNECTION_FAIL_RETRY_COUNT}.
 * Use {@link BleDevice#setListener_ConnectionFail(DeviceConnectionFailListener)} to override the default behavior.
 *
 * @see DeviceConnectionFailListener
 * @see BleDevice#setListener_ConnectionFail(DeviceConnectionFailListener)
 */
@Immutable
public class DefaultDeviceConnectionFailListener implements DeviceConnectionFailListener
{

    /**
     * The default retry count provided to {@link DefaultDeviceConnectionFailListener}.
     * So if you were to call {@link BleDevice#connect()} and all connections failed, in total the
     * library would try to connect {@value #DEFAULT_CONNECTION_FAIL_RETRY_COUNT}+1 times.
     *
     * @see DefaultDeviceConnectionFailListener
     */
    public static final int DEFAULT_CONNECTION_FAIL_RETRY_COUNT = 2;

    /**
     * The default connection fail limit past which {@link DefaultDeviceConnectionFailListener} will start returning {@link NodeConnectionFailListener.Please#retryWithAutoConnectTrue()}.
     */
    public static final int DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT = 2;

    /**
     * The maximum amount of time to keep trying if connection is failing due to (what usually are) transient bonding failures
     */
    public static final Interval MAX_RETRY_TIME_FOR_BOND_FAILURE = Interval.secs(120.0);

    private final int m_retryCount;
    private final int m_failCountBeforeUsingAutoConnect;



    public DefaultDeviceConnectionFailListener()
    {
        this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT);
    }

    public DefaultDeviceConnectionFailListener(int retryCount, int failCountBeforeUsingAutoConnect)
    {
        m_retryCount = retryCount;
        m_failCountBeforeUsingAutoConnect = failCountBeforeUsingAutoConnect;
    }

    public final int getRetryCount()
    {
        return m_retryCount;
    }

    @Override public Please onEvent(ConnectionFailEvent e)
    {
        //--- DRK > Not necessary to check this ourselves, just being explicit.
        if (!e.status().allowsRetry() || e.device().is(RECONNECTING_LONG_TERM))
        {
            return Please.doNotRetry();
        }

        //--- DRK > It has been noticed that bonding can fail several times due to the follow status code but then succeed,
        //---		so we just keep on trying for a little bit in case we can eventually make it.
        //---		NOTE: After testing for a little bit, this doesn't seem to work, regardless of how much time you give it.
//			if( e.bondFailReason() == BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN )
//			{
//				final Interval timeNow = e.attemptTime_total();
//				Interval timeSinceFirstUnbond = e.attemptTime_total();
//				final ConnectionFailEvent[] history = e.history();
//				for( int i = history.length-1; i >= 0; i-- )
//				{
//					final ConnectionFailEvent history_ith = history[i];
//
//					if( history_ith.bondFailReason() == BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN )
//					{
//						timeSinceFirstUnbond = history_ith.attemptTime_total();
//					}
//					else
//					{
//						break;
//					}
//				}
//
//				final Interval totalTimeFailingDueToBondingIssues = timeNow.minus(timeSinceFirstUnbond);
//
//				if( totalTimeFailingDueToBondingIssues.lt(MAX_RETRY_TIME_FOR_BOND_FAILURE) )
//				{
//					return Please.retry();
//				}
//			}

        if (e.failureCountSoFar() <= m_retryCount)
        {
            if (e.failureCountSoFar() >= m_failCountBeforeUsingAutoConnect)
            {
                return Please.retryWithAutoConnectTrue();
            }
            else
            {
                if (e.status() == Status.NATIVE_CONNECTION_FAILED && e.timing() == Timing.TIMED_OUT)
                {
                    if (e.autoConnectUsage() == AutoConnectUsage.USED)
                    {
                        return Please.retryWithAutoConnectFalse();
                    }
                    else if (e.autoConnectUsage() == AutoConnectUsage.NOT_USED)
                    {
                        return Please.retryWithAutoConnectTrue();
                    }
                    else
                    {
                        return Please.retry();
                    }
                }
                else
                {
                    return Please.retry();
                }
            }
        }
        else
        {
            return Please.doNotRetry();
        }
    }

}
