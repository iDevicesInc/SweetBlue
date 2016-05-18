package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.idevicesinc.sweetblue.listeners.BondListener;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener;
import com.idevicesinc.sweetblue.utils.Utils;

import static com.idevicesinc.sweetblue.BleDeviceState.*;


class P_NativeDeviceWrapper
{

    final static Object[] RESET_TO_UNBONDED = new Object[]{BONDED, false, BONDING, false, UNBONDED, true};
    final static Object[] RESET_TO_BONDING = new Object[]{BONDED, false, BONDING, true, UNBONDED, false};
    final static Object[] RESET_TO_BONDED = new Object[]{BONDED, true, BONDING, false, UNBONDED, false};

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
                        onDeviceConnected();
                    }
                    else
                    {
                        onConnectionFail(status);
                    }
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    if (Utils.isSuccess(status))
                    {
                        onDeviceConnecting();
                    }
                    else
                    {
                        onConnectionFail(status);
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    // TODO - Should we do anything here?
                    Log.e("NativeBluetoothState", "Got STATE_DISCONNECTING!!");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    if (Utils.isSuccess(status))
                    {
                        onDeviceDisconnected();
                    }
                    else
                    {
                        onDeviceDisconnected(status);
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

    void onDeviceConnected()
    {
        mDevice.onConnected();
    }

    void onDeviceConnecting()
    {
        mDevice.onConnecting();
    }

    void onDeviceDisconnected()
    {
        mDevice.onDisconnected();
    }

    void onDeviceDisconnected(int status)
    {
        mDevice.onDisconnected(status);
    }

    void updateGattInstance(BluetoothGatt gatt)
    {
        if (mGatt != gatt)
        {
            mGatt = gatt;
        }
    }

    void onBondStateChanged(int previousState, int newState, int failReason)
    {
        if (failReason == BluetoothDevice.ERROR)
        {
            getManager().mTaskManager.failTask(P_Task_Bond.class, mDevice, false);
            getManager().mTaskManager.failTask(P_Task_Unbond.class, mDevice, false);
            getManager().getLogger().e("newState for bond is BluetoothDevice.ERROR!(?)");
        }
        else
        {
            switch (newState)
            {
                case BluetoothDevice.BOND_NONE:

                    P_Task curTask = getManager().mTaskManager.getCurrent();
                    if (curTask instanceof P_Task_Bond)
                    {
                        curTask.mFailReason = failReason;
                        curTask.fail();
                    }
                    else if (curTask instanceof P_Task_Unbond)
                    {
                        curTask.succeed();
                    }
                    else
                    {
                        if (previousState == BluetoothDevice.BOND_BONDING || previousState == BluetoothDevice.BOND_NONE)
                        {
                            mDevice.onBondFailed(P_StateTracker.E_Intent.UNINTENTIONAL, failReason, BondListener.Status.FAILED_EVENTUALLY);
                        }
                        else
                        {
                            mDevice.onUnbond(P_StateTracker.E_Intent.UNINTENTIONAL);
                        }
                    }
                    break;
                case BluetoothDevice.BOND_BONDING:
                    // TODO - Do we need to do anything else here?
                    getManager().mTaskManager.failTask(P_Task_Unbond.class, mDevice, false);
                    mDevice.onBonding(P_StateTracker.E_Intent.INTENTIONAL);
                    break;
                case BluetoothDevice.BOND_BONDED:
                    curTask = getManager().mTaskManager.getCurrent();
                    P_StateTracker.E_Intent intent;
                    if (curTask instanceof P_Task_Bond)
                    {
                        intent = P_StateTracker.E_Intent.INTENTIONAL;
                        curTask.succeed();
                    }
                    else
                    {
                        intent = P_StateTracker.E_Intent.UNINTENTIONAL;
                    }
                    getManager().mTaskManager.failTask(P_Task_Unbond.class, mDevice, false);
                    mDevice.onBond(intent);
            }
        }
    }

    boolean isBonded()
    {
        return mNativeDevice.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    boolean isBonding()
    {
        return mNativeDevice.getBondState() == BluetoothDevice.BOND_BONDING;
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
