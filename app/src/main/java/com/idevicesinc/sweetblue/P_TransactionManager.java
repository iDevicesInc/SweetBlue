package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BleStatuses;
import java.util.ArrayList;


class P_TransactionManager
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

    public boolean isTransactionOperation()
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

    public void setAuthTxn(BleTransaction.Auth auth)
    {
        authTxn = auth;
    }

    public void setInitTxn(BleTransaction.Init init)
    {
        initTxn = init;
    }

    public BleTransaction.Auth getAuthTxn()
    {
        return authTxn;
    }

    public BleTransaction.Init getInitTxn()
    {
        return initTxn;
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public boolean isAtomic()
    {
        return mCurrent != null && mCurrent.isAtomic();
    }

    public void queueTask(P_Task task)
    {
        queueTasks.add(task);
    }

    public void start(BleTransaction txn)
    {
        isRunning = true;
        mCurrent = txn;
        txn.init(mDevice, mEndListener);
        txn.mRunning = true;
        txn.start(mDevice);
        txn.mTimeStarted = System.currentTimeMillis();
    }

    public void cancelAllTxns()
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

    BleTransaction getCurrent()
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

    private class EndListener implements BleTransaction.TxnEndListener
    {

        @Override public void onTxnEnded(BleTransaction txn, EndReason endReason)
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
