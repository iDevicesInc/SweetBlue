package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.UUID;


public final class P_Task_Write extends P_Task_Transactionable
{

    private UUID mServiceUuid;
    private UUID mCharUuid;
    private ReadWriteListener mListener;
    private byte[] mValue;


    public P_Task_Write(BleDevice device, IStateListener listener, UUID serviceUuid, UUID charUuid, byte[] value, ReadWriteListener writeListener)
    {
        super(device, listener);
        mServiceUuid = serviceUuid;
        mCharUuid = charUuid;
        mListener = writeListener;
        mValue = value;
    }

    @Override public final void execute()
    {
        if (!getDevice().mGattManager.write(mServiceUuid, mCharUuid, mValue))
        {
            ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(getDevice(), mServiceUuid, mCharUuid, ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID,
                    ReadWriteListener.Type.WRITE, ReadWriteListener.Target.CHARACTERISTIC, null, ReadWriteListener.Status.NO_MATCHING_TARGET, 133, 0, 0, true);
            if (mListener != null)
            {
                mListener.onEvent(event);
            }
            failImmediately();
        }
    }

    final void onWrite(final ReadWriteListener.ReadWriteEvent event)
    {
        getManager().mPostManager.postCallback(new Runnable()
        {
            @Override public void run()
            {
                if (mListener != null)
                {
                    mListener.onEvent(event);
                }
            }
        });
        succeed();
    }

    final byte[] getValue()
    {
        return mValue;
    }

    @Override final void onTaskTimedOut()
    {
        super.onTaskTimedOut();
        super.onTaskTimedOut();
        if (mListener != null)
        {
            getManager().mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    if (mListener != null)
                    {
                        ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(getDevice(), mServiceUuid, mCharUuid, Uuids.INVALID, ReadWriteListener.Type.WRITE,
                                ReadWriteListener.Target.CHARACTERISTIC, new byte[0], ReadWriteListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0, 0, false);
                        mListener.onEvent(event);
                    }
                }
            });
        }
    }

    @Override final P_TaskPriority defaultPriority()
    {
        return P_TaskPriority.LOW;
    }

}
