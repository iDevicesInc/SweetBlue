package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.text.TextUtils;

import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util;

import java.util.Random;


public class UnitTestDevice implements P_NativeDeviceLayer
{


    private String m_address;
    private BleDevice m_device;


    public UnitTestDevice(BleDevice device)
    {
        m_device = device;
        if (!device.isNull())
        {
            m_address = device.getMacAddress();
        }
    }


    @Override
    public void setNativeDevice(BluetoothDevice device) {
    }

    @Override
    public int getBondState() {
        return BluetoothDevice.BOND_NONE;
    }

    @Override
    public String getAddress() {
        if (TextUtils.isEmpty(m_address))
        {
            m_address = Util.randomMacAddress();
        }
        return m_address;
    }

    @Override public String getName()
    {
        return "";
    }

    @Override
    public boolean createBond() {
        NativeUtil.bondSuccess(getBleDevice(), getRandomTime());
        return true;
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

    @Override public boolean equals(P_NativeDeviceLayer device)
    {
        return device == this;
    }

    @Override
    public boolean createBondSneaky(String methodName, boolean loggingEnabled) {
        NativeUtil.bondSuccess(getBleDevice(), getRandomTime());
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

    @Override public void updateBleDevice(BleDevice device)
    {
        m_device = device;
    }

    @Override public BleDevice getBleDevice()
    {
        return m_device;
    }

    private Interval getRandomTime()
    {
        Random r = new Random();
        int ms = r.nextInt(450) + 50;
        return Interval.millis(ms);
    }
}
