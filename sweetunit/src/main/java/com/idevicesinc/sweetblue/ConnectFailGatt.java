package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;


public class ConnectFailGatt extends UnitTestGatt
{

    private final FailurePoint m_failPoint;
    private final FailureType m_failType;


    public ConnectFailGatt(BleDevice device, FailurePoint failPoint)
    {
        this(device, failPoint, FailureType.DISCONNECT_GATT_ERROR);
    }

    public ConnectFailGatt(BleDevice device, FailurePoint failPoint, FailureType failType)
    {
        this(device, failPoint, failType, Interval.DISABLED);
    }

    public ConnectFailGatt(BleDevice device, FailurePoint failPoint, FailureType failType, Interval delayTime)
    {
        super(device);
        m_failPoint = failPoint;
        m_failType = failType;
        setDelayTime(delayTime);
    }

    public ConnectFailGatt(BleDevice device, GattDatabase gattDb, FailurePoint failPoint, FailureType failType)
    {
        this(device, gattDb, failPoint, failType, Interval.DISABLED);
    }

    public ConnectFailGatt(BleDevice device, GattDatabase gattDb, FailurePoint failPoint, FailureType failType, Interval delayTime)
    {
        super(device, gattDb);
        m_failPoint = failPoint;
        m_failType = failType;
        setDelayTime(delayTime);
    }

    @Override
    public void setToConnecting()
    {
        if (m_failPoint == FailurePoint.PRE_CONNECTING_BLE)
        {
            NativeUtil.setToDisconnected(getBleDevice(), getStatus(), getDelayTime());
        }
        else
        {
            super.setToConnecting();
        }
    }

    @Override
    public void setToConnected()
    {
        if (m_failPoint == FailurePoint.POST_CONNECTING_BLE)
        {
            NativeUtil.setToDisconnected(getBleDevice(), getStatus(), getDelayTime());
        }
        else
        {
            super.setToConnected();
        }
    }

    @Override
    public void setServicesDiscovered()
    {
        if (m_failPoint == FailurePoint.SERVICE_DISCOVERY)
        {
            if (m_failType == FailureType.SERVICE_DISCOVERY_FAILED)
            {
                NativeUtil.failDiscoverServices(getBleDevice(), getStatus(), getDelayTime());
            }
            else
            {
                NativeUtil.setToDisconnected(getBleDevice(), getStatus(), getDelayTime());
            }
        }
    }

    private int getStatus()
    {
        switch (m_failType)
        {
            case DISCONNECT_GATT_ERROR:
            case SERVICE_DISCOVERY_FAILED:
                return BleStatuses.GATT_ERROR;
            default:
                return BleStatuses.GATT_STATUS_NOT_APPLICABLE;
        }
    }

    public enum FailurePoint
    {
        PRE_CONNECTING_BLE,
        POST_CONNECTING_BLE,
        SERVICE_DISCOVERY
    }

    public enum FailureType
    {
        DISCONNECT_GATT_ERROR,
        TIMEOUT,
        SERVICE_DISCOVERY_FAILED
    }
}
