package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.text.TextUtils;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.Random;


public class P_UnitDevice implements P_NativeDeviceLayer {


    private String m_address;


    @Override
    public void setNativeDevice(BluetoothDevice device) {
    }

    @Override
    public int getBondState() {
        return 0;
    }

    @Override
    public String getAddress() {
        if (TextUtils.isEmpty(m_address))
        {
            byte[] add = new byte[6];
            new Random().nextBytes(add);
            m_address = Utils_String.bytesToMacAddress(add);
        }
        return m_address;
    }

    @Override
    public boolean createBond() {
        return false;
    }


    @Override public boolean isDeviceNull()
    {
        return false;
    }

    @Override public boolean removeBond()
    {
        return true;
    }

    @Override public boolean cancelBond()
    {
        return true;
    }

    @Override public boolean equals(BluetoothDevice device)
    {
        return false;
    }

    @Override
    public boolean createBondSneaky(String methodName, boolean loggingEnabled) {
        return true;
    }

    @Override
    public BluetoothDevice getNativeDevice() {
        return null;
    }

    @Override
    public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback) {
        return null;
    }
}
