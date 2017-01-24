package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;

import static com.idevicesinc.sweetblue.BleManagerState.ON;



public final class P_AndroidBluetoothManager implements P_NativeManagerLayer
{

    private BluetoothManager m_manager;
    private BluetoothAdapter m_adaptor;
    private BleManager m_bleManager;




    public void setBleManager(BleManager mgr)
    {
        m_bleManager = mgr;
    }

    @Override
    public int getConnectionState(P_NativeDeviceLayer device)
    {
        if (m_manager != null)
        {
            return m_manager.getConnectionState(device.getNativeDevice(), BluetoothGatt.GATT_SERVER);
        }
        return 0;
    }

    @Override
    public boolean startDiscovery()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.startDiscovery();
        }
        return false;
    }

    @Override
    public boolean cancelDiscovery()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.cancelDiscovery();
        }
        return false;
    }

    @Override
    public void setNativeManager(BluetoothManager manager) {
        m_manager = manager;
    }

    @Override
    public void setNativeAdaptor(BluetoothAdapter adaptor) {
        m_adaptor = adaptor;
    }

    @Override
    public BluetoothAdapter getNativeAdaptor() {
        return m_adaptor;
    }

    @Override
    public BluetoothManager getNativeManager() {
        return m_manager;
    }

    @Override public boolean isLocationEnabledForScanning_byOsServices()
    {
        return Utils.isLocationEnabledForScanning_byOsServices(m_bleManager.getApplicationContext());
    }

    @Override public boolean isLocationEnabledForScanning_byRuntimePermissions()
    {
        return Utils.isLocationEnabledForScanning_byRuntimePermissions(m_bleManager.getApplicationContext());
    }

    @Override public boolean isLocationEnabledForScanning()
    {
        return Utils.isLocationEnabledForScanning(m_bleManager.getApplicationContext());
    }

    @Override public boolean isBluetoothEnabled()
    {
        return m_bleManager.is(ON);
    }

    @Override public boolean startClassicDiscovery()
    {
        return m_adaptor.startDiscovery();
    }

    @Override public void stopClassicDiscovery()
    {
        m_adaptor.cancelDiscovery();
    }

    @Override public void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
    {
        L_Util.startNativeScan(m_adaptor, scanMode, delay, callback);
    }

    @Override public void startMScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
    {
        M_Util.startNativeScan(m_adaptor, scanMode, delay, callback);
    }

    @Override public boolean startLeScan(BluetoothAdapter.LeScanCallback callback)
    {
        return m_adaptor.startLeScan(callback);
    }

    @Override public void stopLeScan(BluetoothAdapter.LeScanCallback callback)
    {
        if (m_bleManager.getScanManager().isPostLollipopScan())
        {
            L_Util.stopNativeScan(m_adaptor);
        }
        else
        {
            m_adaptor.stopLeScan(callback);
        }
    }
}
