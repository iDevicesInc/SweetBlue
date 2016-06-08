package com.idevicesinc.sweetblue.listeners;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;

import java.util.UUID;


public interface NotifyListener
{

    void onEvent(NotifyEvent event);

    enum Type
    {
        NULL,
        INDICATION,
        NOTIFICATION
    }

    class NotifyEvent extends Event implements UsesCustomNull
    {

        private final BleDevice mDevice;
        private final UUID mServiceUuid;
        private final UUID mCharUuid;
        private final byte[] mData;
        private final Status mStatus;
        private final Type mType;


        NotifyEvent(BleDevice device, UUID serviceUuid, UUID charUuid, byte[] data, Status status, Type type)
        {
            mDevice = device;
            mServiceUuid = serviceUuid;
            mCharUuid = charUuid;
            mData = data;
            mStatus = status;
            mType = type;
        }

        public BleDevice device()
        {
            return mDevice;
        }

        public UUID serviceUuid()
        {
            return mServiceUuid;
        }

        public UUID charUuid()
        {
            return mCharUuid;
        }

        public byte[] data()
        {
            return mData;
        }

        public Type type()
        {
            return mType;
        }

        public Status status()
        {
            return mStatus;
        }

        public boolean wasSuccess()
        {
            return mStatus == Status.SUCCESS;
        }

        @Override public boolean isNull()
        {
            return false;
        }
    }

    enum Status implements UsesCustomNull
    {
        /**
         * As of now, only used for {@link DeviceConnectionFailListener.ConnectionFailEvent#txnFailReason()} in some cases.
         */
        NULL,

        /**
         * If {@link ReadWriteEvent#type} {@link Type#isRead()} then {@link ReadWriteEvent#data} will contain some data returned from
         * the device. If type is {@link Type#WRITE} then {@link ReadWriteEvent#data} was sent to the device.
         */
        SUCCESS,

        /**
         * {@link BleDevice#read(UUID, ReadWriteListener)}, {@link BleDevice#write(UUID, byte[])},
         * {@link BleDevice#enableNotify(UUID, ReadWriteListener)}, etc. was called on {@link BleDevice#NULL}.
         */
        NULL_DEVICE,

        /**
         * Device is not {@link BleDeviceState#CONNECTED}.
         */
        NOT_CONNECTED,

        /**
         * Couldn't find a matching {@link ReadWriteEvent#target} for the {@link ReadWriteEvent#charUuid} (or
         * {@link ReadWriteEvent#descUuid} if {@link ReadWriteEvent#target} is {@link Target#DESCRIPTOR}) which was given to
         * {@link BleDevice#read(UUID, ReadWriteListener)}, {@link BleDevice#write(UUID, byte[])}, etc. This most likely
         * means that the internal call to {@link BluetoothGatt#discoverServices()} didn't find any
         * {@link BluetoothGattService} that contained a {@link BluetoothGattCharacteristic} for {@link ReadWriteEvent#charUuid()}.
         */
        NO_MATCHING_TARGET,

        /**
         * You tried to do a read on a characteristic that is write-only, or
         * vice-versa, or tried to read a notify-only characteristic, or etc., etc.
         */
        OPERATION_NOT_SUPPORTED,

        /**
         * The android api level doesn't support the lower level API call in the native stack. For example if you try to use
         * {@link BleDevice#setMtu(int, ReadWriteListener)}, which requires API level 21, and you are at level 18.
         */
        ANDROID_VERSION_NOT_SUPPORTED,

        /**
         * {@link BluetoothGatt#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)}
         * returned <code>false</code> for an unknown reason. This {@link Status} is only relevant for calls to
         * {@link BleDevice#enableNotify(UUID, ReadWriteListener)} and {@link BleDevice#disableNotify(UUID, ReadWriteListener)}
         * (or the various overloads).
         */
        FAILED_TO_TOGGLE_NOTIFICATION,

        /**
         * The operation was cancelled by the device becoming {@link BleDeviceState#DISCONNECTED}.
         */
        CANCELLED_FROM_DISCONNECT,

        /**
         * The operation was cancelled because {@link BleManager} went {@link BleManagerState#TURNING_OFF} and/or
         * {@link BleManagerState#OFF}. Note that if the user turns off BLE from their OS settings (airplane mode, etc.) then
         * {@link ReadWriteEvent#status} could potentially be {@link #CANCELLED_FROM_DISCONNECT} because SweetBlue might get
         * the disconnect callback before the turning off callback. Basic testing has revealed that this is *not* the case, but you never know.
         * <br><br>
         * Either way, the device was or will be disconnected.
         */
        CANCELLED_FROM_BLE_TURNING_OFF,

        /**
         * Used either when {@link ReadWriteEvent#type()} {@link Type#isRead()} and the stack returned a <code>null</code>
         * value for {@link BluetoothGattCharacteristic#getValue()} despite the operation being otherwise "successful", <i>or</i>
         * {@link BleDevice#write(UUID, byte[])} (or overload(s) ) were called with a null data parameter. For the read case, the library
         * will throw an {@link BleManager.UhOhListener.UhOh#READ_RETURNED_NULL}, but hopefully it was just a temporary glitch. If the problem persists try {@link BleManager#reset()}.
         */
        NULL_DATA,

        /**
         * Used either when {@link ReadWriteEvent#type} {@link Type#isRead()} and the operation was "successful" but
         * returned a zero-length array for {@link ReadWriteEvent#data}, <i>or</i> {@link BleDevice#write(UUID, byte[])} (or overload(s) )
         * was called with a non-null but zero-length data parameter. Note that {@link ReadWriteEvent#data} will be a zero-length array for
         * all other error statuses as well, for example {@link #NO_MATCHING_TARGET}, {@link #NOT_CONNECTED}, etc. In other words it's never null.
         */
        EMPTY_DATA,

        /**
         * For now only used when giving a negative or zero value to {@link BleDevice#setMtu(int, ReadWriteListener)}.
         */
        INVALID_DATA,

        /**
         * The operation failed in a "normal" fashion, at least relative to all the other strange ways an operation can fail. This means for
         * example that {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)}
         * returned a status code that was not zero. This could mean the device went out of range, was turned off, signal was disrupted,
         * whatever. Often this means that the device is about to become {@link BleDeviceState#DISCONNECTED}. {@link ReadWriteEvent#gattStatus()}
         * will most likely be non-zero, and you can check against the static fields in {@link BleStatuses} for more information.
         *
         * @see ReadWriteEvent#gattStatus()
         */
        REMOTE_GATT_FAILURE,

        /**
         * Operation took longer than time specified in {@link BleNodeConfig#taskTimeoutRequestFilter} so we cut it loose.
         */
        TIMED_OUT;

        /**
         * Returns <code>true</code> for {@link #CANCELLED_FROM_DISCONNECT} or {@link #CANCELLED_FROM_BLE_TURNING_OFF}.
         */
        public boolean wasCancelled()
        {
            return this == CANCELLED_FROM_DISCONNECT || this == Status.CANCELLED_FROM_BLE_TURNING_OFF;
        }

        @Override public boolean isNull()
        {
            return this == NULL;
        }
    }

}
