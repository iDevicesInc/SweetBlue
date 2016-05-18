package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


class P_DeviceManager
{

    private final HashMap<String, BleDevice> mMap = new HashMap<String, BleDevice>();
    private final ArrayList<BleDevice> mList = new ArrayList<BleDevice>();
    private final Set<String> mConnectedList = new HashSet<>(0);
    private final BleManager mManager;

    private boolean mUpdating = false;


    P_DeviceManager(BleManager mgr)
    {
        mManager = mgr;
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
            mList.get(i).update(curTimeMs);
        }

        mUpdating = false;
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
