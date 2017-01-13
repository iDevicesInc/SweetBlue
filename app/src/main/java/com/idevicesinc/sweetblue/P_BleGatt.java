package com.idevicesinc.sweetblue;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;
import java.util.List;
import java.util.UUID;


@TargetApi(Build.VERSION_CODES.KITKAT)
class P_BleGatt implements P_GattLayer
{



    @Override public String getAddress(BluetoothDevice device)
    {
        return device.getAddress();
    }

    @Override public int getBondState(BluetoothDevice device)
    {
        return device.getBondState();
    }

    @Override public int getNativeConnectionState(BluetoothDevice device, BluetoothManager manager)
    {
        return manager.getConnectionState(device, BluetoothGatt.GATT_SERVER);
    }

    @Override public List<BluetoothGattService> getNativeServiceList(BluetoothGatt gatt, P_Logger logger)
    {
        if (gatt == null)
        {
            return null;
        }
        List<BluetoothGattService> list_native = null;

        try
        {
            list_native = gatt.getServices();
        }
        catch (Exception e)
        {
            logger.e("Got a " + e.getClass().getSimpleName() + " with a message of " + e.getMessage() + " when trying to get the list of native services!");
        }
        return list_native;
    }

    @Override public BluetoothGattService getService(BluetoothGatt gatt, UUID serviceUuid, P_Logger logger)
    {
        BluetoothGattService service = null;
        try
        {
            service = gatt.getService(serviceUuid);
        }
        catch (Exception e)
        {
            logger.e("Got a " + e.getClass().getSimpleName() + " with a message of " + e.getMessage() + " when trying to get the native service!");
        }
        return service;
    }

    @Override public boolean isGattNull(BluetoothGatt gatt)
    {
        return gatt == null;
    }

    @Override public BluetoothGatt connect(BluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
    {
        final BluetoothGatt m_gatt;
        if (Utils.isMarshmallow())
        {
            m_gatt = M_Util.connect(device, useAutoConnect, context, callback);
        }
        else
        {
            m_gatt = device.connectGatt(context, useAutoConnect, callback);
        }
        return m_gatt;
    }

    @Override public void disconnect(BluetoothGatt gatt)
    {
        if (gatt != null)
        {
            gatt.disconnect();
        }
    }

    @Override public boolean readCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        if (gatt != null && characteristic != null)
        {
            return gatt.readCharacteristic(characteristic);
        }
        return false;
    }

    @Override public boolean setCharValue(BluetoothGattCharacteristic characteristic, byte[] data)
    {
        if (characteristic != null)
        {
            return characteristic.setValue(data);
        }
        return false;
    }

    @Override public boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        if (gatt != null && characteristic != null)
        {
            return gatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    @Override public boolean readDescriptor(BluetoothGatt gatt, BluetoothGattDescriptor descriptor)
    {
        if (gatt != null && descriptor != null)
        {
            return gatt.readDescriptor(descriptor);
        }
        return false;
    }

    @Override public boolean setDescValue(BluetoothGattDescriptor descriptor, byte[] data)
    {
        if (descriptor != null)
        {
            return descriptor.setValue(data);
        }
        return false;
    }

    @Override public boolean writeDescriptor(BluetoothGatt gatt, BluetoothGattDescriptor descriptor)
    {
        if (gatt != null && descriptor != null)
        {
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    @Override public boolean createBond(BleDevice device)
    {
        return device.getNative().createBond();
    }

    @Override public boolean createBondSneaky(BluetoothDevice device, String methodName, boolean loggingEnabled)
    {
        final Class[] paramTypes = new Class[]{int.class};

        return Utils_Reflection.callBooleanReturnMethod(device, methodName, paramTypes, loggingEnabled, BluetoothDevice.TRANSPORT_LE);
    }

    @Override public boolean startDiscovery(BluetoothAdapter adapter)
    {
        return adapter.startDiscovery();
    }

    @Override public boolean cancelDiscovery(BluetoothAdapter adapter)
    {
        return adapter.cancelDiscovery();
    }

    @Override public boolean refreshGatt(BleDevice device)
    {
        return Utils.refreshGatt(device);
    }

    @Override public boolean discoverServices(BluetoothGatt gatt)
    {
        return gatt.discoverServices();
    }

    @Override public boolean executeReliableWrite(BluetoothGatt gatt)
    {
        return gatt.executeReliableWrite();
    }

    @Override public boolean readRemoteRssi(BluetoothGatt gatt)
    {
        return gatt.readRemoteRssi();
    }

    @Override public boolean requestConnectionPriority(BleDevice device, BleConnectionPriority priority)
    {
        return L_Util.requestConnectionPriority(device, priority.getNativeMode());
    }

    @Override public boolean requestMtu(BleDevice device, int mtu)
    {
        return L_Util.requestMtu(device, mtu);
    }
}
