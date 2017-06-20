package com.idevicesinc.sweetblue.compat;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.Interval;


@TargetApi(Build.VERSION_CODES.M)
public class M_Util
{

    private M_Util() {}

    public static boolean shouldShowRequestPermissionRationale(Activity context) {
        return context.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static void requestPermissions(Activity context, int requestCode) {
        context.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
    }

    // TODO - Remove this in version 3.0
    @Deprecated
    public static void startNativeScan(BleManager mgr, int scanMode, Interval scanReportDelay, L_Util.ScanCallback listener) {
        final ScanSettings.Builder builder = L_Util.buildSettings(mgr, scanMode, scanReportDelay);

        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);

        final ScanSettings scanSettings = builder.build();

        L_Util.startScan(mgr, scanSettings, listener);
    }

    public static void startNativeScan(BluetoothAdapter adapter, int scanMode, Interval scanReportDelay, L_Util.ScanCallback listener) {
        final ScanSettings.Builder builder = L_Util.buildSettings(adapter, scanMode, scanReportDelay);

        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);

        final ScanSettings scanSettings = builder.build();

        L_Util.startScan(adapter, scanSettings, listener);
    }

    // TODO - Remove this in version 3.0
    @Deprecated
    public static BluetoothGatt connect(BleDevice device, boolean autoConnect, Context context, BluetoothGattCallback callback)
    {
        return connect(device.getNative(), autoConnect, context, callback);
    }

    public static BluetoothGatt connect(BluetoothDevice device, boolean autoConnect, Context context, BluetoothGattCallback callback)
    {
        return device.connectGatt(context, autoConnect, callback, BluetoothDevice.TRANSPORT_LE);
    }

}
