package com.idevicesinc.sweetblue.utils;


import com.idevicesinc.sweetblue.BleAdvertiseMode;
import com.idevicesinc.sweetblue.BleAdvertisePower;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Class to used for advertising Bluetooth services, used with {@link com.idevicesinc.sweetblue.BleManager#startAdvertising(BleAdvertiseInfo)}
 */
public class BleAdvertiseInfo {

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


    public BleAdvertiseInfo(BleAdvertiseMode mode, BleAdvertisePower power) {
        m_power = power;
        m_mode = mode;
    }

    public BleAdvertiseInfo() {
        this(BleAdvertiseMode.BALANCED, BleAdvertisePower.MEDIUM);
    }

    public BleAdvertiseInfo(UUID serviceUuid, BleAdvertiseMode mode, BleAdvertisePower power) {
        this(mode, power);
        addServiceUuid(serviceUuid);
    }

    /**
     * Set the manufacturer id, and data to be advertised
     */
    public void setManufacturerData(int id, byte[] data) {
        m_manufacturerId = id;
        m_manData = data;
    }

    /**
     * Returns the manufacturer Id being used
     */
    public int getManufacturerId() {
        return m_manufacturerId;
    }

    /**
     * Returns the manufacturer data
     */
    public byte[] getManufacturerData() {
        return m_manData;
    }

    /**
     * Add a UUID for a {@link com.idevicesinc.sweetblue.BleService} you'd like to be advertised
     */
    public void addServiceUuid(UUID uuid) {
        serviceUuids.add(uuid);
    }


    public void addServiceData(UUID dataUuid, byte[] data) {
        serviceData.put(dataUuid, data);
    }

    /**
     * Sets whether this advertisement is connectable or not
     */
    public void isConnectable(boolean connectable) {
        m_connectable = connectable;
    }

    /**
     * Whether or not this advertisement is connectable
     */
    public boolean isConnectable() {
        return m_connectable;
    }

    public boolean includeDeviceName() {
        return m_includeName;
    }

    public void includeDeviceName(boolean include) {
        m_includeName = include;
    }

    public boolean includeTxPowerLevel() {
        return m_includeTxPwrLevel;
    }

    public void includeTxPowerLevel(boolean include) {
        m_includeTxPwrLevel = include;
    }

    public void setTimeout(Interval timeout) {
        m_timeOut = timeout;
    }

    public Interval getTimeout() {
        return m_timeOut;
    }

    /**
     * Set the Advertising Mode, which affects the latency of Advertising.
     *
     * @see BleAdvertiseMode
     */
    public void setAdvertisingMode(BleAdvertiseMode mode) {
        m_mode = mode;
    }

    /**
     * Set the Advertising power used when advertising.
     *
     * @see BleAdvertisePower
     */
    public void setAdvertisingPower(BleAdvertisePower power) {
        m_power = power;
    }

    /**
     * Returns a HashSet of UUIDS that will be advertised
     */
    public Set<UUID> getUuids() {
        return serviceUuids;
    }

    public Map<UUID, byte[]> getServiceData() {
        return serviceData;
    }

    /**
     * Returns the advertising latency
     *
     * @see BleAdvertiseMode
     */
    public BleAdvertiseMode getMode() {
        return m_mode;
    }

    /**
     * Returns the avertising power
     *
     * @see BleAdvertisePower
     */
    public BleAdvertisePower getPower() {
        return m_power;
    }


}
