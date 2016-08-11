package com.idevicesinc.sweetblue;


import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.idevicesinc.sweetblue.listeners.BondListener;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener;
import com.idevicesinc.sweetblue.listeners.DeviceStateListener;
import com.idevicesinc.sweetblue.listeners.DiscoveryListener;
import com.idevicesinc.sweetblue.listeners.EnablerDoneListener;
import com.idevicesinc.sweetblue.listeners.ManagerStateListener;
import com.idevicesinc.sweetblue.listeners.NotifyListener;
import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.BleScanInfo;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.P_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.idevicesinc.sweetblue.BleManagerState.*;;


public class BleManager
{

    static BleManager sInstance;

    private Context mContext;
    BleManagerConfig mConfig;
    private SweetBlueHandlerThread mThread;
    private UpdateRunnable mUpdateRunnable;
    P_PostManager mPostManager;
    P_TaskManager mTaskManager;
    BleServer mServer;
    private P_Logger mLogger;
    private P_NativeBleStateTracker mNativeStateTracker;
    private P_BleStateTracker mStateTracker;
    private ManagerStateListener mStateListener;
    private long mLastTaskExecution;
    private long mUpdateInterval;
    private BluetoothManager mNativeManager;
    private P_BleReceiverManager mReceiverManager;
    private P_WakeLockManager mWakeLockManager;
    private P_DeviceManager mDeviceManager;
    DeviceStateListener mDefaultStateListener;
    BondListener mDefaultBondListener;
    NotifyListener mDefaultNotifyListener;
    ReadWriteListener mDefaultReadWriteListener;
    DiscoveryListener mDiscoveryListener;
    DeviceConnectionFailListener mDefaultConnectionFailListener;
    P_ScanManager mScanManager;
    private boolean mForegrounded = false;
    private LifecycleListener mLifecycleListener;


    private BleManager(Context context)
    {
        this(context, new BleManagerConfig());
    }

    private BleManager(Context context, BleManagerConfig config)
    {
        mContext = context.getApplicationContext();
        mConfig = config;
        mNativeManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

        BleManagerState nativeState;
        if( mNativeManager == null )
        {
            nativeState = BleManagerState.get(BluetoothAdapter.STATE_ON);
        }
        else
        {
            nativeState = BleManagerState.get(mNativeManager.getAdapter().getState());
        }
        mDeviceManager = new P_DeviceManager(this);
        mNativeStateTracker = new P_NativeBleStateTracker(this);
        mNativeStateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        mStateTracker = new P_BleStateTracker(this);
        mStateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        mTaskManager = new P_TaskManager(this);
        mReceiverManager = new P_BleReceiverManager(this);

        initConfigDependantMembers();
    }

    public static BleManager get(Context context)
    {
        if (sInstance == null)
        {
            sInstance = new BleManager(context);
        }
        return sInstance;
    }

    public static BleManager get(Context context, BleManagerConfig config)
    {
        if (sInstance == null)
        {
            sInstance = new BleManager(context, config);
        }
        else
        {
            sInstance.setConfig(config);
        }
        return sInstance;
    }

    public void setManagerStateListener(ManagerStateListener listener)
    {
        mStateTracker.setListener(listener);
    }

    public void setDefaultDeviceStateListener(DeviceStateListener listener)
    {
        mDefaultStateListener = listener;
    }

    public void setDefaultNotifyListener(NotifyListener listener)
    {
        mDefaultNotifyListener = listener;
    }

    public void setDefaultReadWriteListener(ReadWriteListener listener)
    {
        mDefaultReadWriteListener = listener;
    }

    public void setDefaultConnectionFailListener(DeviceConnectionFailListener failListener)
    {
        mDefaultConnectionFailListener = failListener;
    }

    public void setDefaultBondListener(BondListener listener)
    {
        mDefaultBondListener = listener;
    }

    public void setDiscoveryListener(DiscoveryListener listener)
    {
        mDiscoveryListener = listener;
    }

    public void turnOn()
    {
        if (isAny(TURNING_ON, ON))
        {
            return;
        }

        if (is(BleManagerState.OFF))
        {
            mStateTracker.update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_ON, true, OFF, false);
        }

        mTaskManager.add(new P_Task_TurnBleOn(mTaskManager));
    }

    public void turnOff()
    {
        if (isAny(TURNING_OFF, OFF))
        {
            return;
        }

        if (is(ON))
        {
            mStateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_OFF, true, ON, false);
        }

        mTaskManager.add(new P_Task_TurnBleOff(mTaskManager));
    }

    public boolean is(BleManagerState state)
    {
        return state.overlaps(getStateMask());
    }

    public boolean isAny(BleManagerState... states)
    {
        for( int i = 0; i < states.length; i++ )
        {
            if( is(states[i]) )  return true;
        }

        return false;
    }

    public BluetoothManager getNativeManager()
    {
        return mNativeManager;
    }

    public BluetoothAdapter getNativeAdapter()
    {
        return mNativeManager.getAdapter();
    }

    public Context getAppContext()
    {
        return mContext;
    }

    public void setConfig(BleManagerConfig config)
    {
        mConfig = config;
        initConfigDependantMembers();
    }

    public BleManagerConfig getConfig()
    {
        return mConfig;
    }

    public void startScan()
    {
        startScan_private(Interval.DISABLED, Interval.DISABLED);
    }

    public void startScan(Interval scanTime)
    {
        startScan_private(scanTime, Interval.DISABLED);
    }

    public void startPeriodicScan(Interval scanTime, Interval pauseTime)
    {
        startScan_private(scanTime, pauseTime);
    }

    public void stopScan()
    {
        if (isAny(SCANNING, SCAN_PAUSED))
        {
            mScanManager.stopScan();
            mTaskManager.succeedTask(P_Task_Scan.class, this);
        }
    }

    public BleDevice getDevice(String macAddress)
    {
        final BleDevice device = mDeviceManager.get(macAddress);
        if (device == null)
        {
            return BleDevice.NULL;
        }
        else
        {
            return device;
        }
    }

    public void removeDeviceFromCache(BleDevice device)
    {
        mDeviceManager.remove(device, null);
    }

    public boolean hasDevice(String macAddress)
    {
        return !getDevice(macAddress).isNull();
    }

    public boolean isForegrounded()
    {
        return mForegrounded;
    }

    public void onPause()
    {
        mForegrounded = false;
    }

    public void onResume()
    {
        mForegrounded = true;
    }

    public void shutdown()
    {
        mPostManager.removeUpdateCallbacks(mUpdateRunnable);
        mReceiverManager.onDestroy();
        if (!mConfig.runOnUIThread && mThread != null)
        {
            mThread.quit();
        }

        sInstance = null;
    }

    public void enableBluetoothAndMarshmallowPrerequisites(Activity callingActivity)
    {
        this.enableBluetoothAndMarshmallowPrerequisites(callingActivity, null);
    }

    public void enableBluetoothAndMarshmallowPrerequisites(Activity callingActivity, EnablerDoneListener doneListener)
    {
        mConfig.bluetoothEnablerController.listener = doneListener;

        BluetoothEnabler.enableBluetoothAndPrerequisites(callingActivity, mConfig.bluetoothEnablerController, this);
    }


    public void resumeBluetoothEnablerIfPaused(BluetoothEnabler.Please resumePlease)
    {
        BluetoothEnabler.resumeEnabler(resumePlease);
    }

    public boolean isBleSupported()
    {
        PackageManager manager = mContext.getPackageManager();

        return manager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }
    public Set<BleDevice> getPreviouslyConnectedDevices()
    {
        Set<String> devList = mDeviceManager.previouslyConnectedDevices();
        Set<BleDevice> devices = new HashSet<>(devList.size());
        BleDevice tmpDevice;
        for (String mac : devList)
        {
            tmpDevice = newDevice(mac);
            if (!tmpDevice.isNull())
            {
                devices.add(tmpDevice);
            }
        }
        return devices;
    }

    public BleDevice newDevice(String macAddress)
    {
        return newDevice(macAddress, null);
    }

    public BleDevice newDevice(String macAddress, BleDeviceConfig config)
    {
        return newDevice(macAddress, null, config);
    }

    public BleDevice newDevice(String macAddress, String deviceName, BleDeviceConfig config)
    {
        final BleDevice existingDevice = getDevice(macAddress);
        if (!existingDevice.isNull())
        {
            if (config != null)
            {
                existingDevice.setConfig(config);
            }
            return existingDevice;
        }

        final BluetoothDevice nativeDevice = getNativeAdapter().getRemoteDevice(macAddress);
        if (nativeDevice == null)
        {
            return BleDevice.NULL;
        }

        final BleDevice newDevice = new BleDevice(this, nativeDevice, BleDeviceOrigin.EXPLICIT, config, deviceName, false);
        postDeviceDiscovery(newDevice, DiscoveryListener.LifeCycle.DISCOVERED);
        return newDevice;
    }

    public void clearConnectedDevice(String macAddress)
    {
        mDeviceManager.clearConnectedDevice(macAddress);
    }

    public void clearAllConnectedDevices()
    {
        mDeviceManager.clearAllConnectedDevices();
    }

    public ArrayList<BleDevice> getDeviceList()
    {
        return mDeviceManager.getList();
    }

    public void pushWakeLock()
    {
        mWakeLockManager.push();
    }

    public void popWakeLock()
    {
        mWakeLockManager.pop();
    }

    void deviceConnected(BleDevice device)
    {
        mDeviceManager.deviceConnected(device);
    }

    int getStateMask()
    {
        return mStateTracker.getState();
    }

    int getNativeStateMask()
    {
        return mNativeStateTracker.getState();
    }

    P_NativeBleStateTracker getNativeStateTracker()
    {
        return mNativeStateTracker;
    }

    P_BleStateTracker getStateTracker()
    {
        return mStateTracker;
    }

    boolean isOnSweetBlueThread()
    {
        return mPostManager.isOnSweetBlueThread();
    }

    P_Logger getLogger()
    {
        return mLogger;
    }

    void update(long curTimeMs)
    {
        if (mTaskManager.update(curTimeMs))
        {
            // The task manager reported that there are tasks to process (or are being processed)
            // So, we record this time, and make sure the update interval is at full speed.
            mLastTaskExecution = curTimeMs;
            mUpdateInterval = mConfig.updateThreadSpeed.getMilliseconds();
            mStateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, IDLE, false);
        }

        mDeviceManager.update(curTimeMs);

        if (mConfig.updateCallback != null)
        {
            mConfig.updateCallback.onUpdate(curTimeMs);
        }

        if (mLastTaskExecution + mConfig.delayBeforeIdleMs < curTimeMs)
        {
            // If the last task execution happened more than the idle delay buffer time ago, then we spin down
            // the update cycle, so we're not chewing up CPU/battery power unnecessarily.
            mUpdateInterval = mConfig.updateThreadIdleIntervalMs;
            mStateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, IDLE, true);
        }
    }

    synchronized void onDiscoveredFromNativeStack(final BluetoothDevice device_native, int rssi, byte[] scanRecord_nullable)
    {
        //--- DRK > Protects against fringe case where scan task is executing and app calls turnOff().
        //---		Here the scan task will be interrupted but still potentially has enough time to
        //---		discover another device or two. We're checking the enum state as opposed to the native
        //---		integer state because in this case the "turn off ble" task hasn't started yet and thus
        //---		hasn't called down into native code and thus the native state hasn't changed.
        if( false == is(ON) )  return;

        //--- DRK > Not sure if queued up messages to library's thread can sneak in a device discovery event
        //---		after user called stopScan(), so just a check to prevent unexpected callbacks to the user.
        if( false == isAny(SCANNING, SCAN_PAUSED) )  return;


        final String macAddress = device_native.getAddress();
        final BleScanInfo mScanInfo = Utils_ScanRecord.parseScanRecord(scanRecord_nullable);

        // ---> RB > We keep track of all the ways you can get the device's name. The most reliable way seems to be
        // getting it from the native device. However, sometimes it's not so reliable, and you have to get it by
        // parsing the scan record. The "device_name" is what we determine to be the best. By that I mean whichever
        // name is not null/empty. It has been observed however, that both may not be null/empty, so we store both
        // so that they are both available.
        String native_name = device_native.getName();
        String device_name = native_name;
        String name_record = mScanInfo.getName();

        if (TextUtils.isEmpty(native_name))
        {
            device_name = mScanInfo.getName();

            if (TextUtils.isEmpty(device_name))
            {
                String[] address_split = macAddress.split(":");
                String lastFourOfMac = address_split[address_split.length - 2] + address_split[address_split.length - 1];
                device_name = Utils_String.concatStrings("<No_Name_", lastFourOfMac, ">");
            }
        }

        BleDevice device = mDeviceManager.get(macAddress);

        DiscoveryListener.LifeCycle cycle = null;
        if (device == null)
        {
            if (mConfig.defaultScanFilter != null)
            {
                ScanFilter.Please please = mConfig.defaultScanFilter.onEvent(new ScanFilter.ScanEvent(device_native, mScanInfo, native_name, device_name, rssi));
                if (!please.ack())
                {
                    return;
                }
            }

            cycle = mDeviceManager.getWasUndiscovered(macAddress) ? DiscoveryListener.LifeCycle.REDISCOVERED : DiscoveryListener.LifeCycle.DISCOVERED;

            device = new BleDevice(this, device_native, BleDeviceOrigin.FROM_DISCOVERY, null, device_name, false);
            device.setNameFromScanRecord(name_record);
            device.setNameFromNative(native_name);
            mDeviceManager.add(device);
            device.onNewlyDiscovered(rssi, mScanInfo, BleDeviceOrigin.FROM_DISCOVERY);
            mLogger.d(Utils_String.concatStrings("Found device with name: ", device.getName()));
        }
        else
        {
            // Even though we don't post the rediscovered event anymore, we have to let this information propagate
            device.onRediscovered(device_native, mScanInfo, rssi, BleDeviceOrigin.FROM_DISCOVERY);
            device.setNameFromScanRecord(name_record);
            device.setNameFromNative(native_name);
        }

        if (cycle != null)
            postDeviceDiscovery(device, cycle);
    }

    private void postDeviceDiscovery(BleDevice device, DiscoveryListener.LifeCycle lifecycle)
    {
        if (mDiscoveryListener != null)
        {
            final DiscoveryListener.DiscoveryEvent event = P_EventFactory.newDiscoveryEvent(device, lifecycle);
            mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    mDiscoveryListener.onEvent(event);
                }
            });
        }
    }

    /**
     * Used for testing in the instrumentation tests
     * @return the current update interval
     */
    long getUpdateSpeed()
    {
        return mUpdateInterval;
    }

    void wake()
    {
        mUpdateInterval =  mConfig.updateThreadSpeed.getMilliseconds();

        mPostManager.removeUpdateCallbacks(mUpdateRunnable);

        mPostManager.postToUpdateThread(mUpdateRunnable);
    }

    void setManagerReady()
    {
        mStateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCAN_READY, true);
    }

    private void startScan_private(Interval scanTime, Interval pauseTime)
    {
        if (!isAny(SCANNING, SCAN_PAUSED, STARTING_SCAN))
        {
            mStateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, STARTING_SCAN, true);

            mTaskManager.add(new P_Task_Scan(mTaskManager, null, scanTime, pauseTime));
        }
    }

    private void initConfigDependantMembers()
    {

        mConfig.initMaps();

        if (mUpdateRunnable != null)
        {
            mPostManager.removeUpdateCallbacks(mUpdateRunnable);
        }
        else
        {
            mUpdateRunnable = new UpdateRunnable();
        }

        if( mWakeLockManager == null )
        {
            mWakeLockManager = new P_WakeLockManager(this, mConfig.manageCpuWakeLock);
        }
        else if( mWakeLockManager != null && mConfig.manageCpuWakeLock == false )
        {
            mWakeLockManager.clear();
            mWakeLockManager = new P_WakeLockManager(this, mConfig.manageCpuWakeLock);
        }

        initPostManager();

        if(mConfig.bluetoothEnablerController != null && mConfig.bluetoothEnablerController instanceof DefaultBluetoothEnablerController)
        {
            ((DefaultBluetoothEnablerController) mConfig.bluetoothEnablerController).initStrings(mContext);
        }

        mUpdateInterval = mConfig.updateThreadSpeed.getMilliseconds();
        mPostManager.postToUpdateThreadDelayed(mUpdateRunnable, mUpdateInterval);
        mLogger = new P_Logger(mPostManager, mConfig.debugThreadNames, mConfig.uuidNameMaps, mConfig.logger, mConfig.loggingEnabled);
        mScanManager = new P_ScanManager(this);
        initLifeCycleCallbacks();
    }

    private void initLifeCycleCallbacks()
    {
        if (mContext instanceof Application)
        {
            if (mConfig.autoPauseResumeDetection)
            {
                mLifecycleListener = new LifecycleListener();
                ((Application) mContext).registerActivityLifecycleCallbacks(mLifecycleListener);
            }
            else
            {
                if (mLifecycleListener != null)
                {
                    ((Application) mContext).unregisterActivityLifecycleCallbacks(mLifecycleListener);
                }
            }
        }
    }

    private void initPostManager()
    {
        Handler mUpdateHandler;
        Handler mUIHandler;
        if (mConfig.runOnUIThread)
        {
            mUpdateHandler = new Handler(Looper.getMainLooper());
            mUIHandler = mUpdateHandler;
        }
        else
        {
            mUIHandler = new Handler(Looper.getMainLooper());
            if (mConfig.updateLooper == null)
            {
                mThread = new SweetBlueHandlerThread();
                mThread.start();
                mUpdateHandler = mThread.prepareHandler();
            }
            else
            {
                mUpdateHandler = new Handler(mConfig.updateLooper);
            }
        }
        mPostManager = new P_PostManager(this, mUIHandler, mUpdateHandler);
    }




    private class LifecycleListener implements Application.ActivityLifecycleCallbacks
    {

        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState)
        {
        }

        @Override public void onActivityStarted(Activity activity)
        {
        }

        @Override public void onActivityResumed(Activity activity)
        {
            onResume();
        }

        @Override public void onActivityPaused(Activity activity)
        {
            onPause();
        }

        @Override public void onActivityStopped(Activity activity)
        {
        }

        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState)
        {
        }

        @Override public void onActivityDestroyed(Activity activity)
        {
        }
    }

    private class UpdateRunnable implements Runnable
    {

        @Override public void run()
        {
            update(System.currentTimeMillis());
            if (mPostManager != null && mConfig != null)
            {
                mPostManager.postToUpdateThreadDelayed(this, mUpdateInterval);
            }
        }
    }

    private static class SweetBlueHandlerThread extends HandlerThread
    {

        private Handler mHandler;

        public SweetBlueHandlerThread()
        {
            super("SweetBlue");
        }

        public Handler prepareHandler()
        {
            mHandler = new Handler(getLooper()){
                @Override public void handleMessage(Message msg)
                {
                    super.handleMessage(msg);
                }
            };
            return mHandler;
        }
    }
}
