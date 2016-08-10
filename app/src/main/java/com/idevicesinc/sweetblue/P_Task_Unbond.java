package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Utils_Reflection;

final class P_Task_Unbond extends P_Task_RequiresBleOn
{

    private static final String REMOVE_BOND			= "removeBond";
    private static final String CANCEL_BOND_PROCESS	= "cancelBondProcess";


    private final P_TaskPriority mPriority;


    public P_Task_Unbond(BleDevice device, IStateListener listener, P_TaskPriority priority)
    {
        super(device, listener);
        mPriority = priority;
    }

    public P_Task_Unbond(BleDevice device, IStateListener listener)
    {
        this(device, listener, P_TaskPriority.MEDIUM);
    }

    @Override public final P_TaskPriority getPriority()
    {
        return mPriority;
    }

    @Override public final void execute()
    {
        if (!getDevice().mGattManager.isBonded())
        {
            getManager().getLogger().w("Device is not bonded!");
        }
        else if (getDevice().mGattManager.isBonding())
        {
            if (!cancelBonding())
            {
                failImmediately();
            }
        }
        else
        {
            if (!removeBond())
            {
                failImmediately();
            }
        }
    }

    private boolean removeBond()
    {
        return Utils_Reflection.callBooleanReturnMethod(getDevice().getNative(), REMOVE_BOND, getManager().mConfig.loggingEnabled);
    }

    private boolean cancelBonding()
    {
        return Utils_Reflection.callBooleanReturnMethod(getDevice().getNative(), CANCEL_BOND_PROCESS, getManager().mConfig.loggingEnabled);
    }
}
