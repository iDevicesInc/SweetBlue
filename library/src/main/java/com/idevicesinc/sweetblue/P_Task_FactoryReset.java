package com.idevicesinc.sweetblue;



import com.idevicesinc.sweetblue.utils.Utils_Reflection;


public class P_Task_FactoryReset extends PA_Task
{

    public P_Task_FactoryReset(BleManager manager, I_StateListener listener)
    {
        super(manager, listener);
    }


    @Override
    protected BleTask getTaskType()
    {
        return BleTask.NUKE_BLE_STACK;
    }

    @Override
    void execute()
    {
        // This only gets called after ble is turned off, so we'll succeed right after calling it.
        Utils_Reflection.callBooleanReturnMethod(getManager().getNativeAdapter(), "factoryReset", false);
        succeed();
    }

    @Override
    public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.CRITICAL;
    }
}
