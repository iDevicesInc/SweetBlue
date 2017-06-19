package com.idevicesinc.sweetblue;


import android.bluetooth.le.AdvertiseSettings;

import com.idevicesinc.sweetblue.annotations.Extendable;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Class used specify Ble Advertising settings.
 */
@Extendable
public class BleAdvertisingSettings {

    /**
     * Type-safe parallel of static final int members of {@link android.bluetooth.le.AdvertiseSettings}.
     * This is only applicable for Android >= 5.0 OS levels.
     */
    public enum BleAdvertisingMode {

        /**
         * This option is recommended and will let SweetBlue automatically choose what advertising mode to use
         * based on whether the app is backgrounded, and if we're doing a continuous, or short-term advertisement
         */
        AUTO(-1),

        /**
         * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_LOW_POWER}.
         */
        LOW_FREQUENCY(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER),

        /**
         * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_BALANCED}.
         */
        MEDIUM_FREQUENCY(AdvertiseSettings.ADVERTISE_MODE_BALANCED),

        /**
         * Strict typing for {@link AdvertiseSettings#ADVERTISE_MODE_LOW_LATENCY}.
         */
        HIGH_FREQUENCY(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);

        private final int m_nativeMode;

        BleAdvertisingMode(int nativeMode)
        {
            m_nativeMode = nativeMode;
        }

        /**
         * Returns one of the static final int members of {@link AdvertiseSettings}, or -1 for {@link #AUTO}.
         */
        public int getNativeMode()
        {
            return m_nativeMode;
        }
    }

    /**
     * Type-safe parallel of static final int members of {@link android.bluetooth.le.AdvertiseSettings}.
     * This is only applicable for Android >= 5.0 OS levels.
     */
    public enum BleTransmissionPower {

        /**
         * Strict typing for {@link AdvertiseSettings#ADVERTISE_TX_POWER_ULTRA_LOW}.
         */
        ULTRA_LOW   (AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW),

        /**
         * Strict typing for {@link AdvertiseSettings#ADVERTISE_TX_POWER_LOW}.
         */
        LOW         (AdvertiseSettings.ADVERTISE_TX_POWER_LOW),

        /**
         * Strict typing for {@link AdvertiseSettings#ADVERTISE_TX_POWER_MEDIUM}.
         */
        MEDIUM      (AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM),

        /**
         * Strict typing for {@link AdvertiseSettings#ADVERTISE_TX_POWER_HIGH}.
         */
        HIGH        (AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        private final int m_nativeMode;

        BleTransmissionPower(int nativeMode)
        {
            m_nativeMode = nativeMode;
        }

        /**
         * Returns one of the static final int members of {@link AdvertiseSettings}
         */
        public int getNativeMode()
        {
            return m_nativeMode;
        }

    }

    private final BleAdvertisingMode m_mode;
    private final BleTransmissionPower m_power;
    private final Interval m_timeout;

    /**
     * Base constructor which sets all relevant Ble advertising settings
     */
    public BleAdvertisingSettings(BleAdvertisingMode mode, BleTransmissionPower power, Interval timeout) {
        m_mode = mode;
        m_power = power;
        m_timeout = timeout;
    }

    /**
     * Overload of {@link #BleAdvertisingSettings(BleAdvertisingMode, BleTransmissionPower, Interval)}, which sets
     * the {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode} to {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode#AUTO},
     * {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower} to {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower#MEDIUM},
     * and the timeout to {@link Interval#ZERO} (no timeout).
     */
    public BleAdvertisingSettings() {
        this(BleAdvertisingMode.AUTO, BleTransmissionPower.MEDIUM, Interval.ZERO);
    }

    /**
     * Overload of {@link #BleAdvertisingSettings(BleAdvertisingMode, BleTransmissionPower, Interval)}, which sets the
     * {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode} to {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode#AUTO}.
     */
    public BleAdvertisingSettings(BleTransmissionPower power, Interval timeout) {
        this(BleAdvertisingMode.AUTO, power, timeout);
    }

    /**
     * Overload of {@link #BleAdvertisingSettings(BleAdvertisingMode, BleTransmissionPower, Interval)}, which sets the
     * {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode} to {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode#AUTO},
     * and the timeout to {@link Interval#ZERO} (no timeout).
     */
    public BleAdvertisingSettings(BleTransmissionPower power) {
        this(BleAdvertisingMode.AUTO, power, Interval.ZERO);
    }

    /**
     * Overload of {@link #BleAdvertisingSettings(BleAdvertisingMode, BleTransmissionPower, Interval)}, which sets the
     * {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode} to {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode#AUTO}, and
     * {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower} to {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower#MEDIUM}.
     */
    public BleAdvertisingSettings(Interval timeout) {
        this(BleAdvertisingMode.AUTO, BleTransmissionPower.MEDIUM, timeout);
    }

    /**
     * Returns the advertising mode specified with {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode}
     */
    public BleAdvertisingMode getAdvertisingMode() {
        return m_mode;
    }

    /**
     * Returns the advertising transmission power specified with {@link com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower}
     */
    public BleTransmissionPower getTransmissionPower() {
        return m_power;
    }

    /**
     * Returns the timeout period.
     */
    public Interval getTimeout() {
        return m_timeout;
    }


}
