package com.idevicesinc.sweetblue.listeners;


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Percent;
import com.idevicesinc.sweetblue.utils.Utils_String;


public interface DiscoveryListener
{
    /**
     * Enumerates changes in the "discovered" state of a device.
     * Used at {@link DiscoveryListener.DiscoveryEvent#lifeCycle()}.
     */
    enum LifeCycle
    {
        /**
         * Used when a device is discovered for the first time after
         * calling {@link BleManager#startScan()} (or its overloads)
         * or {@link BleManager#startPeriodicScan(Interval, Interval)}.
         */
        DISCOVERED,

        /**
         * Used when a device is rediscovered after already being discovered at least once.
         */
        REDISCOVERED,

        /**
         * Used when a device is "undiscovered" after being discovered at least once. There is no native equivalent
         * for this callback. Undiscovery is approximated with a timeout based on the last time we discovered a device, configured
         * by {@link BleDeviceConfig#undiscoveryKeepAlive}. This option is disabled by default. If set, you should expect that the undiscovery
         * callback will take some amount of time to receive after an advertising device is turned off or goes out of range or what have you.
         * It's generally not as fast as other state changes like {@link BleDeviceState#DISCONNECTED} or getting {@link BleDeviceState#DISCOVERED} in the first place.
         *
         * @see BleDeviceConfig#minScanTimeNeededForUndiscovery
         * @see BleDeviceConfig#undiscoveryKeepAlive
         */
        UNDISCOVERED;
    }

    /**
     * Struct passed to {@link DiscoveryListener#onEvent(DiscoveryListener.DiscoveryEvent)}.
     */
    @Immutable
    class DiscoveryEvent extends Event
    {
        /**
         * The {@link BleManager} which is currently {@link BleManagerState#SCANNING}.
         */
        public BleManager manager(){  return device().getManager();  }

        /**
         * The device in question.
         */
        public BleDevice device(){  return m_device;  }
        private final BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public String macAddress()  {  return m_device.getMacAddress();  }

        /**
         * The discovery {@link DiscoveryListener.LifeCycle} that the device has undergone.
         */
        public LifeCycle lifeCycle(){  return m_lifeCycle;  }
        private final LifeCycle m_lifeCycle;

        DiscoveryEvent(final BleDevice device, final LifeCycle lifeCycle)
        {
            m_device = device;
            m_lifeCycle = lifeCycle;
        }

        /**
         * Forwards {@link BleDevice#getRssi()}.
         */
        public int rssi()
        {
            return device().getRssi();
        }

        /**
         * Forwards {@link BleDevice#getRssiPercent()}.
         */
        public Percent rssi_percent()
        {
            return device().getRssiPercent();
        }

        /**
         * Convenience method for checking equality of given {@link DiscoveryListener.LifeCycle} and {@link #lifeCycle()}.
         */
        public boolean was(LifeCycle lifeCycle)
        {
            return lifeCycle == lifeCycle();
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "device", device().getName(),
                            "lifeCycle", lifeCycle(),
                            "rssi", rssi(),
                            "rssi_percent", rssi_percent()
                    );
        }
    }

    /**
     * Called when the discovery lifecycle of a device is updated.
     * <br><br>
     * TIP: Take a look at {@link BleDevice#getLastDisconnectIntent()}. If it is {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL}
     * then from a user-experience perspective it's most often best to automatically connect without user confirmation.
     */
    void onEvent(final DiscoveryEvent e);
}
