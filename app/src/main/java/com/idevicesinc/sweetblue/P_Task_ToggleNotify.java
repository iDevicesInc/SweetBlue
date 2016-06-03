package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.ReadWriteListener;

import java.util.UUID;

public class P_Task_ToggleNotify extends P_Task_RequiresConnection
{

    private UUID mServiceUuid;
    private UUID mCharUuid;
    private ReadWriteListener mListener;
    private boolean mEnable;


    public P_Task_ToggleNotify(BleDevice device, IStateListener listener, UUID serviceUuid, UUID charUuid, boolean enable, ReadWriteListener successListener)
    {
        super(device, listener);
        mServiceUuid = serviceUuid;
        mCharUuid = charUuid;
        mListener = successListener;
        mEnable = enable;
    }

    public P_Task_ToggleNotify(BleDevice device, IStateListener listener, UUID charUuid, boolean enable, ReadWriteListener successListener)
    {
        this(device, listener, null, charUuid, enable, successListener);
    }

    @Override public void execute()
    {
        if (!getDevice().mGattManager.enableNotify(mServiceUuid, mCharUuid, mEnable))
        {
            failImmediately();
        }
    }

    void onToggleNotifyResult(ReadWriteListener.ReadWriteEvent event)
    {
        if (mListener != null)
        {
            mListener.onEvent(event);
            if (event.wasSuccess())
            {
                succeed();
            }
            else
            {
                fail();
            }
        }
    }

    boolean enabling()
    {
        return mEnable;
    }

    @Override public P_TaskPriority getPriority()
    {
        return P_TaskPriority.MEDIUM;
    }
}
