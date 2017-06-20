package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;


final class P_Task_Delayer extends PA_Task
{

    private final Interval m_delayTime;
    private double m_timeDelayed = -1.0;


    public P_Task_Delayer(BleManager manager, I_StateListener listener, Interval delayTime)
    {
        super(manager, listener);
        m_delayTime = delayTime;
    }

    @Override protected BleTask getTaskType()
    {
        return BleTask.DELAY;
    }

    @Override void execute()
    {
        m_timeDelayed = 0.0;
    }

    @Override protected void update(double timeStep)
    {
        super.update(timeStep);
        if (m_timeDelayed >= 0.0)
        {
            m_timeDelayed += timeStep;
            if (m_timeDelayed >= m_delayTime.secs())
            {
                succeed();
            }
        }
    }

    @Override public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.CRITICAL;
    }
}
