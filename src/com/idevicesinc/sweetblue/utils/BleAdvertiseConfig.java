package com.idevicesinc.sweetblue.utils;


import com.idevicesinc.sweetblue.BleAdvertiseMode;
import com.idevicesinc.sweetblue.BleAdvertisePower;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Class to used for advertising Bluetooth services, used with {@link com.idevicesinc.sweetblue.BleServer#startAdvertising(BleAdvertiseConfig)}
 */
public class BleAdvertiseConfig {

    private BleAdvertisePower m_power;
    private BleAdvertiseMode m_mode;
    private Set<UUID> serviceUuids = new HashSet<UUID>();
    private Map<UUID, byte[]> serviceData = new HashMap<UUID, byte[]>();
    private boolean m_connectable = true;
    private boolean m_includeName = true;
    private boolean m_includeTxPwrLevel = false;
    private Interval m_timeOut;
    private int m_manufacturerId = Integer.MIN_VALUE;
    private byte[] m_manData;


    /**
     * Constructor which expects the advertising latency ({@link BleAdvertiseMode}), and power ({@link BleAdvertisePower}) as arguments.
     */
    public BleAdvertiseConfig(BleAdvertiseMode mode, BleAdvertisePower power)
    {
        m_power = power;
        m_mode = mode;
    }

    /**
     * Constructor which sets the advertising latecy to {@link BleAdvertiseMode#BALANCED}, and power to
     * {@link BleAdvertisePower#MEDIUM}.
     */
    public BleAdvertiseConfig()
    {
        this(BleAdvertiseMode.BALANCED, BleAdvertisePower.MEDIUM);
    }

    /**
     * Overload of {@link #BleAdvertiseConfig(BleAdvertiseMode, BleAdvertisePower)}, allowing you to also
     * set the {@link UUID} of the service you would like to be advertised.
     */
    public BleAdvertiseConfig(UUID serviceUuid, BleAdvertiseMode mode, BleAdvertisePower power)
    {
        this(mode, power);
        addServiceUuid(serviceUuid);
    }

    /**
     * Overload of {@link #BleAdvertiseConfig(BleAdvertiseMode, BleAdvertisePower)}, allowing you to also
     * set an array of {@link UUID}s of the services you would like to be advertised.
     */
    public BleAdvertiseConfig(UUID[] serviceUuids, BleAdvertiseMode mode, BleAdvertisePower power)
    {
        this(mode, power);
        this.serviceUuids.addAll(Arrays.asList(serviceUuids));
    }

    /**
     * Set the manufacturer id, and data to be advertised
     */
    public void setManufacturerData(int id, byte[] data)
    {
        m_manufacturerId = id;
        m_manData = data;
    }

    /**
     * Returns the manufacturer Id being used
     */
    public int getManufacturerId()
    {
        return m_manufacturerId;
    }

    /**
     * Returns the manufacturer data
     */
    public byte[] getManufacturerData()
    {
        return m_manData;
    }

    /**
     * Add a UUID for a {@link com.idevicesinc.sweetblue.BleService} you'd like to be advertised
     */
    public void addServiceUuid(UUID uuid)
    {
        serviceUuids.add(uuid);
    }


    public void addServiceData(UUID dataUuid, byte[] data)
    {
        serviceData.put(dataUuid, data);
    }

    /**
     * Sets whether this advertisement is connectable or not
     */
    public void isConnectable(boolean connectable)
    {
        m_connectable = connectable;
    }

    /**
     * Whether or not this advertisement is connectable
     */
    public boolean isConnectable()
    {
        return m_connectable;
    }

    /**
     * Whether or not this advertisement includes the device name
     */
    public boolean includeDeviceName()
    {
        return m_includeName;
    }

    /**
     * Set the advertisement to include the device name
     */
    public void includeDeviceName(boolean include)
    {
        m_includeName = include;
    }

    /**
     * Whether or not this advertisement includes the Tx power level in the packet
     */
    public boolean includeTxPowerLevel()
    {
        return m_includeTxPwrLevel;
    }

    /**
     * Set whether or not this advertisement will include the Tx power level (this takes up 3 bytes in the packet)
     */
    public void includeTxPowerLevel(boolean include)
    {
        m_includeTxPwrLevel = include;
    }

    /**
     * Sets a timeout period when the advertisement will stop. 0 means it will always advertise.
     */
    public void setTimeout(Interval timeout)
    {
        m_timeOut = timeout;
    }

    /**
     * Get the timeout for this advertisement
     */
    public Interval getTimeout()
    {
        return m_timeOut;
    }

    /**
     * Set the Advertising Mode, which affects the latency of Advertising.
     *
     * @see BleAdvertiseMode
     */
    public void setAdvertisingMode(BleAdvertiseMode mode)
    {
        m_mode = mode;
    }

    /**
     * Set the Advertising power used when advertising.
     *
     * @see BleAdvertisePower
     */
    public void setAdvertisingPower(BleAdvertisePower power)
    {
        m_power = power;
    }

    /**
     * Returns a HashSet of UUIDS that will be advertised
     */
    public Set<UUID> getUuids()
    {
        return serviceUuids;
    }

    public Map<UUID, byte[]> getServiceData()
    {
        return serviceData;
    }

    /**
     * Returns the advertising latency
     *
     * @see BleAdvertiseMode
     */
    public BleAdvertiseMode getMode()
    {
        return m_mode;
    }

    /**
     * Returns the avertising power
     *
     * @see BleAdvertisePower
     */
    public BleAdvertisePower getPower()
    {
        return m_power;
    }


}
