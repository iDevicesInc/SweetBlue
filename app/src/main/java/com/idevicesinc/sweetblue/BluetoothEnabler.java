package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.utils.Utils;

import java.util.HashMap;

public class BluetoothEnabler
{
    private static final String SHARED_PREFERENCES_FILE = "location_permission_shared_pref_file";

    private static final String LOCATION_PERMISSION_SYS_DIALOG_WAS_SHOWN_KEY = "location_permission_shared_pref_key";

    private static final int BLUETOOTH_REQUEST_CODE = 6748;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1948;

    private static final int LOCATION_SERVICES_REQUEST_CODE = 1846;

    private static BluetoothEnabler sInstance;

    private static BluetoothEnablerStateUpdater mStateTracker;

    private static BleManager bleManager;

    private static BluetoothEnablerController mController;

    private static Activity mCallingActivity;

    private boolean isEnablerPerformingASystemCall = false;

    private boolean didJustPerformSystemRequest = false;

    private boolean didActivityReturnFromBleDialog = false;

    private boolean wasLocationPermissionSystemDialogShownOnce = false;

    static void enableBluetoothAndPrerequisites(Activity callingActivity, BluetoothEnablerController controller, BleManager manager)
    {
        if(sInstance == null)
        {
            sInstance = new BluetoothEnabler();
        }

        sInstance.init(callingActivity, controller, manager);

        sInstance.startEnablingProcess();
    }

    static void resumeEnabler(Please resumePlease)
    {
        sInstance.processPleaseResponse(resumePlease);
    }

    static void testInit(Activity callingActivity, BluetoothEnablerController controller, BleManager manager)
    {
        if(sInstance == null)
        {
            sInstance = new BluetoothEnabler();
        }

        sInstance.init(callingActivity, controller, manager);
    }

    void init(Activity callingActivity, BluetoothEnablerController controller, BleManager manager)
    {
        mCallingActivity = callingActivity;

        mController = controller;

        bleManager = manager;

        wasLocationPermissionSystemDialogShownOnce = mCallingActivity.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).getBoolean(LOCATION_PERMISSION_SYS_DIALOG_WAS_SHOWN_KEY, false);

        isEnablerPerformingASystemCall = false;

        didJustPerformSystemRequest = false;

        didActivityReturnFromBleDialog = false;

        wasLocationPermissionSystemDialogShownOnce = false;
    }

    private void startEnablingProcess()
    {
        hookupActivityLifecycleEvents();

        BluetoothEnablerState currentState = BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION;

        if(bleManager.isBleSupported() && bleManager.is(BleManagerState.ON))
        {
            currentState = BluetoothEnablerState.PROMPT_LOCATION_PERMISSIONS;

            if(Utils.isLocationEnabledForScanning_byRuntimePermissions(bleManager.getAppContext()))
            {
                currentState = BluetoothEnablerState.PROMPT_LOCATION_SERVICES;

                if(Utils.isLocationEnabledForScanning_byOsServices(bleManager.getAppContext()))
                {
                    currentState = BluetoothEnablerState.DONE;
                }
            }
        }

        mStateTracker = new BluetoothEnablerStateUpdater();

        mStateTracker.update(new BluetoothEnablerStateEvent(currentState, BluetoothEnablerStateEvent.Status.SUCCEEDED));
    }

    private void hookupActivityLifecycleEvents()
    {
        mCallingActivity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks()
        {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {

            }

            @Override
            public void onActivityStarted(Activity activity)
            {

            }

            @Override
            public void onActivityResumed(Activity activity)
            {
                if(activity.equals(mCallingActivity) && mStateTracker.getCurrentState() == BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION && isEnablerPerformingASystemCall == true && didActivityReturnFromBleDialog == false)
                {
                    didActivityReturnFromBleDialog = true;
                }
                else if(activity.equals(mCallingActivity) && mStateTracker.getCurrentState() == BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION && isEnablerPerformingASystemCall == true && didActivityReturnFromBleDialog == true)
                {
                    recordSystemCallEnd();

                    boolean isBluetoothEnabled = bleManager.isAny(BleManagerState.ON, BleManagerState.TURNING_ON);

                    mStateTracker.update(new BluetoothEnablerStateEvent(mStateTracker.getCurrentState().nextState(), isBluetoothEnabled ? BluetoothEnablerStateEvent.Status.SUCCEEDED : BluetoothEnablerStateEvent.Status.USER_DECLINED_SYSTEM_DIALOG));

                }
//                if(activity.equals(mCallingActivity) && mStateTracker.getCurrentState() == BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION && isEnablerPerformingASystemCall == true)
//                {
//                    recordSystemCallEnd();
//                }
//                else if(activity.equals(mCallingActivity) && didJustPerformSystemRequest && mStateTracker.getCurrentState() == BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION)
//                {
//                    boolean isBluetoothEnabled = bleManager.isAny(BleManagerState.ON, BleManagerState.TURNING_ON);
//
//                    mStateTracker.update(new BluetoothEnablerStateEvent(mStateTracker.getCurrentState().nextState(), isBluetoothEnabled ? BluetoothEnablerStateEvent.Status.SUCCEEDED : BluetoothEnablerStateEvent.Status.USER_DECLINED_SYSTEM_DIALOG));
//                }

                else if(activity.equals(mCallingActivity) && mStateTracker.getCurrentState() == BluetoothEnablerState.PROMPT_LOCATION_PERMISSIONS && isEnablerPerformingASystemCall == true)
                {
                    recordSystemCallEnd();

                    boolean isLocationEnabled = Utils.isLocationEnabledForScanning_byRuntimePermissions(mCallingActivity);

                    mStateTracker.update(new BluetoothEnablerStateEvent(mStateTracker.getCurrentState().nextState(), isLocationEnabled ? BluetoothEnablerStateEvent.Status.SUCCEEDED : BluetoothEnablerStateEvent.Status.USER_DECLINED_SYSTEM_DIALOG));
                }

                else if(activity.equals(mCallingActivity) && mStateTracker.getCurrentState() == BluetoothEnablerState.PROMPT_LOCATION_SERVICES && isEnablerPerformingASystemCall == true)
                {
                    recordSystemCallEnd();

                    boolean isLocationServicesEnabled = Utils.isLocationEnabledForScanning_byOsServices(mCallingActivity);

                    mStateTracker.update(new BluetoothEnablerStateEvent(mStateTracker.getCurrentState().nextState(), isLocationServicesEnabled ? BluetoothEnablerStateEvent.Status.SUCCEEDED : BluetoothEnablerStateEvent.Status.USER_DECLINED_SYSTEM_DIALOG));
                }

            }

            @Override
            public void onActivityPaused(Activity activity)
            {

            }

            @Override
            public void onActivityStopped(Activity activity)
            {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState)
            {

            }

            @Override
            public void onActivityDestroyed(Activity activity)
            {

            }
        });
    }

    private void updateBleManagerScanState()
    {
        if(mStateTracker.currentState == BluetoothEnablerState.DONE)
        {
            if(isStateSystemPropertyEnabled(BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION))
            {
                if(isStateSystemPropertyEnabled(BluetoothEnablerState.PROMPT_LOCATION_PERMISSIONS) && isStateSystemPropertyEnabled(BluetoothEnablerState.PROMPT_LOCATION_SERVICES))
                {
                    bleManager.setManagerBLEScanReady();
                }
            }
        }
    }

    private void processPleaseResponse(Please response)
    {
        if(response.action == Please.DO_NEXT)
        {
            if(response.messageDialog != null && !isStateSystemPropertyEnabled(mStateTracker.currentState))
            {
                didJustPerformSystemRequest = false;

                if(mStateTracker.getCurrentState() == BluetoothEnablerState.PROMPT_LOCATION_SERVICES && mStateTracker.historyMap.containsKey(BluetoothEnablerState.LOCATION_PERMISSION_RESULT))
                {
                    if(mStateTracker.historyMap.get(BluetoothEnablerState.PROMPT_LOCATION_SERVICES) == BluetoothEnablerStateEvent.Status.SUCCEEDED)
                    {

                    }
                    else
                    {
                        response.messageDialog.show();
                    }
                }
                else
                {
                    response.messageDialog.show();
                }
            }
            else if(!isStateSystemPropertyEnabled(mStateTracker.currentState))
            {
                if(response.toastText != null)
                    Toast.makeText(mCallingActivity, response.toastText, Toast.LENGTH_LONG).show();

                performSystemRequestForPromptState(mStateTracker.currentState);
            }
            else
            {
                mStateTracker.update(new BluetoothEnablerStateEvent(mStateTracker.getCurrentState().nextState(), BluetoothEnablerStateEvent.Status.ALREADY_ENABLED));
            }
        }
        else if(response.action == Please.SKIP_NEXT)
        {
            BluetoothEnablerState nextState = mStateTracker.getCurrentState().nextState().nextState(); //Skip the next state by getting the one after that

            mStateTracker.update(new BluetoothEnablerStateEvent(nextState, BluetoothEnablerStateEvent.Status.STATE_SKIPPED));
        }
        else if(response.action == Please.PAUSE)
        {

        }
        else if(response.action == Please.END)
        {
            mStateTracker.update(new BluetoothEnablerStateEvent(BluetoothEnablerState.DONE, BluetoothEnablerStateEvent.Status.ENABLER_ENDED));
        }
    }

    private boolean isStateSystemPropertyEnabled(BluetoothEnablerState state)
    {
        if(state == BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION)
        {
            return bleManager.isBleSupported() && bleManager.is(BleManagerState.ON);
        }
        else if(state == BluetoothEnablerState.PROMPT_LOCATION_PERMISSIONS)
        {
            return Utils.isLocationEnabledForScanning_byRuntimePermissions(mCallingActivity);
        }
        else if(state == BluetoothEnablerState.PROMPT_LOCATION_SERVICES)
        {
            return Utils.isLocationEnabledForScanning_byOsServices(mCallingActivity);
        }
        return true;
    }

    private void performSystemRequestForPromptState(BluetoothEnablerState state)
    {
        if(state == BluetoothEnablerState.PROMPT_BLUETOOTH_PERMISSION)
        {
            recordSystemCallStart();

            final Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            mCallingActivity.startActivityForResult(enableBluetoothIntent, BLUETOOTH_REQUEST_CODE);
        }
        else if(state == BluetoothEnablerState.PROMPT_LOCATION_PERMISSIONS)
        {
            if(Utils.isMarshmallow())
            {
                recordSystemCallStart();

                if(wasLocationPermissionSystemDialogShownOnce == true && M_Util.shouldShowRequestPermissionRationale(mCallingActivity) == false)
                {
                    sendUserToPhoneSettings();
                }
                else
                {
                    M_Util.requestPermissions(mCallingActivity, LOCATION_PERMISSION_REQUEST_CODE);

                    recordLocationPermissionSysDialogWasShown();
                }
            }
            else
            {
                mStateTracker.update(new BluetoothEnablerStateEvent(mStateTracker.getCurrentState().nextState(), BluetoothEnablerStateEvent.Status.NOT_NEEDED));
            }

        }
        else if(state == BluetoothEnablerState.PROMPT_LOCATION_SERVICES)
        {
            recordSystemCallStart();

            Intent locationServicesIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

            mCallingActivity.startActivityForResult(locationServicesIntent, LOCATION_SERVICES_REQUEST_CODE);
        }
    }

    private void sendUserToPhoneSettings()
    {

        final Intent intent = new Intent();

        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

        final Uri uri = Uri.fromParts("package", mCallingActivity.getPackageName(), null);

        intent.setData(uri);

        mCallingActivity.startActivityForResult(intent, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void recordLocationPermissionSysDialogWasShown()
    {
        SharedPreferences sharedPreferences = mCallingActivity.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor sharedPrefEditor = sharedPreferences.edit();

        sharedPrefEditor.putBoolean(LOCATION_PERMISSION_SYS_DIALOG_WAS_SHOWN_KEY, true);

        sharedPrefEditor.commit();
    }


    private void recordSystemCallStart()
    {
        isEnablerPerformingASystemCall = true;

        didJustPerformSystemRequest = false;
    }

    private void recordSystemCallEnd()
    {
        isEnablerPerformingASystemCall = false;

        didJustPerformSystemRequest = true;
    }

    public static class BluetoothEnablerStateEvent
    {
        private BluetoothEnablerState state;

        private Status status = Status.SUCCEEDED;

        private HashMap<BluetoothEnablerState, BluetoothEnablerStateEvent.Status> historyMap;

        BluetoothEnablerStateEvent(BluetoothEnablerState state, Status status)
        {
            this.state = state;

            this.status = status;
        }

        public boolean isFor(BluetoothEnablerState state)
        {
            return state.equals(this.state);
        }

        public boolean didSucceed()
        {
            return status == Status.SUCCEEDED;
        }

        /**
         * @return the reason the last state failed. This can be {@link Status#SUCCEEDED} if everything went fine in the
         * previous state.
         */
        public Status status()
        {
            return status;
        }

        /**
         * @return the history of the states and their statuses
         */
        public HashMap<BluetoothEnablerState, BluetoothEnablerStateEvent.Status> history()
        {
            return historyMap;
        }

        boolean isBleScanningReady()
        {
            if(Utils.isMarshmallow())
            {
                if(bleManager.isBleSupported() && bleManager.is(BleManagerState.ON) &&
                        Utils.isLocationEnabledForScanning_byRuntimePermissions(mCallingActivity) && Utils.isLocationEnabledForScanning_byOsServices(mCallingActivity))
                    return true;
                return false;
            }

            if(bleManager.isBleSupported() && bleManager.is(BleManagerState.ON))
                return true;

            return false;
        }

        boolean isClassicScanningReady()
        {
            return bleManager.is(BleManagerState.ON);
        }

        public enum Status
        {
            ALREADY_ENABLED,

            ENABLER_ENDED,

            NOT_NEEDED,

            STATE_SKIPPED,

            SUCCEEDED,

            USER_DECLINED_PROMPT_DIALOG,

            USER_DECLINED_SYSTEM_DIALOG
        }

    }

    private class BluetoothEnablerStateUpdater
    {
        private HashMap<BluetoothEnablerState, BluetoothEnablerStateEvent.Status> historyMap = new HashMap<>(7);

        private BluetoothEnablerState currentState;

        BluetoothEnablerStateUpdater()
        {
        }

        void update(BluetoothEnablerStateEvent event)
        {
            historyMap.put(event.state, event.status);

            currentState = event.state;

            event.historyMap = this.historyMap;

            //PDZ- if the enabler is done then we want to update the BleManager scan state before giving to the onEvent call on last time in the BluetoothEnablerController
            //Otherwise, we need to process the response we get back from the onEvent call.
            if(event.isFor(BluetoothEnablerState.DONE))
            {
                updateBleManagerScanState();

                mController.onEvent(event);
            }
            else
            {
                Please response = mController.onEvent(event);

                BluetoothEnabler.this.processPleaseResponse(response);
            }
        }

        BluetoothEnablerState getCurrentState()
        {
            return currentState;
        }
    }

    /**
     * These are the states of the {@link BluetoothEnabler}. The ordering of the states is as follows: {@link #PROMPT_BLUETOOTH_PERMISSION} -> {@link #BLUETOOTH_PERMISSION_RESULT} ->
     * {@link #PROMPT_LOCATION_PERMISSIONS} -> {@link #LOCATION_PERMISSION_RESULT} -> {@link #PROMPT_LOCATION_SERVICES} -> {@link #LOCATION_SERVICES_RESULT}. If you simply call,
     * {@link Please#doNext()} or {@link Please#promptNextWithDialog(String)} or {@link Please#promptNextWithCustomDialog_private(AlertDialog.Builder, BluetoothEnablerConfig)} then
     * the enabler will progress through the states. Call {@link Please#skipNext()} works as follows: If you are in {@link #BLUETOOTH_PERMISSION_RESULT} and you call {@link Please#skipNext()}
     * if will
     */
    public enum BluetoothEnablerState
    {
        /**
         * The state is used to prompt for the Bluetooth permission. {@link Please#promptNextWithDialog(String)}, {@link Please#promptNextWithDialog(String)}, or {@link Please#doNext()}
         * should be used to prompt the user to allow Bluetooth permissions with or without a reason dialog.
         */
        PROMPT_BLUETOOTH_PERMISSION,

        /**
         * This state follows the system dialog for Bluetooth if the {@link #PROMPT_BLUETOOTH_PERMISSION} state didn't call {@link Please#skipNext()} or {@link Please#doNext()}.
         * Check the result of the system dialog and your prompt dialog, if there was one, to know the results of whether or not the user cancelled intentionally. You can do this
         * by calling {@link BluetoothEnablerStateEvent#didSucceed()} and {@link BluetoothEnablerStateEvent#status()} to get the reason.
         */
        BLUETOOTH_PERMISSION_RESULT,

        /**
         * This state is used to prompt for the Location permissions. {@link Please#promptNextWithDialog(String)}, {@link Please#promptNextWithDialog(String)}, or {@link Please#doNext()}
         * should be used to prompt the user to allow the Location permission with or without a reason dialog.
         */
        PROMPT_LOCATION_PERMISSIONS,

        /**
         * This state follows the system dialog for Location permission if the {@link #PROMPT_LOCATION_PERMISSIONS} state wasn't skipped using {@link Please#end()} or {@link Please#skipNext()}
         * Check the result of the system dialog and your prompt dialog, if there was one, to know the results of whether or not the user cancelled intentionally. You can do this
         * by calling {@link BluetoothEnablerStateEvent#didSucceed()} and {@link BluetoothEnablerStateEvent#status()} to get the reason.
         */
        LOCATION_PERMISSION_RESULT,

        /**
         * This state is used to prompt for the Location services. {@link Please#promptNextWithDialog(String)}, {@link Please#promptNextWithDialog(String)}, or {@link Please#doNext()}
         * should be used to prompt the user to allow the Location permission with or without a reason dialog.
         */
        PROMPT_LOCATION_SERVICES,

        /**
         * This state follows the system dialog for Location permission if the {@link #PROMPT_LOCATION_SERVICES} state wasn't skipped using {@link Please#skipNext()} or {@link Please#end()}.
         * Check the result of the system dialog and your prompt dialog, if there was one, to know the results of whether or not the user cancelled intentionally. You can do this
         * by calling {@link BluetoothEnablerStateEvent#didSucceed()} and {@link BluetoothEnablerStateEvent#status()} to get the reason.
         */
        LOCATION_SERVICES_RESULT,

        /**
         * The ultimate state. This is reached immediately if {@link Please#end()} is called or if all previous states have been advanced through. This is used to finish up
         * anything that needs to be done after enabling everything.
         */
        DONE;

        public BluetoothEnablerState nextState()
        {
            return ordinal() + 1 == BluetoothEnablerState.values().length ? DONE : values()[ordinal() + 1];
        }
    }

    public static class Please
    {
        static final int DO_NEXT = 0;

        static final int SKIP_NEXT = 1;

        static final int END = 2;

        static final int PAUSE = 3;

        int action;

        String toastText;

        Dialog messageDialog;

        protected Please(int action)
        {
            this.action = action;
        }

        public static Please doNext()
        {
            return new Please(DO_NEXT);
        }

        public static Please skipNext()
        {
            return new Please(SKIP_NEXT);
        }

        public static Please pause()
        {
            return new Please(PAUSE);
        }

        public static Please end()
        {
            return new Please(END);
        }

        public static Please promptNextWithDialog(String dialogText)
        {
            return new Please(DO_NEXT).withDialog(dialogText);
        }

        public Please withToast(String toastText)
        {
            this.toastText = toastText;

            return this;
        }

        Please withDialog(String dialogText)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(mCallingActivity);

            builder.setCancelable(false);

            builder.setMessage(dialogText);

            String dialogAcceptString = sInstance.bleManager.getAppContext().getString(R.string.sb_accept);

            builder.setPositiveButton(dialogAcceptString, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    sInstance.performSystemRequestForPromptState(mStateTracker.getCurrentState());
                }
            });

            String dialogDenyString = sInstance.bleManager.getAppContext().getString(R.string.sb_deny);

            builder.setNegativeButton(dialogDenyString, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    sInstance.mStateTracker.update(new BluetoothEnablerStateEvent(sInstance.mStateTracker.getCurrentState().nextState(), BluetoothEnablerStateEvent.Status.USER_DECLINED_PROMPT_DIALOG));
                }
            });

            //This was needed for unit testing
            if(Looper.myLooper() == null)
                Looper.prepare();

            messageDialog = builder.create();

            return this;
        }

        Please withCustomDialog(AlertDialog.Builder customDialogBuilder, BluetoothEnablerConfig config)
        {
            customDialogBuilder.setCancelable(false);

            customDialogBuilder.setPositiveButton(config.dialogAcceptString, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    sInstance.performSystemRequestForPromptState(mStateTracker.getCurrentState());
                }
            });

            customDialogBuilder.setNegativeButton(config.dialogDenyString, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    sInstance.mStateTracker.update(new BluetoothEnablerStateEvent(sInstance.mStateTracker.getCurrentState().nextState(), BluetoothEnablerStateEvent.Status.USER_DECLINED_PROMPT_DIALOG));
                }
            });

            this.messageDialog = customDialogBuilder.create();

            return this;
        }

        static Please promptNextWithCustomDialog_private(AlertDialog.Builder customDialogBuilder, BluetoothEnablerConfig config)
        {

            return new Please(DO_NEXT).withCustomDialog(customDialogBuilder, config);
        }
    }
}
