package com.idevicesinc.sweetblue.utils;

import java.util.UUID;

/**
 * Class used to store a {@link UUID} along with the {@link UuidSize} of the {@link UUID}. The {@link UUID} class is always formatted
 * as a 128-bit UUID, but with scan records, you can have a 16, 32, or 128 bit UUID. Use {@link UuidSize} to specify this, so the packet
 * will be built properly.
 *
 * If you use {@link BleScanInfo} to create your scan packets, you will not need to use this class directly.
 */
public final class BleUuid
{
    private final UUID m_uuid;
    private final UuidSize m_size;

    public BleUuid(UUID uuid, UuidSize size)
    {
        m_uuid = uuid;
        m_size = size;
    }

    /**
     * Returns the backing {@link UUID}
     */
    public final UUID uuid()
    {
        return m_uuid;
    }

    /**
     * Returns the {@link UuidSize} of the backing {@link UUID}
     */
    public final UuidSize uuidSize()
    {
        return m_size;
    }

    /**
     * You shouldn't need to call this yourself, but it's left public for flexibility. This returns the most significant bits
     * of the backing {@link UUID}.
     */
    public final long getMostSignificantBits()
    {
        return m_uuid.getMostSignificantBits();
    }

    /**
     * You shouldn't need to call this yourself, but it's left public for flexibility. This returns the least significant bits
     * of the backing {@link UUID}.
     */
    public final long getLeastSignificantBits()
    {
        return m_uuid.getLeastSignificantBits();
    }

    @Override
    public final boolean equals(Object obj)
    {
        if (obj instanceof BleUuid)
        {
            BleUuid other = (BleUuid) obj;
            return m_uuid.equals(other.m_uuid) && m_size == other.m_size;
        }
        return false;
    }

    @Override
    public final int hashCode()
    {
        int prime = 71;
        int hash = 1;
        hash = m_uuid.hashCode() * hash + prime;
        hash = m_size.hashCode() * hash + prime;
        return hash;
    }

    /**
     * Enumeration used to dictate the "size" of a {@link UUID} when building a scan packet. Most of the time you will most likely use either
     * {@link #SHORT}, and sometimes {@link #FULL}. It's rare that {@link #MEDIUM} is used.
     */
    public enum UuidSize
    {
        /**
         * "Short" Uuid, which is a 16-bit UUID (2 bytes)
         */
        SHORT(2),

        /**
         * "Medium" Uuid, which is a 32-bit UUID (4 bytes)
         */
        MEDIUM(4),

        /**
         * Full 128-bit UUID (16 bytes)
         */
        FULL(16);

        private final int m_size;

        UuidSize(int size)
        {
            m_size = size;
        }

        public final int byteSize()
        {
            return m_size;
        }
    }
}
