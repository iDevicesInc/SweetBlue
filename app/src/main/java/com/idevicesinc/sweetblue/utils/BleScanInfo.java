package com.idevicesinc.sweetblue.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BleScanInfo implements UsesCustomNull, Cloneable
{

    public final static BleScanInfo NULL = new BleScanInfo(true);

    private int mManufacturerId;
    private byte[] mManufacturerData;
    private final Pointer<Integer> mAdvFlags;
    private final Pointer<Integer> mTxPower;
    private final List<UUID> mServiceUuids;
    private final Map<UUID, byte[]> mServiceData;
    private String mLocalName;
    private byte[] mScanRecord;
    private boolean mIsNull;


    public BleScanInfo(boolean isNull)
    {
        mManufacturerId = -1;
        mIsNull = isNull;
        if (mIsNull)
        {
            mManufacturerData = new byte[0];
            mAdvFlags = new Pointer<>(0);
            mTxPower = new Pointer<>(0);
            mServiceUuids = new ArrayList<>(0);
            mServiceData = new HashMap<>(0);
            mLocalName = "NULL";
        }
        else
        {
            mManufacturerData = new byte[32];
            mAdvFlags = new Pointer<>(0);
            mTxPower = new Pointer<>(0);
            mServiceUuids = new ArrayList<>();
            mServiceData = new HashMap<>();
            mLocalName = "";
        }
    }

    public void setRawScanRecord(byte[] scanRecord)
    {
        mScanRecord = scanRecord;
    }

    public void clearServiceData()
    {
        mServiceData.clear();
    }

    public void populateServiceData(Map<UUID, byte[]> data)
    {
        mServiceData.putAll(data);
    }

    public void clearServiceUUIDs()
    {
        mServiceUuids.clear();
    }

    public void populateServiceUUIDs(List<UUID> uuids)
    {
        mServiceUuids.addAll(uuids);
    }

    public void setManufacturerId(int id)
    {
        mManufacturerId = id;
    }

    public void setManufacturerData(byte[] data)
    {
        mManufacturerData = data;
    }

    public void setAdvFlags(int advFlags)
    {
        mAdvFlags.value = advFlags;
    }

    public void setTxPower(int power)
    {
        mTxPower.value = power;
    }

    public void setName(String name)
    {
        mLocalName = name;
    }

    public byte[] getRawScanRecord()
    {
        return mScanRecord;
    }

    public int getManufacturerId()
    {
        return mManufacturerId;
    }

    public byte[] getManufacturerData()
    {
        return mManufacturerData;
    }

    public Pointer<Integer> getAdvFlags()
    {
        return mAdvFlags;
    }

    public Pointer<Integer> getTxPower()
    {
        return mTxPower;
    }

    public List<UUID> getServiceUUIDS()
    {
        return mServiceUuids;
    }

    public Map<UUID, byte[]> getServiceData()
    {
        return mServiceData;
    }

    public String getName()
    {
        return mLocalName;
    }

    @Override public boolean equals(Object o)
    {
        if (o instanceof BleScanInfo)
        {
            BleScanInfo otherInfo = (BleScanInfo) o;
            if (otherInfo.isNull() && isNull())
            {
                return true;
            }
            return otherInfo.mLocalName.equals(mLocalName) && otherInfo.getTxPower() == mTxPower && Arrays.equals(otherInfo.getManufacturerData(), mManufacturerData);
        }
        else
        {
            return super.equals(o);
        }
    }

    @Override public BleScanInfo clone()
    {
        try
        {
            return (BleScanInfo) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
        }
        return null;
    }

    @Override public boolean isNull()
    {
        return mIsNull;
    }
}
