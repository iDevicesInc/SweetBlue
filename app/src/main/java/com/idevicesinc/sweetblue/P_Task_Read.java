package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import java.util.UUID;


public class P_Task_Read extends P_Task_RequiresConnection
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

    @Override public P_TaskPriority getPriority()
    {
        return P_TaskPriority.MEDIUM;
    }

    @Override public void execute()
    {
        if (!getDevice().mGattManager.read(mServiceUuid, mCharUuid))
        {
            failImmediately();
        }
    }

    /**
     * Gets called from {@link P_GattManager} when a read comes in.
     */
    void onRead(ReadWriteListener.ReadWriteEvent event)
    {
        if (mListener != null)
        {
            mListener.onEvent(event);
        }
        succeed();
    }

}
