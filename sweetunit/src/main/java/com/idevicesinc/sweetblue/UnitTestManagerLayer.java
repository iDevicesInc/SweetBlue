package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.text.TextUtils;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class UnitTestManagerLayer implements P_NativeManagerLayer
{

    private int m_nativeState = BleStatuses.STATE_ON;
    private String m_address;
    private Map<String, Integer> deviceStates = new HashMap<>();


    @Override public int getConnectionState(P_NativeDeviceLayer device, int profile)
    {
        Integer state = deviceStates.get(device.getAddress());
        return state == null ? 0 : state;
    }

    @Override public boolean startDiscovery()
    {
        return true;
    }

    @Override public boolean cancelDiscovery()
    {
        return true;
    }

    @Override public boolean isManagerNull()
    {
        return false;
    }

    public void updateDeviceState(BleDevice device, int state)
    {
        deviceStates.put(device.getMacAddress(), state);
    }

    @Override public boolean disable()
    {
        m_nativeState = BleStatuses.STATE_OFF;
        return true;
    }

    @Override public boolean enable()
    {
        m_nativeState = BleStatuses.STATE_ON;
        return true;
    }

    @Override public boolean isMultipleAdvertisementSupported()
    {
        return true;
    }

    @Override public void resetManager(Context context)
    {
    }

    @Override public int getState()
    {
        return m_nativeState;
    }

    @Override public String getAddress()
    {
        if (TextUtils.isEmpty(m_address))
        {
            m_address = UnitTestUtils.randomMacAddress();
        }
        return m_address;
    }

    @Override public Set<BluetoothDevice> getBondedDevices()
    {
        return null;
    }

    @Override public BluetoothAdapter getNativeAdaptor()
    {
        return null;
    }

    @Override public BluetoothManager getNativeManager()
    {
        return null;
    }

    @Override public BluetoothGattServer openGattServer(Context context, P_BleServer_Listeners listeners)
    {
        return null;
    }

    @Override public BluetoothLeAdvertiser getBluetoothLeAdvertiser()
    {
        return null;
    }

    @Override public boolean isLocationEnabledForScanning_byOsServices()
    {
        return true;
    }

    @Override public boolean isLocationEnabledForScanning_byRuntimePermissions()
    {
        return true;
    }

    @Override public boolean isLocationEnabledForScanning()
    {
        return true;
    }

    @Override public boolean isBluetoothEnabled()
    {
        return true;
    }

    @Override public void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
    {
    }

    @Override public void startMScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
    {
    }

    @Override public boolean startLeScan(BluetoothAdapter.LeScanCallback callback)
    {
        return true;
    }

    @Override public void stopLeScan(BluetoothAdapter.LeScanCallback callback)
    {
    }

    @Override public BluetoothDevice getRemoteDevice(String macAddress)
    {
        return null;
    }
}
