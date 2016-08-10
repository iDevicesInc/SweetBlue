package com.idevicesinc.sweetblue.listeners;


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.P_DeviceStateTracker;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;

import java.util.ArrayList;
import java.util.UUID;

public final class P_EventFactory
{

    public static NativeStateListener.NativeStateEvent newNativeStateEvent(BleManager mgr, int oldStateBits, int newStateBits, int status)
    {
        return new NativeStateListener.NativeStateEvent(mgr, oldStateBits, newStateBits, status);
    }

    public static ManagerStateListener.StateEvent newManagerStateEvent(BleManager mgr, int oldStateBits, int newStateBits, int status)
    {
        return new ManagerStateListener.StateEvent(mgr, oldStateBits, newStateBits, status);
    }

    public static DeviceStateListener.StateEvent newDeviceStateEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
    {
        return new DeviceStateListener.StateEvent(device, oldStateBits, newStateBits, intentMask, gattStatus);
    }

    public static ReadWriteListener.ReadWriteEvent newReadWriteEvent(BleDevice device, UUID serviceUuid, UUID charUuid, UUID descUuid, ReadWriteListener.Type type, ReadWriteListener.Target target, byte[] data, ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
    {
        return new ReadWriteListener.ReadWriteEvent(device, serviceUuid, charUuid, descUuid, type, target, data, status, gattStatus, totalTime, transitTime, solicited);
    }

    public static ReadWriteListener.ReadWriteEvent newReadWriteEvent(BleDevice device, ReadWriteListener.Type type, int rssi, ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
    {
        return new ReadWriteListener.ReadWriteEvent(device, type, rssi, status, gattStatus, totalTime, transitTime, solicited);
    }

    public static DiscoveryListener.DiscoveryEvent newDiscoveryEvent(BleDevice device, DiscoveryListener.LifeCycle lifeCycle)
    {
        return new DiscoveryListener.DiscoveryEvent(device, lifeCycle);
    }

    public static BondListener.BondEvent newBondEvent(BleDevice device, BondListener.Status status, int failReason, State.ChangeIntent intent)
    {
        return new BondListener.BondEvent(device, status, failReason, intent);
    }

    public static NotifyListener.NotifyEvent newNotifyEvent(BleDevice device, UUID serviceUuid, UUID charUuid, byte[] data, NotifyListener.Type type, NotifyListener.Status status)
    {
        return new NotifyListener.NotifyEvent(device, serviceUuid, charUuid, data, status, type);
    }

    public static DeviceConnectionFailListener.ConnectionFailEvent newConnectionFailEvent(BleDevice device, DeviceConnectionFailListener.Status reason, DeviceConnectionFailListener.Timing timing,
                                                                                          int failSofar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, BleDeviceState highestStateReached,
                                                                                          BleDeviceState highestStateReached_total, P_BaseConnectionFailListener.AutoConnectUsage autoConnectUsage, int bondFailReason,
                                                                                          ReadWriteListener.ReadWriteEvent txnFailReason, ArrayList<DeviceConnectionFailListener.ConnectionFailEvent> history)
    {
        return new DeviceConnectionFailListener.ConnectionFailEvent(device, reason, timing, failSofar, latestAttemptTime, totalAttemptTime, gattStatus, highestStateReached, highestStateReached_total,
                autoConnectUsage, bondFailReason, txnFailReason, history);
    }

}
