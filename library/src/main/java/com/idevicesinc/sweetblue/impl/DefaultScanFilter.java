package com.idevicesinc.sweetblue.impl;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Convenience implementation of {@link ScanFilter} which filters using
 * a whitelist of known primary advertising {@link UUID}s passed in to the constructor.
 */
public class DefaultScanFilter implements ScanFilter
{

    private final ArrayList<UUID> m_uuidList;
    private final ArrayList<String> m_nameList;


    /**
     * Constructor to build a {@link ScanFilter} with the provided {@link Collection} of {@link UUID}s. This "whitelists" the provided UUIDs, and will
     * only dispatch {@link com.idevicesinc.sweetblue.BleDevice}s that are advertising with any of those UUIDs to the {@link com.idevicesinc.sweetblue.DiscoveryListener}.
     */
    public DefaultScanFilter(Collection<UUID> whitelist)
    {
        m_uuidList = new ArrayList<>(whitelist);
        m_nameList = null;
    }

    /**
     * Similar to {@link #DefaultScanFilter(Collection)}, only a convenience constructor if you are only concerned with looking for one {@link UUID} in particular.
     */
    public DefaultScanFilter(UUID whitelist)
    {
        m_uuidList = new ArrayList<>();
        m_uuidList.add(whitelist);
        m_nameList = null;
    }

    /**
     * Constructor which sets up a name filter, with the provided names to filter for. This will look at the name provided by {@link BleDevice#getName_native()} to
     * do the filtering. If any of the provided names are contained in the name, the device will be discovered. This filter check is case insensitive.
     */
    public DefaultScanFilter(String... nameList)
    {
        m_nameList = new ArrayList<>(Arrays.asList(nameList));
        m_uuidList = null;
    }

    /**
     * Constructor to use if you wish to filter by name AND by {@link UUID}.
     */
    public DefaultScanFilter(UUID uuid, String name)
    {
        m_uuidList = new ArrayList<>();
        m_uuidList.add(uuid);
        m_nameList = new ArrayList<>();
        m_nameList.add(name);
    }

    /**
     * Constructor to use if you wish to filter by multiple names and {@link UUID}s.
     */
    public DefaultScanFilter(Collection<UUID> uuidList, String... names)
    {
        m_uuidList = new ArrayList<>(uuidList);
        m_nameList = new ArrayList<>(Arrays.asList(names));
    }

    /**
     * Acknowledges the discovery if there's an overlap between the given advertisedServices
     * and the {@link Collection} passed into the constructor of {@link DefaultScanFilter}, OR the name given
     * by {@link BleDevice#getName_native()} contains any of the names given from {@link #DefaultScanFilter(String...)},
     * {@link #DefaultScanFilter(UUID, String)}, or {@link #DefaultScanFilter(Collection, String...)}.
     */
    @Override public Please onEvent(final ScanEvent e)
    {
        // This should never happen, but leaving it here to be thorough. If both lists are null, then accept everything.
        if (m_nameList == null && m_uuidList == null)
            return Please.acknowledge();

        if (m_nameList != null && Utils.haveMatchingName(e.name_native(), m_nameList))
            return Please.acknowledge();

        if (m_uuidList != null)
            return Please.acknowledgeIf( Utils.haveMatchingIds(e.advertisedServices(), m_uuidList) );
        else
            // If we got here, we've checked the name list, and didn't find a match, and the uuid list was null,
            // so at this point, we should ignore the device.
            return Please.ignore();
    }
}
