package com.idevicesinc.sweetblue.compat;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;

import java.util.ArrayList;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class L_Util
{

    private L_Util() {}


    public interface L_ScanCallback {
        void onScanResult(int callbackType, L_ScanResult result);
        void onBatchScanResults(List<L_ScanResult> results);
        void onScanFailed(int errorCode);
    }

    public static class L_ScanResult {
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

    private static L_ScanCallback m_UserCallback;

    private static L_ScanResult toLScanResult(ScanResult result) {
        L_ScanResult res = new L_ScanResult();
        res.device = result.getDevice();
        res.rssi = result.getRssi();
        res.record = result.getScanRecord().getBytes();
        return res;
    }

    private static List<L_ScanResult> toLScanResults(List<ScanResult> results) {
        int size = results.size();
        List<L_ScanResult> res = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            res.add(toLScanResult(results.get(i)));
        }
        return res;
    }

    private static ScanCallback m_callback = new ScanCallback()
    {
        @Override public void onScanResult(int callbackType, ScanResult result)
        {
            if (m_UserCallback != null) {
                m_UserCallback.onScanResult(callbackType, toLScanResult(result));
            }
        }

        @Override public void onBatchScanResults(List<ScanResult> results)
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

    static ScanCallback getNativeCallback() {
        return m_callback;
    }


    public static boolean requestMtu(BleDevice device, int mtu) {
        return device.getNativeGatt().requestMtu(mtu);
    }

    public static boolean isAdvertisingSupportedByChipset(BleManager mgr) {
        return mgr.getNativeAdapter().isMultipleAdvertisementSupported();
    }

    public static void stopNativeScan(BleManager mgr) {
        mgr.getNativeAdapter().getBluetoothLeScanner().stopScan(m_callback);
    }

    public static boolean requestConnectionPriority(BleDevice device, int mode) {
        return device.getNativeGatt().requestConnectionPriority(mode);
    }

    public static void startNativeScan(BleManager mgr, int scanMode, Interval scanReportDelay, L_ScanCallback listener) {

        final ScanSettings settings = buildSettings(mgr, scanMode, scanReportDelay).build();

        startScan(mgr, settings, listener);
    }

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

    static void startScan(BleManager mgr, ScanSettings scanSettings, L_ScanCallback listener) {
        m_UserCallback = listener;
        mgr.getNativeAdapter().getBluetoothLeScanner().startScan(null, scanSettings, m_callback);
    }

}
