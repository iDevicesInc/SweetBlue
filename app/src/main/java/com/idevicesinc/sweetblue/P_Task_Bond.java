package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.compat.K_Util;
import com.idevicesinc.sweetblue.listeners.BondListener;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;


public final class P_Task_Bond extends P_Task_RequiresBleOn
{

    private static final String METHOD_NAME__CREATE_BOND = "createBond";

    private final P_TaskPriority mPriority;


    public P_Task_Bond(BleDevice device, IStateListener listener, P_TaskPriority priority)
    {
        super(device, listener);
        mPriority = priority;
    }

    public P_Task_Bond(BleDevice device, IStateListener listener)
    {
        this(device, listener, P_TaskPriority.MEDIUM);
    }

    @Override public final P_TaskPriority getPriority()
    {
        return mPriority;
    }

    @Override public final void execute()
    {
        if (getDevice().is(BleDeviceState.BONDED))
        {
            getManager().getLogger().w("Already bonded!");
            redundant();
        }
        else if (getDevice().is(BleDeviceState.BONDING))
        {
            // do nothing
        }
        else
        {
            if (!createBond())
            {
                failImmediately();
                getManager().getLogger().w("Bond failed immediately!");
            }
        }
    }

    @Override public final void update(long curTimeMs)
    {
        if (timeExecuting() > 10000) {
            getDevice().onBondFailed(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.BOND_FAIL_REASON_NOT_AVAILABLE, BondListener.Status.FAILED_EVENTUALLY);
            fail();
        }
    }

    private boolean createBond()
    {
        if (getDevice().getConfig().useLeTransportForBonding)
        {
            if (!createBondUsingLe())
            {
                return createBondAuto();
            }
            else
            {
                return true;
            }
        }
        else
        {
            return createBondAuto();
        }
    }

    private boolean createBondAuto()
    {
        if (Utils.isKitKat())
        {
            return K_Util.createBond(getDevice());
        }
        else
        {
            mFailReason = BleStatuses.BOND_FAIL_REASON_NOT_AVAILABLE;
            return false;
        }
    }

    private boolean createBondUsingLe()
    {
        if (Utils.isKitKat())
        {
            final Class[] paramTypes = new Class[] { int.class };
            return Utils_Reflection.callBooleanReturnMethod(getDevice().getNative(), METHOD_NAME__CREATE_BOND, paramTypes, getManager().mConfig.loggingEnabled, 2/*BluetoothDevice.TRANSPORT_LE*/);
        }
        else
        {
            mFailReason = BleStatuses.BOND_FAIL_REASON_NOT_AVAILABLE;
            return false;
        }
    }
}
