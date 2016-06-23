package com.idevicesinc.sweetblue;


import android.os.Looper;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.ReflectionUuidNameMap;
import com.idevicesinc.sweetblue.utils.UpdateCallback;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.UuidNameMap;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BleManagerConfig extends BleDeviceConfig
{

    public final static float DEFAULT_TASK_TIMEOUT                      = 12.5f;


    /**
     * Set this to true if you want SweetBlue to run in the UI thread. This is <code>false</code> by default for
     * better app performance. There really shouldn't be a need to set this to <code>true</code>, but it's here
     * for last ditch efforts to fix issues on unruly devices.
     */
    public boolean runOnUIThread                                        = false;

    /**
     * Post all callbacks to the UI thread. This is <code>true</code> by default. If set to false, the callback will be
     * returned on the thread that SweetBlue is running on. If {@link #runOnUIThread} is <code>true</code>, then this
     * option will do nothing.
     */
    public boolean postCallbacksToUIThread                              = true;

    /**
     * Allow SweetBlue to revert to classic discovery when scanning for devices. The default is <code>true</code>. This is used
     * in two places in SweetBlue. First, sometimes, starting a BLE scan will fail sometimes, and the second place is on devices
     * running Marshmallow and above, if the necessary permissions haven't been requested/granted. Note that when using classic
     * discovery, no scan record is returned, so you can only then filter on device name.
     *
     */
    public boolean revertToClassicDiscoveryIfNeeded                     = true;

    /**
     * This sets the interval that SweetBlue's internal update loop runs at. The default is {@link UpdateThreadSpeed#TWENTY_FIVE_MS}.
     */
    public UpdateThreadSpeed updateThreadSpeed                          = UpdateThreadSpeed.TWENTY_FIVE_MS;

    /**
     * This is the interval that Sweetblue's internal update loop runs at when no tasks have entered the queue within the time specified
     * in {@link #delayBeforeIdleMs}.
     */
    public int updateThreadIdleIntervalMs                               = 500;

    /**
     * This sets the amount of time that SweetBlue waits before going into the {@link BleManagerState#IDLE} state, which slows down the
     * internal update loop to {@link #updateThreadIdleIntervalMs}. As soon as a new task is entered into the system, the interval returns
     * to {@link #updateThreadSpeed}.
     */
    public int delayBeforeIdleMs                                        = 250;

    /**
     * Sets whether logging is enabled or not. Default is <code>false</code>
     */
    public boolean loggingEnabled                                       = false;

    /**
     * This allows you to pipe SweetBlue's log statements elsewhere. Default uses {@link DefaultLogger}, which outputs the log
     * statements to android's logcat.
     */
    public SweetLogger logger                                           = new DefaultLogger();

    /**
     * This sets the amount of time tasks are allowed to run before being timed out. Default is {@link #DEFAULT_TASK_TIMEOUT}.
     */
    public Interval taskTimeout                                         = Interval.secs(DEFAULT_TASK_TIMEOUT);

    public BluetoothEnablerController bluetoothEnablerController        = new DefaultBluetoothEnablerController(new BluetoothEnablerConfig());
    public boolean autoPauseResumeDetection                             = true;
    public UpdateCallback updateCallback                                = null;
    public boolean manageCpuWakeLock                                    = true;
    public BleScanAPI scanApi                                           = BleScanAPI.AUTO;
    public BleScanPower scanPower                                       = BleScanPower.AUTO;
    public ScanFilter defaultScanFilter                                 = null;
    public Comparator<BleDevice> defaultDeviceSorter                    = new DeviceNameComparator();

    /**
     * This sets the amount of time before the scan is "reset". By reset, I mean the scan will be stopped, then started again. This needs to happen, as android
     * seems to cache results, and you don't get very accurate scans, if you do an infinite scan using {@link BleManager#startScan()}.
     */
    public Interval scanResetInterval                                   = Interval.secs(7.0);

    public List<UuidNameMap> uuidNameMaps					            = null;


    Looper updateLooper                                                 = null;


    final String[] debugThreadNames =
            {
                    "MAIN", "UPDATE", "CAM", "DON", "ELI", "FAY", "GUS", "HAL", "IAN", "JAY", "LEO",
                    "MAX", "NED", "OLA", "PAT", "RON", "SAL", "TED", "VAL", "WES", "YEE", "ZED"
            };

    void initMaps()
    {
        if (loggingEnabled && uuidNameMaps == null)
        {
            uuidNameMaps = new ArrayList<>();
            uuidNameMaps.add(new ReflectionUuidNameMap(Uuids.class));
        }
    }

    /**
     * Default sorter class for sorting the list of devices in {@link BleManager}. This sorts by
     * {@link BleDevice#getName()}.
     */
    public static class DeviceNameComparator implements Comparator<BleDevice> {

        @Override public int compare(BleDevice lhs, BleDevice rhs)
        {
            return lhs.getName().compareTo(rhs.getName());
        }
    }

    public static class DefaultLogger implements SweetLogger
    {
        @Override public void onLogEntry(int level, String tag, String msg)
        {
            Log.println(level, tag, msg);
        }
    }

    public static class DefaultScanFilter implements ScanFilter
    {

        private final ArrayList<UUID> mWhiteList;

        public DefaultScanFilter(UUID uuid)
        {
            mWhiteList = new ArrayList<>(1);
            mWhiteList.add(uuid);
        }

        public DefaultScanFilter(Collection<UUID> whitelist)
        {
            mWhiteList = new ArrayList<>(whitelist);
        }

        @Override public Please onEvent(ScanEvent e)
        {
            return Please.acknowledgeIf(Utils.haveMatchingIds(e.advertisedServices(), mWhiteList));
        }
    }

    @Override protected BleManagerConfig clone()
    {
        return (BleManagerConfig) super.clone();
    }

}
