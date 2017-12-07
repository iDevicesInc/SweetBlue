package com.idevicesinc.sweetblue.compat;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.util.Log;
import com.idevicesinc.sweetblue.BleAdvertisingSettings;
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

    public interface AdvertisingCallback
    {
        void onStartSuccess(BleAdvertisingSettings settings);
        void onStartFailure(int errorCode);
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

    private static ScanCallback m_UserScanCallback;
    private static AdvertisingCallback m_userAdvCallback;

    private static android.bluetooth.le.ScanCallback m_callback = new android.bluetooth.le.ScanCallback()
    {
        @Override public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result)
        {
            if (m_UserScanCallback != null) {
                m_UserScanCallback.onScanResult(callbackType, toLScanResult(result));
            }
        }

        @Override public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results)
        {
            if (m_UserScanCallback != null) {
                m_UserScanCallback.onBatchScanResults(toLScanResults(results));
            }
        }

        @Override public void onScanFailed(int errorCode)
        {
            if (m_UserScanCallback != null) {
                m_UserScanCallback.onScanFailed(errorCode);
            }
        }
    };

    private static final AdvertiseCallback m_nativeAdvertiseCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            if (m_userAdvCallback != null)
            {
                m_userAdvCallback.onStartSuccess(fromNativeSettings(settingsInEffect));
            }
        }

        @Override
        public void onStartFailure(int errorCode)
        {
            if (m_userAdvCallback != null)
            {
                m_userAdvCallback.onStartFailure(errorCode);
            }
        }
    };

    public static android.bluetooth.le.ScanCallback getNativeScanCallback() {
        return m_callback;
    }

    public static AdvertiseCallback getNativeAdvertisingCallback()
    {
        return m_nativeAdvertiseCallback;
    }

    static void setAdvCallback(AdvertisingCallback callback)
    {
        m_userAdvCallback = callback;
    }


    public static BleAdvertisingSettings fromNativeSettings(AdvertiseSettings settings)
    {
        return new BleAdvertisingSettings(BleAdvertisingSettings.BleAdvertisingMode.fromNative(settings.getMode()),
                BleAdvertisingSettings.BleTransmissionPower.fromNative(settings.getTxPowerLevel()),
                Interval.millis(settings.getTimeout()));
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
        if (adapter == null)
        {
            Log.e("ScanManager", "Tried to stop the scan, but the Bluetooth Adapter instance was null!");
            return;
        }

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

    public static void startNativeScan(BluetoothAdapter adapter, int scanMode, Interval scanReportDelay, ScanCallback listener) {

        final ScanSettings settings = buildSettings(adapter, scanMode, scanReportDelay).build();

        startScan(adapter, settings, listener);
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
        m_UserScanCallback = listener;
        mgr.getNativeAdapter().getBluetoothLeScanner().startScan(null, scanSettings, m_callback);
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
        m_UserScanCallback = listener;
        // Add a last ditch check to make sure the adapter isn't null before trying to start the scan.
        // We check in the task, but by the time we reach this method, it could have been shut off
        // Either the adapter, or the scanner object may be null, so we check it here
        if (adapter == null || adapter.getBluetoothLeScanner() == null)
        {
            m_callback.onScanFailed(android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
            return;
        }
        adapter.getBluetoothLeScanner().startScan(null, scanSettings, m_callback);
    }

    public static boolean startAdvertising(BluetoothAdapter adapter, AdvertiseSettings settings, AdvertiseData adData, AdvertisingCallback callback)
    {
        final BluetoothLeAdvertiser adv = adapter.getBluetoothLeAdvertiser();
        if (adv == null)
            return false;

        m_userAdvCallback = callback;
        adv.startAdvertising(settings, adData, m_nativeAdvertiseCallback);
        return true;
    }

    public static void stopAdvertising(BluetoothAdapter adapter)
    {
        if (adapter != null)
        {
            final BluetoothLeAdvertiser adv = adapter.getBluetoothLeAdvertiser();
            if (adv != null)
            {
                adv.stopAdvertising(m_nativeAdvertiseCallback);
            }
        }
    }





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

}
