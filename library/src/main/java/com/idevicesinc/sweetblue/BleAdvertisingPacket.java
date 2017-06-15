package com.idevicesinc.sweetblue;


import android.annotation.TargetApi;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.os.Build;
import android.os.ParcelUuid;

import com.idevicesinc.sweetblue.utils.BitwiseEnum;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class to used for advertising Bluetooth services, used with {@link com.idevicesinc.sweetblue.BleServer#startAdvertising(BleAdvertisingPacket)}
 */
public final class BleAdvertisingPacket {


    private final UUID[] serviceUuids;
    private final Map<UUID, byte[]> serviceData;
    private final int m_options;
    private final int m_manufacturerId;
    private final byte[] m_manData;


    /**
     * Base constructor which all other constructors in this class overload. This sets all the packet information to be included
     * in your advertisement.
     */
    public BleAdvertisingPacket(UUID[] serviceUuids, Map<UUID, byte[]> serviceData, int options, int manufacturerId, byte[] manufacturerData)
    {
        this.serviceUuids = serviceUuids;
        this.serviceData = serviceData;
        m_options = options;
        m_manufacturerId = manufacturerId;
        m_manData = manufacturerData;
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID serviceUuid)
    {
        this(new UUID[] { serviceUuid }, null, Option.CONNECTABLE.or(Option.INCLUDE_NAME), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID serviceUuid, Option... options)
    {
        this(new UUID[] { serviceUuid }, null, Option.getFlags(options), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID serviceUuid, int manufacturerId)
    {
        this(new UUID[] { serviceUuid }, null, Option.CONNECTABLE.or(Option.INCLUDE_NAME), manufacturerId, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID serviceUuid, int manufacturerId, Option... options)
    {
        this(new UUID[] { serviceUuid }, null, Option.getFlags(options), manufacturerId, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID serviceUuid, int manufacturerId, byte[] manufacturerData)
    {
        this(new UUID[] { serviceUuid }, null, Option.CONNECTABLE.or(Option.INCLUDE_NAME), manufacturerId, manufacturerData);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID serviceUuid, int manufacturerId, byte[] manufacturerData, Option... options)
    {
        this(new UUID[] { serviceUuid }, null, Option.getFlags(options), manufacturerId, manufacturerData);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID[] serviceUuids)
    {
        this(serviceUuids, null, Option.CONNECTABLE.or(Option.INCLUDE_NAME), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID[] serviceUuids, Option... options) {
        this(serviceUuids, null, Option.getFlags(options), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID[] serviceUuids, int manufacturerId)
    {
        this(serviceUuids, null, Option.CONNECTABLE.or(Option.INCLUDE_NAME), manufacturerId, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID[] serviceUuids, int manufacturerId, Option... options)
    {
        this(serviceUuids, null, Option.getFlags(options), manufacturerId, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID[] serviceUuids, int manufacturerId, byte[] manufacturerData)
    {
        this(serviceUuids, null, Option.CONNECTABLE.or(Option.INCLUDE_NAME), manufacturerId, manufacturerData);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID[] serviceUuids, int manufacturerId, byte[] manufacturerData, Option... options)
    {
        this(serviceUuids, null, Option.getFlags(options), manufacturerId, manufacturerData);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID serviceUuid, final UUID serviceDataUuid, final byte[] serviceData)
    {
        this(new UUID[] { serviceUuid }, new HashMap<UUID, byte[]>(1) {{ put(serviceDataUuid, serviceData); }}, Option.CONNECTABLE.or(Option.INCLUDE_NAME), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID serviceUuid, final UUID serviceDataUuid, final byte[] serviceData, Option... options)
    {
        this(new UUID[] { serviceUuid }, new HashMap<UUID, byte[]>(1) {{ put(serviceDataUuid, serviceData); }}, Option.getFlags(options), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID[] serviceUuids, final UUID serviceDataUuid, final byte[] serviceData)
    {
        this(serviceUuids, new HashMap<UUID, byte[]>(1) {{ put(serviceDataUuid, serviceData); }}, Option.CONNECTABLE.or(Option.INCLUDE_NAME), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID[] serviceUuids, final UUID serviceDataUuid, final byte[] serviceData, Option... options)
    {
        this(serviceUuids, new HashMap<UUID, byte[]>(1) {{ put(serviceDataUuid, serviceData); }}, Option.getFlags(options), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID serviceUuid, Map<UUID, byte[]> serviceUuidsAndData)
    {
        this(new UUID[] { serviceUuid }, serviceUuidsAndData, Option.CONNECTABLE.or(Option.INCLUDE_NAME), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID serviceUuid, Map<UUID, byte[]> serviceUuidsAndData, Option... options)
    {
        this(new UUID[] { serviceUuid }, serviceUuidsAndData, Option.getFlags(options), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(UUID serviceUuid, Map<UUID, byte[]> serviceUuidsAndData, int manufacturerId, byte[] manufacturerData)
    {
        this(new UUID[] { serviceUuid }, serviceUuidsAndData, Option.CONNECTABLE.or(Option.INCLUDE_NAME), manufacturerId, manufacturerData);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(UUID serviceUuid, Map<UUID, byte[]> serviceUuidsAndData, int manufacturerId, byte[] manufacturerData, Option... options)
    {
        this(new UUID[] { serviceUuid }, serviceUuidsAndData, Option.getFlags(options), manufacturerId, manufacturerData);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(final UUID serviceDataUuid, final byte[] serviceData)
    {
        this(new UUID[0], new HashMap<UUID, byte[]>(1) {{ put(serviceDataUuid, serviceData); }}, Option.CONNECTABLE.or(Option.INCLUDE_NAME), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(final UUID serviceDataUuid, final byte[] serviceData, Option... options)
    {
        this(new UUID[0], new HashMap<UUID, byte[]>(1) {{ put(serviceDataUuid, serviceData); }}, Option.getFlags(options), 0, null);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}, which sets the {@link BleAdvertisingPacket.Option#CONNECTABLE},
     * and {@link BleAdvertisingPacket.Option#INCLUDE_NAME} flags.
     */
    public BleAdvertisingPacket(final UUID serviceDataUuid, final byte[] serviceData, int manufacturerId, byte[] manufacturerData)
    {
        this(new UUID[0], new HashMap<UUID, byte[]>(1) {{ put(serviceDataUuid, serviceData); }}, Option.CONNECTABLE.or(Option.INCLUDE_NAME), manufacturerId, manufacturerData);
    }

    /**
     * Overload of {@link #BleAdvertisingPacket(UUID[], Map, int, int, byte[])}
     */
    public BleAdvertisingPacket(final UUID serviceDataUuid, final byte[] serviceData, int manufacturerId, byte[] manufacturerData, Option... options)
    {
        this(new UUID[0], new HashMap<UUID, byte[]>(1) {{ put(serviceDataUuid, serviceData); }}, Option.getFlags(options), manufacturerId, manufacturerData);
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
     * Whether or not this advertisement is connectable
     */
    public boolean isConnectable()
    {
        return (m_options & Option.CONNECTABLE.bit()) == Option.CONNECTABLE.bit();
    }

    /**
     * Whether or not this advertisement includes the device name
     */
    public boolean includeDeviceName()
    {
        return (m_options & Option.INCLUDE_NAME.bit()) == Option.INCLUDE_NAME.bit();
    }

    /**
     * Whether or not this advertisement includes the Tx power level in the packet
     */
    public boolean includeTxPowerLevel()
    {
        return (m_options & Option.INCLUDE_TX_POWER.bit()) == Option.INCLUDE_TX_POWER.bit();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    /*package*/ AdvertiseSettings getNativeSettings(BleAdvertisingMode mode, BleTransmissionPower power, Interval timeout) {
        AdvertiseSettings.Builder settings = new AdvertiseSettings.Builder();
        settings.setAdvertiseMode(mode.getNativeMode());
        settings.setTxPowerLevel(power.getNativeMode());
        settings.setConnectable(isConnectable());
        settings.setTimeout((int) timeout.millis());
        return settings.build();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    /*package*/ AdvertiseData getNativeData() {
        AdvertiseData.Builder data = new AdvertiseData.Builder();
        for (UUID id : serviceUuids)
        {
            data.addServiceUuid(new ParcelUuid(id));
        }
        if (m_manufacturerId != 0 && m_manData != null)
        {
            data.addManufacturerData(m_manufacturerId, m_manData);
        }
        if (serviceData != null && serviceData.size() > 0)
        {
            for (UUID dataUuid : serviceData.keySet())
            {
                data.addServiceData(new ParcelUuid(dataUuid), serviceData.get(dataUuid));
            }
        }
        data.setIncludeDeviceName(includeDeviceName());
        data.setIncludeTxPowerLevel(includeTxPowerLevel());
        return data.build();
    }

    /**
     * Returns true if this advertising packet contains the uuid given.
     */
    public boolean hasUuid(UUID uuid)
    {
        if (serviceUuids != null && serviceUuids.length > 0)
        {
            for (UUID id : serviceUuids)
            {
                if (id.equals(uuid))
                {
                    return true;
                }
            }
        }
        if (serviceData != null && serviceData.size() > 0)
        {
            for (UUID id : serviceData.keySet())
            {
                if (id.equals(uuid))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a HashSet of UUIDS that will be advertised
     */
    public UUID[] getUuids()
    {
        return serviceUuids;
    }

    /**
     * Returns a Map of 16bit service UUIDs, along with the associated byte arrays.
     */
    public Map<UUID, byte[]> getServiceData()
    {
        return serviceData;
    }


    /**
     * Enumeration for advertising options
     */
    public static enum Option implements BitwiseEnum
    {

        CONNECTABLE(1),
        INCLUDE_NAME(2),
        INCLUDE_TX_POWER(4);

        private final int m_bit;

        private Option(int bit)
        {
            m_bit = bit;
        }

        public static int getFlags(Option[] options) {
            if (options == null || options.length == 0) {
                return 0;
            }
            int flags = 0;
            for (Option o : options) {
                flags |= o.bit();
            }
            return flags;
        }

        @Override public int or(BitwiseEnum state)
        {
            return m_bit | state.bit();
        }

        @Override public int or(int bits)
        {
            return m_bit | bits;
        }

        @Override public int bit()
        {
            return m_bit;
        }

        @Override public boolean overlaps(int mask)
        {
            return (m_bit & mask) != 0x0;
        }
    }


}
