package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.Utils;


public class P_Task_RequestMtu extends P_Task_Transactionable
{

    private final int mMtuSize;
    private final ReadWriteListener mListener;


    public P_Task_RequestMtu(BleDevice device, IStateListener listener, int mtuSize, ReadWriteListener rwlistener)
    {
        super(device, listener);
        mMtuSize = mtuSize;
        mListener = rwlistener;
    }

    @Override public void execute()
    {
        if (Utils.isLollipop())
        {
            getDevice().mGattManager.requestMtuChange(mMtuSize);
        }
        else
        {
            fail();
        }
    }

    void onMtuChangeResult(ReadWriteListener.ReadWriteEvent event)
    {
        if (event.wasSuccess())
        {
            succeed();
        }
        else
        {
            fail();
        }
        if (mListener != null)
        {
            mListener.onEvent(event);
        }
    }

    @Override P_TaskPriority defaultPriority()
    {
        return P_TaskPriority.MEDIUM;
    }
}
