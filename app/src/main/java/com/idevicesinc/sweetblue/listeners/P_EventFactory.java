package com.idevicesinc.sweetblue.listeners;


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.P_DeviceStateTracker;
import com.idevicesinc.sweetblue.utils.State;

import java.util.UUID;

public class P_EventFactory
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

}
