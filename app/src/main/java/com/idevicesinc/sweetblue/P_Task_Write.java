package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;

import java.util.UUID;


public class P_Task_Write extends P_Task_Transactionable
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

    @Override public void execute()
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

    void onWrite(final ReadWriteListener.ReadWriteEvent event)
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

    byte[] getValue()
    {
        return mValue;
    }

    @Override P_TaskPriority defaultPriority()
    {
        return P_TaskPriority.LOW;
    }

}
