package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import java.util.UUID;


public class BleNotify extends BleOp<BleNotify>
{

    Interval m_forceReadTimeout = Interval.INFINITE;


    public BleNotify()
    {
    }

    public BleNotify(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleNotify(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }


    @Override
    public final boolean isValid()
    {
        return charUuid != null;
    }

    @Override
    final BleNotify createDuplicate()
    {
        BleNotify notify = getDuplicateOp();
        notify.m_forceReadTimeout = m_forceReadTimeout;
        return notify;
    }

    @Override
    final BleNotify createNewOp()
    {
        return new BleNotify();
    }

    public final BleNotify setForceReadTimeout(Interval timeout)
    {
        m_forceReadTimeout = timeout;
        return this;
    }
}
