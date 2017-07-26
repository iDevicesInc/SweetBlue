package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Class used to feed options for scanning via {@link BleManager#startScan(ScanOptions)}.
 */
public final class ScanOptions
{

    Interval m_scanTime;
    Interval m_pauseTime;
    BleManagerConfig.ScanFilter m_scanFilter;
    BleManager.DiscoveryListener m_discoveryListener;
    boolean m_isPeriodic;
    boolean m_isPriorityScan;
    boolean m_forceIndefinite;


    public ScanOptions()
    {}

    public ScanOptions(BleManagerConfig.ScanFilter scanFilter)
    {
        m_scanFilter = scanFilter;
    }

    public ScanOptions(BleManager.DiscoveryListener listener_nullable)
    {
        m_discoveryListener = listener_nullable;
    }

    public ScanOptions(BleManagerConfig.ScanFilter scanFilter, BleManager.DiscoveryListener listener_nullable)
    {
        m_scanFilter = scanFilter;
        m_discoveryListener = listener_nullable;
    }


    /**
     * Scan indefinitely until {@link BleManager#stopScan()} is called. If this is called after
     * {@link #scanPeriodically(Interval, Interval)}, this will override the periodic scan.
     */
    public final ScanOptions scanInfinitely()
    {
        return scanFor(Interval.INFINITE);
    }

    /**
     * Scan for the specified amount of time. This method implies a one-time scan. If you want to
     * perform a periodic scan, then use {@link #scanPeriodically(Interval, Interval)} instead.
     *
     * If this is called after {@link #scanPeriodically(Interval, Interval)}, it will override the periodic scan.
     */
    public final ScanOptions scanFor(Interval time)
    {
        m_isPeriodic = false;
        m_pauseTime = null;
        m_scanTime = time;
        return this;
    }

    /**
     * Force a indefinite scan. If you choose to scan indefinitely, and don't set this, SweetBlue will automatically pause the scan, and resume it shortly
     * thereafter, to make sure scan results keep coming in as expected. If you pass in <code>true</code> here, the scan will just run until you call
     * {@link BleManager#stopScan()}, or {@link BleManager#stopAllScanning()}. There's really no reason to do this, but it's left in here to be flexible.
     */
    @Advanced
    public final ScanOptions forceIndefinite(boolean force)
    {
        m_forceIndefinite = force;
        return this;
    }

    /**
     * Do a periodic scan. If you want to do a one-time scan, then use {@link #scanFor(Interval)}
     * instead.
     */
    public final ScanOptions scanPeriodically(Interval scanTime, Interval pauseTime)
    {
        m_isPeriodic = true;
        m_scanTime = scanTime;
        m_pauseTime = pauseTime;
        return this;
    }

    /**
     * Set a {@link com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter} for this scan.
     */
    public final ScanOptions withScanFilter(BleManagerConfig.ScanFilter filter)
    {
        m_scanFilter = filter;
        return this;
    }

    /**
     * Set a {@link com.idevicesinc.sweetblue.BleManager.DiscoveryListener} for this scan.
     */
    public final ScanOptions withDiscoveryListener(BleManager.DiscoveryListener listener)
    {
        m_discoveryListener = listener;
        return this;
    }

    /**
     * This will set the scan to be of the highest priority. This should ONLY be used if you absolutely
     * need it! With this active, ONLY scanning will happen (even if you call connect on a device, or
     * read/write, etc), until you call {@link BleManager#stopScan()} or {@link BleManager#stopPeriodicScan()}, or {@link BleManager#stopAllScanning()}.
     */
    @Advanced
    public final ScanOptions asHighPriority(boolean highPriority)
    {
        m_isPriorityScan = highPriority;
        return this;
    }

}
