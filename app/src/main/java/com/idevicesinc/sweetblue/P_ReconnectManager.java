package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BleStatuses;

class P_ReconnectManager
{

    private final BleDevice mDevice;
    private int mReconnectTries;
    private int mMaxReconnecTries;


    public P_ReconnectManager(BleDevice device)
    {
        mDevice = device;
        mMaxReconnecTries = mDevice.getConfig().reconnectionTries;
    }

    void setMaxReconnectTries(int tries)
    {
        mMaxReconnecTries = tries;
    }

    boolean shouldFail()
    {
        boolean fail = mReconnectTries >= mMaxReconnecTries;
        if (fail)
        {
            if (mReconnectTries > 0)
            {
                mDevice.getManager().popWakeLock();
            }
            mDevice.stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.RECONNECTING_SHORT_TERM, false);
        }
        return fail;
    }

    void reconnect(int gattStatus)
    {
        if (mReconnectTries == 0)
        {
            mDevice.getManager().pushWakeLock();
        }
        mReconnectTries++;
        mDevice.stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, gattStatus, BleDeviceState.CONNECTED, false, BleDeviceState.RECONNECTING_SHORT_TERM, true);
        mDevice.connect(mDevice.getConnectionFailListener());
    }

}
