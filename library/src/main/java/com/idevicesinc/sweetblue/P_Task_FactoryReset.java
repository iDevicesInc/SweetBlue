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
        // It seems when this method is invoked, it causes the BLE radio to turn off. So, we won't call success() here, as it will
        // be handled in P_BleManager_Listeners when we see the state change back to ON.
        Utils_Reflection.callBooleanReturnMethod(getManager().getNativeAdapter(), "factoryReset", false);
    }

    void onBleTurnedOff()
    {
        getManager().managerLayer().enable();
    }

    @Override
    public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.CRITICAL;
    }
}
