package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;

final class P_Task_BondPopupHack extends PA_Task_RequiresBleOn
{

    private double scanTime = 0;


    public P_Task_BondPopupHack(BleDevice device, I_StateListener listener)
    {
        super(device, listener);
    }

    @Override protected BleTask getTaskType()
    {
        return BleTask.DISCOVER_SERVICES;
    }

    @Override void execute()
    {
        getDevice().layerManager().startDiscovery();
    }

    @Override protected void update(double timeStep)
    {
        super.update(timeStep);
        scanTime += timeStep;
        Interval maxTime = BleDeviceConfig.interval(getDevice().conf_device().forceBondHackInterval, getDevice().conf_mngr().forceBondHackInterval);
        if (Interval.isDisabled(maxTime))
        {
            // In case the interval comes back null, or disabled, set it to one second. It could be argued that disabled should mean 0 delay, but if that
            // were the case, this hack wouldn't work.
            maxTime = Interval.ONE_SEC;
        }
        if (scanTime >= maxTime.secs())
        {
            getDevice().layerManager().cancelDiscovery();
            succeed();
        }
    }

    @Override protected void failWithoutRetry()
    {
        super.failWithoutRetry();
        getManager().getTaskQueue().fail(P_Task_Bond.class, getDevice());
    }

    @Override public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.MEDIUM;
    }
}
