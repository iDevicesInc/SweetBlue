package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener;
import com.idevicesinc.sweetblue.utils.BleStatuses;

import java.lang.reflect.Method;

final class P_Task_DiscoverServices extends P_Task_RequiresConnection
{

    public P_Task_DiscoverServices(BleDevice device, IStateListener listener)
    {
        super(device, listener);
    }

    @Override public final P_TaskPriority getPriority()
    {
        return P_TaskPriority.MEDIUM;
    }

    @Override public final void execute()
    {
        final boolean useGattRefresh = getDevice().getConfig() == null ? getManager().mConfig.useGattRefresh : getDevice().getConfig().useGattRefresh;

        if (useGattRefresh)
        {
            refresh();
        }

        getDevice().mGattManager.discoverServices(this);

    }

    @Override final void onFail(boolean immediate)
    {
        super.onFail(immediate);
        getDevice().mGattManager.onConnectionFail(DeviceConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, DeviceConnectionFailListener.Timing.EVENTUALLY,
                BleStatuses.GATT_STATUS_DISCOVER_SERVICES_FAILED);
    }

    @Override final void onTaskTimedOut()
    {
        super.onTaskTimedOut();
        getDevice().mGattManager.onConnectionFail(DeviceConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, DeviceConnectionFailListener.Timing.EVENTUALLY,
                BleStatuses.GATT_STATUS_DISCOVER_SERVICES_FAILED);
    }

    final void discoverServicesImmediatelyFailed()
    {
        failImmediately();
        getDevice().mGattManager.onConnectionFail(DeviceConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, DeviceConnectionFailListener.Timing.IMMEDIATELY,
                BleStatuses.GATT_STATUS_DISCOVER_SERVICES_FAILED);
        // TODO - Throw error to errorlistener
    }

    private void refresh()
    {
        try
        {
            Method mRefreshMethod = getDevice().getNativeGatt().getClass().getMethod("refresh", (Class[]) null);
            mRefreshMethod.invoke(getDevice().getNativeGatt(), (Object[]) null);
        }
        catch (Exception e)
        {
        }
    }
}
