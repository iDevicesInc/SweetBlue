package com.idevicesinc.sweetblue;


import android.util.Log;

import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.ReflectionUuidNameMap;
import com.idevicesinc.sweetblue.utils.UpdateCallback;
import com.idevicesinc.sweetblue.utils.UuidNameMap;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.List;

public class BleManagerConfig
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


    final String[] debugThreadNames =
            {
                    "AMY", "BEN", "CAM", "DON", "ELI", "FAY", "GUS", "HAL", "IAN", "JAY", "LEO",
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

    public static class DefaultLogger implements SweetLogger
    {
        @Override public void onLogEntry(int level, String tag, String msg)
        {
            Log.println(level, tag, msg);
        }
    }

}
