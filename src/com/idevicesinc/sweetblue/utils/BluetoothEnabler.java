package com.idevicesinc.sweetblue.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.annotations.Advanced;

/**
 * BluetoothEnabler is used to handle the new logic for getting BLE scan results that is introduced with {@link android.os.Build.VERSION_CODES#M}.  With {@link android.os.Build.VERSION_CODES#M} you need might need to request
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} at runtime as well as make sure that the user has turned on location services on their device.
 * <br><br>
 * To use this class call {@link BluetoothEnabler#start(Activity)} or {@link BluetoothEnabler#start(Activity, BluetoothEnablerFilter)} in your activity and call the {@link #onActivityOrPermissionResult(int)} in {@link Activity#onActivityResult(int, int, Intent)} and
 * {@link Activity#onRequestPermissionsResult(int, String[], int[])} if you want to handle it yourself. Nothing else needs to be done.
 */
public class BluetoothEnabler
{
	/**
	 * Overload of {@link #start(Activity)} that uses an instance {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.DefaultBluetoothEnablerFilter}.
	 */
	public static BluetoothEnabler start(final Activity activity)
	{
		return new BluetoothEnabler(activity);
	}

	/**
	 * Kicks off the complex flow needed to fully enable Bluetooth on Build versions greater than or equal to {@link android.os.Build.VERSION_CODES#M}.
	 */
	public static BluetoothEnabler start(final Activity activity, final BluetoothEnablerFilter filter)
	{
		return new BluetoothEnabler(activity, filter);
	}

    /**
     * Provide an implementation to {@link BluetoothEnabler#BluetoothEnabler#start(Activity, BluetoothEnablerFilter)} to
     * receive callbacks or simply use the provided class {@link DefaultBluetoothEnablerFilter} by calling {@link BluetoothEnabler#start(Activity)}.
     * This filter will be the main way of handling different enabling events and their results.
     */
    public static interface BluetoothEnablerFilter
    {
        /**
         * Enumerates changes in the "enabling" {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Stage} before a
         * Bluetooth LE scan is started. Used at {@link BluetoothEnablerFilter.BluetoothEnablerEvent} to denote
         * what the current stage is as well as the {@link BluetoothEnablerEvent#nextStage()} to give the following stage to the current one.
         * The order of the stages is: START -> BLUETOOTH -> LOCATION_PERMISSION -> LOCATION_SERVICES -> NULL.
         */
        public static enum Stage implements UsesCustomNull
        {
            /**
             * The final stage once the enabler has finished or has been stopped.
             */
            NULL,

            /**
             * The initial enabling stage. This stage begins the process and kicks off the following stage {@link BluetoothEnablerFilter.Stage#BLUETOOTH}.
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
             * Used when checking if the device needs Location services turned on and enabling Location services if they are disabled. This step isn't necessarily needed for overall
             * Bluetooth scanning. It is only needed for Bluetooth Low Energy scanning in {@link android.os.Build.VERSION_CODES#M}; otherwise, SweetBlue will default to classic scanning.
             */
            LOCATION_SERVICES;


            private Stage next()
            {
                return ordinal() + 1 < values().length ? values()[ordinal() + 1] : NULL;
            }

            /**
             * Returns whether or not the {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Stage} is {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Stage#NULL}.
             */
            public boolean isNull()
            {
                return this == NULL;
            }

            /**
             * Returns if the {@link BluetoothEnabler} is on the last {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Stage}
             */
            public boolean isLast()
            {
                return this == LOCATION_SERVICES;
            }
        }

        /**
         * The Status of the current {@link BluetoothEnablerFilter.Stage}
         */
        public static enum Status implements UsesCustomNull
        {
            /**
             * If the current stage hasn't been assigned any of the other Statuses. If nothing has been done in the stage yet or it hasn't been skipped then it is {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Status#NULL}.
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
             * If the app wasn't compiled against {@link android.os.Build.VERSION_CODES#M} then the {@link BluetoothEnablerFilter.Stage#LOCATION_PERMISSION} isn't needed
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
         * Events passed to {@link BluetoothEnablerFilter#onEvent(BluetoothEnablerEvent)} so that the programmer can assign logic to the user's decision to
         * enable or disable certain required permissions and settings. Each event contains a {@link BluetoothEnablerFilter.Stage} which holds the current
         * enabling stage and a {@link BluetoothEnablerFilter.Status} of that stage. Stages which haven't been performed yet start off as
         * {@link BluetoothEnablerFilter.Status#NULL}, stages skipped are {@link BluetoothEnablerFilter.Status#SKIPPED} and
         * stages that don't need anything done are {@link BluetoothEnablerFilter.Status#ALREADY_ENABLED}. Otherwise, the status of the stage is whatever the user selected.
         */
        public static class BluetoothEnablerEvent extends Event
        {
            /**
             * Returns the {@link BluetoothEnablerFilter.Stage} which follows the current stage of this event.
             */
            public Stage nextStage()  {  return m_stage.next();  }

            /**
             * Returns the {@link BluetoothEnablerFilter.Stage} associated with this event.
             */
            public Stage stage() { return m_stage; }
            private final Stage m_stage;

            /**
             * Returns the {@link BluetoothEnablerFilter.Status} of the current Stage.
             */
            public Status status() { return m_status; }
            private final Status m_status;

            /**
             * Returns the {@link Activity} associated with the Event
             */
            public Activity activity() { return m_activity; }
            public final Activity m_activity;

            private BluetoothEnablerEvent(Activity activity, Stage stage, Status status)
            {
                m_stage = stage;
                m_status = status;
                m_activity = activity;
            }

            /**
             * Returns whether the passed {@link BluetoothEnablerFilter.Stage} is enabled.
             */
            public boolean isEnabled(Stage stage)
            {
                return BluetoothEnabler.isEnabled(BleManager.get(m_activity), stage);
            }

            /**
             * Returns the {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.BluetoothEnablerEvent} as a string
             */
            @Override public String toString()
            {
                return Utils_String.toString
                (
                    this.getClass(),
                    "stage",        stage(),
                    "status",       status(),
                    "nextStage",    nextStage()
                );
            }
        }

        /**
         * Return value for the interface method {@link BluetoothEnablerFilter#onEvent(BluetoothEnablerEvent)}.
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
            private String m_toastText = "";
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

            private boolean shouldShowToast()
            {
                return !m_toastText.equals("");
            }

            /**
             * Perform the next {@link BluetoothEnablerFilter.Stage}.
             */
            public static Please doNext()
            {
                return new Please(DO_NEXT);
            }

            /**
             * Skip the next {@link BluetoothEnablerFilter.Stage} and move to the following one.
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
             * Pauses the enabler. Calling this will cause the enabler to pause but not stop. This call allows you to do something in the activity before you resume the
             * enabler and move to the next stage or stop it all together. Call {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler#resume(BluetoothEnablerFilter.Please)} to continue the process.
             * For example, calling {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler#resume(BluetoothEnablerFilter.Please)} with Please.skipNext() will resume the enabler but skip the next stage.
             */
            public static Please pause()
            {
                return new Please(PAUSE);
            }

            /**
             * If the next stage isn't skipped or {@link BluetoothEnablerFilter.Status#ALREADY_ENABLED} then pop a dialog before with the given message before
             * performing the stage action.
             */
            public Please withDialog(String message)
            {
                m_dialogText = message;
                return this;
            }

            /**
             * Use the provided Activity to perform the stage action. Be *VERY* caution when using this. It is recommended to handle all enabling {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Stage}s
             * in one activity to avoid sticky situations that arise from multiple activities.
             */
            @Advanced
            public Please withActivity(Activity activity)
            {
                m_activity = activity;
                return this;
            }

            /**
             * Perform the next stage with the given requestCode used for {@link Activity#onActivityResult(int, int, Intent)} or {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
             */
            public Please withRequestCode(int requestCode)
            {
                m_requestCode = requestCode;
                return this;
            }

            /**
             * Perform the next stage with a Toast. This is especially helpful for requesting Location Services as well as when the user has denied Location Permissions and selected "Never ask again"
             * since, in these situations, the settings on the phone will be opened and the app won't be visible.
             */
            public Please withToast(String message)
            {
                m_toastText = message;
                return this;
            }

            /**
             * Do the next stage with implicity activity result handling. This means that the next stage will be performed with the {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Please#NULL_REQUEST_CODE}
             * and the {@link Activity#onActivityResult(int, int, Intent)} and {@link Activity#onRequestPermissionsResult(int, String[], int[])} calls will be implicitly handled internally in {@link BluetoothEnabler}.
             * When using this for all {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Stage}s you don't need to add anything to the {@link Activity#onRequestPermissionsResult(int, String[], int[])} or
             * {@link Activity#onActivityResult(int, int, Intent)} methods in your Activity.
             */
            public Please withImplicitActivityResultHandling()
            {
                m_implicitActivityResultHandling = true;
                return this;
            }
        }

        /**
         * Called after moving to the next {@link BluetoothEnablerFilter.Stage}
         */
        Please onEvent(final BluetoothEnablerEvent e);
    }

    /**
     * A default implementation of {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter} used in {@link BluetoothEnabler#BluetoothEnabler(Activity)}. It provides a
     * basic implementation for use/example and can be overridden.
     */
    public static class DefaultBluetoothEnablerFilter implements BluetoothEnablerFilter
    {
		@Override public Please onEvent(BluetoothEnablerEvent e)
        {
            final String locationPermissionAndServicesNeedEnablingString = "Android Marshmallow (6.0+) requires Location Permission to the app to be able to scan for Bluetooth devices.\n\nMarshmallow also requires Location Services to improve Bluetooth device discovery.  While it is not required for use in this app, it is recommended to better discover devices.\n\nPlease accept to allow Location Permission and Services";
            final String locationPermissionNeedEnablingString = "Android Marshmallow (6.0+) requires Location Permission to be able to scan for Bluetooth devices. Please accept to allow Location Permission.";
            final String locationServicesNeedEnablingString = "Android Marshmallow (6.0+) requires Location Services for improved Bluetooth device scanning. While it is not required, it is recommended that Location Services are turned on to improve device discovery.";
            final String locationPermissionToastString = "Please click the Permissions button, then enable Location, then press back";
            final String locationServicesToastString = "Please enable Location Services then press back button";

            if(e.nextStage() == Stage.BLUETOOTH)
            {
                return Please.doNext().withImplicitActivityResultHandling();
            }
            else if(e.nextStage() == Stage.LOCATION_PERMISSION)
            {
                if(e.status() == Status.ALREADY_ENABLED || e.status() == Status.ENABLED)
                {
                    if(!e.isEnabled(Stage.LOCATION_SERVICES) && !e.isEnabled(Stage.LOCATION_PERMISSION))
                    {
                        if(BleManager.get(e.activity()).willLocationPermissionSystemDialogBeShown(e.activity()))
                        {
                            return Please.doNext().withImplicitActivityResultHandling().withDialog(locationPermissionAndServicesNeedEnablingString);
                        }
                        else
                        {
                            return Please.doNext().withImplicitActivityResultHandling().withDialog(locationPermissionAndServicesNeedEnablingString).withToast(locationPermissionToastString);
                        }
                    }
                    else if(!e.isEnabled(Stage.LOCATION_PERMISSION))
                    {
                        if(BleManager.get(e.activity()).willLocationPermissionSystemDialogBeShown(e.activity()))
                        {
                            return Please.doNext().withImplicitActivityResultHandling().withDialog(locationPermissionNeedEnablingString);
                        }
                        else
                        {
                            return Please.doNext().withImplicitActivityResultHandling().withDialog(locationPermissionNeedEnablingString).withToast(locationPermissionToastString);
                        }
                    }
                    else if(!e.isEnabled(Stage.LOCATION_SERVICES))
                    {
                        return Please.doNext().withImplicitActivityResultHandling().withDialog(locationServicesNeedEnablingString).withToast(locationServicesToastString);
                    }
                    return Please.stop();
                }
                else if(e.status() == Status.CANCELLED_BY_DIALOG || e.status() == Status.CANCELLED_BY_INTENT)
                {
                    return Please.stop();
                }
            }
            else if(e.nextStage() == Stage.LOCATION_SERVICES )
            {
                if(e.status() == Status.ALREADY_ENABLED || e.status() == Status.ENABLED)
                {
                    if(!e.isEnabled(Stage.LOCATION_SERVICES))
                    {
                        return Please.doNext().withImplicitActivityResultHandling().withToast(locationServicesToastString);
                    }
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

    private static boolean isEnabled(BleManager bleManager, BluetoothEnablerFilter.Stage stage)
    {
        if(stage == BluetoothEnablerFilter.Stage.BLUETOOTH)
        {
            return bleManager.isBleSupported() && bleManager.is(BleManagerState.ON);
        }
        else if(stage == BluetoothEnablerFilter.Stage.LOCATION_PERMISSION)
        {
            return bleManager.isLocationEnabledForScanning_byRuntimePermissions();
        }
        else if(stage == BluetoothEnablerFilter.Stage.LOCATION_SERVICES)
        {
            return bleManager.isLocationEnabledForScanning_byOsServices();
        }
        return true;
    }

    private final BleManager m_bleManager;
    private final BluetoothEnablerFilter m_enablerFilter;
    private final Activity m_passedActivity;
	private final Application.ActivityLifecycleCallbacks m_lifecycleCallback;


    private BluetoothEnablerFilter.Please m_lastPlease = null;
    private BluetoothEnablerFilter.Stage m_currentStage;
    private boolean m_mayBePerformingSystemCall;

    private BluetoothEnabler(Activity activity)
    {
        this(activity, new DefaultBluetoothEnablerFilter());
    }

    private BluetoothEnabler(Activity activity, BluetoothEnablerFilter enablerFilter)
    {
        m_bleManager = BleManager.get(activity);
        m_passedActivity = activity;
        m_currentStage = BluetoothEnablerFilter.Stage.START;

        m_lifecycleCallback = new Application.ActivityLifecycleCallbacks()
		{
			@Override public void onActivityCreated(Activity activity, Bundle savedInstanceState){}
			@Override public void onActivityStarted(Activity activity){}
			@Override public void onActivityStopped(Activity activity){}
			@Override public void onActivitySaveInstanceState(Activity activity, Bundle outState){}
			@Override public void onActivityDestroyed(Activity activity){}

            @Override public void onActivityPaused(Activity activity)
            {
                if(m_passedActivity == activity && m_currentStage != null && (m_currentStage == BluetoothEnablerFilter.Stage.BLUETOOTH || m_currentStage == BluetoothEnablerFilter.Stage.LOCATION_PERMISSION))
                {
                    m_mayBePerformingSystemCall = true;
                }
            }

			@Override public void onActivityResumed(Activity activity)
			{
                //Activity resumes after startActivity (bluetooth) gets called
                if(activity == m_passedActivity && m_mayBePerformingSystemCall && m_currentStage != BluetoothEnablerFilter.Stage.START && m_lastPlease != null && m_lastPlease.m_implicitActivityResultHandling)
                {
                    m_mayBePerformingSystemCall = false;
                    BluetoothEnabler.this.onActivityOrPermissionResult(m_lastPlease.m_requestCode, true);
                }
            }
        };

        m_passedActivity.getApplication().registerActivityLifecycleCallbacks(m_lifecycleCallback);
        m_enablerFilter = enablerFilter;

		BluetoothEnablerFilter.BluetoothEnablerEvent startEvent = new BluetoothEnablerFilter.BluetoothEnablerEvent(m_passedActivity, m_currentStage, BluetoothEnablerFilter.Status.NULL);

        nextStage(startEvent);
    }

    private void nextStage(BluetoothEnablerFilter.BluetoothEnablerEvent nextEvent)
    {
        if(m_currentStage == BluetoothEnablerFilter.Stage.START)
        {
            updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.NULL);
        }
        else if(m_currentStage == BluetoothEnablerFilter.Stage.BLUETOOTH)
        {
            if(nextEvent.status() == BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG);
            }
            else if(nextEvent.status() == BluetoothEnablerFilter.Status.SKIPPED)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.SKIPPED);
            }
            else if(m_bleManager.isBleSupported() && !m_bleManager.is(BleManagerState.ON))
            {
                Activity resultActivity = m_lastPlease.m_activity != null ? m_lastPlease.m_activity : m_passedActivity;
                m_bleManager.turnOnWithIntent(resultActivity, m_lastPlease.m_requestCode);
            }
            else
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.ALREADY_ENABLED);
            }
        }
        else if(m_currentStage == BluetoothEnablerFilter.Stage.LOCATION_PERMISSION)
        {
            if(!Utils.isMarshmallow())
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.NOT_NEEDED);
            }
            else
            {
                if(nextEvent.status() == BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG)
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG);
                }
                else if(nextEvent.status() == BluetoothEnablerFilter.Status.SKIPPED)
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.SKIPPED);
                }
                else if(!m_bleManager.isLocationEnabledForScanning_byRuntimePermissions())
                {
                    Activity resultActivity = m_lastPlease.m_activity != null ? m_lastPlease.m_activity : m_passedActivity;
                    m_bleManager.turnOnLocationWithIntent_forPermissions(resultActivity, m_lastPlease.m_requestCode);
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.ALREADY_ENABLED);
                }
            }
        }
        else if(m_currentStage == BluetoothEnablerFilter.Stage.LOCATION_SERVICES)
        {
            if(nextEvent.status() == BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG);
            }
            else if(nextEvent.status() == BluetoothEnablerFilter.Status.SKIPPED)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.SKIPPED);
            }
            else if(!m_bleManager.isLocationEnabledForScanning_byOsServices())
            {
                Activity resultActivity = m_lastPlease.m_activity != null ? m_lastPlease.m_activity : m_passedActivity;
                m_bleManager.turnOnLocationWithIntent_forOsServices(resultActivity, m_lastPlease.m_requestCode);
            }
            else
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.ALREADY_ENABLED);
            }
        }
    }

    private boolean isNextStageAlreadyEnabled(BluetoothEnablerFilter.Stage stage)
    {
        if(stage == BluetoothEnablerFilter.Stage.BLUETOOTH)
        {
            return m_bleManager.isBleSupported() && m_bleManager.is(BleManagerState.ON);
        }
        else if(stage == BluetoothEnablerFilter.Stage.LOCATION_PERMISSION)
        {
            return m_bleManager.isLocationEnabledForScanning_byRuntimePermissions();
        }
        else if(stage == BluetoothEnablerFilter.Stage.LOCATION_SERVICES)
        {
            return m_bleManager.isLocationEnabledForScanning_byOsServices();
        }
        return true;
    }

    private void handlePleaseResponse()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(m_passedActivity);
        if(m_lastPlease.shouldPopDialog() && m_currentStage == BluetoothEnablerFilter.Stage.BLUETOOTH && !m_lastPlease.wasSkipped())
        {
            if(m_lastPlease.m_stateCode == BluetoothEnablerFilter.Please.END)
            {
                builder.setMessage(m_lastPlease.m_dialogText);
                builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
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
        if(m_lastPlease.shouldShowToast() && !wasCancelledByDialog)
        {
            Toast.makeText(m_passedActivity, m_lastPlease.m_toastText, Toast.LENGTH_LONG).show();
        }

        if(m_lastPlease.m_stateCode == BluetoothEnablerFilter.Please.PAUSE)
        {

        }
        else if(m_lastPlease.m_stateCode == BluetoothEnablerFilter.Please.DO_NEXT )
        {
            m_currentStage = m_currentStage.next();
            BluetoothEnablerFilter.BluetoothEnablerEvent nextEvent = wasCancelledByDialog ? new BluetoothEnablerFilter.BluetoothEnablerEvent(m_passedActivity, m_currentStage, BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerFilter.BluetoothEnablerEvent(m_passedActivity, m_currentStage, BluetoothEnablerFilter.Status.NULL);
            nextStage(nextEvent);
        }
        else if(m_lastPlease.m_stateCode == BluetoothEnablerFilter.Please.SKIP_NEXT)
        {
            m_currentStage = m_currentStage.next();
            BluetoothEnablerFilter.BluetoothEnablerEvent nextEvent = wasCancelledByDialog ? new BluetoothEnablerFilter.BluetoothEnablerEvent(m_passedActivity, m_currentStage, BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerFilter.BluetoothEnablerEvent(m_passedActivity, m_currentStage, BluetoothEnablerFilter.Status.SKIPPED);
            nextStage(nextEvent);
        }
        else if(m_lastPlease.m_stateCode == BluetoothEnablerFilter.Please.END)
        {
            m_currentStage = BluetoothEnablerFilter.Stage.NULL;
            m_passedActivity.getApplication().unregisterActivityLifecycleCallbacks(m_lifecycleCallback);
        }
    }

    private void updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status newStatus)
    {
        BluetoothEnablerFilter.BluetoothEnablerEvent currentEvent = new BluetoothEnablerFilter.BluetoothEnablerEvent(m_passedActivity, m_currentStage, newStatus);
        m_lastPlease = m_enablerFilter.onEvent(currentEvent);

        if( false == m_currentStage.isLast() )
        {
            handlePleaseResponse();
        }
        else
        {
            m_currentStage = BluetoothEnablerFilter.Stage.NULL;
        }
    }


    /**
     * A potentially-required method to be placed in your activity's {@link Activity#onRequestPermissionsResult(int, String[], int[])} and {@link Activity#onActivityResult(int, int, Intent)} methods.
     * This method will reconnect the enabler after the app is re-entered. Otherwise, the enabler won't continue. This method isn't necessarily needed if, for every stage, you use
     * {@link BluetoothEnablerFilter.Please#withImplicitActivityResultHandling()} as that will let the {@link BluetoothEnabler} handle all results.
     */
    public void onActivityOrPermissionResult(int requestCode)
    {
        onActivityOrPermissionResult(requestCode, false);
    }

    private void onActivityOrPermissionResult(int requestCode, boolean calledImplicitly)
    {
        if(requestCode == m_lastPlease.m_requestCode && m_lastPlease.m_implicitActivityResultHandling == calledImplicitly)
        {
            if(m_currentStage == BluetoothEnablerFilter.Stage.BLUETOOTH)
            {
                if(m_bleManager.isBleSupported() && !m_bleManager.is(BleManagerState.ON))
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.CANCELLED_BY_INTENT);
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.ENABLED);
                }
            }
            else if(m_currentStage == BluetoothEnablerFilter.Stage.LOCATION_PERMISSION)
            {
                if(!m_bleManager.isLocationEnabledForScanning_byRuntimePermissions())
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.CANCELLED_BY_INTENT);
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.ENABLED);
                }
            }
            else if(m_currentStage == BluetoothEnablerFilter.Stage.LOCATION_SERVICES)
            {
                if(!m_bleManager.isLocationEnabledForScanning_byOsServices())
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.CANCELLED_BY_INTENT);
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerFilter.Status.ENABLED);
                }
            }
        }
    }

    /**
     * Returns whether the passed {@link BluetoothEnablerFilter.Stage} is enabled.
     */
    public boolean isEnabled(BluetoothEnablerFilter.Stage stage)
    {
        return isEnabled(m_bleManager, stage);
    }

    /**
     * Resume the enabler with the given Please. Enabler will continue where is left off. For example, calling {@link #resume(BluetoothEnablerFilter.Please)} with
     * {@link BluetoothEnablerFilter.Please#doNext()} will resume the enabler and do the next stage.
     */
    public void resume(BluetoothEnablerFilter.Please please)
    {
        m_lastPlease = please;
        handlePleaseResponse();
    }

    /**
     * Returns the current {@link BluetoothEnablerFilter.Stage} the enabler is on
     */
    public BluetoothEnablerFilter.Stage getStage()
    {
        return m_currentStage;
    }

    /**
     * Returns whether the {@link BluetoothEnabler} is performing a system call. For example, this method will return <code>true</code> when the
     * Location Services screen is open and waiting for the user to either press back or enable Location Services and press the back button.
     */
    public boolean isPerformingSystemCall()
    {
        return m_mayBePerformingSystemCall && getStage().isNull() == false;
    }

    /**
     * Returns whether the {@link BluetoothEnabler} is done
     */
    public boolean isDone()
    {
        return getStage().isNull() == true;
    }
}