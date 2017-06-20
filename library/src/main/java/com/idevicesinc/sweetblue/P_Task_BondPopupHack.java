package com.idevicesinc.sweetblue;


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
        if (scanTime > 1)
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
