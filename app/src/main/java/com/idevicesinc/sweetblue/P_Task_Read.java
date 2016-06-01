package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import java.util.UUID;


public class P_Task_Read extends P_Task_RequiresConnection
{

    private ReadWriteListener mListener;
    private UUID mCharUuid;


    public P_Task_Read(BleDevice device, IStateListener stateListener, UUID charUuid, ReadWriteListener listener)
    {
        super(device, stateListener);
        mListener = listener;
        mCharUuid = charUuid;
    }

    @Override public P_TaskPriority getPriority()
    {
        return P_TaskPriority.MEDIUM;
    }

    @Override public void execute()
    {
        getDevice().mGattManager.read(mCharUuid);
    }

}
