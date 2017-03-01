package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.utils.ByteBuffer;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.Random;
import java.util.UUID;

/**
 * Utility class for simulating Bluetooth operations (read/writes, notifications, etc). When unit testing, you will need to use this class
 * to simulate bluetooth operations. For instance, in your test, you try to read a characteristic, then you'll have to call {@link #readSuccess(BleDevice, BluetoothGattCharacteristic, byte[])},
 * or {@link #readSuccess(BleDevice, BluetoothGattCharacteristic, byte[], long)} to simulate getting the data back from the "connected" device.
 */
public final class UnitTestUtils
{

    private UnitTestUtils() {}


    private static final byte DATA_TYPE_FLAGS = 0x01;
    private static final byte DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    private static final byte DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    private static final byte DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    private static final byte DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    private static final byte DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    private static final byte DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    private static final byte DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    private static final byte DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    private static final byte DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    private static final byte DATA_TYPE_SERVICE_DATA = 0x16;
    private static final byte DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = (byte) 0xFF;


    /**
     * Returns a random mac address
     */
    public static String randomMacAddress()
    {
        byte[] add = new byte[6];
        new Random().nextBytes(add);
        return Utils_String.bytesToMacAddress(add);
    }

    public static void readError(BleDevice device, BluetoothGattCharacteristic characteristic, int gattStatus)
    {
        readError(device, characteristic, gattStatus, 50);
    }

    public static void readError(final BleDevice device, final BluetoothGattCharacteristic characteristic, final int gattStatus, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onCharacteristicRead(null, characteristic, gattStatus);
            }
        }, delay);
    }

    public static void readSuccess(final BleDevice device, final BluetoothGattCharacteristic characteristic, final byte[] data)
    {
        readSuccess(device, characteristic, data, 50);
    }

    public static void readSuccess(final BleDevice device, final BluetoothGattCharacteristic characteristic, final byte[] data, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                characteristic.setValue(data);
                device.m_listeners.onCharacteristicRead(null, characteristic, BleStatuses.GATT_SUCCESS);
            }
        }, delay);
    }

    public static void readDescSuccess(final BleDevice device, final BluetoothGattDescriptor descriptor, final byte[] data)
    {
        readDescSuccess(device, descriptor, data, 50);
    }

    public static void readDescSuccess(final BleDevice device, final BluetoothGattDescriptor descriptor, final byte[] data, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                descriptor.setValue(data);
                device.m_listeners.onDescriptorRead(null, descriptor, BleStatuses.GATT_SUCCESS);
            }
        }, delay);
    }

    public static void readDescError(final BleDevice device, final BluetoothGattDescriptor descriptor, final int gattStatus)
    {
        readDescError(device, descriptor, gattStatus, 50);
    }

    public static void readDescError(final BleDevice device, final BluetoothGattDescriptor descriptor, final int gattStatus, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onDescriptorRead(null, descriptor, gattStatus);
            }
        }, delay);
    }

    public static void writeDescSuccess(final BleDevice device, final BluetoothGattDescriptor descriptor)
    {
        writeDescSuccess(device, descriptor, 50);
    }

    public static void writeDescSuccess(final BleDevice device, final BluetoothGattDescriptor descriptor, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onDescriptorWrite(null, descriptor, BleStatuses.GATT_SUCCESS);
            }
        }, delay);
    }

    public static void writeDescError(final BleDevice device, final BluetoothGattDescriptor descriptor, final int gattStatus)
    {
        writeDescError(device, descriptor, gattStatus, 50);
    }

    public static void writeDescError(final BleDevice device, final BluetoothGattDescriptor descriptor, final int gattStatus, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onDescriptorWrite(null, descriptor, gattStatus);
            }
        }, delay);
    }

    public static void writeSuccess(final BleDevice device, final BluetoothGattCharacteristic characteristic)
    {
        writeSuccess(device, characteristic, 50);
    }

    public static void writeSuccess(final BleDevice device, final BluetoothGattCharacteristic characteristic, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onCharacteristicWrite(null, characteristic, BleStatuses.GATT_SUCCESS);
            }
        }, delay);
    }

    public static void writeError(final BleDevice device, final BluetoothGattCharacteristic characteristic, final int gattStatus)
    {
        writeError(device, characteristic, gattStatus, 50);
    }

    public static void writeError(final BleDevice device, final BluetoothGattCharacteristic characteristic, final int gattStatus, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onCharacteristicWrite(null, characteristic, gattStatus);
            }
        }, delay);
    }

    public static void requestMTUSuccess(final BleDevice device, final int mtu)
    {
        requestMTUSuccess(device, mtu, 50);
    }

    public static void requestMTUSuccess(final BleDevice device, final int mtu, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onMtuChanged(null, mtu, BleStatuses.GATT_SUCCESS);
            }
        }, delay);
    }

    public static void requestMTUError(final BleDevice device, final int mtu, final int gattStatus)
    {
        requestMTUError(device, mtu, gattStatus, 50);
    }

    public static void requestMTUError(final BleDevice device, final int mtu, final int gattStatus, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onMtuChanged(null, mtu, gattStatus);
            }
        }, delay);
    }

    public static void remoteRssiSuccess(final BleDevice device, final int rssi)
    {
        remoteRssiSuccess(device, rssi, 50);
    }

    public static void remoteRssiSuccess(final BleDevice device, final int rssi, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onReadRemoteRssi(null, rssi, BleStatuses.GATT_SUCCESS);
            }
        }, delay);
    }

    public static void remoteRssiError(final BleDevice device, final int gattStatus)
    {
        remoteRssiError(device, gattStatus, 50);
    }

    public static void remoteRssiError(final BleDevice device, final int gattStatus, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                device.m_listeners.onReadRemoteRssi(null, device.getRssi(), gattStatus);
            }
        }, delay);
    }

    public static void sendNotification(final BleDevice device, final BluetoothGattCharacteristic characteristic, final byte[] data)
    {
        sendNotification(device, characteristic, data, 50);
    }

    public static void sendNotification(final BleDevice device, final BluetoothGattCharacteristic characteristic, final byte[] data, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                characteristic.setValue(data);
                device.m_listeners.onCharacteristicChanged(null, characteristic);
            }
        }, delay);
    }

    public static void failDiscoverServices(BleDevice device, int gattStatus)
    {
        device.m_listeners.onServicesDiscovered(null, gattStatus);
    }

    public static void disconnectDevice(BleDevice device, int gattStatus)
    {
        disconnectDevice(device, gattStatus, true, 50);
    }

    public static void disconnectDevice(BleDevice device, int gattStatus, long delay)
    {
        disconnectDevice(device, gattStatus, true, delay);
    }

    public static void disconnectDevice(final BleDevice device, final int gattStatus, final boolean updateDeviceState, long delay)
    {
        device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                if (updateDeviceState)
                {
                    ((UnitTestManagerLayer) device.layerManager().getManagerLayer()).updateDeviceState(device, BluetoothGatt.STATE_DISCONNECTED);
                }
                device.m_listeners.onConnectionStateChange(null, gattStatus, BluetoothGatt.STATE_DISCONNECTED);
            }
        }, delay);
    }

    public static void advertiseNewDevice(BleManager mgr, int rssi, byte[] scanRecord)
    {
        if (mgr.is(BleManagerState.SCANNING))
        {
            mgr.getScanManager().postScanResult(null, rssi, scanRecord);
        }
        else
        {
            mgr.getLogger().e("Tried to advertise a device when not scanning!");
        }
    }

    public static void advertiseNewDevice(BleManager mgr, int rssi, String deviceName)
    {
        advertiseNewDevice(mgr, rssi, newScanRecord(deviceName));
    }

    public static byte[] newScanRecord(String name)
    {
        return newScanRecord(null, null, null, name, null, null, null);
    }

    public static byte[] newScanRecord(String name, UUID serviceUuid, byte[] serviceData)
    {
        return newScanRecord(null, serviceUuid, serviceData, name, null, null, null);
    }

    public static byte[] newScanRecord(String name, UUID serviceUuid)
    {
        return newScanRecord(null, serviceUuid, null, name, null, null, null);
    }

    public static byte[] newScanRecord(String name, UUID serviceUuid, byte[] serviceData, short manufacturerId, byte[] manufacturerData)
    {
        return newScanRecord(null, serviceUuid, serviceData, name, null, manufacturerId, manufacturerData);
    }

    public static byte[] newScanRecord(Byte advFlags, UUID serviceUuid, byte[] serviceData, String name, Byte txPowerLevel, Short manufacturerId, byte[] manufacturerData)
    {
        final ByteBuffer buff = new ByteBuffer();
        if (advFlags != null)
        {
            buff.append((byte) 2);
            buff.append(DATA_TYPE_FLAGS);
            buff.append(advFlags);
        }
        if (serviceUuid != null)
        {
            if (serviceData != null && serviceData.length > 0)
            {
                buff.append((byte) (3 + serviceData.length));
                buff.append(DATA_TYPE_SERVICE_DATA);
                long msb = serviceUuid.getMostSignificantBits();
                short m = (short) (msb << 48);
                buff.append(Utils_Byte.shortToBytes(m));
                buff.append(serviceData);
            }
            else
            {
                long lsb = serviceUuid.getLeastSignificantBits();
                long msb = serviceUuid.getMostSignificantBits();
                buff.append((byte) 17);
                buff.append(Utils_Byte.longToBytes(lsb));
                buff.append(Utils_Byte.longToBytes(msb));
            }
        }
        else if (serviceData != null && serviceData.length > 0)
        {
            buff.append((byte) (1 + serviceData.length));
            buff.append(DATA_TYPE_SERVICE_DATA);
            buff.append(serviceData);
        }
        if (name != null && name.length() > 0)
        {
            buff.append((byte) (name.length() + 1));
            buff.append(DATA_TYPE_LOCAL_NAME_COMPLETE);
            buff.append(name.getBytes());
        }
        if (txPowerLevel != null)
        {
            buff.append((byte) 2);
            buff.append(DATA_TYPE_TX_POWER_LEVEL);
            buff.append(txPowerLevel);
        }
        if (manufacturerId != null)
        {
            if (manufacturerData != null && manufacturerData.length > 0)
            {
                buff.append((byte) (3 + manufacturerData.length));
                buff.append(DATA_TYPE_MANUFACTURER_SPECIFIC_DATA);
                buff.append(Utils_Byte.shortToBytes(manufacturerId));
                buff.append(manufacturerData);
            }
            else
            {
                buff.append((byte) 3);
                buff.append(DATA_TYPE_MANUFACTURER_SPECIFIC_DATA);
                buff.append(Utils_Byte.shortToBytes(manufacturerId));
            }
        }
        else if (manufacturerData != null && manufacturerData.length > 0)
        {
            buff.append((byte) (1 + manufacturerData.length));
            buff.append(DATA_TYPE_MANUFACTURER_SPECIFIC_DATA);
            buff.append(manufacturerData);
        }
        return buff.bytesAndClear();
    }

}
