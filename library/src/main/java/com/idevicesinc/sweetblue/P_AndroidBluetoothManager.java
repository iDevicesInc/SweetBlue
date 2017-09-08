package com.idevicesinc.sweetblue;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.os.DeadObjectException;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import static com.idevicesinc.sweetblue.BleManagerState.OFF;
import static com.idevicesinc.sweetblue.BleManagerState.ON;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class P_AndroidBluetoothManager implements P_NativeManagerLayer
{

    private BluetoothManager m_manager;
    private BluetoothAdapter m_adaptor;
    private BleManager m_bleManager;
    private static Method m_getLeState_marshmallow;
    private static Integer m_refState;
    private static Integer m_state;


    public final void setBleManager(BleManager mgr)
    {
        m_bleManager = mgr;
    }


    @Override
    public final int getConnectionState(P_NativeDeviceLayer device, int profile)
    {
        if (m_manager != null)
        {
            return m_manager.getConnectionState(device.getNativeDevice(), BluetoothGatt.GATT_SERVER);
        }
        return 0;
    }

    @Override
    public final boolean startDiscovery()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.startDiscovery();
        }
        return false;
    }

    @Override public final boolean isManagerNull()
    {
        return m_manager == null || m_adaptor == null;
    }

    @Override public final void resetManager(Context context)
    {
        m_manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        m_adaptor = m_manager.getAdapter();
    }

    @Override public final boolean disable()
    {
        if (m_adaptor != null)
            return m_adaptor.disable();

        return false;
    }

    @Override public final boolean enable()
    {
        if (m_adaptor != null)
            return m_adaptor.enable();

        return false;
    }

    @Override public final boolean isMultipleAdvertisementSupported()
    {
        return L_Util.isAdvertisingSupportedByChipset(m_adaptor);
    }

    @Override public final int getState()
    {
        if (m_adaptor != null)
            return m_adaptor.getState();

        return BleStatuses.STATE_OFF;
    }

    @Override public final int getBleState()
    {
        try
        {
            if (m_getLeState_marshmallow == null)
            {
                m_getLeState_marshmallow = BluetoothAdapter.class.getDeclaredMethod("getLeState");
            }
            m_refState = (Integer) m_getLeState_marshmallow.invoke(m_adaptor);
            m_state = m_adaptor.getState();
            // This is to fix an issue on the S7 (and perhaps other phones as well), where the OFF
            // state is never returned from the getLeState method. This is because the BLE_ states represent if LE only mode is on/off. This does NOT
            // relate to the Bluetooth radio being on/off. So, we check if STATE_BLE_ON, and the normal getState() method returns OFF, we
            // will return a state of OFF here.
            if (m_refState == BleStatuses.STATE_BLE_ON && m_state == OFF.getNativeCode())
            {
                return m_state;
            }
            else
            {
                // --- RB  > Not sure why this was setting to off, when the above handles the exception we want to handle. Commenting out for now.
//                m_refState = BleStatuses.STATE_OFF;
            }
            return m_refState;
        } catch (Exception e)
        {
            if (e instanceof DeadObjectException)
            {
                BleManager.UhOhListener.UhOh uhoh = BleManager.UhOhListener.UhOh.DEAD_OBJECT_EXCEPTION;
                m_bleManager.uhOh(uhoh);
            }
            return m_adaptor.getState();
        }
    }

    @Override public final String getAddress()
    {
        if (m_adaptor != null)
            return m_adaptor.getAddress();

        return BleDevice.NULL_MAC();
    }

    @Override
    public String getName()
    {
        if (m_adaptor != null)
            return m_adaptor.getName();
        return "";
    }

    @Override
    public void setName(String name)
    {
        if (m_adaptor != null)
            m_adaptor.setName(name);
    }

    @Override public final P_NativeServerLayer openGattServer(Context context, P_BleServer_Listeners listeners)
    {
        return new P_AndroidBleServer(m_manager.openGattServer(context, listeners));
    }

    @Override
    public final void startAdvertising(AdvertiseSettings settings, AdvertiseData adData, AdvertiseCallback callback)
    {
        final BluetoothLeAdvertiser ad = L_Util.getBluetoothLeAdvertiser(m_adaptor);
        if (ad != null)
        {
            ad.startAdvertising(settings, adData, callback);
        }
        else
        {
            m_bleManager.getLogger().e("Tried to start advertising, but the BluetoothLeAdvertiser was null!");
        }
    }

    @Override
    public final void stopAdvertising(AdvertiseCallback callback)
    {
        final BluetoothLeAdvertiser ad = L_Util.getBluetoothLeAdvertiser(m_adaptor);
        if (ad != null)
        {
            ad.stopAdvertising(callback);
        }
        else
        {
            m_bleManager.getLogger().e("Tried to stop advertising, but the BluetoothLeAdvertiser was null!");
        }
    }

    @Override public final Set<BluetoothDevice> getBondedDevices()
    {
        if (m_adaptor != null)
            return m_adaptor.getBondedDevices();

        return new HashSet<>(0);
    }

    @Override
    public final boolean cancelDiscovery()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.cancelDiscovery();
        }
        return false;
    }

    @Override public final BluetoothDevice getRemoteDevice(String macAddress)
    {
        if (m_adaptor != null)
            return m_adaptor.getRemoteDevice(macAddress);

        return null;
    }

    @Override
    public final BluetoothAdapter getNativeAdaptor()
    {
        return m_adaptor;
    }

    @Override
    public final BluetoothManager getNativeManager()
    {
        return m_manager;
    }

    @Override public final boolean isLocationEnabledForScanning_byOsServices()
    {
        return Utils.isLocationEnabledForScanning_byOsServices(m_bleManager.getApplicationContext());
    }

    @Override public final boolean isLocationEnabledForScanning_byRuntimePermissions()
    {
        return Utils.isLocationEnabledForScanning_byRuntimePermissions(m_bleManager.getApplicationContext());
    }

    @Override public final boolean isLocationEnabledForScanning()
    {
        return Utils.isLocationEnabledForScanning(m_bleManager.getApplicationContext());
    }

    @Override public final boolean isBluetoothEnabled()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.isEnabled();
        }
        else
        {
            // If the BleManager instance is somehow null here, we'll try to assign it now
            if (m_bleManager == null)
            {
                m_bleManager = BleManager.s_instance;
                
                // If the manager is still somehow null here, we'll just return false for the time being
                if (m_bleManager /*still*/ == null)
                    return false;
            }
            return m_bleManager.is(ON);
        }
    }

    @Override public final void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
    {
        L_Util.startNativeScan(m_adaptor, scanMode, delay, callback);
    }

    @Override public final void startMScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
    {
        M_Util.startNativeScan(m_adaptor, scanMode, delay, callback);
    }

    @Override public final boolean startLeScan(BluetoothAdapter.LeScanCallback callback)
    {
        if (m_adaptor != null)
            return m_adaptor.startLeScan(callback);

        return false;
    }

    @Override public final void stopLeScan(BluetoothAdapter.LeScanCallback callback)
    {
        if (m_adaptor != null)
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
        else
            m_bleManager.getLogger().e("Tried to stop scan (if it's even running), but the Bluetooth Adaptor is null!");
    }
}
