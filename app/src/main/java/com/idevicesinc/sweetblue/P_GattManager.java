package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.listeners.BondListener;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener;
import com.idevicesinc.sweetblue.listeners.NotifyListener;
import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.List;
import java.util.UUID;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener.Status;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener.Timing;
import static com.idevicesinc.sweetblue.BleDeviceState.*;


final class P_GattManager
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

    public final void discoverServices(final P_Task_DiscoverServices task)
    {
        if (!mGatt.discoverServices())
        {
            task.discoverServicesImmediatelyFailed();
        }
    }

    public final BluetoothGatt getGatt()
    {
        return mGatt;
    }

    public final BluetoothDevice getNativeDevice()
    {
        return mNativeDevice;
    }

    public final String getMacAddress()
    {
        return mNativeDevice.getAddress();
    }

    public final boolean read(UUID serviceUuid, UUID charUuid)
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

    public final boolean write(UUID serviceUuid, UUID charUuid, byte[] data)
    {
        BluetoothGattCharacteristic bchar = getCharacteristic(serviceUuid, charUuid);
        if (bchar != null)
        {
            bchar.setValue(data);
            mGatt.writeCharacteristic(bchar);
            return true;
        }
        else
        {
            return false;
        }
    }

    public final boolean enableNotify(UUID serviceUuid, UUID charUuid, boolean enable)
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
            if (isCharNotify(bchar))
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

    private boolean isCharNotify(BluetoothGattCharacteristic bchar)
    {
        return (bchar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0x0;
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
                        onConnectionFail(Status.NATIVE_CONNECTION_FAILED, Timing.EVENTUALLY, status);
                    }
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    if (Utils.isSuccess(status))
                    {
                        onDeviceConnecting();
                    }
                    else
                    {
                        onConnectionFail(Status.NATIVE_CONNECTION_FAILED, Timing.EVENTUALLY, status);
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    // TODO - Should we do anything here?
                    Log.e("NativeBluetoothState", "Got STATE_DISCONNECTING!!");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    final P_Task_Connect connect = getManager().mTaskManager.getCurrent(P_Task_Connect.class, mDevice);
                    if (connect != null)
                    {
                        onConnectionFail(Status.NATIVE_CONNECTION_FAILED, Timing.EVENTUALLY, status);
                        return;
                    }
                    final P_Task_DiscoverServices discover = getManager().mTaskManager.getCurrent(P_Task_DiscoverServices.class, mDevice);
                    if (discover != null)
                    {
                        onConnectionFail(Status.NATIVE_CONNECTION_FAILED, Timing.EVENTUALLY, status);
                        return;
                    }
                    final P_Task_Disconnect disconnect = getManager().mTaskManager.getCurrent(P_Task_Disconnect.class, mDevice);
                    if (disconnect != null)
                    {
                        onDeviceDisconnected();
                    }
                    else
                    {
                        onConnectionFail(Status.ROGUE_DISCONNECT, Timing.EVENTUALLY, status);
                    }
                    break;
                default:
                    Log.e("NativeBluetoothState", Utils_String.makeString("Got state ", newState, " with a status of ", status, ", and it was unhandled!"));
            }

        }

        @Override public final void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            updateGattInstance(gatt);
            if (Utils.isSuccess(status))
            {
                getManager().mTaskManager.succeedTask(P_Task_DiscoverServices.class, mDevice);
                mDevice.onServicesDiscovered();
            }
            else
            {
                getManager().mTaskManager.failTask(P_Task_DiscoverServices.class, mDevice, false);
                onConnectionFail(Status.DISCOVERING_SERVICES_FAILED, Timing.EVENTUALLY, status);
            }
        }

        @Override public final void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            updateGattInstance(gatt);
            P_Task_Read read = getManager().mTaskManager.getCurrent(P_Task_Read.class, mDevice);
            if (read != null)
            {
                ReadWriteListener.Status rwStatus = Utils.isSuccess(status) ? ReadWriteListener.Status.SUCCESS : ReadWriteListener.Status.REMOTE_GATT_FAILURE;
                final byte[] val = characteristic.getValue() == null ? null : characteristic.getValue().clone();
                final ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(mDevice, characteristic.getService().getUuid(), characteristic.getUuid(),
                        ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID, ReadWriteListener.Type.READ, ReadWriteListener.Target.CHARACTERISTIC, val,
                        rwStatus, status, 0, 0, true);
                read.onRead(event);
                getManager().mPostManager.postCallback(new Runnable()
                {
                    @Override public void run()
                    {
                        if (getManager().mDefaultReadWriteListener != null)
                        {
                            getManager().mDefaultReadWriteListener.onEvent(event);
                        }

                    }
                });
            }
        }

        @Override public final void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            updateGattInstance(gatt);
            P_Task_Write write = getManager().mTaskManager.getCurrent(P_Task_Write.class, mDevice);
            if (write != null)
            {
                final ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(mDevice, characteristic.getService().getUuid(), characteristic.getUuid(),
                        ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.CHARACTERISTIC, write.getValue(),
                        ReadWriteListener.Status.SUCCESS, status, 0, 0, true);
                write.onWrite(event);
                getManager().mPostManager.postCallback(new Runnable()
                {
                    @Override public void run()
                    {
                        if (getManager().mDefaultReadWriteListener != null)
                        {
                            getManager().mDefaultReadWriteListener.onEvent(event);
                        }
                    }
                });
            }
        }

        // Notifications/Indications
        @Override public final void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            updateGattInstance(gatt);
            NotifyListener.Type type = isCharNotify(characteristic) ? NotifyListener.Type.NOTIFICATION : NotifyListener.Type.INDICATION;
            final NotifyListener.NotifyEvent event = P_EventFactory.newNotifyEvent(mDevice, characteristic.getService().getUuid(),
                    characteristic.getUuid(), characteristic.getValue(), type, NotifyListener.Status.SUCCESS);
            mDevice.onNotify(event);
            getManager().mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    if (getManager().mDefaultNotifyListener != null)
                    {
                        getManager().mDefaultNotifyListener.onEvent(event);
                    }
                }
            });
        }

        @Override public final void onMtuChanged(BluetoothGatt gatt, int mtu, int status)
        {
            updateGattInstance(gatt);
            P_Task_RequestMtu task = getManager().mTaskManager.getCurrent(P_Task_RequestMtu.class, mDevice);
            if (task != null)
            {
                boolean success = Utils.isSuccess(status);
                ReadWriteListener.Status mtuStatus = success ? ReadWriteListener.Status.SUCCESS : ReadWriteListener.Status.REMOTE_GATT_FAILURE;
                ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(mDevice, ReadWriteListener.Type.WRITE, mDevice.getRssi(),
                        mtuStatus, status, 0, 0, true);
                task.onMtuChangeResult(event);
            }

        }

        @Override public final void onReliableWriteCompleted(BluetoothGatt gatt, int status)
        {
            updateGattInstance(gatt);
        }

        @Override public final void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            updateGattInstance(gatt);
        }

        @Override public final void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            updateGattInstance(gatt);
            P_Task_ToggleNotify notify = getManager().mTaskManager.getCurrent(P_Task_ToggleNotify.class, mDevice);
            if (notify != null)
            {
                ReadWriteListener.Type type = notify.enabling() ? ReadWriteListener.Type.ENABLING_NOTIFICATION : ReadWriteListener.Type.DISABLING_NOTIFICATION;
                ReadWriteListener.Status rwStatus = Utils.isSuccess(status) ? ReadWriteListener.Status.SUCCESS : ReadWriteListener.Status.FAILED_TO_TOGGLE_NOTIFICATION;
                final ReadWriteListener.ReadWriteEvent event = P_EventFactory.newReadWriteEvent(mDevice, descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(),
                        ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID, type, ReadWriteListener.Target.DESCRIPTOR, new byte[0],
                        rwStatus, status, 0, 0, true);
                notify.onToggleNotifyResult(event);
                getManager().mPostManager.postCallback(new Runnable()
                {
                    @Override public void run()
                    {
                        if (getManager().mDefaultReadWriteListener != null)
                        {
                            getManager().mDefaultReadWriteListener.onEvent(event);
                        }
                    }
                });
            }
        }

        @Override public final void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
        {
            updateGattInstance(gatt);
        }
    }

    final void onDeviceConnected()
    {
        mDevice.onConnected();
    }

    final void onDeviceConnecting()
    {
        mDevice.onConnecting();
    }

    final void onDeviceDisconnected()
    {
        closeGatt();
        mDevice.onDisconnectedExplicitly();
    }

    private void closeGatt()
    {
        if (mGatt != null)
        {
            //mGatt.disconnect();
            mGatt.close();
        }
    }

    final void onDeviceDisconnected(int status)
    {
        closeGatt();
        mDevice.onDisconnected(status);
    }

    final void updateGattInstance(BluetoothGatt gatt)
    {
        if (mGatt != gatt)
        {
            mGatt = gatt;
        }
    }

    final void checkCurrentBondState()
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

    final void onBondStateChanged(int previousState, int newState, int failReason)
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
                        mDevice.onUnbond(P_StateTracker.E_Intent.UNINTENTIONAL);
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

    final void requestMtuChange(int mtu)
    {
        if (Utils.isLollipop())
        {
            L_Util.requestMtu(mDevice, mtu);
        }
        else
        {
            getManager().getLogger().e("Tried to request an MTU size change on a device running an OS lower than Lollipop! This feature is not available on this OS version");
        }
    }

    final boolean isBonded()
    {
        return mNativeDevice.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    final boolean isBonding()
    {
        return mNativeDevice.getBondState() == BluetoothDevice.BOND_BONDING;
    }

    final void connect()
    {
        if (Utils.isMarshmallow())
        {
            M_Util.connect(mNativeDevice, getManager().getAppContext(), getManager().mConfig.useAndroidAutoConnect, mGattCallbacks);
        }
        else
        {
            mNativeDevice.connectGatt(getManager().getAppContext(), getManager().mConfig.useAndroidAutoConnect, mGattCallbacks);
        }
    }

    final void disconnect()
    {
        if (mGatt != null)
        {
            mGatt.disconnect();
        }
    }

    final void onConnectionFail(Status status, DeviceConnectionFailListener.Timing timing, int gattStatus)
    {
        closeGatt();
        mDevice.onConnectionFailed(status, timing, gattStatus);
    }

    private BleManager getManager()
    {
        return mDevice.getManager();
    }


}
