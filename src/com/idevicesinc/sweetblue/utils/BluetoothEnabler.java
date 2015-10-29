package com.idevicesinc.sweetblue.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.annotations.Advanced;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * BluetoothEnabler is used to handle the new logic for getting BLE scan results that is introduced with {@link android.os.Build.VERSION_CODES#M}.  With {@link android.os.Build.VERSION_CODES#M} you need might need to request
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} at runtime (behavior isn't too consistent)
 * as well as make sure that the user has turned on location services on their device.
 * <br><br>
 * To use this class create an instance of it in your activity and call the {@link #onActivityOrPermissionResult(int)} on the instance created in the two required methods. Nothing else needs to be done.
 */
public class BluetoothEnabler
{

    /**
     * Provide an implementation to {@link BluetoothEnabler#BluetoothEnabler#BluetoothEnabler(Activity, BluetoothEnablerListener)} to receive callbacks or simply use the provided class
     * {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.DefaultBluetoothEnablerListener} by caling {@link BluetoothEnabler#BluetoothEnabler(Activity)}. This listener will be the main
     * way of handling different enabling events and their results.
     *
     */
    public static interface BluetoothEnablerListener
    {
        /**
         * Enumerates changes in the "enabling" stage before a
         * Bluetooth LE scan is started. Used at {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.BluetoothEnablerEvent} to denote
         * what the current stage as well as in {@link BluetoothEnablerEvent#nextStage()} to give the following stage to the current one.
         */
        public static enum Stage
        {
            /**
             * The initial enabling stage. This stage begins the process and kicks off the following stage {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage#BLUETOOTH}.
             */
            START,

            /**
             * Used when checking if the device needs Bluetooth turned on and enabling Bluetooth if it is disabled.
             */
            BLUETOOTH,

            /**
             * Used when checking and requesting location permissions from the user. This stage will be skipped if the device isn't running {@link android.os.Build.VERSION_CODES#M}.
             */
            LOCATION_PERMISSION,

            /**
             * Used when checking if the device needs Location services turned on and enabling Location services if they are disabled.
             */
            LOCATION_SERVICES;


            private Stage next()
            {
                return ordinal() + 1 < values().length ? values()[ordinal() + 1] : LOCATION_SERVICES;
            }
        }

        /**
         * The Status of the current {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage}
         */
        public static enum Status implements UsesCustomNull
        {
            /**
             * If the current stage hasn't been assigned any of the above Statuses. If nothing has been done in the stage yet or it hasn't been skipped then it is NULL
             */
            NULL,

            /**
             * If the current stage has already been enabled on the device
             */
            ALREADY_ENABLED,

            /**
             * If the user was prompted to enable a setting and they responded by enabling it
             */
            ENABLED,

            /**
             * If the app wasn't compiled against {@link android.os.Build.VERSION_CODES#M} then the {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage#LOCATION_PERMISSION} isn't needed
             * because the underlying function call{@link Activity#requestPermissions(String[], int)} doesn't exist for this SDK version.
             */
            NOT_NEEDED,

            /**
             * If there was a dialog for the current state and the user declined (denied) the dialog.
             */
            CANCELLED_BY_DIALOG,

            /**
             * If the user accepted the dialog but didn't actually enable/grant the requested setting
             */
            CANCELLED_BY_INTENT,

            /**
             * If the programmer of the application chose to skip a stage
             */
            SKIPPED;

            /**
             * Returns <code>true</code> if <code>this</code> == {@link #NULL}.
             */
            @Override public boolean isNull()
            {
                return this == NULL;
            }
        }

        /**
         * Events passed to {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener#onEvent(BluetoothEnablerEvent)} so that the programmer can assign logic to the user's decision to
         * enable or disable certain required permissions and settings. Each event contains a {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage} which holds the current
         * enabling stage and a {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Status} of that stage. Stages which haven't been performed yet start off as
         * {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Status#NULL}, stages skipped are {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Status#SKIPPED} and
         * stages that don't need anything done are {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Status#ALREADY_ENABLED}. Otherwise, the status of the stage is whatever the user selected.
         */
        public static class BluetoothEnablerEvent extends Event
        {
            /**
             *
             * Returns the {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage} following the Stage for this event.
             */
            public Stage nextStage()
            {
                return m_stage.next();
            }

            /**
             * Returns the {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage} associated with this event.
             */
            public Stage stage()
            {
                return m_stage;
            }
            private final Stage m_stage;

            /**
             * Returns the {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Status} of the current Stage.
             */
            public Status status()
            {
                return m_status;
            }
            private final Status m_status;

            private BluetoothEnablerEvent(Stage stage, Status status)
            {
                m_stage = stage;
                m_status = status;
            }

            @Override public String toString()
            {
                return Utils_String.toString
                (
                        this.getClass(),
                        "stage", stage(),
                        "status", status()
                );
            }
        }

        /**
         * Return value for the interface method {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener#onEvent(BluetoothEnablerEvent)}.
         * Use static constructor methods to create instances.
         */
        public static class Please
        {
            final static int DO_NEXT = 0;
            final static int SKIP_NEXT = 1;
            final static int END = 3;
            final static int PAUSE = 4;

            private final static int NULL_REQUEST_CODE = 51214; //Large random int because we need to make sure that there is low probability that the user is using the same
            private final int m_stateCode;

            private  Activity m_activity = null;
            private String m_dialogText = "";
            private int m_requestCode = NULL_REQUEST_CODE;
            private boolean m_implicitActivityResultHandling = false;

            private Please(int stateCode)
            {
                m_stateCode = stateCode;
            }

            private boolean wasSkipped()
            {
                return m_stateCode == SKIP_NEXT;
            }

            private boolean shouldPopDialog()
            {
                return m_dialogText.equals("") || m_stateCode == PAUSE ? false : true;
            }

            /**
             * Perform the next {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage}.
             */
            public static Please doNext()
            {
                return new Please(DO_NEXT);
            }

            /**
             * Skip the next {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage} and move the following one.
             */
            public static Please skipNext()
            {
                return new Please(SKIP_NEXT);
            }

            /**
             * Bypass all remaining stages and move to the end of the last stage; enabler will finish at this point
             */
            public static Please stop()
            {
                return new Please(END);
            }

            /**
             * Pause the enabler. Call {@link #resume(Please)} to continue the process.
             */
            public static Please pause()
            {
                return new Please(PAUSE);
            }

            /**
             * If the next stage isn't skipped or {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Status#ALREADY_ENABLED} then pop a dialog before
             */
            public Please withDialog(String message)
            {
                m_dialogText = message;
                return this;
            }

            @Advanced
            public Please withActivity(Activity activity)
            {
                m_activity = activity;
                return this;
            }

            /**
             * Perform the next stage with the given requestCode
             */
            public Please withRequestCode(int requestCode)
            {
                m_requestCode = requestCode;
                return this;
            }

            public Please withImplicitActivityResultHandling()
            {
                m_implicitActivityResultHandling = true;
                return this;
            }
        }

        /**
         * Called after moving to the next {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener.Stage}
         */
        Please onEvent(final BluetoothEnablerEvent e);
    }

    /**
     * A default implementation of BluetoothEnablerListener used in {@link BluetoothEnabler#BluetoothEnabler(Activity)}. It provides a
     * basic implementation for use/example and can be overridden.
     */
    public static class DefaultBluetoothEnablerListener implements BluetoothEnablerListener
    {
        private BleManager m_bleMngr;

        public DefaultBluetoothEnablerListener(BleManager manager)
        {
            m_bleMngr = manager;
        }

        @Override
        public Please onEvent(BluetoothEnablerEvent e)
        {
            if(e.nextStage() == Stage.BLUETOOTH)
            {
                return Please.doNext().withImplicitActivityResultHandling();
            }
            else if(e.nextStage() == Stage.LOCATION_PERMISSION)
            {
                if(e.status() == Status.ALREADY_ENABLED || e.status() == Status.ENABLED)
                {
//                    return Please.doNext().withDialog("Location permissions are needed for Bluetooth LE scan to show available devices. If you would like to see your devices please allow the Location permission.").withImplicitActivityResultHandling();
                    if(!m_bleMngr.isLocationEnabledForScanning_byRuntimePermissions() && ! m_bleMngr.isLocationEnabledForScanning_byOsServices())
                    {
                        return Please.doNext().withImplicitActivityResultHandling().withDialog("Android Marshmallow (6.0+) requires location permission to the app to be able to scan for Bluetooth devices.\n\nMarshmallow also requires Location Services to improve Bluetooth device discovery.  While it is not required for use in this app, it is recommended to better discover devices.\n\nPlease accept to allow Location Permission and Services");
                    }
                    else if(!m_bleMngr.isLocationEnabledForScanning_byRuntimePermissions())
                    {
                        return Please.doNext().withImplicitActivityResultHandling().withDialog("Android Marshmallow (6.0+) requires location to be able to scan for Bluetooth devices. Please accept to allow Location Permission");
                    }
                    else if(!m_bleMngr.isLocationEnabledForScanning_byOsServices())
                    {
                        return Please.doNext().withImplicitActivityResultHandling().withDialog("Android Marshmallow (6.0+) requires Location Services for improved Bluetooth device scanning. While it is not required, it is recommended that Location Services are turned on to improve device discovery");
                    }
                    else
                    {
                        return Please.stop();
                    }
                }
                else if(e.status() == Status.CANCELLED_BY_DIALOG || e.status() == Status.CANCELLED_BY_INTENT)
                {
                    return Please.stop();
                }
            }
            else if(e.nextStage() == Stage.LOCATION_SERVICES && e.stage() != Stage.LOCATION_SERVICES)
            {
                if(e.status() == Status.ALREADY_ENABLED || e.status() == Status.ENABLED)
                {
                    return Please.doNext().withImplicitActivityResultHandling();
                }
                else if(e.status() == Status.CANCELLED_BY_DIALOG || e.status() == Status.CANCELLED_BY_INTENT)
                {
                    return Please.stop();
                }
            }
            else if(e.stage() == Stage.LOCATION_SERVICES)
            {
                return Please.stop();
            }
            //This will handle any SKIPPED steps since it will fall through all the if/else above
            return Please.doNext();
        }
    }

    private final BleManager m_bleManager;
    private final BluetoothEnablerListener m_startupListener;

    private final Activity m_passedActivity;

    private BluetoothEnablerListener.Please m_lastPlease = null;
    private BluetoothEnablerListener.Stage m_currentStage;
    private boolean m_pausedByEnabler;

    private Application.ActivityLifecycleCallbacks m_lifecycleCallback;

    /**
     * A constructor which uses an instance of {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.DefaultBluetoothEnablerListener} as the
     * enabler listener and will take an activity from which the BleManager will get gotten.
     */
    public BluetoothEnabler(Activity activity)
    {
        this(activity, new DefaultBluetoothEnablerListener(BleManager.get(activity)));
    }

    /**
     * A constructor which taken an activity and a custom implementation of {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerListener}. A BleManager will
     * be obtained from the passed activity.
     */
    public BluetoothEnabler(Activity activity, BluetoothEnablerListener enablerListener)
    {
        m_bleManager = BleManager.get(activity);
        m_passedActivity = activity;
        m_currentStage = BluetoothEnablerListener.Stage.START;
        BluetoothEnablerListener.BluetoothEnablerEvent startEvent = new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.NULL);

        m_lifecycleCallback = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                //Activity resumes after startActivity (bluetooth) gets called
                if(m_pausedByEnabler && activity == m_passedActivity && m_currentStage != BluetoothEnablerListener.Stage.START && m_lastPlease != null && m_lastPlease.m_implicitActivityResultHandling)
                {
                    BluetoothEnabler.this.onActivityOrPermissionResult(m_lastPlease.m_requestCode, true);
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
               if(m_currentStage != null && m_passedActivity == activity && m_currentStage == BluetoothEnablerListener.Stage.BLUETOOTH)
               {
                   m_pausedByEnabler = true;
               }
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        };

        m_passedActivity.getApplication().registerActivityLifecycleCallbacks(m_lifecycleCallback);
        m_startupListener = enablerListener;
        nextStage(startEvent);
    }

    private void nextStage(BluetoothEnablerListener.BluetoothEnablerEvent nextEvent)
    {
        if(m_currentStage == BluetoothEnablerListener.Stage.START)
        {
           updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.NULL);
        }
        else if(m_currentStage == BluetoothEnablerListener.Stage.BLUETOOTH)
        {
            if(nextEvent.status() == BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG);
            }
            else if(nextEvent.status() == BluetoothEnablerListener.Status.SKIPPED)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.SKIPPED);
            }
            else if(m_bleManager.isBleSupported() && !m_bleManager.is(BleManagerState.ON))
            {
                Activity resultActivity = m_lastPlease.m_activity != null ? m_lastPlease.m_activity : m_passedActivity;
                m_bleManager.turnOnWithIntent(resultActivity, m_lastPlease.m_requestCode);
            }
            else
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ALREADY_ENABLED);
            }
        }
        else if(m_currentStage == BluetoothEnablerListener.Stage.LOCATION_PERMISSION)
        {
            if(!Utils.isMarshmallow())
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.NOT_NEEDED);
            }
            else
            {
                if(nextEvent.status() == BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG)
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG);
                }
                else if(nextEvent.status() == BluetoothEnablerListener.Status.SKIPPED)
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.SKIPPED);
                }
                else if(!m_bleManager.isLocationEnabledForScanning_byRuntimePermissions())
                {
                    Activity resultActivity = m_lastPlease.m_activity != null ? m_lastPlease.m_activity : m_passedActivity;
                    m_bleManager.turnOnLocationWithIntent_forPermissions(resultActivity, m_lastPlease.m_requestCode);
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ALREADY_ENABLED);
                }
            }
        }
        else if(m_currentStage == BluetoothEnablerListener.Stage.LOCATION_SERVICES)
        {
            if(nextEvent.status() == BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG);
            }
            else if(nextEvent.status() == BluetoothEnablerListener.Status.SKIPPED)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.SKIPPED);
            }
            else if(!m_bleManager.isLocationEnabledForScanning_byOsServices())
            {
                Activity resultActivity = m_lastPlease.m_activity != null ? m_lastPlease.m_activity : m_passedActivity;
                m_bleManager.turnOnLocationWithIntent_forOsServices(resultActivity, m_lastPlease.m_requestCode);
            }
            else
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ALREADY_ENABLED);
            }
        }
    }

    private boolean isNextStageAlreadyEnabled(BluetoothEnablerListener.Stage stage)
    {
        if(stage == BluetoothEnablerListener.Stage.BLUETOOTH)
        {
            return m_bleManager.isBleSupported() && m_bleManager.is(BleManagerState.ON);
        }
        else if(stage == BluetoothEnablerListener.Stage.LOCATION_PERMISSION)
        {
            return m_bleManager.isLocationEnabledForScanning_byRuntimePermissions();
        }
        else if(stage == BluetoothEnablerListener.Stage.LOCATION_SERVICES)
        {
            return m_bleManager.isLocationEnabledForScanning_byOsServices();
        }
        return true;
    }

    private void handlePleaseResponse()
    {
        if(m_lastPlease.m_activity != null)
        {
            m_lastPlease.m_activity.startActivity(new Intent());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(m_passedActivity);
        if(m_lastPlease.shouldPopDialog() && m_currentStage == BluetoothEnablerListener.Stage.BLUETOOTH && !m_lastPlease.wasSkipped())
        {
            if(m_lastPlease.m_stateCode == BluetoothEnablerListener.Please.END)
            {
                builder.setMessage(m_lastPlease.m_dialogText);
                builder.setNeutralButton("OK"
                        , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishPleaseResponse();
                    }
                });
                builder.show();
            }
            else
            {
                builder.setMessage(m_lastPlease.m_dialogText);
                builder.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishPleaseResponse(true);
                    }
                });

                builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishPleaseResponse();
                    }
                });
                builder.show();
            }
        }
        //PDZ- Handles popping a dialog after the last stage, LOCATION_SERVICES returns. Not currently supporting this feature
//        else if(m_lastPlease.shouldPopDialog() && m_currentStage == BluetoothEnablerListener.Stage.LOCATION_SERVICES && !m_lastPlease.wasSkipped())
//        {
//            builder.setMessage(m_lastPlease.m_dialogText);
//            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                }
//            });
//            builder.show();
//        }
        else
        {
            finishPleaseResponse();
        }
    }


    private void finishPleaseResponse()
    {
        finishPleaseResponse(false);
    }

    private void finishPleaseResponse(boolean wasCancelledByDialog)
    {
        if(m_lastPlease.m_stateCode == BluetoothEnablerListener.Please.PAUSE)
        {

        }
        else if(m_lastPlease.m_stateCode == BluetoothEnablerListener.Please.DO_NEXT )
        {
            m_currentStage = m_currentStage.next();
            BluetoothEnablerListener.BluetoothEnablerEvent nextEvent = wasCancelledByDialog ? new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.NULL);
            nextStage(nextEvent);
        }
        else if(m_lastPlease.m_stateCode == BluetoothEnablerListener.Please.SKIP_NEXT)
        {
            m_currentStage = m_currentStage.next();
            BluetoothEnablerListener.BluetoothEnablerEvent nextEvent = wasCancelledByDialog ? new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.SKIPPED);
            nextStage(nextEvent);
        }
        else if(m_lastPlease.m_stateCode == BluetoothEnablerListener.Please.END)
        {
            m_currentStage = BluetoothEnablerListener.Stage.LOCATION_SERVICES;
            m_passedActivity.getApplication().unregisterActivityLifecycleCallbacks(m_lifecycleCallback);
        }
    }

    private void updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status newStatus)
    {
        BluetoothEnablerListener.BluetoothEnablerEvent currentEvent = new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, newStatus);
        m_lastPlease = m_startupListener.onEvent(currentEvent);
        handlePleaseResponse();
    }


    /**
     * A potentially-required method to be placed in your activity's {@link Activity#onRequestPermissionsResult(int, String[], int[])} and {@link Activity#onActivityResult(int, int, Intent)} methods.
     * This method will re-connect the enabler after the app is re-entered. Otherwise, the enabler won't continue unless {@link BluetoothEnablerListener.Please#withImplicitActivityResultHandling()} is called.
     */
    public void onActivityOrPermissionResult(int requestCode)
    {
        onActivityOrPermissionResult(requestCode, false);
    }

    private void onActivityOrPermissionResult(int requestCode, boolean calledImplicitly)
    {
        if(requestCode == m_lastPlease.m_requestCode && m_lastPlease.m_implicitActivityResultHandling == calledImplicitly)
        {
            if(m_currentStage == BluetoothEnablerListener.Stage.BLUETOOTH)
            {
                if(m_bleManager.isBleSupported() && !m_bleManager.is(BleManagerState.ON))
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_INTENT);
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ENABLED);
                }
            }
            else if(m_currentStage == BluetoothEnablerListener.Stage.LOCATION_PERMISSION)
            {
                if(!m_bleManager.isLocationEnabledForScanning_byRuntimePermissions())
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_INTENT);
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ENABLED);
                }
            }
            else if(m_currentStage == BluetoothEnablerListener.Stage.LOCATION_SERVICES)
            {
                if(!m_bleManager.isLocationEnabledForScanning_byOsServices())
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_INTENT);
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ENABLED);
                }
            }
        }
    }

    /**
     * Resume the enabler with the given Please. Enabler will continue where is left off.
     */
    public void resume(BluetoothEnablerListener.Please please)
    {
        m_lastPlease = please;
        handlePleaseResponse();
    }
}