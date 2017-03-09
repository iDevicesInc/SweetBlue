package com.idevicesinc.sweetblue.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public final class BleScanInfo
{
    private int m_manufactuerId;
    private byte[] m_manufacturerData;
    private final Pointer<Integer> m_advFlags;
    private final Pointer<Integer> m_txPower;
    private final List<UUID> m_serviceUuids;
    private final Map<UUID, byte[]> m_serviceData;
    private String m_localName;

    public BleScanInfo()
    {
        m_manufactuerId = -1;
        m_manufacturerData = new byte[32];
        m_advFlags = new Pointer<>(0);
        m_txPower = new Pointer<>(0);
        m_serviceUuids = new ArrayList<>();
        m_serviceData = new HashMap<>();
        m_localName = "";
    }

    public BleScanInfo(Pointer<Integer> advFlags, Pointer<Integer> txPower, List<UUID> serviceUuids, int mfgId, byte[] mfgData, Map<UUID, byte[]> serviceData, String localName)
    {
        m_advFlags = advFlags;
        m_txPower = txPower;
        m_serviceUuids = serviceUuids;
        m_manufactuerId = mfgId;
        m_manufacturerData = mfgData;
        m_serviceData = serviceData;
        m_localName = localName;
    }

    public void clearServiceData()
    {
        m_serviceData.clear();
    }

    public void populateServiceData(Map<UUID, byte[]> data)
    {
        m_serviceData.putAll(data);
    }

    public void clearServiceUUIDs()
    {
        m_serviceUuids.clear();
    }

    public void populateServiceUUIDs(List<UUID> uuids)
    {
        m_serviceUuids.addAll(uuids);
    }

    public void setManufacturerId(int id)
    {
        m_manufactuerId = id;
    }

    public void setManufacturerData(byte[] data)
    {
        m_manufacturerData = data;
    }

    public int getManufacturerId()
    {
        return m_manufactuerId;
    }

    public byte[] getManufacturerData()
    {
        return m_manufacturerData;
    }

    public Pointer<Integer> getAdvFlags()
    {
        return m_advFlags;
    }

    public Pointer<Integer> getTxPower()
    {
        return m_txPower;
    }

    public List<UUID> getServiceUUIDS()
    {
        return m_serviceUuids;
    }

    public Map<UUID, byte[]> getServiceData()
    {
        return m_serviceData;
    }

    public String getName()
    {
        return m_localName;
    }
}
