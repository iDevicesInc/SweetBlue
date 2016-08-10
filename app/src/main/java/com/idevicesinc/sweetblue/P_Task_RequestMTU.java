package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_Task_RequestMtu extends P_Task_Transactionable
{

    private final int mMtuSize;
    private final ReadWriteListener mListener;


    public P_Task_RequestMtu(BleDevice device, IStateListener listener, int mtuSize, ReadWriteListener rwlistener)
    {
        super(device, listener);
        mMtuSize = mtuSize;
        mListener = rwlistener;
    }

    @Override public final void execute()
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

    @Override void onTaskTimedOut()
    {
        super.onTaskTimedOut();
        if (mListener != null)
        {
            ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(getDevice(), ReadWriteListener.Type.WRITE, getDevice().getRssi(),
                    ReadWriteListener.Status.TIMED_OUT, BleStatuses.GATT_REQUEST_MTU_TIME_OUT, 0, 0, true);
            mListener.onEvent(event);
        }
    }

    final void onMtuChangeResult(ReadWriteListener.ReadWriteEvent event)
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

    @Override final P_TaskPriority defaultPriority()
    {
        return P_TaskPriority.MEDIUM;
    }
}
