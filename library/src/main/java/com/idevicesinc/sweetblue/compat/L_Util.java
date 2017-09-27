package com.idevicesinc.sweetblue.compat;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.Interval;

import java.util.ArrayList;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class L_Util
{

    private L_Util() {}


    public interface ScanCallback
    {
        void onScanResult(int callbackType, ScanResult result);
        void onBatchScanResults(List<ScanResult> results);
        void onScanFailed(int errorCode);
    }

    public static class ScanResult
    {
        private BluetoothDevice device;
        private int rssi;
        private byte[] record;

        public BluetoothDevice getDevice() {
            return device;
        }

        public int getRssi() {
            return rssi;
        }

        public byte[] getRecord() {
            return record;
        }
    }

    private static ScanCallback m_UserCallback;

    private static ScanResult toLScanResult(android.bluetooth.le.ScanResult result) {
        ScanResult res = new ScanResult();
        res.device = result.getDevice();
        res.rssi = result.getRssi();
        res.record = result.getScanRecord().getBytes();
        return res;
    }

    private static List<ScanResult> toLScanResults(List<android.bluetooth.le.ScanResult> results) {
        int size = results.size();
        List<ScanResult> res = new ArrayList<ScanResult>(size);
        for (int i = 0; i < size; i++) {
            res.add(toLScanResult(results.get(i)));
        }
        return res;
    }

    private static android.bluetooth.le.ScanCallback m_callback = new android.bluetooth.le.ScanCallback()
    {
        @Override public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result)
        {
            if (m_UserCallback != null) {
                m_UserCallback.onScanResult(callbackType, toLScanResult(result));
            }
        }

        @Override public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results)
        {
            if (m_UserCallback != null) {
                m_UserCallback.onBatchScanResults(toLScanResults(results));
            }
        }

        @Override public void onScanFailed(int errorCode)
        {
            if (m_UserCallback != null) {
                m_UserCallback.onScanFailed(errorCode);
            }
        }
    };

    public static android.bluetooth.le.ScanCallback getNativeCallback() {
        return m_callback;
    }


    // TODO - Remove this in version 3.0
    @Deprecated
    public static boolean requestMtu(BleDevice device, int mtu) {
        return device.getNativeGatt().requestMtu(mtu);
    }

    public static boolean requestMtu(BluetoothGatt gatt, int mtu) {
        return gatt.requestMtu(mtu);
    }

    // TODO - Remove this for version 3.0
    @Deprecated
    public static boolean isAdvertisingSupportedByChipset(BleManager mgr) {
        return mgr.getNativeAdapter().isMultipleAdvertisementSupported();
    }

    public static boolean isAdvertisingSupportedByChipset(BluetoothAdapter adapter) {
        return adapter.isMultipleAdvertisementSupported();
    }

    public static BluetoothLeAdvertiser getBluetoothLeAdvertiser(BluetoothAdapter adapter)
    {
        return adapter.getBluetoothLeAdvertiser();
    }

    // TODO - Remove this for version 3.0
    @Deprecated
    public static void stopNativeScan(BleManager mgr) {
        mgr.getNativeAdapter().getBluetoothLeScanner().stopScan(m_callback);
    }

    public static void stopNativeScan(BluetoothAdapter adapter) {
        final BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner != null)
            scanner.stopScan(m_callback);
        else
            Log.w("ScanManager", "Tried to stop the scan, but the BluetoothLeScanner instance was null. This implies the scanning has stopped already.");
    }


    // TODO - Remove this for version 3.0
    @Deprecated
    public static boolean requestConnectionPriority(BleDevice device, int mode)
    {
        return requestConnectionPriority(device.getNativeGatt(), mode);
    }

    public static boolean requestConnectionPriority(BluetoothGatt gatt, int mode) {
        return gatt.requestConnectionPriority(mode);
    }

    // TODO - Remove this in version 3.0
    @Deprecated
    public static void startNativeScan(BleManager mgr, int scanMode, Interval scanReportDelay, ScanCallback listener) {

        final ScanSettings settings = buildSettings(mgr, scanMode, scanReportDelay).build();

        startScan(mgr, settings, listener);
    }

    // TODO - Remove this in version 3.0
    @Deprecated
    static ScanSettings.Builder buildSettings(BleManager mgr, int scanMode, Interval scanReportDelay) {
        final ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(scanMode);

        if( mgr.getNativeAdapter().isOffloadedScanBatchingSupported() )
        {
            final long scanReportDelay_millis = false == Interval.isDisabled(scanReportDelay) ? scanReportDelay.millis() : 0;
            builder.setReportDelay(scanReportDelay_millis);
        }
        else
        {
            builder.setReportDelay(0);
        }
        return builder;
    }

    // TODO - Remove this in version 3.0
    @Deprecated
    static void startScan(BleManager mgr, ScanSettings scanSettings, ScanCallback listener) {
        m_UserCallback = listener;
        mgr.getNativeAdapter().getBluetoothLeScanner().startScan(null, scanSettings, m_callback);
    }


    public static void startNativeScan(BluetoothAdapter adapter, int scanMode, Interval scanReportDelay, ScanCallback listener) {

        final ScanSettings settings = buildSettings(adapter, scanMode, scanReportDelay).build();

        startScan(adapter, settings, listener);
    }

    static ScanSettings.Builder buildSettings(BluetoothAdapter adapter, int scanMode, Interval scanReportDelay) {
        final ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(scanMode);

        if( adapter.isOffloadedScanBatchingSupported() )
        {
            final long scanReportDelay_millis = false == Interval.isDisabled(scanReportDelay) ? scanReportDelay.millis() : 0;
            builder.setReportDelay(scanReportDelay_millis);
        }
        else
        {
            builder.setReportDelay(0);
        }
        return builder;
    }

    static void startScan(BluetoothAdapter adapter, ScanSettings scanSettings, ScanCallback listener) {
        m_UserCallback = listener;
        // Add a last ditch check to make sure the adapter isn't null before trying to start the scan.
        // We check in the task, but by the time we reach this method, it could have been shut off
        if (adapter == null)
        {
            m_callback.onScanFailed(android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
            return;
        }
        adapter.getBluetoothLeScanner().startScan(null, scanSettings, m_callback);
    }

}
