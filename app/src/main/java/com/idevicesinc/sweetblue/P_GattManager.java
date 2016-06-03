package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.text.TextUtils;
import android.util.Log;

import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.listeners.BondListener;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener;
import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.List;
import java.util.UUID;

import static com.idevicesinc.sweetblue.BleDeviceState.*;


class P_GattManager
{

    final static Object[] RESET_TO_UNBONDED = new Object[]{BONDED, false, BONDING, false, UNBONDED, true};
    final static Object[] RESET_TO_BONDING = new Object[]{BONDED, false, BONDING, true, UNBONDED, false};
    final static Object[] RESET_TO_BONDED = new Object[]{BONDED, true, BONDING, false, UNBONDED, false};


    private final BleDevice mDevice;
    private final BluetoothDevice mNativeDevice;
    private final GattCallbacks mGattCallbacks;
    private BluetoothGatt mGatt;


    P_GattManager(BleDevice device, BluetoothDevice nativeDevice)
    {
        mDevice = device;
        mNativeDevice = nativeDevice;
        mGattCallbacks = new GattCallbacks();
    }

    public void discoverServices(final P_Task_DiscoverServices task)
    {
        if (!mGatt.discoverServices())
        {
            task.discoverServicesImmediatelyFailed();
        }
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

    public boolean read(UUID serviceUuid, UUID charUuid)
    {
        BluetoothGattCharacteristic bchar = getCharacteristic(serviceUuid, charUuid);
        if (bchar != null)
        {
            mGatt.readCharacteristic(bchar);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean write(UUID serviceUuid, UUID charUuid)
    {
        BluetoothGattCharacteristic bchar = getCharacteristic(serviceUuid, charUuid);
        if (bchar != null)
        {
            mGatt.writeCharacteristic(bchar);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean enableNotify(UUID serviceUuid, UUID charUuid, boolean enable)
    {
        BluetoothGattCharacteristic bchar = getCharacteristic(serviceUuid, charUuid);
        if (bchar != null)
        {
            boolean success;
            success = mGatt.setCharacteristicNotification(bchar, enable);
            if (!success)
            {
                return false;
            }
            BluetoothGattDescriptor desc = bchar.getDescriptor(Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID);
            if (desc == null)
            {
                return false;
            }
            byte[] writeValue;
            if ((bchar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0x0)
            {
                writeValue = enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            }
            else
            {
                writeValue = enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            }
            desc.setValue(writeValue);
            mGatt.writeDescriptor(desc);
            return true;
        }
        else
        {
            return false;
        }
    }

    private BluetoothGattService getService(UUID serviceUuid)
    {
        return mGatt.getService(serviceUuid);
    }

    private BluetoothGattCharacteristic getCharacteristic(UUID charUuid)
    {
        BluetoothGattCharacteristic bchar = null;
        List<BluetoothGattService> services = mGatt.getServices();
        for (BluetoothGattService service : services)
        {
            bchar = service.getCharacteristic(charUuid);
            if (bchar != null)
            {
                break;
            }
        }
        return bchar;
    }

    private BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid, UUID charUuid)
    {
        if (serviceUuid == null)
        {
            return getCharacteristic(charUuid);
        }
        BluetoothGattService service = getService(serviceUuid);
        BluetoothGattCharacteristic bchar = null;
        if (service != null)
        {
            bchar = service.getCharacteristic(charUuid);
        }
        return bchar;
    }

    private class GattCallbacks extends BluetoothGattCallback
    {

        @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            getManager().getLogger().e(Utils_String.makeString("Connection state change. Status: ", status, " Newstate: ", newState));
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
                default:
                    Log.e("NativeBluetoothState", Utils_String.makeString("Got state ", newState, " with a status of ", status, ", and it was unhandled!"));
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
                //onConnectionFail(status);
            }
        }

        @Override public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            P_Task_Read read = getManager().mTaskManager.getCurrent(P_Task_Read.class, mDevice);
            if (read != null)
            {
                final byte[] val = characteristic.getValue() == null ? null : characteristic.getValue().clone();
                ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(mDevice, characteristic.getService().getUuid(), characteristic.getUuid(),
                        ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID, ReadWriteListener.Type.READ, ReadWriteListener.Target.CHARACTERISTIC, val,
                        ReadWriteListener.Status.SUCCESS, status, 0, 0, true);
                read.onRead(event);
                // TODO - Also post the read event to the "catch-all" listener stored in BleManager
            }
        }

        @Override public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            P_Task_Write write = getManager().mTaskManager.getCurrent(P_Task_Write.class, mDevice);
            if (write != null)
            {
                ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(mDevice, characteristic.getService().getUuid(), characteristic.getUuid(),
                        ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.CHARACTERISTIC, write.getValue(),
                        ReadWriteListener.Status.SUCCESS, status, 0, 0, true);
                write.onWrite(event);
                // TODO - Also post the write event to the "catch-all" listener stored in BleManager
            }
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
            P_Task_ToggleNotify notify = getManager().mTaskManager.getCurrent(P_Task_ToggleNotify.class, mDevice);
            if (notify != null)
            {
                ReadWriteListener.Type type = notify.enabling() ? ReadWriteListener.Type.ENABLING_NOTIFICATION : ReadWriteListener.Type.DISABLING_NOTIFICATION;
                ReadWriteListener.Status rwStatus = Utils.isSuccess(status) ? ReadWriteListener.Status.SUCCESS : ReadWriteListener.Status.FAILED_TO_TOGGLE_NOTIFICATION;
                ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(mDevice, descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(),
                        ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID, type, ReadWriteListener.Target.DESCRIPTOR, new byte[0],
                        rwStatus, status, 0, 0, true);
                notify.onToggleNotifyResult(event);
                // TODO - Also post the notify result event to the "catch-all" listener stored in BleManager
            }
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
        closeGatt();
        mDevice.onDisconnected();
    }

    private void closeGatt()
    {
        if (mGatt != null)
        {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    void onDeviceDisconnected(int status)
    {
        closeGatt();
        mDevice.onDisconnected(status);
    }

    void updateGattInstance(BluetoothGatt gatt)
    {
        if (mGatt != gatt)
        {
            mGatt = gatt;
        }
    }

    void checkCurrentBondState()
    {
        int curState = mNativeDevice.getBondState();
        switch (curState)
        {
            case BluetoothDevice.BOND_NONE:
                mDevice.stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, RESET_TO_UNBONDED);
                break;
            case BluetoothDevice.BOND_BONDING:
                mDevice.stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, RESET_TO_BONDING);
                break;
            case BluetoothDevice.BOND_BONDED:
                mDevice.stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, RESET_TO_BONDED);
                break;
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
        if (Utils.isMarshmallow())
        {
            M_Util.connect(mNativeDevice, getManager().getAppContext(), mGattCallbacks);
        }
        else
        {
            mNativeDevice.connectGatt(getManager().getAppContext(), false, mGattCallbacks);
        }
    }

    void disconnect()
    {
        if (mGatt != null)
        {
            mGatt.disconnect();
        }
    }

    private void onConnectionFail(int gattStatus)
    {
        closeGatt();
        mDevice.onConnectionFailed(gattStatus);
        // TODO - Implement connection fail listener stuff
    }

    private BleManager getManager()
    {
        return mDevice.getManager();
    }


}
