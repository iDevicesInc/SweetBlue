package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.util.SparseArray;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.BleScanInfo;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An optional whitelisting mechanism for scanning. Provide an implementation at
 * {@link BleManagerConfig#defaultScanFilter} or one of the various {@link BleManager#startScan()}
 * overloads, i.e. {@link BleManager#startScan(ScanFilter)},
 * {@link BleManager#startScan(Interval, ScanFilter)}, etc.
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface ScanFilter
{
    /**
     * An enumeration of the various ways that the (up to) two {@link ScanFilter} held in the scan
     * filter manager will be applied when scanning
     *
     * @see ScanFilter
     */
    enum ApplyMode
    {
        /**
         * Device will pass filtering if either filter accepts it
         */
        CombineEither,

        /**
         * Device will pass filtering only if both filters accept it
         */
        CombineBoth,

        /**
         * Ephemeral filter will override the default filter for the duration of the scan.  If
         * the ephemeral filter is null, this is equivalent to running with no filter set (which
         * results in all devices passing the filtering check)
         */
        Override
    }

    /**
     * Instances of this class are passed to {@link ScanFilter#onEvent(ScanEvent)} to aid in making a decision.
     */
    @Immutable
    class ScanEvent extends Event
    {
        /**
         * Other parameters are probably enough to make a decision but this native instance is provided just in case.
         */
        public BluetoothDevice nativeInstance(){  return m_nativeInstance;  }
        private final BluetoothDevice m_nativeInstance;

        /**
         * A list of {@link UUID}s parsed from {@link #scanRecord()} as a convenience. May be empty, notably
         * if {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is invoked.
         */
        public List<UUID> advertisedServices(){  return m_advertisedServices;  }
        private final List<UUID> m_advertisedServices;

        /**
         * The unaltered device name retrieved from the native bluetooth stack.
         */
        public String name_native(){  return m_rawDeviceName;  }
        private final String m_rawDeviceName;

        /**
         * See {@link BleDevice#getName_normalized()} for an explanation.
         */
        public String name_normalized(){  return m_normalizedDeviceName;  }
        private final String m_normalizedDeviceName;

        /**
         * The raw scan record received when the device was discovered. May be empty, especially
         * if {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is invoked.
         */
        public byte[] scanRecord(){  return m_scanRecord;  }
        private final byte[] m_scanRecord;

        /**
         * The RSSI received when the device was discovered.
         */
        public int rssi(){  return m_rssi;  }
        private final int m_rssi;

        /**
         * Returns the transmission power of the device in decibels, or {@link BleNodeConfig#INVALID_TX_POWER} if device is not advertising its transmission power.
         */
        public int txPower(){  return m_txPower;  }
        private final int m_txPower;

        /**
         * Returns the mac address of the discovered device.
         */
        public String macAddress(){  return m_nativeInstance.getAddress();  }

        /**
         * See explanation at {@link BleDevice#getLastDisconnectIntent()}.
         * <br><br>
         * TIP: If {@link ScanEvent#lastDisconnectIntent} isn't {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#NULL} then most likely you can early-out
         * and return <code>true</code> from {@link ScanFilter#onEvent(ScanEvent)} without having to check
         * uuids or names matching, because obviously you've seen and connected to this device before.
         */
        public State.ChangeIntent lastDisconnectIntent(){  return m_lastDisconnectIntent;  }
        private final State.ChangeIntent m_lastDisconnectIntent;

        /**
         * Returns the advertising flags, if any, parsed from {@link #scanRecord()}.
         */
        public int advertisingFlags()  {  return m_advertisingFlags;  }
        private final int m_advertisingFlags;

        /**
         * Returns the manufacturer-specific data, if any, parsed from {@link #scanRecord()}.
         */
        public SparseArray<byte[]> manufacturerCombinedData(){  return m_manufacturerCombinedData;  }
        private final SparseArray<byte[]> m_manufacturerCombinedData;

        public byte[] manufacturerData(){ return m_manufacturerData;}
        private byte[] m_manufacturerData;

        public int manufacturerId(){ return m_manufacturerId;}
        private int m_manufacturerId;

        /**
         * Returns the service data, if any, parsed from {@link #scanRecord()}.
         */
        public Map<UUID, byte[]> serviceData()  {  return m_serviceData;  }
        private final Map<UUID, byte[]> m_serviceData;

        ScanEvent(
                BluetoothDevice nativeInstance, String rawDeviceName,
                String normalizedDeviceName, byte[] scanRecord, int rssi, State.ChangeIntent lastDisconnectIntent,
                BleScanInfo scanInfo
        )
        {
            this.m_nativeInstance = nativeInstance;
            this.m_advertisedServices = scanInfo != null ? scanInfo.getServiceUUIDS() : new ArrayList<UUID>(0);
            this.m_rawDeviceName = rawDeviceName != null ? rawDeviceName : "";
            this.m_normalizedDeviceName = normalizedDeviceName;
            this.m_scanRecord = scanRecord != null ? scanRecord : P_Const.EMPTY_BYTE_ARRAY;
            this.m_rssi = rssi;
            this.m_lastDisconnectIntent = lastDisconnectIntent;
            this.m_txPower = scanInfo != null ? scanInfo.getTxPower().value : 0;
            this.m_advertisingFlags = scanInfo != null ? scanInfo.getAdvFlags().value : 0;
            this.m_manufacturerData = scanInfo != null ? scanInfo.getManufacturerData() : P_Const.EMPTY_BYTE_ARRAY;
            this.m_manufacturerId = scanInfo != null ? scanInfo.getManufacturerId() : 0;
            this.m_serviceData = scanInfo != null ? scanInfo.getServiceData() : new HashMap<UUID, byte[]>(0);

            this.m_manufacturerCombinedData = new SparseArray<>();
        }

        /*package*/ static ScanEvent fromScanRecord(final BluetoothDevice device_native, final String rawDeviceName, final String normalizedDeviceName, final int rssi, final State.ChangeIntent lastDisconnectIntent, final byte[] scanRecord)
        {
            final Pointer<Integer> advFlags = new Pointer<Integer>();
            final Pointer<Integer> txPower = new Pointer<Integer>();
            final List<UUID> serviceUuids = new ArrayList<UUID>();
            final SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();
            final Map<UUID, byte[]> serviceData = new HashMap<UUID, byte[]>();
            final String name = rawDeviceName != null ? rawDeviceName : Utils_ScanRecord.parseName(scanRecord);

            BleScanInfo scanInfo = Utils_ScanRecord.parseScanRecord(scanRecord);

            final ScanEvent e = new ScanEvent(device_native, name, normalizedDeviceName, scanRecord, rssi, lastDisconnectIntent, scanInfo);

            return e;
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "macAddress", macAddress(),
                            "name", name_normalized(),
                            "services", advertisedServices()
                    );
        }
    }

    /**
     * Small struct passed back from {@link ScanFilter#onEvent(ScanEvent)}.
     * Use static constructor methods to create an instance.
     */
    public static class Please
    {
        static int STOP_SCAN			= 0x1;
        static int STOP_PERIODIC_SCAN	= 0x2;

        private final boolean m_ack;
        private final BleDeviceConfig m_config;
        int m_stopScanOptions;

        private Please(boolean ack, BleDeviceConfig config_nullable)
        {
            m_ack = ack;
            m_config = config_nullable;
        }

        boolean ack()
        {
            return m_ack;
        }

        BleDeviceConfig getConfig()
        {
            return m_config;
        }

        /**
         * Shorthand for calling {@link BleManager#stopScan(ScanFilter)}.
         */
        public Please thenStopScan()
        {
            m_stopScanOptions |= STOP_SCAN;

            return this;
        }

        /**
         * Shorthand for calling {@link BleManager#stopPeriodicScan(ScanFilter)}.
         */
        public Please thenStopPeriodicScan()
        {
            m_stopScanOptions |= STOP_PERIODIC_SCAN;

            return this;
        }

        /**
         * Shorthand for calling both {@link BleManager#stopScan(ScanFilter)} and {@link BleManager#stopPeriodicScan(ScanFilter)}.
         */
        public Please thenStopAllScanning()
        {
            thenStopScan();
            thenStopPeriodicScan();

            return this;
        }

        /**
         * Return this from {@link ScanFilter#onEvent(ScanEvent)} to acknowledge the discovery.
         * {@link DiscoveryListener#onEvent(com.idevicesinc.sweetblue.DiscoveryListener.DiscoveryEvent)}
         * will be called presently with a newly created {@link BleDevice}.
         */
        public static Please acknowledge()
        {
            return new Please(true, null);
        }

        /**
         * Returns {@link #acknowledge()} if the given condition holds <code>true</code>, {@link #ignore()} otherwise.
         */
        public static Please acknowledgeIf(boolean condition)
        {
            return condition ? acknowledge() : ignore();
        }

        /**
         * Same as {@link #acknowledgeIf(boolean)} but lets you pass a {@link BleDeviceConfig} as well.
         */
        public static Please acknowledgeIf(boolean condition, BleDeviceConfig config)
        {
            return condition ? acknowledge(config) : ignore();
        }

        /**
         * Same as {@link #acknowledge()} but allows you to pass a {@link BleDeviceConfig}
         * instance to the {@link BleDevice} that's about to be created.
         */
        public static Please acknowledge(BleDeviceConfig config)
        {
            return new Please(true, config);
        }

        /**
         * Return this from {@link ScanFilter#onEvent(ScanEvent)} to say no to the discovery.
         */
        public static Please ignore()
        {
            return new Please(false, null);
        }

        /**
         * Returns {@link #ignore()} if the given condition holds <code>true</code>, {@link #acknowledge()} otherwise.
         */
        public static Please ignoreIf(final boolean condition)
        {
            return condition ? ignore() : acknowledge();
        }
    }

    /**
     * Return {@link Please#acknowledge()} to acknowledge the discovery, in which case {@link DiscoveryListener#onEvent(DiscoveryListener.DiscoveryEvent)}
     * will be called shortly. Otherwise return {@link Please#ignore()} to ignore the discovered device.
     *
     * @return {@link Please#acknowledge()}, {@link Please#ignore()}, or {@link Please#acknowledge(BleDeviceConfig)} (or other static constructor methods that may be added in the future).
     */
    Please onEvent(final ScanEvent e);
}
