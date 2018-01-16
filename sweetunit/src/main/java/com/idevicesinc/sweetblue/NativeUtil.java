package com.idevicesinc.sweetblue;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.L_UtilBridge;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;

import java.util.UUID;

/**
 * Utility class for simulating Bluetooth operations (read/writes, notifications, etc). When unit testing, you will need to use this class
 * to simulate bluetooth operations. {@link UnitTestGatt}, {@link UnitTestDevice}, and {@link UnitTestManagerLayer} use this class. If you are implementing your own
 * version of those classes, you will need to use this class to simulate the native callbacks.
 * <p>
 * This is not in the utils package as it accesses several package private methods.
 */
public final class NativeUtil
{

    private NativeUtil()
    {
    }


    /**
     * Sends a broadcast for a bluetooth state change, such as {@link BluetoothAdapter#STATE_ON}, {@link BluetoothAdapter#STATE_OFF}, etc.
     *
     * @deprecated This is deprecated in favor of {@link #sendBluetoothStateChange(BleManager, int, int)}, as some build servers have issues with sending
     * broadcasts.
     */
    @Deprecated
    public static void sendBluetoothStateBroadcast(Context context, int previousState, int newState)
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, previousState);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, newState);
        context.sendBroadcast(intent);
    }

    /**
     * Sends a bluetooth state change, such as {@link BluetoothAdapter#STATE_ON}, {@link BluetoothAdapter#STATE_OFF}, etc.
     */
    public static void sendBluetoothStateChange(BleManager manager, int previousState, int newState)
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, previousState);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, newState);
        manager.m_listeners.onNativeBleStateChangeFromBroadcastReceiver(null, intent);
    }

    /**
     * Overload of {@link #bondSuccess(BleDevice, Interval)} which delays the callback by 50ms.
     */
    public static void bondSuccess(BleDevice device)
    {
        bondSuccess(device, Interval.millis(50));
    }

    /**
     * Send the callback that a bond was successful, and delays the callback by the amount of time specified
     */
    public static void bondSuccess(final BleDevice device, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                int oldState;
                if (device.is(BleDeviceState.UNBONDED))
                {
                    oldState = BluetoothDevice.BOND_NONE;
                }
                else if (device.is(BleDeviceState.BONDING))
                {
                    oldState = BluetoothDevice.BOND_BONDING;
                }
                else
                {
                    oldState = BluetoothDevice.BOND_BONDED;
                }
                device.getManager().m_listeners.onNativeBondStateChanged(device.getManager().m_config.newDeviceLayer(device), oldState, BluetoothDevice.BOND_BONDED, 0);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #bondFail(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void bondFail(BleDevice device, int failReason)
    {
        bondFail(device, failReason, Interval.millis(50));
    }

    /**
     * Send a callback that a bond has failed with the provided reason..something like {@link BleStatuses#UNBOND_REASON_AUTH_FAILED}, or {@link BleStatuses#UNBOND_REASON_REMOTE_DEVICE_DOWN}, and
     * delays the callback by the amount specified.
     */
    public static void bondFail(final BleDevice device, final int failReason, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                int oldState;
                if (device.is(BleDeviceState.UNBONDED))
                {
                    oldState = BluetoothDevice.BOND_NONE;
                }
                else if (device.is(BleDeviceState.BONDING))
                {
                    oldState = BluetoothDevice.BOND_BONDING;
                }
                else
                {
                    oldState = BluetoothDevice.BOND_BONDED;
                }
                device.getManager().m_listeners.onNativeBondStateChanged(device.getManager().m_config.newDeviceLayer(device), oldState, BluetoothDevice.BOND_NONE, failReason);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #readError(BleDevice, BluetoothGattCharacteristic, int, Interval)} which delays the callback by 50ms.
     */
    public static void readError(BleDevice device, BluetoothGattCharacteristic characteristic, int gattStatus)
    {
        readError(device, characteristic, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that a read has failed, with the gattStatus provided, for instance {@link BleStatuses#GATT_ERROR}, which delays the callback by the amount specified.
     */
    public static void readError(final BleDevice device, final BluetoothGattCharacteristic characteristic, final int gattStatus, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onCharacteristicRead(null, characteristic, gattStatus);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #readSuccess(BleDevice, BluetoothGattCharacteristic, byte[], Interval)} which delays the callback by 50ms.
     */
    public static void readSuccess(final BleDevice device, final BluetoothGattCharacteristic characteristic, final byte[] data)
    {
        readSuccess(device, characteristic, data, Interval.millis(50));
    }

    /**
     * Send a callback that a read was successful, with the data to send back from the read, and delays the callback by the amount specified.
     */
    public static void readSuccess(final BleDevice device, final BluetoothGattCharacteristic characteristic, final byte[] data, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                characteristic.setValue(data);
                device.m_listeners.onCharacteristicRead(null, characteristic, BleStatuses.GATT_SUCCESS);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #readDescSuccess(BleDevice, BluetoothGattDescriptor, byte[], Interval)} which delays the callback by 50ms.
     */
    public static void readDescSuccess(final BleDevice device, final BluetoothGattDescriptor descriptor, final byte[] data)
    {
        readDescSuccess(device, descriptor, data, Interval.millis(50));
    }

    /**
     * Send a callback that a descriptor read was successful, with the data to return, and delays the callback by the amount specified.
     */
    public static void readDescSuccess(final BleDevice device, final BluetoothGattDescriptor descriptor, final byte[] data, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                descriptor.setValue(data);
                device.m_listeners.onDescriptorRead(null, descriptor, BleStatuses.GATT_SUCCESS);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #readDescError(BleDevice, BluetoothGattDescriptor, int, long)} which delays the callback by 50ms.
     */
    public static void readDescError(final BleDevice device, final BluetoothGattDescriptor descriptor, final int gattStatus)
    {
        readDescError(device, descriptor, gattStatus, 50);
    }

    /**
     * Send a callback that a descriptor read failed with the given gattStatus, and delays the callback by the amount specified.
     */
    public static void readDescError(final BleDevice device, final BluetoothGattDescriptor descriptor, final int gattStatus, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onDescriptorRead(null, descriptor, gattStatus);
            }
        }, delay);
    }

    /**
     * Overload of {@link #writeDescSuccess(BleDevice, BluetoothGattDescriptor, Interval)} which delays the callback by 50ms.
     */
    public static void writeDescSuccess(final BleDevice device, final BluetoothGattDescriptor descriptor)
    {
        writeDescSuccess(device, descriptor, Interval.millis(50));
    }

    /**
     * Send a callback that a descriptor write suceeded, and delay the callback by the amount specified.
     */
    public static void writeDescSuccess(final BleDevice device, final BluetoothGattDescriptor descriptor, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onDescriptorWrite(null, descriptor, BleStatuses.GATT_SUCCESS);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #writeDescError(BleDevice, BluetoothGattDescriptor, int, Interval)} which delays the callback by 50ms.
     */
    public static void writeDescError(final BleDevice device, final BluetoothGattDescriptor descriptor, final int gattStatus)
    {
        writeDescError(device, descriptor, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that a descriptor write failed, with the given gattStatus, and delay the callback by the amount specified.
     */
    public static void writeDescError(final BleDevice device, final BluetoothGattDescriptor descriptor, final int gattStatus, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onDescriptorWrite(null, descriptor, gattStatus);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #writeSuccess(BleDevice, BluetoothGattCharacteristic, Interval)} which delays the callback by 50ms.
     */
    public static void writeSuccess(final BleDevice device, final BluetoothGattCharacteristic characteristic)
    {
        writeSuccess(device, characteristic, Interval.millis(50));
    }

    /**
     * Send a callback that a write succeeded, and delay the callback by the amount specified.
     */
    public static void writeSuccess(final BleDevice device, final BluetoothGattCharacteristic characteristic, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onCharacteristicWrite(null, characteristic, BleStatuses.GATT_SUCCESS);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #writeError(BleDevice, BluetoothGattCharacteristic, int, Interval)} which delays the callback by 50ms.
     */
    public static void writeError(final BleDevice device, final BluetoothGattCharacteristic characteristic, final int gattStatus)
    {
        writeError(device, characteristic, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that a write failed, with the given gattStatus, and delay the callback by the amount specified.
     */
    public static void writeError(final BleDevice device, final BluetoothGattCharacteristic characteristic, final int gattStatus, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onCharacteristicWrite(null, characteristic, gattStatus);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #requestMTUSuccess(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void requestMTUSuccess(final BleDevice device, final int mtu)
    {
        requestMTUSuccess(device, mtu, Interval.millis(50));
    }

    /**
     * Send a callback that says an MTU request was successful, with the newly negotiated mtu size, and delay the callback by the amount specified.
     */
    public static void requestMTUSuccess(final BleDevice device, final int mtu, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onMtuChanged(null, mtu, BleStatuses.GATT_SUCCESS);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #requestMTUError(BleDevice, int, int, Interval)} which delays the callback by 50ms.
     */
    public static void requestMTUError(final BleDevice device, final int mtu, final int gattStatus)
    {
        requestMTUError(device, mtu, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that says an MTU request failed, with the given gattStatus, and delay the callback by the amount specified.
     */
    public static void requestMTUError(final BleDevice device, final int mtu, final int gattStatus, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onMtuChanged(null, mtu, gattStatus);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #remoteRssiSuccess(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void remoteRssiSuccess(final BleDevice device, final int rssi)
    {
        remoteRssiSuccess(device, rssi, Interval.millis(50));
    }

    /**
     * Send a callback that a read remote rssi succeeded with the given rssi value, and delay the callback by the amount specified.
     */
    public static void remoteRssiSuccess(final BleDevice device, final int rssi, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onReadRemoteRssi(null, rssi, BleStatuses.GATT_SUCCESS);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #remoteRssiError(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void remoteRssiError(final BleDevice device, final int gattStatus)
    {
        remoteRssiError(device, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that a remote rssi read has failed with the given gattStatus, and delay the callback by the amount specified.
     */
    public static void remoteRssiError(final BleDevice device, final int gattStatus, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onReadRemoteRssi(null, device.getRssi(), gattStatus);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #sendNotification(BleDevice, BluetoothGattCharacteristic, byte[], Interval)} which delays the callback by 50ms.
     */
    public static void sendNotification(final BleDevice device, final BluetoothGattCharacteristic characteristic, final byte[] data)
    {
        sendNotification(device, characteristic, data, Interval.millis(50));
    }

    /**
     * Simulate a notification being received with the given data, and delay the callback by the amount specified.
     */
    public static void sendNotification(final BleDevice device, final BluetoothGattCharacteristic characteristic, final byte[] data, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                characteristic.setValue(data);
                device.m_listeners.onCharacteristicChanged(null, characteristic);
            }
        }, delay.millis());
    }

    public static void failDiscoverServices(BleDevice device, int gattStatus)
    {
        device.m_listeners.onServicesDiscovered(null, gattStatus);
    }

    public static void failDiscoverServices(final BleDevice device, final int gattStatus, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                device.m_listeners.onServicesDiscovered(null, gattStatus);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #setToConnecting(BleDevice, int)} which sets the gattStatus to {@link BleStatuses#GATT_SUCCESS}.
     */
    public static void setToConnecting(final BleDevice device)
    {
        setToConnecting(device, BleStatuses.GATT_SUCCESS);
    }

    /**
     * Overload of {@link #setToConnecting(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void setToConnecting(final BleDevice device, int gattStatus)
    {
        setToConnecting(device, gattStatus, Interval.millis(50));
    }

    /**
     * Overload of {@link #setToConnecting(BleDevice, int, boolean, Interval)} which updates the internal state as well.
     */
    public static void setToConnecting(final BleDevice device, int gattStatus, Interval delay)
    {
        setToConnecting(device, gattStatus, true, delay);
    }

    /**
     * Send a callback to set a device's state to {@link BluetoothGatt#STATE_CONNECTING}, with the given gattStatus, whether or not to update the internal state, and delay
     * the callback by the amount specified.
     */
    public static void setToConnecting(final BleDevice device, final int gattStatus, final boolean updateDeviceState, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (updateDeviceState)
                {
                    ((UnitTestManagerLayer) device.layerManager().getManagerLayer()).updateDeviceState(device, BluetoothGatt.STATE_CONNECTING);
                }
                device.m_listeners.onConnectionStateChange(null, gattStatus, BluetoothGatt.STATE_CONNECTING);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #setToConnected(BleDevice, int)} which sets the gattStatus to {@link BleStatuses#GATT_SUCCESS}.
     */
    public static void setToConnected(final BleDevice device)
    {
        setToConnected(device, BleStatuses.GATT_SUCCESS);
    }

    /**
     * Overload of {@link #setToConnected(BleDevice, int, Interval)}, which delays the callback by 50ms.
     */
    public static void setToConnected(final BleDevice device, int gattStatus)
    {
        setToConnected(device, gattStatus, Interval.millis(50));
    }

    /**
     * Overload of {@link #setToConnected(BleDevice, int, boolean, Interval)} which updates the internal state as well.
     */
    public static void setToConnected(final BleDevice device, int gattStatus, Interval delay)
    {
        setToConnected(device, gattStatus, true, delay);
    }

    /**
     * Send a callback to set a device's state to {@link BluetoothGatt#STATE_CONNECTED}, with the given gattStatus, whether or not to update the internal state, and delay
     * the callback by the amount specified.
     */
    public static void setToConnected(final BleDevice device, final int gattStatus, final boolean updateDeviceState, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (updateDeviceState)
                {
                    ((UnitTestManagerLayer) device.layerManager().getManagerLayer()).updateDeviceState(device, BluetoothGatt.STATE_CONNECTED);
                }
                device.m_listeners.onConnectionStateChange(null, gattStatus, BluetoothGatt.STATE_CONNECTED);
            }
        }, delay.millis());
    }

    /**
     * Overload of {@link #setToDisconnected(BleDevice, int)} with sets the gattStatus to {@link BleStatuses#GATT_SUCCESS}.
     */
    public static void setToDisconnected(BleDevice device)
    {
        setToDisconnected(device, BleStatuses.GATT_SUCCESS);
    }

    /**
     * Overload of {@link #setToDisconnected(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void setToDisconnected(BleDevice device, int gattStatus)
    {
        setToDisconnected(device, gattStatus, Interval.millis(50));
    }

    /**
     * Overload of {@link #setToDisconnected(BleDevice, int, boolean, Interval)} which sets the device's internal state.
     */
    public static void setToDisconnected(BleDevice device, int gattStatus, Interval delay)
    {
        setToDisconnected(device, gattStatus, true, delay);
    }

    /**
     * Send a callback to set a device's state to {@link BluetoothGatt#STATE_DISCONNECTED}, with the given gattStatus, whether or not to update the internal state, and delay
     * the callback by the amount specified.
     */
    public static void setToDisconnected(final BleDevice device, final int gattStatus, final boolean updateDeviceState, Interval delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (updateDeviceState)
                {
                    ((UnitTestManagerLayer) device.layerManager().getManagerLayer()).updateDeviceState(device, BluetoothGatt.STATE_DISCONNECTED);
                }
                device.m_listeners.onConnectionStateChange(null, gattStatus, BluetoothGatt.STATE_DISCONNECTED);
            }
        }, delay.millis());
    }

    public static void advertiseNewDevice(final BleManager mgr, final int rssi, final byte[] scanRecord, Interval delay)
    {
        mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (mgr.is(BleManagerState.SCANNING))
                {
                    mgr.getScanManager().addScanResult(null, rssi, scanRecord);
                }
                else
                {
                    mgr.getLogger().e("Tried to advertise a device when not scanning!");
                }
            }
        }, delay.millis());
    }

    public static void advertiseNewDevice(final BleManager mgr, final int rssi, final byte[] scanRecord, String macAddress, Interval delay)
    {
        mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (mgr.is(BleManagerState.SCANNING))
                {
                    mgr.getScanManager().addScanResult(null, rssi, scanRecord);
                }
                else
                {
                    mgr.getLogger().e("Tried to advertise a device when not scanning!");
                }
            }
        }, delay.millis());
    }

    /**
     * Simulate a device that is advertising, so SweetBlue picks up on it (as long as scanning is occurring at the time you call this method).
     * Use one of the methods {@link Utils_ScanRecord#newScanRecord(String)}, {@link Utils_ScanRecord#newScanRecord(String, UUID)}, etc to get the byte[] of the scan record easily.
     * This will generate a random mac address for the device.
     *
     * @see #advertiseDevice(BleManager, int, byte[], String)
     */
    public static void advertiseNewDevice(BleManager mgr, int rssi, byte[] scanRecord)
    {
        advertiseNewDevice(mgr, rssi, scanRecord, Interval.ZERO);
    }

    /**
     * Overload of {@link #advertiseNewDevice(BleManager, int, byte[])}, which creates the byte[] scanRecord from the name you provide.
     */
    public static void advertiseNewDevice(BleManager mgr, int rssi, String deviceName)
    {
        advertiseNewDevice(mgr, rssi, Utils_ScanRecord.newScanRecord(deviceName));
    }

    public static void setToAdvertising(BleManager mgr, AdvertiseSettings settings, L_Util.AdvertisingCallback callback)
    {
        setToAdvertising(mgr, settings, callback, Interval.millis(50));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setToAdvertising(BleManager mgr, final AdvertiseSettings settings, L_Util.AdvertisingCallback callback, Interval delay)
    {
        if (Utils.isLollipop())
        {
            L_UtilBridge.setAdvListener(callback);
            mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    L_Util.getNativeAdvertisingCallback().onStartSuccess(settings);
                }
            }, delay.millis());
        }
    }

}
