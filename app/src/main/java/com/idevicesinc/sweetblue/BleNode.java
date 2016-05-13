package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import java.util.UUID;


public abstract class BleNode implements UsesCustomNull
{

    private final BleManager mManager;
    private final P_ServiceManager mServiceManager;


    public BleNode(BleManager mgr)
    {
        mManager = mgr;
        mServiceManager = newServiceManager();
    }

    public abstract String getMacAddress();

    public abstract <T extends BleNodeConfig> T getConfig();

    abstract P_ServiceManager newServiceManager();

    <T extends P_ServiceManager> T getServiceManager()
    {
        return (T) mServiceManager;
    }

    public BleManager getManager()
    {
        if (isNull())
        {
            return BleManager.sInstance;
        }
        else
        {
            return mManager;
        }
    }

    public BluetoothGattCharacteristic getNativeCharacteristic(UUID serviceUuid, UUID charUuid)
    {
        return mServiceManager.getCharacteristic(serviceUuid, charUuid);
    }

    public BluetoothGattDescriptor getNativeDescriptor(UUID serviceUuid, UUID charUuid, UUID descUuid)
    {
        return mServiceManager.getDescriptor(serviceUuid, charUuid, descUuid);
    }

    /**
     * Returns the native service for the given UUID in case you need lower-level access.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattService getNativeService(final UUID serviceUuid)
    {
        return mServiceManager.getServiceDirectlyFromNativeNode(serviceUuid);
    }

    /**
     * Overload of {@link #getNativeDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
     * matching the given {@link UUID}.
     */
    public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor getNativeDescriptor(final UUID descUuid)
    {
        return getNativeDescriptor(null, null, descUuid);
    }

    /**
     * Overload of {@link #getNativeDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
     * inside the given characteristic matching the given {@link UUID}.
     */
    public @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattDescriptor getNativeDescriptor_inChar(final UUID charUuid, final UUID descUuid)
    {
        return getNativeDescriptor(null, charUuid, descUuid);
    }
}
