package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;


public class P_UnitTestManagerLayer implements P_NativeManagerLayer
{

    @Override public int getConnectionState(P_NativeDeviceLayer device)
    {
        return 0;
    }

    @Override public boolean startDiscovery()
    {
        return true;
    }

    @Override public boolean cancelDiscovery()
    {
        return true;
    }

    @Override public void setNativeManager(BluetoothManager manager)
    {

    }

    @Override public void setNativeAdaptor(BluetoothAdapter adaptor)
    {

    }

    @Override public BluetoothAdapter getNativeAdaptor()
    {
        return null;
    }

    @Override public BluetoothManager getNativeManager()
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

    @Override public boolean startClassicDiscovery()
    {
        return true;
    }

    @Override public void stopClassicDiscovery()
    {
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
}
