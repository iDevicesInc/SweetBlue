package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.List;

import static com.idevicesinc.sweetblue.BleManagerState.BLE_SCAN_READY;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING_PAUSED;
import static com.idevicesinc.sweetblue.BleManagerState.STARTING_SCAN;



final class P_ScanManager
{

    private final BleManager mManager;
    private PreLollipopScanCallback mPreLollipopScanCallback;
    private PostLollipopScanCallback mPostLollipopScanCallback;
    private BleScanApi mCurrentApi;


    public P_ScanManager(BleManager mgr)
    {
        mManager = mgr;
        mCurrentApi = mgr.m_config.scanApi;
        switch (mCurrentApi)
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


    public final boolean startScan()
    {
        mManager.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleManagerState.SCANNING, true, SCANNING_PAUSED, false, STARTING_SCAN, false);
        switch (mManager.m_config.scanApi)
        {
            case CLASSIC:
                mCurrentApi = BleScanApi.CLASSIC;
                return mManager.getNativeAdapter().startDiscovery();
            case POST_LOLLIPOP:
                if (mManager.is(BLE_SCAN_READY))
                {
                    if (Utils.isLollipop())
                    {
                        mCurrentApi = BleScanApi.POST_LOLLIPOP;
                        return startScanPostLollipop();
                    }
                    else
                    {
                        mManager.getLogger().e("Tried to start post lollipop scan on a device not running lollipop or above! Defaulting to pre-lollipop scan instead.");
                        mCurrentApi = BleScanApi.PRE_LOLLIPOP;
                        return startScanPreLollipop();
                    }
                }
                else
                {
                    mManager.getLogger().e("Tried to start BLE scan, but scanning is not ready (most likely need to get permissions). Falling back to classic discovery.");
                    mCurrentApi = BleScanApi.CLASSIC;
                    return mManager.getNativeAdapter().startDiscovery();
                }
            case AUTO:
            case PRE_LOLLIPOP:
                mCurrentApi = BleScanApi.PRE_LOLLIPOP;
                return startScanPreLollipop();
            default:
                return false;
        }
    }

    public final void stopScan()
    {
        stopScan_private(true);
    }

    final void pauseScan()
    {
        stopScan_private(false);
    }

    final synchronized void postScanResult(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
    {
        mManager.getPostManager().postToUpdateThread(new Runnable()
        {
            @Override public void run()
            {
                mManager.onDiscoveredFromNativeStack(device, rssi, scanRecord);
            }
        });
    }

    public final void stopScan_private(boolean stopping)
    {
        switch (mCurrentApi)
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
            mManager.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false);
        }
        else
        {
            mManager.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false, SCANNING_PAUSED, true);
        }
    }

    private boolean startScanPreLollipop()
    {
        if (!mManager.getNativeAdapter().startLeScan(mPreLollipopScanCallback))
        {
            if (mManager.m_config.revertToClassicDiscoveryIfNeeded)
            {
                return mManager.getNativeAdapter().startDiscovery();
            }
            else
            {
                return false;
            }
        }
        else
        {
            return true;
        }
    }

    private boolean startScanPostLollipop()
    {
        int nativePowerMode;
        BleScanPower power = mManager.m_config.scanPower;
        if (power == BleScanPower.AUTO)
        {
            if (mManager.isForegrounded())
            {
                nativePowerMode = BleScanPower.HIGH_POWER.getNativeMode();
            }
            else
            {
                nativePowerMode = BleScanPower.MEDIUM_POWER.getNativeMode();
            }
        }
        else
        {
            if (power == BleScanPower.VERY_LOW_POWER)
            {
                if (!Utils.isMarshmallow())
                {
                    mManager.getLogger().e("BleScanPower set to VERY_LOW, but device is not running Marshmallow. Defaulting to LOW instead.");
                    power = BleScanPower.LOW_POWER;
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
        return true;
    }

    private void stopScanPreLollipop()
    {
        try
        {
            mManager.getNativeAdapter().stopLeScan(mPreLollipopScanCallback);
        } catch (Exception e)
        {
            mManager.getLogger().e("Got an exception (" + e.getClass().getSimpleName() + ") with a message of " + e.getMessage() + " when trying to stop a pre-lollipop scan!");
        }
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

    final boolean isPreLollipopScan()
    {
        return mPreLollipopScanCallback != null && mPostLollipopScanCallback == null;
    }

    final boolean isPostLollipopScan()
    {
        return mPreLollipopScanCallback == null && mPostLollipopScanCallback != null;
    }

    final boolean isClassicScan()
    {
        return mPreLollipopScanCallback == null && mPostLollipopScanCallback == null;
    }

}
