package com.idevicesinc.sweetblue.utils;


/**
 * POJO to hold the manufacturer id, and manufacturer data parsed from a device's scan record.
 */
public class ManufacturerData
{

    /**
     * The manufacturer id
     */
    public short m_id;

    /**
     * Manufacturer data for the manufacturer id stored in {@link #m_id}
     */
    public byte[] m_data;

}
