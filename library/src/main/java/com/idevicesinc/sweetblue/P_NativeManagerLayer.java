package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.Set;


interface P_NativeManagerLayer {

    int getConnectionState(P_NativeDeviceLayer device, int profile);
    boolean startDiscovery();
    boolean cancelDiscovery();
    boolean isManagerNull();
    boolean disable();
    boolean enable();
    boolean isMultipleAdvertisementSupported();
    void resetManager(Context context);
    int getState();
    int getBleState();
    String getAddress();
    String getName();
    void setName(String name);
    Set<BluetoothDevice> getBondedDevices();
    BluetoothAdapter getNativeAdaptor();
    BluetoothManager getNativeManager();
    P_NativeServerLayer openGattServer(Context context, P_BleServer_Listeners listeners);
    void startAdvertising(AdvertiseSettings settings, AdvertiseData adData, AdvertiseCallback callback);
    void stopAdvertising(AdvertiseCallback callback);
    boolean isLocationEnabledForScanning_byOsServices();
    boolean isLocationEnabledForScanning_byRuntimePermissions();
    boolean isLocationEnabledForScanning();
    boolean isBluetoothEnabled();
    void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback);
    void startMScan(int scanMode, Interval delay, L_Util.ScanCallback callback);
    boolean startLeScan(BluetoothAdapter.LeScanCallback callback);
    void stopLeScan(BluetoothAdapter.LeScanCallback callback);
    BluetoothDevice getRemoteDevice(String macAddress);

}
