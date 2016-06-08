package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.List;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.SCAN_PAUSED;
import static com.idevicesinc.sweetblue.BleManagerState.STARTING_SCAN;


class P_ScanManager
{

    private final BleManager mManager;
    private PreLollipopScanCallback mPreLollipopScanCallback;
    private PostLollipopScanCallback mPostLollipopScanCallback;


    public P_ScanManager(BleManager mgr)
    {
        mManager = mgr;
        BleScanAPI scanAPI = mgr.mConfig.scanApi;
        switch (scanAPI)
        {
            case CLASSIC:
                mPreLollipopScanCallback = null;
                mPostLollipopScanCallback = null;
                break;
            case PRE_LOLLIPOP:
                mPreLollipopScanCallback = new PreLollipopScanCallback();
                mPostLollipopScanCallback = null;
                break;
            case POST_LOLLIPOP:
                if(Utils.isLollipop())
                {
                    mPreLollipopScanCallback = null;
                    mPostLollipopScanCallback = new PostLollipopScanCallback();
                }
                else
                {
                    mPreLollipopScanCallback = new PreLollipopScanCallback();
                    mPostLollipopScanCallback = null;
                }
                break;
            default:
                mPreLollipopScanCallback = new PreLollipopScanCallback();
                mPostLollipopScanCallback = null;
                break;
        }
    }


    public void startScan()
    {
        mManager.getStateTracker().update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleManagerState.SCANNING, true, SCAN_PAUSED, false, STARTING_SCAN, false);
        switch (mManager.mConfig.scanApi)
        {
            case CLASSIC:
                mManager.getNativeAdapter().startDiscovery();
                break;
            case POST_LOLLIPOP:
                if (Utils.isLollipop())
                {
                    startScanPostLollipop();
                }
                else
                {
                    mManager.getLogger().e("Tried to start post lollipop scan on a device not running lollipop or above! Defaulting to pre-lollipop scan instead.");
                    startScanPreLollipop();
                }
                break;
            case AUTO:
            case PRE_LOLLIPOP:
                startScanPreLollipop();
        }
    }

    public void stopScan()
    {
        stopScan_private(true);
    }

    void pauseScan()
    {
        stopScan_private(false);
    }

    void postScanResult(BluetoothDevice device, int rssi, byte[] scanRecord)
    {
        if (mManager.isOnSweetBlueThread())
        {
            mManager.onDiscoveredFromNativeStack(device, rssi, scanRecord);
        }
        else
        {
            final BluetoothDevice device_native = device;
            final int mRssi = rssi;
            final byte[] mScanRecord = scanRecord;
            mManager.mPostManager.postToUpdateThread(new Runnable()
            {
                @Override public void run()
                {
                    mManager.onDiscoveredFromNativeStack(device_native, mRssi, mScanRecord);
                }
            });
        }
    }

    public void stopScan_private(boolean stopping)
    {
        switch (mManager.mConfig.scanApi)
        {
            case CLASSIC:
                mManager.getNativeAdapter().cancelDiscovery();
                break;
            case POST_LOLLIPOP:
                if (Utils.isLollipop())
                {
                    stopScanPostLollipop();
                }
                else
                {
                    stopScanPreLollipop();
                }
                break;
            case AUTO:
            case PRE_LOLLIPOP:
                stopScanPreLollipop();
        }
        if (stopping)
        {
            mManager.getStateTracker().update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false);
        }
        else
        {
            mManager.getStateTracker().update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false, SCAN_PAUSED, true);
        }
    }

    private void startScanPreLollipop()
    {
        mManager.getNativeAdapter().startLeScan(mPreLollipopScanCallback);
    }

    private void startScanPostLollipop()
    {
        int nativePowerMode;
        BleScanPower power = mManager.mConfig.scanPower;
        if (power == BleScanPower.AUTO)
        {
            if (mManager.isForegrounded())
            {
                nativePowerMode = BleScanPower.HIGH.getNativeMode();
            }
            else
            {
                nativePowerMode = BleScanPower.MEDIUM.getNativeMode();
            }
        }
        else
        {
            if (power == BleScanPower.VERY_LOW)
            {
                if (!Utils.isMarshmallow())
                {
                    mManager.getLogger().e("BleScanPower set to VERY_LOW, but device is not running Marshmallow. Defaulting to LOW instead.");
                    power = BleScanPower.LOW;
                }
            }
            nativePowerMode = power.getNativeMode();
        }
        if (Utils.isMarshmallow())
        {
            M_Util.startNativeScan(mManager, nativePowerMode, Interval.ZERO, mPostLollipopScanCallback);
        }
        else
        {
            L_Util.startNativeScan(mManager, nativePowerMode, Interval.ZERO, mPostLollipopScanCallback);
        }
    }

    private void stopScanPreLollipop()
    {
        mManager.getNativeAdapter().stopLeScan(mPreLollipopScanCallback);
    }

    private void stopScanPostLollipop()
    {
        L_Util.stopNativeScan(mManager);
    }




    private class PreLollipopScanCallback implements BluetoothAdapter.LeScanCallback
    {

        @Override public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            postScanResult(device, rssi, scanRecord);
        }
    }

    private class PostLollipopScanCallback implements L_Util.ScanCallback
    {

        @Override public void onScanResult(int callbackType, L_Util.ScanResult result)
        {
            postScanResult(result.getDevice(), result.getRssi(), result.getRecord());
        }

        @Override public void onBatchScanResults(List<L_Util.ScanResult> results)
        {
            mManager.getLogger().d("Got batched scan results.");
            for (L_Util.ScanResult res : results)
            {
                postScanResult(res.getDevice(), res.getRssi(), res.getRecord());
            }
        }

        @Override public void onScanFailed(int errorCode)
        {
            mManager.getLogger().e(Utils_String.concatStrings("Post lollipop scan failed with error code ", String.valueOf(errorCode)));
        }
    }

    boolean isPreLollipopScan()
    {
        return mPreLollipopScanCallback != null && mPostLollipopScanCallback == null;
    }

    boolean isPostLollipopScan()
    {
        return mPreLollipopScanCallback == null && mPostLollipopScanCallback != null;
    }

    boolean isClassicScan()
    {
        return mPreLollipopScanCallback == null && mPostLollipopScanCallback == null;
    }

}
