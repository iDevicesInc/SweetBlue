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

    public boolean runOnUIThread                            = false;
    public boolean postCallbacksToUIThread                  = true;
    public int updateThreadIntervalMs                       = 25;
    public int updateThreadIdleIntervalMs                   = 500;
    public boolean loggingEnabled                           = false;
    public SweetLogger logger                               = new DefaultLogger();
    public Interval taskTimeout                             = Interval.DISABLED;
    public List<UuidNameMap> uuidNameMaps					= null;
    public UpdateCallback updateCallback                    = null;
    public boolean manageCpuWakeLock                        = true;
    public Comparator<BleDevice> defaultDeviceSorter        = new DeviceNameComparator();
    public BleScanAPI scanApi                              = BleScanAPI.AUTO;
    public BleScanPower scanPower                          = BleScanPower.AUTO;
    public ScanFilter defaultScanFilter                     = null;
    public boolean autoPauseResumeDetection                 = true;
    public



    Looper updateLooper                                     = null;


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
     * {@link BleDevice#getName_debug()}.
     */
    public static class DeviceNameComparator implements Comparator<BleDevice> {

        @Override public int compare(BleDevice lhs, BleDevice rhs)
        {
            return lhs.getName_debug().compareTo(rhs.getName_debug());
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

        public DefaultScanFilter(Collection whitelist)
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
