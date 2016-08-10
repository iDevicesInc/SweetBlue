package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener;
import com.idevicesinc.sweetblue.listeners.P_BaseConnectionFailListener.Please;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener.ConnectionFailEvent;
import com.idevicesinc.sweetblue.listeners.P_BaseConnectionFailListener;
import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.ArrayList;


public final class P_ConnectionFailManager
{

    private final BleDevice mDevice;
    private DeviceConnectionFailListener mConnectionFailListener = BleDevice.DEFAULT_CONNECTION_FAIL_LISTENER;

    private int mFailCount = 0;
    private BleDeviceState mHighestStateReached_total = null;
    private Long mTimeOfFirstConnect = null;
    private Long mTimeOfLastConnectFail = null;
    private final ArrayList<ConnectionFailEvent> mHistory = new ArrayList<>();



    public P_ConnectionFailManager(BleDevice device)
    {
        mDevice = device;

        resetFailCount();
    }

    private void resetFailCount()
    {
        mFailCount = 0;
        mHighestStateReached_total = null;
        mTimeOfFirstConnect = mTimeOfLastConnectFail = null;
        mHistory.clear();
    }

    final void onExplicitDisconnect()
    {
        resetFailCount();
    }

    final void onFullyInitialized()
    {
        resetFailCount();
    }

    final void onExplicitConnectionStarted()
    {
        resetFailCount();

        mTimeOfFirstConnect = System.currentTimeMillis();
    }

    public final DeviceConnectionFailListener.Please onConnectionFailed(DeviceConnectionFailListener.Status status, DeviceConnectionFailListener.Timing timing, int gattStatus, BleDeviceState highestStateReached,
                                                                  P_BaseConnectionFailListener.AutoConnectUsage autoConnectUsage, int bondReasionFail, ReadWriteListener.ReadWriteEvent txnFailReason)
    {
        if (status == null)   return P_BaseConnectionFailListener.Please.doNotRetry();

        final long currentTime = System.currentTimeMillis();

        //--- DRK > Can be null if this is a spontaneous connect (can happen with autoConnect sometimes for example).
        mTimeOfFirstConnect = mTimeOfFirstConnect != null ? mTimeOfFirstConnect : currentTime;
        final Long timeOfLastConnectFail = mTimeOfLastConnectFail != null ? mTimeOfLastConnectFail : mTimeOfFirstConnect;
        final Interval attemptTime_latest = Interval.delta(timeOfLastConnectFail, currentTime);
        final Interval attemptTime_total = Interval.delta(mTimeOfFirstConnect, currentTime);

        mDevice.getManager().getLogger().w(status + ", timing=" + timing);

        mFailCount++;

        if( mHighestStateReached_total == null )
        {
            mHighestStateReached_total = highestStateReached;
        }
        else
        {
            if( highestStateReached != null && highestStateReached.getConnectionOrdinal() > mHighestStateReached_total.getConnectionOrdinal() )
            {
                mHighestStateReached_total = highestStateReached;
            }
        }

        final ConnectionFailEvent moreInfo = P_EventFactory.newConnectionFailEvent
                (
                mDevice, status, timing, mFailCount, attemptTime_latest, attemptTime_total, gattStatus,
                highestStateReached, mHighestStateReached_total, autoConnectUsage, bondReasionFail, txnFailReason, mHistory
                );

        mHistory.add(moreInfo);

        //--- DRK > Not invoking callback if we're attempting short-term reconnect.
        Please retryChoice__PE_Please = mDevice.is(BleDeviceState.RECONNECTING_SHORT_TERM) ? Please.doNotRetry() : invokeCallback(moreInfo);

        //--- DRK > Disabling retry if app-land decided to call connect() themselves in fail callback...hopefully fringe but must check for now.
        retryChoice__PE_Please = mDevice.is(BleDeviceState.CONNECTING_OVERALL) ? Please.doNotRetry() : retryChoice__PE_Please;

        if( status != null && status.wasCancelled() )
        {
            retryChoice__PE_Please = Please.doNotRetry();
        }
        else
        {
            // TODO -=> RB -  Is this reconnect stuff necessary here? Most likely, but things are different in the rewrite version, so will look at this
            // later, when the need arises
//            final P_ReconnectManager reconnectMngr = mDevice.reconnectMngr();
//            final int gattStatusOfOriginalDisconnect = reconnectMngr.gattStatusOfOriginalDisconnect();
//            final boolean wasRunning = reconnectMngr.isRunning();
//
//            reconnectMngr.onConnectionFailed(moreInfo);
//
//            if( wasRunning && !reconnectMngr.isRunning() )
//            {
//                if( mDevice.is(BleDeviceState.RECONNECTING_SHORT_TERM) )
//                {
//                    retryChoice__PE_Please = Please.doNotRetry();
//                    mDevice.ond
//                    mDevice.onNativeDisconnect(/*wasExplicit=*/false, gattStatusOfOriginalDisconnect, /*doShortTermReconnect=*/false, /*saveLastDisconnect=*/true);
//                }
//            }
        }

        if( retryChoice__PE_Please != null && retryChoice__PE_Please.isRetry() )
        {
            // TODO -=> RB - This should be unnecessary, as the reconnect logic is taken care of in BleDevice.
//            mDevice.attemptReconnect();
        }
        else
        {
            mFailCount = 0;
        }

        return retryChoice__PE_Please;

    }

    final Please invokeCallback(final ConnectionFailEvent moreInfo)
    {
        Please retryChoice__PE_Please = Please.doNotRetry();

        if( mConnectionFailListener != null )
        {
            final Please please = mConnectionFailListener.onEvent(moreInfo);
            retryChoice__PE_Please = please != null ? please : null;

            mDevice.getManager().getLogger().checkPlease(please, Please.class);
        }
        else if( mDevice.getManager().mDefaultConnectionFailListener != null )
        {
            final Please please = mDevice.getManager().mDefaultConnectionFailListener.onEvent(moreInfo);
            retryChoice__PE_Please = please != null ? please : null;

            mDevice.getManager().getLogger().checkPlease(please, Please.class);
        }

        retryChoice__PE_Please = retryChoice__PE_Please != null ? retryChoice__PE_Please : Please.doNotRetry();

        return retryChoice__PE_Please;
    }

    public final void setConnectionFailListener(DeviceConnectionFailListener failListener)
    {
        mConnectionFailListener = failListener;
    }

}
