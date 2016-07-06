package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.UUID;


public class P_Task_Read extends P_Task_Transactionable
{

    private ReadWriteListener mListener;
    private UUID mCharUuid;
    private UUID mServiceUuid;


    public P_Task_Read(BleDevice device, IStateListener stateListener, UUID charUuid, ReadWriteListener listener)
    {
        this(device, stateListener, null, charUuid, listener);
    }

    public P_Task_Read(BleDevice device, IStateListener stateListener, UUID serviceUuid, UUID charUuid, ReadWriteListener listener)
    {
        super(device, stateListener);
        mListener = listener;
        mCharUuid = charUuid;
        mServiceUuid = serviceUuid;
    }

    @Override P_TaskPriority defaultPriority()
    {
        return P_TaskPriority.LOW;
    }

    @Override public void execute()
    {
        if (!getDevice().mGattManager.read(mServiceUuid, mCharUuid))
        {
            ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(getDevice(), mServiceUuid, mCharUuid, ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID,
                    ReadWriteListener.Type.READ, ReadWriteListener.Target.CHARACTERISTIC, null, ReadWriteListener.Status.NO_MATCHING_TARGET, 133, 0, 0, true);
            if (mListener != null)
            {
                mListener.onEvent(event);
            }
            failImmediately();
        }
    }

    @Override void onTaskTimedOut()
    {
        super.onTaskTimedOut();
        if (mListener != null)
        {
            getManager().mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    if (mListener != null)
                    {
                        ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(getDevice(), mServiceUuid, mCharUuid, Uuids.INVALID, ReadWriteListener.Type.READ,
                                ReadWriteListener.Target.CHARACTERISTIC, new byte[0], ReadWriteListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0, 0, false);
                        mListener.onEvent(event);
                    }
                }
            });
        }
    }

    /**
     * Gets called from {@link P_GattManager} when a read comes in.
     */
    void onRead(final ReadWriteListener.ReadWriteEvent event)
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

}
