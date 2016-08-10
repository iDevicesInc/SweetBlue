package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BleStatuses;
import java.util.ArrayList;


final class P_TransactionManager
{

    private final BleDevice mDevice;
    private boolean isRunning;
    private ArrayList<P_Task> queueTasks;
    private BleTransaction.Auth authTxn;
    private BleTransaction.Init initTxn;
    private BleTransaction mCurrent;
    private EndListener mEndListener = new EndListener();


    P_TransactionManager(BleDevice device)
    {
        mDevice = device;
        queueTasks = new ArrayList<>(0);
    }

    public final boolean isTransactionOperation()
    {
        StackTraceElement[] elements = new Exception().getStackTrace();
        for (StackTraceElement e : elements)
        {
            if (e.getClassName().equals(BleTransaction.class.getName()) || e.getClassName().equals(P_TransactionManager.class.getName()))
            {
                return true;
            }
        }
        return false;
    }

    public final void setAuthTxn(BleTransaction.Auth auth)
    {
        authTxn = auth;
    }

    public final void setInitTxn(BleTransaction.Init init)
    {
        initTxn = init;
    }

    public final BleTransaction.Auth getAuthTxn()
    {
        return authTxn;
    }

    public final BleTransaction.Init getInitTxn()
    {
        return initTxn;
    }

    public final boolean isRunning()
    {
        return isRunning;
    }

    public final boolean isAtomic()
    {
        return mCurrent != null && mCurrent.isAtomic();
    }

    public final void queueTask(P_Task task)
    {
        queueTasks.add(task);
    }

    public final void start(BleTransaction txn)
    {
        isRunning = true;
        mCurrent = txn;
        txn.init(mDevice, mEndListener);
        txn.mRunning = true;
        txn.start(mDevice);
        txn.mTimeStarted = System.currentTimeMillis();
    }

    public final void cancelAllTxns()
    {
        if (mCurrent != null && mCurrent.isRunning())
        {
            mCurrent.cancel();
        }
        if (authTxn != null && authTxn.isRunning())
        {
            authTxn.cancel();
        }
        if (initTxn != null && initTxn.isRunning())
        {
            initTxn.cancel();
        }
    }

    final BleTransaction getCurrent()
    {
        return mCurrent;
    }

    private void addQueuedTasks()
    {
        for (int i = 0; i < queueTasks.size(); i++)
        {
            mDevice.getManager().mTaskManager.add(queueTasks.get(i));
        }
        queueTasks.clear();
    }

    private final class EndListener implements BleTransaction.TxnEndListener
    {

        @Override public final void onTxnEnded(BleTransaction txn, EndReason endReason)
        {
            if (txn == authTxn)
            {
                if (endReason == EndReason.SUCCEEDED)
                {
                    mDevice.stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.AUTHENTICATED, true,
                            BleDeviceState.AUTHENTICATING, false);
                    if (initTxn != null)
                    {
                        mDevice.onInitialized();
                        start(initTxn);
                    }
                    else
                    {
                        mDevice.onInitialized();
                        addQueuedTasks();
                    }
                }
                else
                {
                    mDevice.disconnect();
                }
            }
            else if (txn == initTxn)
            {
                if (endReason == EndReason.SUCCEEDED)
                {
                    mDevice.onInitialized();
                    addQueuedTasks();
                }
                else
                {
                    mDevice.disconnect();
                }
            }
            else if (txn instanceof BleTransaction.Ota)
            {
                mDevice.stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.PERFORMING_OTA, false);
                addQueuedTasks();
            }
        }
    }

}
