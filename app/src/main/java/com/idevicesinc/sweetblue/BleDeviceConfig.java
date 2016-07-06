package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;

public class BleDeviceConfig extends BleNodeConfig implements Cloneable
{

    /**
     * Default fallback value for {@link #rssi_min}.
     */
    public static final int DEFAULT_RSSI_MIN					= -120;

    /**
     * Default fallback value for {@link #rssi_max}.
     */
    public static final int DEFAULT_RSSI_MAX					= -30;

    /**
     * The default MTU size in bytes for gatt reads/writes/notifies/etc.
     */
    public static final int DEFAULT_MTU_SIZE					= 23;

    /**
     * Default value for {@link #minScanTimeToUndiscover}.
     */
    public static final double DEFAULT_MINIMUM_SCAN_TIME		= 5.0;


    public boolean cacheDeviceOnUndiscovery                     = true;

    /**
     * Default is <code>false</code> - whether to use <code>BluetoothGatt.refresh()</code> right before service discovery.
     * This method is not in the public Android API, so its use is disabled by default. You may find it useful to enable
     * if your remote device is routinely changing its gatt service profile. This method call supposedly clears a cache
     * that would otherwise prevent changes from being discovered.
     */
    public Boolean useGattRefresh								= false;

    /**
     * The number of times SweetBlue will retry connecting to a device, if it fails. Default is <code>3</code>.
     */
    public int reconnectionTries                                = 3;

    /**
     * Default is {@link Interval#DISABLED}. If a device exceeds this amount of time since its
     * last discovery then it is a candidate for being undiscovered.
     * You may want to configure this number based on the phone or
     * manufacturer. For example, based on testing, in order to make undiscovery snappier the Galaxy S5 could use lower times.
     */
    public Interval timeToUndiscover                            = Interval.DISABLED;

    /**
     * Default is {@link #DEFAULT_MINIMUM_SCAN_TIME}seconds - Undiscovery of devices must be
     * approximated by checking when the last time was that we discovered a device,
     * and if this time is greater than {@link #timeToUndiscover} then the device is undiscovered. However a scan
     * operation must be allowed a certain amount of time to make sure it discovers all nearby devices that are
     * still advertising. This is that time in seconds.
     * <br><br>
     * Use {@link Interval#DISABLED} to disable undiscovery altogether.
     */
    public Interval minScanTimeToUndiscover                     = Interval.secs(DEFAULT_MINIMUM_SCAN_TIME);

    public boolean useLeTransportForBonding                     = false;

    /**
     * This will set the bond behavior of a device when connecting. See {@link BondOnConnectOption} for possible values.
     * Default is {@link BondOnConnectOption#NONE}.
     */
    public BondOnConnectOption bondOnConnectOption              = BondOnConnectOption.NONE;

    /**
     * Tells SweetBlue to use Android's built-in autoConnect option. It's been observed that this doesn't work very
     * well for some devices, so it's <code>false</code> by default.
     */
    public boolean useAndroidAutoConnect                        = false;

    public int rssi_min                                         = DEFAULT_RSSI_MIN;

    public int rssi_max                                         = DEFAULT_RSSI_MAX;

    public BleTransaction.Auth defaultAuthTxn                   = null;

    public BleTransaction.Init defaultInitTxn                   = null;


    @Override protected BleDeviceConfig clone()
    {
        return (BleDeviceConfig) super.clone();
    }


    public enum BondOnConnectOption
    {
        /**
         * Do no automatic bond
         */
        NONE,

        /**
         * Perform a bond before connecting, if the device is not already bonded.
         */
        BOND,

        /**
         * Bond with the device before connecting. If the device is already bonded, then unbond first, then bond.
         */
        RE_BOND;
    }
}
