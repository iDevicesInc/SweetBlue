package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import com.idevicesinc.sweetblue.utils.Utils;


class P_NativeDeviceWrapper
{

    private final BleDevice mDevice;
    private final BluetoothDevice mNativeDevice;
    private final GattCallbacks mGattCallbacks;
    private BluetoothGatt mGatt;


    P_NativeDeviceWrapper(BleDevice device, BluetoothDevice nativeDevice)
    {
        mDevice = device;
        mNativeDevice = nativeDevice;
        mGattCallbacks = new GattCallbacks();
    }

    public void discoverServices()
    {
        mGatt.discoverServices();
    }

    public BluetoothGatt getGatt()
    {
        return mGatt;
    }

    public BluetoothDevice getNativeDevice()
    {
        return mNativeDevice;
    }

    public String getMacAddress()
    {
        return mNativeDevice.getAddress();
    }

    private class GattCallbacks extends BluetoothGattCallback
    {

        @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            updateGattInstance(gatt);
            switch (newState)
            {
                case BluetoothProfile.STATE_CONNECTED:
                    if (Utils.isSuccess(status))
                    {
                        mDevice.onConnected();
                    }
                    else
                    {
                        onConnectionFail(status);
                    }
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    if (Utils.isSuccess(status))
                    {
                        mDevice.onConnecting();
                    }
                    else
                    {
                        onConnectionFail(status);
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    // TODO - Should we do anything here?
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    if (Utils.isSuccess(status))
                    {
                        mDevice.onDisconnected();
                    }
                    else
                    {
                        mDevice.onDisconnected(status);
                    }
                    break;
            }

        }

        @Override public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (Utils.isSuccess(status))
            {
                getManager().mTaskManager.succeedTask(P_Task_DiscoverServices.class, mDevice);
                mDevice.onServicesDiscovered();
            }
            else
            {
                getManager().mTaskManager.failTask(P_Task_DiscoverServices.class, mDevice, false);
                onConnectionFail(status);
            }
        }

        @Override public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
        }

        @Override public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
        }

        // Notifications/Indications
        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
        }

        @Override public void onMtuChanged(BluetoothGatt gatt, int mtu, int status)
        {
        }

        @Override public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
        {
        }

        @Override public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
        }

        @Override public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
        }

        @Override public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
        {
        }
    }

    void updateGattInstance(BluetoothGatt gatt)
    {
        if (mGatt != gatt)
        {
            mGatt = gatt;
        }
    }

    void connect()
    {
        mGatt = mNativeDevice.connectGatt(getManager().getAppContext(), false, mGattCallbacks);
    }

    private void onConnectionFail(int gattStatus)
    {
        mDevice.onConnectionFailed(gattStatus);
        // TODO - Implement connection fail listener stuff
    }

    private BleManager getManager()
    {
        return mDevice.getManager();
    }


}
