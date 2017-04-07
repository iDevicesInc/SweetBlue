package com.idevicesinc.sweetblue;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.DeadObjectException;

import com.idevicesinc.sweetblue.compat.K_Util;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Utils;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;


@TargetApi(Build.VERSION_CODES.KITKAT)
final class P_AndroidGatt implements P_GattLayer
{

    private static final String FIELD_NAME_AUTH_RETRY = "mAuthRetry";

    private BluetoothGatt m_gatt;

    private final BleDevice m_device;


    P_AndroidGatt(BleDevice device)
    {
        m_device = device;
    }


    @Override public BleDevice getBleDevice()
    {
        return m_device;
    }

    @Override
    public void setGatt(BluetoothGatt gatt) {
        m_gatt = gatt;
    }

    @Override
    public BluetoothGatt getGatt() {
        return m_gatt;
    }

    @Override
    public Boolean getAuthRetryValue() {
        if( m_gatt != null )
        {
            try
            {
//                final Field[] fields = m_gatt.getClass().getDeclaredFields();
                Field field = m_gatt.getClass().getDeclaredField(FIELD_NAME_AUTH_RETRY);
                final boolean isAccessible_saved = field.isAccessible();
                field.setAccessible(true);
                Boolean result = field.getBoolean(m_gatt);
                field.setAccessible(isAccessible_saved);

                return result;
            }
            catch (Exception e)
            {
                getManager().ASSERT(false, "Problem getting value of " + m_gatt.getClass().getSimpleName() + "." + FIELD_NAME_AUTH_RETRY);
            }
        }
        else
        {
            getManager().ASSERT(false, "Expected gatt object to be not null");
        }
        return null;
    }

    @Override
    public boolean equals(BluetoothGatt gatt) {
        return gatt == m_gatt;
    }

    private BleManager getManager()
    {
        return BleManager.s_instance;
    }

    @Override
    public BleManager.UhOhListener.UhOh closeGatt() {
        BleManager.UhOhListener.UhOh uhoh = null;
        if( m_gatt == null )  return uhoh;

        //--- DRK > Tried this to see if it would kill autoConnect, but alas it does not, at least on S5.
        //---		Don't want to keep it here because I'm afraid it has a better chance to do bad than good.
//			if( disconnectAlso )
//			{
//				m_gatt.disconnect();
//			}

        //--- DRK > This can randomly throw an NPE down stream...NOT from m_gatt being null, but a few methods downstream.
        //---		See below for more info.
        try
        {
            m_gatt.close();
        }
        catch(Exception e)
        {
            if (e instanceof DeadObjectException)
            {
                //--- RB > It has been observed by some customers that a DeadObjectException can happen here. Nothing we can do about it, just
                // checking for it, and throwing to the UhOh Listener as a DeadObjectException

//				android.os.DeadObjectException
//				at android.os.BinderProxy.transactNative(Native Method)
//				at android.os.BinderProxy.transact(Binder.java:503)
//				at android.bluetooth.IBluetoothGatt$Stub$Proxy.unregisterClient(IBluetoothGatt.java:1009)
//				at android.bluetooth.BluetoothGatt.unregisterApp(BluetoothGatt.java:820)
//				at android.bluetooth.BluetoothGatt.close(BluetoothGatt.java:759)
//				at com.idevicesinc.sweetblue.P_NativeDeviceWrapper.closeGatt(P_NativeDeviceWrapper.java:319)
//				at com.idevicesinc.sweetblue.P_NativeDeviceWrapper.closeGattIfNeeded(P_NativeDeviceWrapper.java:301)
//				at com.idevicesinc.sweetblue.BleDevice.onNativeConnectFail(BleDevice.java:5782)
//				at com.idevicesinc.sweetblue.P_BleDevice_Listeners$1.onStateChange(P_BleDevice_Listeners.java:51)
//				at com.idevicesinc.sweetblue.PA_Task.setState(PA_Task.java:148)
//				at com.idevicesinc.sweetblue.PA_Task.setEndingState(PA_Task.java:288)
//				at com.idevicesinc.sweetblue.P_TaskQueue.endCurrentTask(P_TaskQueue.java:288)
//				at com.idevicesinc.sweetblue.P_TaskQueue.tryEndingTask_mainThread(P_TaskQueue.java:395)
//				at com.idevicesinc.sweetblue.P_TaskQueue.tryEndingTask(P_TaskQueue.java:387)
//				at com.idevicesinc.sweetblue.PA_Task.timeout(PA_Task.java:183)
//				at com.idevicesinc.sweetblue.PA_Task.update_internal(PA_Task.java:354)
//				at com.idevicesinc.sweetblue.P_TaskQueue.update(P_TaskQueue.java:236)
//				at com.idevicesinc.sweetblue.BleManager.update(BleManager.java:3245)
//				at com.idevicesinc.sweetblue.BleManager$1.onUpdate(BleManager.java:746)
//				at com.idevicesinc.sweetblue.utils.UpdateLoop$1.run(UpdateLoop.java:24)
//				at android.os.Handler.handleCallback(Handler.java:739)
//				at android.os.Handler.dispatchMessage(Handler.java:95)
//				at android.os.Looper.loop(Looper.java:148)
//				at android.app.ActivityThread.main(ActivityThread.java:7303)
//				at java.lang.reflect.Method.invoke(Native Method)
//				at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1230)
//				at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1120)
                uhoh = BleManager.UhOhListener.UhOh.DEAD_OBJECT_EXCEPTION;
            }
            else
            {
                //--- DRK > From Flurry crash reports...happened several times on S4 running 4.4.4 but was not able to reproduce.
//				This error occurred: java.lang.NullPointerException
//				android.os.Parcel.readException(Parcel.java:1546)
//				android.os.Parcel.readException(Parcel.java:1493)
//				android.bluetooth.IBluetoothGatt$Stub$Proxy.unregisterClient(IBluetoothGatt.java:905)
//				android.bluetooth.BluetoothGatt.unregisterApp(BluetoothGatt.java:710)
//				android.bluetooth.BluetoothGatt.close(BluetoothGatt.java:649)
//				com.idevicesinc.sweetblue.P_NativeDeviceWrapper.closeGatt(P_NativeDeviceWrapper.java:238)
//				com.idevicesinc.sweetblue.P_NativeDeviceWrapper.closeGattIfNeeded(P_NativeDeviceWrapper.java:221)
//				com.idevicesinc.sweetblue.BleDevice.onNativeConnectFail(BleDevice.java:2193)
//				com.idevicesinc.sweetblue.P_BleDevice_Listeners$1.onStateChange_synchronized(P_BleDevice_Listeners.java:78)
//				com.idevicesinc.sweetblue.P_BleDevice_Listeners$1.onStateChange(P_BleDevice_Listeners.java:49)
//				com.idevicesinc.sweetblue.PA_Task.setState(PA_Task.java:118)
//				com.idevicesinc.sweetblue.PA_Task.setEndingState(PA_Task.java:242)
//				com.idevicesinc.sweetblue.P_TaskQueue.endCurrentTask(P_TaskQueue.java:220)
//				com.idevicesinc.sweetblue.P_TaskQueue.tryEndingTask(P_TaskQueue.java:267)
//				com.idevicesinc.sweetblue.P_TaskQueue.fail(P_TaskQueue.java:260)
//				com.idevicesinc.sweetblue.P_BleDevice_Listeners.onConnectionStateChange_synchronized(P_BleDevice_Listeners.java:168)
                uhoh = BleManager.UhOhListener.UhOh.RANDOM_EXCEPTION;
            }
        }
        m_gatt = null;
        return uhoh;
    }

    @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
    {
        if (m_gatt == null)
        {
            return null;
        }
        List<BluetoothGattService> list_native = null;

        try
        {
            list_native = m_gatt.getServices();
        }
        catch (Exception e)
        {
            BleManager.UhOhListener.UhOh uhoh;
            if (e instanceof ConcurrentModificationException)
            {
                uhoh = BleManager.UhOhListener.UhOh.CONCURRENT_EXCEPTION;
            }
            else
            {
                uhoh = BleManager.UhOhListener.UhOh.RANDOM_EXCEPTION;
            }
            m_device.getManager().uhOh(uhoh);
            logger.e("Got a " + e.getClass().getSimpleName() + " with a message of " + e.getMessage() + " when trying to get the list of native services!");
        }
        return list_native;
    }

    @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
    {
        BluetoothGattService service = null;
        try
        {
            service = m_gatt.getService(serviceUuid);
        }
        catch (Exception e)
        {
            BleManager.UhOhListener.UhOh uhoh;
            if (e instanceof ConcurrentModificationException)
            {
                uhoh = BleManager.UhOhListener.UhOh.CONCURRENT_EXCEPTION;
            }
            else
            {
                uhoh = BleManager.UhOhListener.UhOh.RANDOM_EXCEPTION;
            }
            m_device.getManager().uhOh(uhoh);
            logger.e("Got a " + e.getClass().getSimpleName() + " with a message of " + e.getMessage() + " when trying to get the native service!");
        }
        return service;
    }

    @Override public boolean isGattNull()
    {
        return m_gatt == null;
    }

    @Override public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
    {
        m_gatt = device.connect(context, useAutoConnect, callback);
        return m_gatt;
    }

    @Override
    public boolean requestMtu(int mtu) {
        if (m_gatt != null)
        {
            return L_Util.requestMtu(m_gatt, mtu);
        }
        return false;
    }

    @Override
    public boolean refreshGatt() {
        if (m_gatt != null)
        {
            Utils.refreshGatt(m_gatt);
        }
        return false;
    }

    @Override
    public boolean requestConnectionPriority(BleConnectionPriority priority)
    {
        if (m_gatt != null)
        {
            return L_Util.requestConnectionPriority(m_gatt, priority.getNativeMode());
        }
        return false;
    }

    @Override public void disconnect()
    {
        if (m_gatt != null)
        {
            m_gatt.disconnect();
        }
    }

    @Override public boolean readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        if (m_gatt != null && characteristic != null)
        {
            return m_gatt.readCharacteristic(characteristic);
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

    @Override public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        if (m_gatt != null && characteristic != null)
        {
            return m_gatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    @Override
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (m_gatt != null && characteristic != null)
        {
            return m_gatt.setCharacteristicNotification(characteristic, enable);
        }
        return false;
    }

    @Override public boolean readDescriptor(BluetoothGattDescriptor descriptor)
    {
        if (m_gatt != null && descriptor != null)
        {
            return m_gatt.readDescriptor(descriptor);
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

    @Override public boolean writeDescriptor(BluetoothGattDescriptor descriptor)
    {
        if (m_gatt != null && descriptor != null)
        {
            return m_gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    @Override public boolean discoverServices()
    {
        return m_gatt.discoverServices();
    }

    @Override public boolean executeReliableWrite()
    {
        return m_gatt.executeReliableWrite();
    }

    @Override
    public boolean beginReliableWrite() {
        return m_gatt.beginReliableWrite();
    }

    @Override
    public void abortReliableWrite(BluetoothDevice device)
    {
        if( Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2 )
        {
            m_gatt.abortReliableWrite(device);
        }
        else
        {
            K_Util.abortReliableWrite(m_gatt);
        }
    }

    @Override public boolean readRemoteRssi()
    {
        return m_gatt.readRemoteRssi();
    }

}
