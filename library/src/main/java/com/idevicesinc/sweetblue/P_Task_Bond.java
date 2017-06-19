package com.idevicesinc.sweetblue;

import android.annotation.SuppressLint;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_Task_Bond extends PA_Task_RequiresBleOn
{
    private static final String METHOD_NAME__CREATE_BOND = "createBond";

    //--- DRK > Originally used because for tab 4 (and any other bonding failure during connection) we'd force disconnect from the connection failing
    //---		and then put another bond task on the queue, but because we hadn't actually yet killed the transaction lock, the bond task would
    //---		cut the unbond task in the queue. Not adding bonding task in the disconnect flow now though so this is probably useless for future use.
    static enum E_TransactionLockBehavior
    {
        PASSES,
        DOES_NOT_PASS;
    }

    private final PE_TaskPriority m_priority;
    private final boolean m_explicit;
    private final boolean m_partOfConnection;
    private final boolean m_direct;
    private final E_TransactionLockBehavior m_lockBehavior;

    private int m_failReason = BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE;

    public P_Task_Bond(BleDevice device, boolean explicit, boolean direct, boolean partOfConnection, I_StateListener listener, PE_TaskPriority priority, E_TransactionLockBehavior lockBehavior)
    {
        super(device, listener);

        m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
        m_explicit = explicit;
        m_partOfConnection = partOfConnection;
        m_lockBehavior = lockBehavior;
        m_direct = direct;
    }

    public P_Task_Bond(BleDevice device, boolean explicit, boolean direct, boolean partOfConnection, I_StateListener listener, E_TransactionLockBehavior lockBehavior)
    {
        this(device, explicit, direct, partOfConnection, listener, null, lockBehavior);
    }

    public final boolean isDirect()
    {
        return m_direct;
    }

    @Override public final boolean isExplicit()
    {
        return m_explicit;
    }

    @SuppressLint("NewApi")
    @Override public final void execute()
    {
        //--- DRK > Commenting out this block for now because Android can lie and tell us we're bonded when we're actually not,
        //---		so therefore we always try to force a bond regardless. Not sure if that actually forces
        //---		Android to "come clean" about its actual bond status or not, but worth a try.
        //---		UPDATE: No, it doesn't appear this works...Android lies even to itself, so commenting this back in.
        if (getDevice().m_nativeWrapper.isNativelyBonded())
        {
            getLogger().w("Already bonded!");

            succeed();
        }
        else
        {
            if (getDevice().m_nativeWrapper./*already*/isNativelyBonding())
            {
                // nothing to do
            }
            else if (false == m_explicit)
            {
                // DRK > Fail cause we're not natively bonding and this task was implicit, meaning we should be implicitly bonding.
                fail();
            }
            else if (false == createBond())
            {
                failImmediately();

                getLogger().w("Bond failed immediately.");
            }
        }
    }

    private boolean createBond()
    {
        final boolean useLeTransportForBonding = BleDeviceConfig.bool(getDevice().conf_device().useLeTransportForBonding, getDevice().conf_mngr().useLeTransportForBonding);

        if (useLeTransportForBonding)
        {
            final boolean theSneakyWayWorked = createBond_theSneakyWay();

            if (theSneakyWayWorked == false)
            {
                return createBond_theNormalWay();
            }
            else
            {
                return true;
            }
        }
        else
        {
            return createBond_theNormalWay();
        }
    }

    private boolean createBond_theNormalWay()
    {
        if (Utils.isKitKat())
        {
            return getDevice().layerManager().createBond();
        }
        else
        {
            m_failReason = BleStatuses.BOND_FAIL_REASON_NOT_AVAILABLE;
            return false;
        }
    }

    private boolean createBond_theSneakyWay()
    {
        if (Utils.isKitKat())
        {
            return getDevice().layerManager().createBondSneaky(METHOD_NAME__CREATE_BOND);
        }
        else
        {
            return false;
        }
    }

    @Override public final boolean isMoreImportantThan(PA_Task task)
    {
        if (task instanceof P_Task_TxnLock)
        {
            if (m_lockBehavior == E_TransactionLockBehavior.PASSES)
            {
                P_Task_TxnLock task_cast = (P_Task_TxnLock) task;

                if (this.getDevice() == task_cast.getDevice())
                {
                    return true;
                }
            }
        }

        return super.isMoreImportantThan(task);
    }

    public final void onNativeSuccess()
    {
        succeed();
    }

    public final void onNativeFail(int failReason)
    {
        m_failReason = failReason;

        fail();
    }

    public final int getFailReason()
    {
        return m_failReason;
    }

    @Override public final PE_TaskPriority getPriority()
    {
        return m_priority;
    }

    @Override protected final boolean isSoftlyCancellableBy(PA_Task task)
    {
        if (this.getDevice().equals(task.getDevice()))
        {
            if (task.getClass() == P_Task_Disconnect.class)
            {
                if (this.m_partOfConnection && this.getState() == PE_TaskState.EXECUTING)
                {
                    return true;
                }
            }
            else if (task.getClass() == P_Task_Unbond.class)
            {
                return true;
            }
        }

        return super.isSoftlyCancellableBy(task);
    }

    @Override protected final BleTask getTaskType()
    {
        return BleTask.BOND;
    }
}
