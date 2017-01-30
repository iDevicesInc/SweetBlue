package com.idevicesinc.sweetblue;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;

import java.util.Set;

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
    public int getConnectionState(P_NativeDeviceLayer device, int profile)
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

    @Override public boolean isManagerNull()
    {
        return m_manager == null || m_adaptor == null;
    }

    @Override public void resetManager(Context context)
    {
        m_manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        m_adaptor = m_manager.getAdapter();
    }

    @Override public boolean disable()
    {
        return m_adaptor.disable();
    }

    @Override public boolean enable()
    {
        return m_adaptor.enable();
    }

    @Override public boolean isMultipleAdvertisementSupported()
    {
        return L_Util.isAdvertisingSupportedByChipset(m_adaptor);
    }

    @Override public int getState()
    {
        return m_adaptor.getState();
    }

    @Override public String getAddress()
    {
        return m_adaptor.getAddress();
    }

    @Override public BluetoothGattServer openGattServer(Context context, P_BleServer_Listeners listeners)
    {
        return m_manager.openGattServer(context, listeners);
    }

    @Override public BluetoothLeAdvertiser getBluetoothLeAdvertiser()
    {
        return L_Util.getBluetoothLeAdvertiser(m_adaptor);
    }

    @Override public Set<BluetoothDevice> getBondedDevices()
    {
        return m_adaptor.getBondedDevices();
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

    @Override public BluetoothDevice getRemoteDevice(String macAddress)
    {
        return m_adaptor.getRemoteDevice(macAddress);
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
