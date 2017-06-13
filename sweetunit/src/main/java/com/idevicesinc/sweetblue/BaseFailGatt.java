package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import java.util.Random;


public class BaseFailGatt extends UnitTestGatt
{

    private Interval m_delayTime;


    public BaseFailGatt(BleDevice device)
    {
        super(device);
    }

    public BaseFailGatt(BleDevice device, GattDatabase gattDb)
    {
        super(device, gattDb);
    }

    public void setDelayTime(Interval delay)
    {
        m_delayTime = delay;
    }

    public Interval getDelayTime()
    {
        if (Interval.isDisabled(m_delayTime))
        {
            Random r = new Random();
            return Interval.millis(r.nextInt(4999) + 1);
        }
        else
        {
            return m_delayTime;
        }
    }
}
