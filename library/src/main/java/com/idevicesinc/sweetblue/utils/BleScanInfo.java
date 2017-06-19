package com.idevicesinc.sweetblue.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Class used to store information from a BLE scan record. This class can also be used to create a scan record.
 */
public final class BleScanInfo implements UsesCustomNull
{

    public final static BleScanInfo NULL = new BleScanInfo();

    private Short m_manufactuerId;
    private byte[] m_manufacturerData;
    private Pointer<Integer> m_advFlags;
    private Pointer<Integer> m_txPower;
    private final List<BleUuid> m_serviceUuids;
    private final Map<UUID, byte[]> m_serviceData;
    private boolean m_completeUuidList;
    private String m_localName;
    private boolean m_shortName;

    /**
     * Basic constructor to use if you are building a scan record to advertise.
     */
    public BleScanInfo()
    {
        m_serviceUuids = new ArrayList<>();
        m_serviceData = new HashMap<>();
        m_completeUuidList = false;
    }

    /**
     * Constructor used internally when a {@link com.idevicesinc.sweetblue.BleDevice} is discovered.
     */
    public BleScanInfo(Pointer<Integer> advFlags, Pointer<Integer> txPower, List<UUID> serviceUuids, boolean uuidCompleteList, short mfgId, byte[] mfgData, Map<UUID, byte[]> serviceData, String localName, boolean shortName)
    {
        m_advFlags = advFlags;
        m_txPower = txPower;
        m_serviceUuids = new ArrayList<>();
        addServiceUUIDs(serviceUuids);
        m_manufactuerId = mfgId;
        m_manufacturerData = mfgData;
        if (serviceData == null)
        {
            m_serviceData = new HashMap<>(0);
        }
        else
        {
            m_serviceData = serviceData;
        }
        m_localName = localName;
        m_shortName = shortName;
        m_completeUuidList = uuidCompleteList;
    }

    /**
     * Clear all service data that may be in this {@link BleScanInfo} instance.
     * See also {@link #clearServiceUUIDs()}.
     */
    public final BleScanInfo clearServiceData()
    {
        m_serviceData.clear();
        return this;
    }

    /**
     * Set the service data for this {@link BleScanInfo} instance.
     */
    public final BleScanInfo addServiceData(Map<UUID, byte[]> data)
    {
        m_serviceData.putAll(data);
        return this;
    }

    /**
     * Clears any service UUIDs in this instance.
     */
    public final BleScanInfo clearServiceUUIDs()
    {
        m_serviceUuids.clear();
        return this;
    }

    /**
     * Add the given List of {@link UUID}s to this instance's UUID list.
     */
    public final BleScanInfo addServiceUUIDs(List<UUID> uuids)
    {
        if (uuids != null)
        {
            for (UUID u : uuids)
            {
                BleUuid.UuidSize size = shortUuid(u) ? BleUuid.UuidSize.SHORT : BleUuid.UuidSize.FULL;
                m_serviceUuids.add(new BleUuid(u, size));
            }
        }
        return this;
    }

    /**
     * Add the given {@link UUID} and data to this instance's service data map.
     */
    public final BleScanInfo addServiceData(UUID uuid, byte[] data)
    {
        m_serviceData.put(uuid, data);
        return this;
    }

    /**
     * Add a {@link UUID} with the given {@link BleUuid.UuidSize} to this instance's {@link UUID} list.
     */
    public final BleScanInfo addServiceUuid(UUID uuid, BleUuid.UuidSize size)
    {
        m_serviceUuids.add(new BleUuid(uuid, size));
        return this;
    }

    /**
     * Overload of {@link #addServiceUuid(UUID, BleUuid.UuidSize)}, which sets the size to {@link BleUuid.UuidSize#SHORT}, if it can fit, otherwise it will
     * default to {@link BleUuid.UuidSize#FULL}
     */
    public final BleScanInfo addServiceUuid(UUID uuid)
    {
        final BleUuid.UuidSize size = shortUuid(uuid) ? BleUuid.UuidSize.SHORT : BleUuid.UuidSize.FULL;
        return addServiceUuid(uuid, size);
    }

    /**
     * Set the manufacturer Id
     */
    public final BleScanInfo setManufacturerId(short id)
    {
        m_manufactuerId = id;
        return this;
    }

    /**
     * Set the manufacturer data
     */
    public final BleScanInfo setManufacturerData(byte[] data)
    {
        m_manufacturerData = data;
        return this;
    }

    /**
     * Overload of {@link #setName(String, boolean)}, which defaults to a complete name (not short).
     */
    public final BleScanInfo setName(String name)
    {
        return setName(name, false);
    }

    /**
     * Set the device name, and if it's a shortened name or not.
     */
    public final BleScanInfo setName(String name, boolean shortName)
    {
        m_localName = name;
        m_shortName = shortName;
        return this;
    }

    /**
     * Get the manufacturer Id from this {@link BleScanInfo} instance.
     */
    public final short getManufacturerId()
    {
        if (m_manufactuerId == null)
        {
            return -1;
        }
        return m_manufactuerId;
    }

    /**
     * Get the manufacturer data from this instance.
     */
    public final byte[] getManufacturerData()
    {
        if (m_manufacturerData == null)
        {
            return P_Const.EMPTY_BYTE_ARRAY;
        }
        return m_manufacturerData;
    }

    /**
     * Set the advertising flags. This method expects a byte bitmask (so all flags are already OR'd).
     */
    public final BleScanInfo setAdvFlags(byte mask)
    {
        if (m_advFlags == null)
        {
            m_advFlags = new Pointer<>((int) mask);
        }
        else
        {
            m_advFlags.value = (int) mask;
        }
        return this;
    }

    /**
     * Convenience method to set the advertising flags, which allows you to pass in every flag you want, and this
     * method will OR them together for you.
     */
    public final BleScanInfo setAdvFlags(byte... flags)
    {
        if (flags == null || flags.length == 0)
        {
            return this;
        }
        if (m_advFlags == null)
        {
            m_advFlags = new Pointer<>(0);
        }
        for (byte b : flags)
        {
            m_advFlags.value |= b;
        }
        return this;
    }

    /**
     * Get the advertising flags for this instance.
     */
    public final Pointer<Integer> getAdvFlags()
    {
        if (m_advFlags == null)
        {
            return new Pointer<>(0);
        }
        return m_advFlags;
    }

    /**
     * Set the TX power
     */
    public final BleScanInfo setTxPower(byte power)
    {
        if (m_txPower == null)
        {
            m_txPower = new Pointer<>((int) power);
        }
        else
        {
            m_txPower.value = (int) power;
        }
        return this;
    }

    /**
     * Gets the Tx power
     */
    public final Pointer<Integer> getTxPower()
    {
        if (m_txPower == null)
        {
            return new Pointer<>(0);
        }
        return m_txPower;
    }

    /**
     * Returns a list of service {@link UUID}s. This ONLY includes {@link UUID}s that do NOT have any data associated with them.
     *
     * See also {@link #getServiceData()}.
     */
    public final List<UUID> getServiceUUIDS()
    {
        List<UUID> list = new ArrayList<>();
        if (m_serviceUuids != null)
        {
            for (BleUuid u : m_serviceUuids)
            {
                list.add(u.uuid());
            }
        }
        return list;
    }

    /**
     * Returns a {@link Map} of the service data in this instance.
     *
     * See also {@link #getServiceUUIDS()}.
     */
    public final Map<UUID, byte[]> getServiceData()
    {
        return m_serviceData;
    }

    /**
     * Returns the device name
     */
    public final String getName()
    {
        if (m_localName == null)
        {
            return "";
        }
        return m_localName;
    }

    /**
     * Returns whether the name is a shortened version or not.
     */
    public final boolean isShortName()
    {
        return m_shortName;
    }

    /**
     * Returns <code>true</code> if this instance is considered null.
     */
    @Override public final boolean isNull()
    {
        return this == NULL;
    }

    /**
     * Build a byte[] scan record from the data stored in this instance.
     */
    public final byte[] buildPacket()
    {
        Map<BleUuid, byte[]> map = new HashMap<>(m_serviceUuids.size() + m_serviceData.size());
        if (m_serviceUuids.size() > 0)
        {
            for (BleUuid u : m_serviceUuids)
            {
                map.put(u, null);
            }
        }
        if (m_serviceData.size() > 0)
        {
            for (UUID u : m_serviceData.keySet())
            {
                map.put(new BleUuid(u, BleUuid.UuidSize.SHORT), m_serviceData.get(u));
            }
        }
        return Utils_ScanRecord.newScanRecord(m_advFlags.value.byteValue(), map, m_completeUuidList, m_localName, m_shortName, m_txPower.value.byteValue(), m_manufactuerId, m_manufacturerData);
    }


    private static boolean shortUuid(UUID u)
    {
        long msb = u.getMostSignificantBits();
        short m = (short) (msb >>> 32);
        UUID test = Uuids.fromShort(Utils_Byte.bytesToHexString(Utils_Byte.shortToBytes(m)));
        return test.equals(u);
    }
}
