package com.idevicesinc.sweetblue;

import android.app.AlertDialog;
import android.content.Context;

import com.idevicesinc.sweetblue.listeners.EnablerDoneListener;

public final class DefaultBluetoothEnablerController extends BluetoothEnablerController
{
    protected static BluetoothEnablerConfig mConfig;

    public DefaultBluetoothEnablerController(final BluetoothEnablerConfig config)
    {
        super();

        mConfig = config;
    }

    @Override
    public BluetoothEnabler.Please onEvent(BluetoothEnabler.BluetoothEnablerStateEvent event)
    {
        if(event.isFor(BluetoothEnabler.BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION))
        {
            if(mConfig.shouldShowBluetoothDialog)
            {
                if(mConfig.bluetoothCustomDialogBuilder != null)
                {
                    return Please.promptNextWithCustomDialog(mConfig.bluetoothCustomDialogBuilder);
                }
                return Please.promptNextWithDialog(mConfig.bluetoothDialogPromptMessage);
            }
        }
        else if(event.isFor(BluetoothEnabler.BluetoothEnablerState.BLUETOOTH_PERMISSION_RESULT))
        {
            if(event.didSucceed() || event.status() == BluetoothEnabler.BluetoothEnablerStateEvent.Status.ALREADY_ENABLED)
            {
                return Please.doNext();
            }

            return Please.end();
        }
        if(event.isFor(BluetoothEnabler.BluetoothEnablerState.PROMPT_LOCATION_PERMISSIONS))
        {
//            return Please.promptNextWithDialog(mConfig.locationPermissionAndServicesPromptMessage);

            if(mConfig.shouldShowCombinedLocationDialog)
            {
                if(mConfig.locationPermissionAndServicesDialogBuilder != null)
                {
                    return Please.promptNextWithCustomDialog(mConfig.locationPermissionAndServicesDialogBuilder);
                }
                return Please.promptNextWithDialog(mConfig.locationPermissionAndServicesPromptMessage);
            }
            else if(mConfig.shouldShowLocationPermissionDialog)
            {
                if(mConfig.locationPermissionDialogBuilder != null)
                {
                    return Please.promptNextWithCustomDialog(mConfig.locationPermissionDialogBuilder).withToast(mConfig.locationPermissionToastMessage);
                }
                return Please.promptNextWithDialog(mConfig.locationPermissionPromptMessage).withToast(mConfig.locationPermissionToastMessage);
            }
        }
        else if(event.isFor(BluetoothEnabler.BluetoothEnablerState.LOCATION_PERMISSION_RESULT))
        {
            if(event.didSucceed() || event.status() == BluetoothEnabler.BluetoothEnablerStateEvent.Status.ALREADY_ENABLED)
            {
                return Please.doNext();
            }

            return BluetoothEnabler.Please.end();
        }
        if(event.isFor(BluetoothEnabler.BluetoothEnablerState.PROMPT_LOCATION_SERVICES))
        {
            if(mConfig.shouldShowLocationServicesDialog)
            {
                if(mConfig.locationServicesDialogBuilder != null)
                {
                    return Please.promptNextWithCustomDialog(mConfig.locationServicesDialogBuilder).withToast(mConfig.locationServicesToastMessage);
                }
                return Please.promptNextWithDialog(mConfig.locationServicesPromptMessage).withToast(mConfig.locationServicesToastMessage);
            }
        }
        else if(event.isFor(BluetoothEnabler.BluetoothEnablerState.LOCATION_SERVICES_RESULT))
        {
            if(event.didSucceed() || event.status() == BluetoothEnabler.BluetoothEnablerStateEvent.Status.ALREADY_ENABLED)
            {
                return Please.doNext();
            }

            return Please.end();
        }
        else if(event.isFor(BluetoothEnabler.BluetoothEnablerState.DONE))
        {
            if (listener != null) {
                EnablerDoneListener.ScanTypeAvailable scanTypeAvailable = EnablerDoneListener.ScanTypeAvailable.NONE;
                if(event.isBleScanningReady())
                {
                    scanTypeAvailable = EnablerDoneListener.ScanTypeAvailable.BLE;
                }
                else if(event.isClassicScanningReady())
                {
                    scanTypeAvailable = EnablerDoneListener.ScanTypeAvailable.CLASSIC;
                }
                listener.onFinished(scanTypeAvailable);
            }
        }

        return BluetoothEnabler.Please.doNext();
    }

    void initStrings(Context context)
    {
        if(mConfig != null)
        {
            mConfig.initStrings(context);
        }
    }

    public static class Please extends BluetoothEnabler.Please
    {
        protected Please(int action)
        {
            super(action);
        }

        public static Please promptNextWithCustomDialog(AlertDialog.Builder customDialogBuilder)
        {
            return (Please) BluetoothEnabler.Please.promptNextWithCustomDialog_private(customDialogBuilder, DefaultBluetoothEnablerController.mConfig);
        }

    }

}
