package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;


final class P_Task_Scan extends PA_Task_RequiresBleOn
{

    private final boolean m_explicit = true;
    private final double m_scanTime;
    private final PE_TaskPriority m_priority;
    private final boolean m_periodicScan;


    public P_Task_Scan(BleManager manager, I_StateListener listener, double scanTime, boolean periodicScan, PE_TaskPriority priority)
    {
        super(manager, listener);

        m_priority = priority == null ? PE_TaskPriority.TRIVIAL : priority;

        m_scanTime = scanTime;

        m_periodicScan = periodicScan;
    }

    public E_Intent getIntent()
    {
        return m_explicit ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
    }

    @Override protected double getInitialTimeout()
    {
        // Account for the classic scan boost here, we don't want to count the time doing the classic boost towards the timeout of the BLE scan
        if (isClassicBoosted())
        {
            return m_scanTime + getManager().m_config.scanClassicBoostLength.secs();
        }
        return m_scanTime;
    }

    @Override public void execute()
    {
        //--- DRK > Because scanning has to be done on a separate thread, isExecutable() can return true
        //---		but then by the time we get here it can be false. isExecutable() is currently not thread-safe
        //---		either, thus we're doing the manual check in the native stack. Before 5.0 the scan would just fail
        //---		so we'd fail as we do below, but Android 5.0 makes this an exception for at least some phones (OnePlus One (A0001)).
        if (false == isBluetoothEnabled())
        {
            fail();
        }
        else
        {

            if (isClassicBoosted())
            {
                if (!getManager().getScanManager().classicBoost(getManager().m_config.scanClassicBoostLength.secs()))
                {
                    fail();

                    getManager().uhOh(BleManager.UhOhListener.UhOh.CLASSIC_DISCOVERY_FAILED);
                }
            }
            else
            {
                if (!getManager().getScanManager().startScan(getIntent(), m_scanTime, m_periodicScan))
                {
                    fail();

                    getManager().uhOh(BleManager.UhOhListener.UhOh.START_BLE_SCAN_FAILED);
                }
                else
                {
                    // Success for now
                }
            }
        }
    }

    boolean isClassicBoosted()
    {
        boolean isClassicScan = getManager().m_config.scanApi == BleScanApi.CLASSIC || getManager().m_config.scanMode == BleScanMode.CLASSIC;
        return !isClassicScan && Interval.isEnabled(getManager().m_config.scanClassicBoostLength);
    }

    void onClassicBoostFinished()
    {
        if (!getManager().getScanManager().startScan(getIntent(), m_scanTime, m_periodicScan))
        {
            fail();

            getManager().uhOh(BleManager.UhOhListener.UhOh.START_BLE_SCAN_FAILED);
        }
    }

    private boolean isBluetoothEnabled()
    {
        return getManager().isBluetoothEnabled();
    }

    private double getMinimumScanTime()
    {
        return Interval.secs(getManager().m_config.idealMinScanTime);
    }

    @Override protected void update(double timeStep)
    {
        if (this.getState() == PE_TaskState.EXECUTING)
        {
            if (getTotalTimeExecuting(getManager().currentTime()) >= getMinimumScanTime() && (getQueue().getSize() > 0 && isSelfInterruptableBy(getQueue().peek())))
            {
                selfInterrupt();
            }
            else if (getManager().getScanManager().isClassicScan() && getTotalTimeExecuting(getManager().currentTime()) >= BleManagerConfig.MAX_CLASSIC_SCAN_TIME)
            {
                selfInterrupt();
            }
        }
    }

    @Override public PE_TaskPriority getPriority()
    {
        return m_priority;
    }

    private boolean isSelfInterruptableBy(final PA_Task otherTask)
    {
//        if (otherTask.getPriority().ordinal() > PE_TaskPriority.TRIVIAL.ordinal())
        if (otherTask.getPriority().ordinal() > m_priority.ordinal())
        {
            return true;
        }
//        else if (otherTask.getPriority().ordinal() >= this.getPriority().ordinal())
        else if (otherTask.getPriority().ordinal() == m_priority.ordinal())
        {
            //--- DRK > Not sure infinite timeout check really matters here.
            return this.getTotalTimeExecuting() >= getMinimumScanTime();
//				return getTimeout() == TIMEOUT_INFINITE && this.getTotalTimeExecuting() >= getManager().m_config.minimumScanTime;
        }
        else
        {
            return false;
        }
    }

    @Override public boolean isInterruptableBy(PA_Task otherTask)
    {
        return otherTask.getPriority().ordinal() > m_priority.ordinal();
//        return otherTask.getPriority().ordinal() >= PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING.ordinal();
    }

    @Override public boolean isExplicit()
    {
        return m_explicit;
    }

    @Override protected BleTask getTaskType()
    {
        return null;
    }
}
