package com.idevicesinc.sweetblue;


public abstract class BleTransaction
{

    TxnEndListener mEndListener;
    private BleDevice mDevice;
    long mTimeStarted;
    boolean mRunning;


    void init(BleDevice device, TxnEndListener listener)
    {
        mDevice = device;
        mEndListener = listener;
    }

    public abstract void start(BleDevice device);


    public abstract static class Auth extends BleTransaction {}

    public abstract static class Init extends BleTransaction {}

    public abstract static class Ota extends BleTransaction {}

    protected void onEnd(BleDevice device, TxnEndListener.EndReason reason)
    {
    }

    protected final void succeed()
    {
        end(TxnEndListener.EndReason.SUCCEEDED);
    }

    protected final void fail()
    {
        end(TxnEndListener.EndReason.FAILED);
    }

    protected final void cancel()
    {
        end(TxnEndListener.EndReason.CANCELED);
    }

    protected BleDevice getDevice()
    {
        return mDevice;
    }

    boolean isRunning()
    {
        return mRunning;
    }

    private void end(TxnEndListener.EndReason reason)
    {
        mRunning = false;
        onEnd(mDevice, reason);
        if (mEndListener != null)
        {
            mEndListener.onTxnEnded(this, reason);
        }
    }

    protected boolean isAtomic()
    {
        return false;
    }

    interface TxnEndListener
    {
        void onTxnEnded(BleTransaction txn, EndReason endReason);

        enum EndReason
        {
            SUCCEEDED,
            CANCELED,
            FAILED;
        }
    }

}
