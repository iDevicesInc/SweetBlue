package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.DiscoveryListener;
import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


class P_DeviceManager
{

    private final HashMap<String, BleDevice> mMap = new HashMap<String, BleDevice>();
    private final HashSet<String> mUndiscoveredSet = new HashSet<>();
    private final ArrayList<BleDevice> mList = new ArrayList<BleDevice>();
    private final Set<String> mConnectedList = new HashSet<>(0);
    private final BleManager mManager;
    private final P_PersistanceManager mPersistanceManager;

    private boolean mUpdating = false;


    P_DeviceManager(BleManager mgr)
    {
        mManager = mgr;
        mPersistanceManager = new P_PersistanceManager(mManager);
        mConnectedList.addAll(mPersistanceManager.getPreviouslyConnectedDevices());
    }

    public ArrayList<BleDevice> getList()
    {
        return getList_private(false);
    }

    public ArrayList<BleDevice> getList_sorted()
    {
        return getList_private(true);
    }

    private ArrayList<BleDevice> getList_private(boolean sort)
    {
        if (sort && mManager.mConfig.defaultDeviceSorter != null)
        {
            Collections.sort(mList, mManager.mConfig.defaultDeviceSorter);
        }
        return mList;
    }

    public BleDevice getDevice(final int mask_BleDeviceState)
    {
        for (int i = 0; i < getCount(); i++)
        {
            BleDevice device = get(i);

            if (device.isAny(mask_BleDeviceState))
            {
                return device;
            }
        }

        return BleDevice.NULL;
    }

    public int getCount()
    {
        return mList.size();
    }

    public BleDevice get(String macAddress)
    {
        return mMap.get(macAddress);
    }

    public boolean getWasUndiscovered(String macAddres)
    {
        return mUndiscoveredSet.contains(macAddres);
    }

    void deviceConnected(BleDevice device)
    {
        if (!mConnectedList.contains(device.getMacAddress()))
        {
            mConnectedList.add(device.getMacAddress());
            mPersistanceManager.storePreviouslyConnectedDevices(mConnectedList);
        }
    }

    void add(BleDevice device)
    {
        if (mMap.containsKey(device.getMacAddress()))
        {
            mManager.getLogger().e(Utils_String.concatStrings("Already registered device ", device.getMacAddress()));

            return;
        }

        mList.add(device);
        mMap.put(device.getMacAddress(), device);
    }

    void remove(BleDevice device, P_DeviceManager cache)
    {
        if (mUpdating)
        {
            mManager.getLogger().e("Removing device while updating!");
        }
        if (!mMap.containsKey(device.getMacAddress()))
        {
            mManager.getLogger().i(Utils_String.concatStrings("Device map does not contain a device with mac address ", device.getMacAddress()));
            return;
        }

        mList.remove(device);
        mMap.remove(device.getMacAddress());
        mUndiscoveredSet.add(device.getMacAddress());

        final boolean cacheDevice = device.getConfig().cacheDeviceOnUndiscovery;

        if (cacheDevice && cache != null)
        {
            cache.add(device);
        }
    }

    void update(long curTimeMs)
    {
        if (mUpdating)
        {
            // Already updating
            return;
        }

        mUpdating = true;


        for (int i = mList.size() - 1; i >= 0; i--)
        {
            if (!checkForStaleDevice(mList.get(i), curTimeMs))
            {
                mList.get(i).update(curTimeMs);
            }
        }

        mUpdating = false;
    }

    boolean checkForStaleDevice(BleDevice device, long curTime)
    {
        P_Task_Scan scanTask = mManager.mTaskManager.getCurrent(P_Task_Scan.class, mManager);

        if (scanTask != null)
        {
            if (Interval.isEnabled(device.getConfig().minScanTimeToUndiscover) && Interval.isEnabled(device.getConfig().timeToUndiscover))
            {
                if (scanTask.timeScanning() >= device.getConfig().minScanTimeToUndiscover.millis() && curTime - device.lastDiscovery() >= device.getConfig().timeToUndiscover.millis())
                {
                    final boolean purgeable = device.getOrigin() != BleDeviceOrigin.EXPLICIT && ((device.getStateMask() & ~BleDeviceState.PURGEABLE_MASK) == 0x0);
                    if (purgeable)
                    {
                        device.onUndiscovered(P_StateTracker.E_Intent.UNINTENTIONAL);
                        remove(device, null);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void clearConnectedDevice(String macAddress)
    {
        if (mConnectedList.remove(macAddress))
        {
            mPersistanceManager.storePreviouslyConnectedDevices(mConnectedList);
        }
    }

    void clearAllConnectedDevices()
    {
        if (mConnectedList.size() > 0)
        {
            mConnectedList.clear();
            mPersistanceManager.storePreviouslyConnectedDevices(mConnectedList);
        }
    }

    Set<String> previouslyConnectedDevices()
    {
        return mConnectedList;
    }

    public BleDevice get(int index)
    {
        return mList.get(index);
    }

    boolean hasDevice(String macAddress)
    {
        for (int i = mList.size() - 1; i >= 0; i--)
        {
            if (mList.get(i).getMacAddress().equals(macAddress))
            {
                return true;
            }
        }
        return false;
    }

    boolean hasDeviceInState(BleDeviceState... states)
    {
        if (states == null || states.length == 0)
        {
            return mList.size() > 0;
        }
        for (int i = mList.size() - 1; i >= 0; i--)
        {
            BleDevice device = get(i);

            if (device.isAny(states))
            {
                return true;
            }
        }
        return false;
    }

}
