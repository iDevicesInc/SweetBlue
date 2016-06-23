package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;


public class P_Task_Scan extends P_Task_RequiresBleOn
{

    private final Interval mScanTime;
    private final Interval mPauseTime;
    private long mLastPauseUpdate;
    private long mTimePaused;
    private long mLastScanUpdate;
    private long mTimeScanning;
    private long mLastResetTime;
    private final boolean mPeriodicScan;


    public P_Task_Scan(P_TaskManager manager, IStateListener listener, Interval scanTime, Interval pauseTime)
    {
        super(manager, listener);
        mScanTime = scanTime;
        mPauseTime = pauseTime;
        mPeriodicScan = Interval.isEnabled(mScanTime) && Interval.isEnabled(mPauseTime);
    }

    @Override public P_TaskPriority getPriority()
    {
        return P_TaskPriority.TRIVIAL;
    }

    @Override public void execute()
    {
        if (getManager().is(BleManagerState.SCAN_READY) || getManager().mConfig.revertToClassicDiscoveryIfNeeded)
        {
            if (!getManager().mScanManager.startScan())
            {
                failImmediately();
            }
            else
            {
                // Successfully started the scan
            }
        }
        else
        {
            getManager().getLogger().e("Tried to start scan, but failed because scanning is not ready (most likely due to missing permissions, or location services is not on).");
            fail();
        }
    }

    @Override public void update(long curTimeMs)
    {
        if (mPeriodicScan && isPaused())
        {
            if (mLastPauseUpdate != 0)
            {
                mTimePaused += curTimeMs - mLastPauseUpdate;
            }
            mLastPauseUpdate = curTimeMs;
            if (mTimePaused >= mPauseTime.millis())
            {
                mTimePaused = 0;
                mLastPauseUpdate = 0;
                getManager().mScanManager.startScan();
                resume();
            }
        }
    }

    public long timeScanning()
    {
        return mTimeScanning;
    }

    @Override public boolean isInterruptible()
    {
        return true;
    }

    @Override void onInterrupted()
    {
        super.onInterrupted();
        getManager().mScanManager.pauseScan();
    }

    @Override void checkTimeOut(long curTimeMs)
    {
        if (mLastScanUpdate != 0)
        {
            mTimeScanning += curTimeMs - mLastScanUpdate;
        }
        mLastScanUpdate = curTimeMs;
        if (Interval.isEnabled(mScanTime) && !isPaused())
        {
            if (mTimeScanning >= mScanTime.millis())
            {
                if (!mPeriodicScan)
                {
                    getManager().mScanManager.stopScan();
                    succeed();
                }
                else
                {
                    mLastScanUpdate = 0;
                    mTimeScanning = 0;
                    getManager().mScanManager.pauseScan();
                    pause();
                }
            }
        }
        else
        {
            if (mLastResetTime == 0)
            {
                mLastResetTime = curTimeMs;
            }
            if (curTimeMs - mLastResetTime >= getManager().mConfig.scanResetInterval.millis())
            {
                getManager().mScanManager.stopScan();
                getManager().mScanManager.startScan();
                mLastResetTime = curTimeMs;
            }
        }
    }
}
