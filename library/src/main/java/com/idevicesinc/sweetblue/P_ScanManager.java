package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.BOOST_SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING_PAUSED;
import static com.idevicesinc.sweetblue.BleManagerState.STARTING_SCAN;



final class P_ScanManager
{

    static final int Mode_NULL = -1;
    static final int Mode_BLE = 0;
    static final int Mode_CLASSIC = 1;
    static final int Mode_BLE_POST_LOLLIPOP = 2;


    private final BleManager m_manager;
    private PreLollipopScanCallback m_preLollipopScanCallback;
    private PostLollipopScanCallback m_postLollipopScanCallback;
    private AtomicReference<BleScanApi> mCurrentApi;
    private AtomicReference<BleScanPower> mCurrentPower;
    private Set<ScanInfo> m_scanEntries;

    private final int m_retryCountMax = 3;
    private boolean m_triedToStartScanAfterTurnedOn;
    private boolean m_doingInfiniteScan;
    private boolean m_forceActualInfinite;
    private boolean m_triedToStartScanAfterResume;
    private boolean m_periodicScan;
    private double m_timeNotScanning;
    private double m_timePausedScan;
    private double m_totalTimeScanning;
    private double m_intervalTimeScanning;

    private double m_classicLength;
    private double m_timeClassicBoosting;

    private int m_mode;

    private final Object entryLock = new Object();


    public P_ScanManager(BleManager mgr)
    {
        m_manager = mgr;
        mCurrentApi = new AtomicReference<>(mgr.m_config.scanApi);
        mCurrentPower = new AtomicReference<>(BleScanPower.AUTO);
        m_scanEntries = new HashSet<>();
        m_preLollipopScanCallback = new PreLollipopScanCallback();
        if(Utils.isLollipop())
        {
            m_postLollipopScanCallback = new PostLollipopScanCallback();
        }
    }

    public final boolean classicBoost(double scanTime)
    {
        m_classicLength = scanTime;
        return startClassicBoost();
    }

    public final boolean startScan(PA_StateTracker.E_Intent intent, double scanTime, boolean periodicScan)
    {
        m_periodicScan = periodicScan;
        m_timePausedScan = 0.0;
        m_totalTimeScanning = 0.0;
        BleScanApi scanApi = m_manager.m_config.scanApi == BleScanApi.AUTO ? determineAutoApi() : m_manager.m_config.scanApi;
        switch (scanApi)
        {
            case CLASSIC:
                mCurrentApi.set(BleScanApi.CLASSIC);
                return tryClassicDiscovery(intent, true);
            case POST_LOLLIPOP:
                if (isBleScanReady())
                {
                    if (Utils.isLollipop())
                    {
                        mCurrentApi.set(BleScanApi.POST_LOLLIPOP);
                        return startScanPostLollipop(scanTime);
                    }
                    else
                    {
                        m_manager.getLogger().e("Tried to start post lollipop scan on a device not running lollipop or above! Defaulting to pre-lollipop scan instead.");
                        mCurrentApi.set(BleScanApi.PRE_LOLLIPOP);
                        return startScanPreLollipop(intent);
                    }
                }
                else
                {
                    m_manager.getLogger().e("Tried to start BLE scan, but scanning is not ready (most likely need to get permissions). Falling back to classic discovery.");
                    mCurrentApi.set(BleScanApi.CLASSIC);
                    return tryClassicDiscovery(PA_StateTracker.E_Intent.UNINTENTIONAL, true);
                }
            case AUTO:
            case PRE_LOLLIPOP:
                mCurrentApi.set(BleScanApi.PRE_LOLLIPOP);
                return startScanPreLollipop(intent);
            default:
                return false;
        }
    }

    private BleScanApi determineAutoApi()
    {
        if (m_mode != Mode_BLE_POST_LOLLIPOP)
        {
            return BleScanApi.POST_LOLLIPOP;
        }
        return BleScanApi.PRE_LOLLIPOP;
    }

    public final void stopScan()
    {
        stopScan_private(true);
    }

    public final void stopNativeScan(final P_Task_Scan scanTask)
    {
        if (m_periodicScan)
        {
            pauseScan();
            return;
        }
        if( m_mode == Mode_BLE )
        {
            try
            {
                stopScanPreLollipop();
            }
            catch(NullPointerException e)
            {
                //--- DRK > Catching this because of exception thrown one time...only ever seen once, so not very reproducible.
                //			java.lang.NullPointerException
                //			07-02 15:04:48.149: E/AndroidRuntime(24389): 	at android.bluetooth.BluetoothAdapter$GattCallbackWrapper.stopLeScan(BluetoothAdapter.java:1819)
                //			07-02 15:04:48.149: E/AndroidRuntime(24389): 	at android.bluetooth.BluetoothAdapter.stopLeScan(BluetoothAdapter.java:1722)
                m_manager.getLogger().w(e.getStackTrace().toString());

                m_manager.uhOh(BleManager.UhOhListener.UhOh.RANDOM_EXCEPTION);
            }
        }
        else if ( m_mode == Mode_BLE_POST_LOLLIPOP)
        {
            try
            {
                stopScanPostLollipop();
            }
            catch (NullPointerException e)
            {
                m_manager.getLogger().w(e.getStackTrace().toString());

                m_manager.uhOh(BleManager.UhOhListener.UhOh.RANDOM_EXCEPTION);
            }
        }
        else if( m_mode == Mode_CLASSIC )
        {
            //--- DRK > This assert tripped, but not sure what I can do about it. Technically discovery can be cancelled
            //---		by another app or something, so its usefulness as a logic checker is debatable.
//			ASSERT(m_btMngr.getAdapter().isDiscovering(), "Trying to cancel discovery when not natively running.");

            stopClassicDiscovery();
        }

        if( m_manager.m_config.enableCrashResolver )
        {
            m_manager.m_crashResolver.stop();
        }

        m_manager.getNativeStateTracker().remove(BleManagerState.SCANNING, scanTask.getIntent(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);
    }

    public final void setInfiniteScan(boolean infinite, boolean force)
    {
        m_doingInfiniteScan = infinite;
        m_forceActualInfinite = force;
    }

    public final boolean isInfiniteScan()
    {
        return m_doingInfiniteScan;
    }



    final boolean isPreLollipopScan()
    {
        return m_mode == Mode_BLE;
    }

    final boolean isPostLollipopScan()
    {
        return m_mode == Mode_BLE_POST_LOLLIPOP;
    }

    final boolean isClassicScan()
    {
        return m_mode == Mode_CLASSIC;
    }

    final void pauseScan()
    {
        stopScan_private(false);
    }

    final void addScanResult(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
    {
        final ScanInfo info = new ScanInfo(device, rssi, scanRecord);
        synchronized (entryLock)
        {
            m_scanEntries.add(info);
        }
    }

    final void addBatchScanResults(final List<L_Util.ScanResult> devices)
    {
        synchronized (entryLock)
        {
            for (L_Util.ScanResult res : devices)
            {
                final ScanInfo info = new ScanInfo(res.getDevice(), res.getRssi(), res.getRecord());
                m_scanEntries.add(info);
            }
        }
    }

    final int getCurrentMode()
    {
        return m_mode;
    }

    final void resetTimeNotScanning()
    {
        m_timeNotScanning = 0.0;
    }

    // Returns if the startScan boolean is true or not.
    final boolean update(double timeStep, long currentTime)
    {
        if (m_manager.is(SCANNING))
        {
            m_totalTimeScanning += timeStep;
            m_intervalTimeScanning += timeStep;

            int size = m_scanEntries.size();

            handleScanEntries(size);

            if (!m_forceActualInfinite && m_doingInfiniteScan && Interval.isEnabled(m_manager.m_config.infiniteScanInterval) && m_intervalTimeScanning >= m_manager.m_config.infiniteScanInterval.secs())
            {
                pauseScan();
            }
        }

        if (m_manager.is(SCANNING_PAUSED))
        {
            m_timePausedScan += timeStep;

            if (m_doingInfiniteScan)
            {
                Interval pauseTime = Interval.isEnabled(m_manager.m_config.infinitePauseInterval) ? m_manager.m_config.infinitePauseInterval : Interval.secs(BleManagerConfig.DEFAULT_SCAN_INFINITE_PAUSE_TIME);
                if (m_timePausedScan >= pauseTime.secs())
                {
                    m_manager.getLogger().i("Restarting paused scan...");
                    startScan(PA_StateTracker.E_Intent.INTENTIONAL, Interval.INFINITE.secs(), false);
                }
            }
        }

        if( !m_manager.isAny(SCANNING) )
        {
            m_timeNotScanning += timeStep;
        }

        boolean stopClassicBoost = false;

        if (m_manager.is(BOOST_SCANNING))
        {
            m_timeClassicBoosting += timeStep;
            if (m_timeClassicBoosting >= m_classicLength)
            {
                stopClassicBoost = true;
            }
        }

        boolean startScan = false;

        if( Interval.isEnabled(m_manager.m_config.autoScanActiveTime) && m_manager.ready() && !m_manager.is(BOOST_SCANNING))
        {
            if( m_manager.isForegrounded() )
            {
                if (Interval.isEnabled(m_manager.m_config.autoScanDelayAfterBleTurnsOn) && m_triedToStartScanAfterTurnedOn && (currentTime - m_manager.timeTurnedOn()) >= m_manager.m_config.autoScanDelayAfterBleTurnsOn.millis())
                {
                    m_triedToStartScanAfterTurnedOn = true;

                    if (!m_manager.isScanning())
                    {
                        m_manager.getLogger().i("Auto starting scan after BLE turned back on...");
                        startScan = true;
                    }
                }
                else if ( Interval.isEnabled(m_manager.m_config.autoScanDelayAfterResume) && !m_triedToStartScanAfterResume && m_manager.timeForegrounded() >= Interval.secs(m_manager.m_config.autoScanDelayAfterResume) )
                {
                    m_triedToStartScanAfterResume = true;

                    if (!m_manager.isScanning())
                    {
                        m_manager.getLogger().i("Auto starting scan after resume...");
                        startScan = true;
                    }
                }
            }
            if( m_periodicScan && !m_manager.isAny(SCANNING, STARTING_SCAN) )
            {
                double scanInterval = Interval.secs(m_manager.isForegrounded() ? m_manager.m_config.autoScanPauseInterval : m_manager.m_config.autoScanPauseTimeWhileAppIsBackgrounded);

                if( Interval.isEnabled(scanInterval) && m_timeNotScanning >= scanInterval )
                {
                    m_manager.getLogger().i("Starting scan as part of a periodic scan...");
                    startScan = true;
                }
            }
        }

        if( startScan )
        {
            if( m_manager.doAutoScan() )
            {
                m_manager.startScan_private(new ScanOptions().scanPeriodically(m_manager.m_config.autoScanActiveTime, m_manager.m_config.autoScanPauseInterval));
            }
        }

        final P_Task_Scan scanTask = m_manager.getTaskQueue().get(P_Task_Scan.class, m_manager);

        if( scanTask != null )
        {

            if (stopClassicBoost)
            {
                m_timeClassicBoosting = 0;
                stopClassicDiscovery();
                scanTask.onClassicBoostFinished();
            }

            //--- DRK > Not sure why this was originally also for the ARMED case...
//			if( scanTask.getState() == PE_TaskState.ARMED || scanTask.getState() == PE_TaskState.EXECUTING )
            if( scanTask.getState() == PE_TaskState.EXECUTING )
            {
                m_manager.tryPurgingStaleDevices(scanTask.getAggregatedTimeArmedAndExecuting());
            }
        }

        return startScan;
    }

    final boolean isPeriodicScan()
    {
        return m_periodicScan;
    }

    final void onResume()
    {
        m_triedToStartScanAfterResume = false;

        if( m_doingInfiniteScan && !m_manager.isScanning())
        {
            m_triedToStartScanAfterResume = true;

            m_manager.startScan();
        }
        else if( Interval.isDisabled(m_manager.m_config.autoScanDelayAfterResume) )
        {
            m_triedToStartScanAfterResume = true;
        }
    }

    final BleScanApi getCurrentApi()
    {
        return mCurrentApi.get();
    }

    final BleScanPower getCurrentPower()
    {
        return mCurrentPower.get();
    }

    final void onPause()
    {
        m_triedToStartScanAfterResume = false;
        if( m_manager.m_config.stopScanOnPause && m_manager.is(SCANNING) )
        {
            m_manager.stopScan_private(PA_StateTracker.E_Intent.UNINTENTIONAL);
        }
    }





    private void handleScanEntries(int size)
    {
        if ( size > 0 )
        {
            final List<ScanInfo> infos;

            // Get our max scan entries to process based off the update loop rate, with
            // a minimum of 5.
            final long upRate = m_manager.m_config.autoUpdateRate.millis();
            final int maxEntries = (int) Math.min(size, Math.max(5, upRate));
            infos = new ArrayList<>(maxEntries);
            synchronized (entryLock)
            {
                int current = 0;
                final Iterator<ScanInfo> it = m_scanEntries.iterator();
                while (it.hasNext() && current < maxEntries)
                {
                    infos.add(it.next());
                    it.remove();
                    current++;
                }
            }

            final List<DiscoveryEntry> entries = new ArrayList<>(infos.size());

            for (ScanInfo info : infos)
            {
                final P_NativeDeviceLayer layer = m_manager.m_config.newDeviceLayer(BleDevice.NULL);
                layer.setNativeDevice(info.m_device);

                if (m_mode == Mode_BLE)
                {
                    m_manager.getCrashResolver().notifyScannedDevice(layer, m_preLollipopScanCallback, null);
                }
                else
                {
                    m_manager.getCrashResolver().notifyScannedDevice(layer, null, L_Util.getNativeCallback());
                }

                entries.add(DiscoveryEntry.newEntry(layer, info.m_rssi, info.m_record));
            }

            m_manager.onDiscoveredFromNativeStack(entries);
        }
    }

    private boolean startClassicDiscovery()
    {
        return m_manager.managerLayer().startDiscovery();
    }

    private boolean startClassicBoost()
    {
        boolean success = startClassicDiscovery();
        if (success)
        {
            m_manager.m_stateTracker.update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, STARTING_SCAN, false, BOOST_SCANNING, true);
        }
        return success;
    }

    private boolean isBleScanReady()
    {
        return m_manager.isLocationEnabledForScanning() && m_manager.isLocationEnabledForScanning_byRuntimePermissions() && m_manager.isLocationEnabledForScanning_byOsServices();
    }

    private void stopScan_private(boolean stopping)
    {
        m_intervalTimeScanning = 0.0;
        switch (mCurrentApi.get())
        {
            case CLASSIC:
                stopClassicDiscovery();
                break;
            case POST_LOLLIPOP:
                if (Utils.isLollipop())
                {
                    stopScanPostLollipop();
                }
                else
                {
                    stopScanPreLollipop();
                }
                break;
            case AUTO:
            case PRE_LOLLIPOP:
                stopScanPreLollipop();
        }
        if (stopping)
        {
            m_manager.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false, BOOST_SCANNING, false, SCANNING_PAUSED, false);
        }
        else
        {
            m_manager.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false, SCANNING_PAUSED, true, BOOST_SCANNING, false);
        }
        // Clear out the scan entries list so we don't end up caching old discoveries (it's possible there's a large amount of time between scans, so
        // what's held in the list may not actually be within range anymore, or some other data on it has changed).
        synchronized (entryLock)
        {
            m_scanEntries.clear();
        }
    }

    private boolean startScanPreLollipop(PA_StateTracker.E_Intent intent)
    {
        //--- DRK > Not sure how useful this retry loop is. I've definitely seen startLeScan
        //---		fail but then work again at a later time (seconds-minutes later), so
        //---		it's possible that it can recover although I haven't observed it in this loop.
        int retryCount = 0;

        while (retryCount <= m_retryCountMax)
        {
            final boolean success = startLeScan();

            if (success)
            {
                if (retryCount >= 1)
                {
                    //--- DRK > Not really an ASSERT case...rather just really want to know if this can happen
                    //---		so if/when it does I want it to be loud.
                    //---		UPDATE: Yes, this hits...TODO: Now have to determine if this is my fault or Android's.
                    //---		Error message is "09-29 16:37:11.622: E/BluetoothAdapter(16286): LE Scan has already started".
                    //---		Calling stopLeScan below "fixes" the issue.
                    //---		UPDATE: Seems like it mostly happens on quick restarts of the app while developing, so
                    //---		maybe the scan started in the previous app sessions is still lingering in the new session.
//					ASSERT(false, "Started Le scan on attempt number " + retryCount);
                }

                break;
            }

            retryCount++;

            if (retryCount <= m_retryCountMax)
            {
                if (retryCount == 1)
                {
                    m_manager.getLogger().w("Failed first startLeScan() attempt. Calling stopLeScan() then trying again...");

                    //--- DRK > It's been observed that right on app start up startLeScan can fail with a log
                    //---		message saying it's already started...not sure if it's my fault or not but throwing
                    //---		this in as a last ditch effort to "fix" things.
                    //---
                    //---		UPDATE: It's been observed through simple test apps that when restarting an app through eclipse,
                    //---		Android somehow, sometimes, keeps the same actual BleManager instance in memory, so it's not
                    //---		far-fetched to assume that the scan from the previous app run can sometimes still be ongoing.
                    //m_btMngr.getAdapter().stopLeScan(m_listeners.m_scanCallback);
                    stopLeScan();
                }
                else
                {
                    m_manager.getLogger().w("Failed startLeScan() attempt number " + retryCount + ". Trying again...");
                }
            }
        }

        if (retryCount > m_retryCountMax)
        {
            m_manager.getLogger().w("Pre-Lollipop LeScan totally failed to start!");

            tryClassicDiscovery(PA_StateTracker.E_Intent.UNINTENTIONAL, /*suppressUhOh=*/false);
            return true;
        }
        else
        {
            if (retryCount > 0)
            {
                m_manager.getLogger().w("Started native scan with " + (retryCount + 1) + " attempts.");
            }

            if (m_manager.m_config.enableCrashResolver)
            {
                m_manager.getCrashResolver().start();
            }

            m_mode = Mode_BLE;

            setStateToScanning();

            return true;
        }
    }

    private void setStateToScanning()
    {
        m_manager.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, true, SCANNING_PAUSED, false, STARTING_SCAN, false, BOOST_SCANNING, false);
    }

    private boolean startScanPostLollipop(double scanTime)
    {
        int nativePowerMode;
        BleScanPower power = m_manager.m_config.scanPower;
        if (power == BleScanPower.AUTO)
        {
            if (m_manager.isForegrounded())
            {
                if (scanTime == Double.POSITIVE_INFINITY)
                {
                    power = BleScanPower.MEDIUM_POWER;
                    nativePowerMode = BleScanPower.MEDIUM_POWER.getNativeMode();
                }
                else
                {
                    power = BleScanPower.HIGH_POWER;
                    nativePowerMode = BleScanPower.HIGH_POWER.getNativeMode();
                }
            }
            else
            {
                power = BleScanPower.LOW_POWER;
                nativePowerMode = BleScanPower.LOW_POWER.getNativeMode();
            }
        }
        else
        {
            if (power == BleScanPower.VERY_LOW_POWER)
            {
                if (!Utils.isMarshmallow())
                {
                    m_manager.getLogger().e("BleScanPower set to VERY_LOW, but device is not running Marshmallow. Defaulting to LOW instead.");
                    power = BleScanPower.LOW_POWER;
                }
            }
            nativePowerMode = power.getNativeMode();
        }
        if (Utils.isMarshmallow())
        {
            startMScan(nativePowerMode);
        }
        else
        {
            startLScan(nativePowerMode);
        }
        m_mode = Mode_BLE_POST_LOLLIPOP;
        mCurrentPower.set(power);
        setStateToScanning();
        return true;
    }

    private boolean tryClassicDiscovery(final PA_StateTracker.E_Intent intent, final boolean suppressUhOh)
    {
        boolean intentional = intent == PA_StateTracker.E_Intent.INTENTIONAL;
        if (intentional || m_manager.m_config.revertToClassicDiscoveryIfNeeded)
        {
            if (false == startClassicDiscovery())
            {
                m_manager.getLogger().w("Classic discovery failed to start!");

                fail();

                m_manager.uhOh(BleManager.UhOhListener.UhOh.CLASSIC_DISCOVERY_FAILED);

                return false;
            }
            else
            {
                if (false == suppressUhOh)
                {
                    m_manager.uhOh(BleManager.UhOhListener.UhOh.START_BLE_SCAN_FAILED__USING_CLASSIC);
                }

                m_mode = Mode_CLASSIC;
                setStateToScanning();

                return true;
            }
        }
        else
        {
            fail();

            m_manager.uhOh(BleManager.UhOhListener.UhOh.START_BLE_SCAN_FAILED);

            return false;
        }
    }

    private boolean startLeScan()
    {
        return m_manager.managerLayer().startLeScan(m_preLollipopScanCallback);
    }

    private void startLScan(int mode)
    {
        m_manager.managerLayer().startLScan(mode, m_manager.m_config.scanReportDelay, m_postLollipopScanCallback);
    }

    private void startMScan(int mode)
    {
        m_manager.managerLayer().startMScan(mode, m_manager.m_config.scanReportDelay, m_postLollipopScanCallback);
    }

    private void fail()
    {
        m_manager.getTaskQueue().fail(P_Task_Scan.class, m_manager);
    }

    private void stopScanPreLollipop()
    {
        try
        {
            stopLeScan();
        } catch (Exception e)
        {
            m_manager.getLogger().e("Got an exception (" + e.getClass().getSimpleName() + ") with a message of " + e.getMessage() + " when trying to stop a pre-lollipop scan!");
        }
    }

    private void stopLeScan()
    {
        m_manager.managerLayer().stopLeScan(m_preLollipopScanCallback);
    }

    private void stopScanPostLollipop()
    {
        m_manager.managerLayer().stopLeScan(m_preLollipopScanCallback);
    }

    private void stopClassicDiscovery()
    {
        m_manager.managerLayer().cancelDiscovery();
    }



    static final class DiscoveryEntry
    {
        private final P_NativeDeviceLayer deviceLayer;
        private final int rssi;
        private final byte[] scanRecord;

        BleDevice m_bleDevice;
        BleDeviceOrigin m_origin;
        BleManagerConfig.ScanFilter.ScanEvent m_scanEvent;
        boolean m_newlyDiscovered;


        DiscoveryEntry(P_NativeDeviceLayer layer, int rssi, byte[] record)
        {
            deviceLayer = layer;
            this.rssi = rssi;
            scanRecord = record;
        }

        P_NativeDeviceLayer device()
        {
            return deviceLayer;
        }

        int rssi()
        {
            return rssi;
        }

        byte[] record()
        {
            return scanRecord;
        }

        static DiscoveryEntry newEntry(P_NativeDeviceLayer layer, int rssi, byte[] record)
        {
            return new DiscoveryEntry(layer, rssi, record);
        }
    }

    private final static class ScanInfo
    {
        private final BluetoothDevice m_device;
        private final int m_rssi;
        private final byte[] m_record;

        ScanInfo(BluetoothDevice device, int rssi, byte[] record)
        {
            m_device = device;
            m_rssi = rssi;
            m_record = record;
        }

        @Override
        public int hashCode()
        {
            if (m_device != null)
            {
                return m_device.getAddress().hashCode();
            }
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (obj instanceof ScanInfo)
            {
                ScanInfo other = (ScanInfo) obj;
                if ((m_device == null && other.m_device != null) || (m_device != null && other.m_device == null))
                    return false;
                return m_device.getAddress().equals(other.m_device.getAddress());
            }
            return false;
        }
    }

    private final class PreLollipopScanCallback implements BluetoothAdapter.LeScanCallback
    {

        @Override public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            addScanResult(device, rssi, scanRecord);
        }
    }

    private final class PostLollipopScanCallback implements L_Util.ScanCallback
    {

        public static final int SCAN_FAILED_ALREADY_STARTED = 1;

        @Override public void onScanResult(int callbackType, L_Util.ScanResult result)
        {
            addScanResult(result.getDevice(), result.getRssi(), result.getRecord());
        }

        @Override public void onBatchScanResults(List<L_Util.ScanResult> results)
        {
            addBatchScanResults(results);
        }

        @Override public void onScanFailed(int errorCode)
        {
            m_manager.getLogger().e(Utils_String.concatStrings("Post lollipop scan failed with error code ", String.valueOf(errorCode)));
            if (errorCode != SCAN_FAILED_ALREADY_STARTED)
            {
                fail();
            }
            else
            {
                tryClassicDiscovery(PA_StateTracker.E_Intent.UNINTENTIONAL, /*suppressUhOh=*/false);

                m_mode = Mode_CLASSIC;
            }
        }
    }

}
