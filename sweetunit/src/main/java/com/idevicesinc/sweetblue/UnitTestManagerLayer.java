package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.text.TextUtils;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class UnitTestManagerLayer implements P_NativeManagerLayer
{

    private int m_nativeState = BleStatuses.STATE_ON;
    private String m_address;
    private String m_name = "MockedDevice";
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
        if (BleManager.s_instance != null)
        {
            BleManager.s_instance.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    setToTurningOff();
                }
            }, 50);
            BleManager.s_instance.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    setToOff();
                }
            }, 150);
        }
        else
        {
            m_nativeState = BleStatuses.STATE_OFF;
        }
        return true;
    }

    @Override public boolean enable()
    {
        if (BleManager.s_instance != null)
        {
            BleManager.s_instance.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    setToTurningOn();
                }
            }, 50);
            BleManager.s_instance.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    setToOn();
                }
            }, 150);
        }
        else
        {
            m_nativeState = BleStatuses.STATE_ON;
        }
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

    @Override public int getBleState()
    {
        return m_nativeState;
    }

    @Override
    public String getName()
    {
        return m_name;
    }

    @Override
    public void setName(String name)
    {
        m_name = name;
    }

    @Override public String getAddress()
    {
        if (TextUtils.isEmpty(m_address))
        {
            m_address = Util.randomMacAddress();
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

    @Override public P_NativeServerLayer openGattServer(Context context, P_BleServer_Listeners listeners)
    {
        return new UnitTestServerLayer();
    }

    @Override
    public void startAdvertising(AdvertiseSettings settings, AdvertiseData adData, AdvertiseCallback callback)
    {
    }

    @Override
    public void stopAdvertising(AdvertiseCallback callback)
    {
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
        return m_nativeState == BleStatuses.STATE_ON;
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

    protected void manuallySetState(int newState)
    {
        m_nativeState = newState;
    }

    protected void setToTurningOff()
    {
        NativeUtil.sendBluetoothStateChange(BleManager.s_instance, m_nativeState, BluetoothAdapter.STATE_TURNING_OFF);
        m_nativeState = BluetoothAdapter.STATE_TURNING_OFF;
    }

    protected void setToOff()
    {
        NativeUtil.sendBluetoothStateChange(BleManager.s_instance, m_nativeState, BluetoothAdapter.STATE_OFF);
        m_nativeState = BluetoothAdapter.STATE_OFF;
    }

    protected void setToTurningOn()
    {
        NativeUtil.sendBluetoothStateChange(BleManager.s_instance, m_nativeState, BluetoothAdapter.STATE_TURNING_ON);
        m_nativeState = BluetoothAdapter.STATE_TURNING_ON;
    }

    protected void setToOn()
    {
        NativeUtil.sendBluetoothStateChange(BleManager.s_instance, m_nativeState, BluetoothAdapter.STATE_ON);
        m_nativeState = BluetoothAdapter.STATE_ON;
    }
}
