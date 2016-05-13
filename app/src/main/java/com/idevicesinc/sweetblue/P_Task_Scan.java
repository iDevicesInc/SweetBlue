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
    private final boolean mPeriodicScan;


    public P_Task_Scan(P_TaskManager manager, IStateListener listener, Interval scanTime, Interval pauseTime)
    {
        super(manager, listener);
        mScanTime = scanTime;
        mPauseTime = pauseTime;
        mPeriodicScan = Interval.isEnabled(mScanTime) && Interval.isEnabled(pauseTime);
    }

    @Override public P_TaskPriority getPriority()
    {
        return P_TaskPriority.TRIVIAL;
    }

    @Override public void execute()
    {
        getManager().mScanManager.startScan();
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

    @Override void checkTimeOut(long curTimeMs)
    {
        if (Interval.isEnabled(mScanTime) && !isPaused())
        {
            if (mLastScanUpdate != 0)
            {
                mTimeScanning += curTimeMs - mLastScanUpdate;
            }
            mLastScanUpdate = curTimeMs;
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
                    getManager().mScanManager.stopScanNoStateChange();
                    pause();
                }
            }
        }
        else
        {
            super.checkTimeOut(curTimeMs);
        }
    }
}
