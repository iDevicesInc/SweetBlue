package com.idevicesinc.sweetblue;


abstract class P_Task_Transactionable extends P_Task_RequiresConnection
{

    protected P_TaskPriority mPriority = null;


    public P_Task_Transactionable(BleDevice device, IStateListener listener)
    {
        super(device, listener);
    }


    abstract P_TaskPriority defaultPriority();

    @Override public P_TaskPriority getPriority()
    {
        return mPriority != null ? mPriority : defaultPriority();
    }

}
