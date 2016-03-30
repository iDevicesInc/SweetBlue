package com.idevicesinc.sweetblue;


public interface PI_BleStatusHelper
{
    boolean isLocationEnabledForScanning_byOsServices();
    boolean isLocationEnabledForScanning_byRuntimePermissions();
    boolean isLocationEnabledForScanning();
    boolean isBluetoothEnabled();

}
