package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.net.MailTo;

import com.idevicesinc.sweetblue.compat.K_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;


final class P_AndroidBleDevice implements P_NativeDeviceLayer {

    private BluetoothDevice m_device;


    @Override
    public int getBondState() {
        if (m_device != null)
        {
            return m_device.getBondState();
        }
        return 0;
    }

    @Override
    public String getAddress() {
        if (m_device != null)
        {
            return m_device.getAddress();
        }
        return "";
    }

    @Override
    public boolean createBond() {
        if (m_device != null)
        {
            if (Utils.isKitKat())
            {
                return K_Util.createBond(m_device);
            }
        }
        return false;
    }

    @Override
    public boolean createBondSneaky(String methodName, boolean loggingEnabled) {
        if (m_device != null && Utils.isKitKat())
        {
            final Class[] paramTypes = new Class[] { int.class };
            return Utils_Reflection.callBooleanReturnMethod(m_device, methodName, paramTypes, loggingEnabled);
        }
        return false;
    }

    @Override
    public void setNativeDevice(BluetoothDevice device) {
        m_device = device;
    }

    @Override
    public BluetoothDevice getNativeDevice() {
        return m_device;
    }

    @Override
    public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback) {
        if (m_device != null)
        {
            if (Utils.isMarshmallow())
            {
                return M_Util.connect(m_device, useAutoConnect, context, callback);
            }
            else
            {
                return m_device.connectGatt(context, useAutoConnect, callback);
            }
        }
        return null;
    }

}
