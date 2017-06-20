package com.idevicesinc.sweetblue.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.P_StringHandler;
import com.idevicesinc.sweetblue.annotations.Advanced;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * This class is used to handle the new hairy logic for getting bluetooth low-energy scan results that is introduced with {@link android.os.Build.VERSION_CODES#M}.
 * With {@link android.os.Build.VERSION_CODES#M} you need to have {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
 * in your AndroidManifest.xml, and also enable them at runtime, AND also make sure location services are on.
 * <br><br>
 * See more information at <a target="_blank" href="https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues#android-m-issues">https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues#android-m-issues</a>
 * <br><br>
 * This class is simply a convenience that wraps various helper methods of {@link BleManager} (see the "See Also" section, which has enough links that it might give you
 * an idea of why {@link BluetoothEnabler} was written). As such you don't need to use it, but in combination with {@link BluetoothEnabler.DefaultBluetoothEnablerFilter}
 * it comes in handy as a one-line addition to most simple apps.
 *
 * @see BleManager#isLocationEnabledForScanning()
 * @see BleManager#isLocationEnabledForScanning_byManifestPermissions()
 * @see BleManager#isLocationEnabledForScanning_byRuntimePermissions()
 * @see BleManager#isLocationEnabledForScanning_byOsServices()
 * @see BleManager#turnOnLocationWithIntent_forPermissions(Activity, int)
 * @see BleManager#turnOnLocationWithIntent_forOsServices(Activity, int)
 * @see BleManager#turnOnWithIntent(Activity, int)
 * @see BleManager#willLocationPermissionSystemDialogBeShown(Activity)
 * @see <a target="_blank" href="https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues#android-m-issues">https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues#android-m-issues</a>
 */
public final class BluetoothEnabler
{
    /**
     * Overload of {@link #start(Activity)} but given {@link Context} must be an {@link Activity}.
     */
    public static BluetoothEnabler start(final Context context)
    {
        return start(context, new DefaultBluetoothEnablerFilter());
    }

    /**
     * Overload of {@link #start(Activity, BluetoothEnablerFilter)} but given {@link Context} must be an {@link Activity}.
     */
    public static BluetoothEnabler start(final Context context, final BluetoothEnablerFilter filter)
    {
        if( context instanceof Activity )
        {
            return start((Activity)context, filter);
        }
        else
        {
            Log.e(BluetoothEnabler.class.getSimpleName(), "Given Context must be an Activity!");

            return null;
        }
    }

    /**
     * Static equivalent of {@link #resume(BluetoothEnablerFilter.Please)} that you can use to call down into the
     * singleton created by {@link #start(Activity, BluetoothEnablerFilter)} (or overloads).
     */
    public static void resume_static(final BluetoothEnablerFilter.Please please)
    {
        if( s_instance != null )
        {
            s_instance.resume(please);
        }
    }

    /**
     * Static equivalent of {@link #onActivityOrPermissionResult(int)} that you can use to call down into the
     * singleton created by {@link #start(Activity, BluetoothEnablerFilter)} (or overloads).
     */
    public static void onActivityOrPermissionResult_static(int requestCode)
    {
        if( s_instance != null )
        {
            s_instance.onActivityOrPermissionResult(requestCode);
        }
    }

	/**
	 * Overload of {@link #start(Activity, BluetoothEnablerFilter)} that uses an instance of {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.DefaultBluetoothEnablerFilter}.
	 */
	public static BluetoothEnabler start(final Activity activity)
	{
		return start(activity, new DefaultBluetoothEnablerFilter());
	}

	/**
	 * Kicks off the complex flow needed to fully enable Bluetooth on Build versions greater than or equal to {@link android.os.Build.VERSION_CODES#M}.
	 */
	public static BluetoothEnabler start(final Activity activity, final BluetoothEnablerFilter filter)
	{
		if( s_instance != null && false == s_instance.isDone() )
		{
			s_instance.setNewFilter(filter);
		}
		else
		{
			s_instance = new BluetoothEnabler(activity, filter);
            s_instance.dispatchEvent(s_instance.getStage(), s_instance.getStage().next(), BluetoothEnablerFilter.Status.NULL);

		}

        return s_instance;
	}

	//--- DRK > Lazy singleton probably not ideal but protects enough against common misuse cases that it's probably justified.
	//---		Can just make constructors public in future if people need it.
	private static BluetoothEnabler s_instance;

    /**
     * Provide an implementation to {@link BluetoothEnabler#BluetoothEnabler(Activity, BluetoothEnablerFilter)} to receive callbacks or simply use the provided class
     * {@link DefaultBluetoothEnablerFilter} by calling {@link #start(Activity)}.
     */
    public static interface BluetoothEnablerFilter
    {
        /**
         * Enumerates changes in the "enabling" stage before a
         * Bluetooth LE scan is started. Used at {@link BluetoothEnablerFilter.BluetoothEnablerEvent} to denote
         * what the current stage as well as in {@link BluetoothEnablerEvent#nextStage()} to give the following stage to the current one.
         */
        public static enum Stage implements UsesCustomNull
        {
			/**
			 * Fulfills the soft contract of {@link UsesCustomNull}.
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

			private Stage previous()
			{
				return ordinal() - 1 > 0 ? values()[ordinal() - 1] : NULL;
			}

            public boolean isNull()
            {
                return this == NULL;
            }

            public boolean isLast()
            {
                return this == LOCATION_SERVICES;
            }

			public boolean isLocationRelated()
			{
				return this == LOCATION_SERVICES || this == LOCATION_PERMISSION;
			}
        }

        /**
         * The Status of the current {@link BluetoothEnablerFilter.Stage}
         */
        public static enum Status implements UsesCustomNull
        {
			/**
			 * Fulfills the soft contract of {@link UsesCustomNull}.
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
            SKIPPED,

            /**
             * A permission is needed in your AndroidManifest.xml - for now this only applies to {@link Stage#LOCATION_PERMISSION},
             * which requires either {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
             */
            MANIFEST_PERMISSION_NEEDED,

			/**
			 * If the programmer of the application chose to call {@link Please#stop()}.
			 */
			STOPPED;

            /**
             * Returns <code>true</code> if <code>this</code> == {@link #NULL}.
             */
            @Override public boolean isNull()
            {
                return this == NULL;
            }

            public boolean isCancelled()
            {
                return this == CANCELLED_BY_DIALOG || this == CANCELLED_BY_INTENT;
            }

			public boolean wasPreviouslyNotEnabled()
			{
				return this != ALREADY_ENABLED;
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
             * Returns the {@link BluetoothEnablerFilter.Stage} associated with this event.
             */
            public Stage stage() { return m_stage; }
            private final Stage m_stage;

			/**
			 * Returns the {@link BluetoothEnablerFilter.Stage} following the Stage for this event.
			 */
			public Stage nextStage() { return m_nextStage; }
			private final Stage m_nextStage;

			/**
			 * Returns the {@link BluetoothEnablerFilter.Stage} following the Stage for this event.
			 */
			public Stage previousStage() { return m_stage.previous(); }

            /**
             * Returns the {@link BluetoothEnablerFilter.Status} of the current Stage.
             */
            public Status status() { return m_status; }
            private final Status m_status;

            /**
             * Returns the {@link Activity} associated with the Event
             */
            public Activity activity() { return m_activity; }
			private final Activity m_activity;

			/**
			 * Returns the {@link BluetoothEnabler} associated with the Event.
			 */
			public BluetoothEnabler enabler() { return m_enabler; }
			private final BluetoothEnabler m_enabler;

			/**
			 * Returns the arbitrary app-specific data passed to BluetoothEnabler.BluetoothEnablerFilter.Please#withAppData(Object), or <code>null</code>.
			 *
			 * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.Please#withAppData(Object)
			 */
			public Object appData() {  return m_appData;  }
			private final Object m_appData;


            public BleManager bleManager()  {  return BleManager.get(activity());  }

			public boolean isDone()
			{
				return nextStage() == Stage.NULL;
			}

            private BluetoothEnablerEvent(Activity activity, Stage stage, Stage nextStage, Status status, BluetoothEnabler enabler, final Object appData)
            {
                m_stage = stage;
				m_nextStage = nextStage;
                m_status = status;
                m_activity = activity;
				m_enabler = enabler;
				m_appData = appData;
            }

            /**
             * Returns whether the passed {@link BluetoothEnablerFilter.Stage} is enabled.
             */
            public boolean isEnabled(Stage stage)
            {
                return BluetoothEnabler.isEnabled(BleManager.get(m_activity), stage);
            }

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
            private final static int PE_Option__DO_NEXT		= 0;
			private final static int PE_Option__SKIP_NEXT	= 1;
			private final static int PE_Option__END			= 3;
			private final static int PE_Option__PAUSE		= 4;

            private final static int NULL_REQUEST_CODE = 51214; //Large random int because we need to make sure that there is low probability that the user is using the same

            private final int m_pleaseOption;

            private  Activity m_activity = null;
            private String m_dialogText = "";
            private String m_toastText = "";
            private int m_requestCode = NULL_REQUEST_CODE;
            private boolean m_implicitActivityResultHandling = false;
			private Object m_appData = null;

            private Please(int pleaseoption)
            {
                m_pleaseOption = pleaseoption;
            }

            private boolean wasSkipped()
            {
                return m_pleaseOption == PE_Option__SKIP_NEXT;
            }

            private boolean shouldPopDialog(final Stage stage)
            {
                return m_dialogText != null && m_dialogText.isEmpty() == false;
            }

            private boolean shouldShowToast(final Stage stage)
            {
                return m_toastText != null && m_toastText.isEmpty() == false;
            }

			private Activity activityOrDefault(final Activity defaultActivity)
			{
				return m_activity != null ? m_activity : defaultActivity;
			}

            /**
             * Perform the next {@link BluetoothEnablerFilter.Stage}.
             */
            public static Please doNext()
            {
                return new Please(PE_Option__DO_NEXT);
            }

            /**
             * Skip the next {@link BluetoothEnablerFilter.Stage} and move the following one.
             */
            public static Please skipNext()
            {
                return new Please(PE_Option__SKIP_NEXT);
            }

            /**
             * Bypass all remaining stages and move to the end of the last stage; enabler will finish at this point
             */
            public static Please stop()
            {
                return new Please(PE_Option__END);
            }

            /**
             * Pause the enabler. Call {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler#resume(BluetoothEnablerFilter.Please)} to continue the process.
             */
            public static Please pause()
            {
                return new Please(PE_Option__PAUSE);
            }

            /**
             * If the next stage isn't skipped or {@link BluetoothEnablerFilter.Status#ALREADY_ENABLED} then pop a dialog before
             */
            public Please withDialog(String message)
            {
                m_dialogText = message;
                return this;
            }

			/**
			 * Add arbitrary app-specific data which will be passed to the next {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.BluetoothEnablerFilter.BluetoothEnablerEvent}
			 * through
			 */
			public Please withAppData(final Object appData)
			{
				m_appData = appData;

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

            /**
             * Perform the next stage with a Toast
             */
            public Please withToast(String message)
            {
                m_toastText = message;
                return this;
            }

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

    private static boolean hasActivity()
    {
        return s_instance.m_defaultActivity.get() != null;
    }


    /**
     * A default implementation of BluetoothEnablerListener used in {@link BluetoothEnabler#start(Activity)}. It provides a
     * basic implementation for use/example and can be overridden.
     */
    public static class DefaultBluetoothEnablerFilter implements BluetoothEnablerFilter
    {
		@Override public Please onEvent(BluetoothEnablerEvent e)
        {
            // Just bail out if we have lost the activity reference.
            if (!hasActivity())
            {
                Log.e("BluetoothEnabler", "Lost the reference to the Activity, bailing out of enabler process.");
                return Please.stop();
            }
			if( e.stage().isLocationRelated() && e.status().isCancelled() )
			{
                final String fineButDotDotDot = P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.DENYING_LOCATION_ACCESS);

				return Please.stop().withDialog(fineButDotDotDot);
			}
			else if( e.stage() == Stage.LOCATION_PERMISSION && e.status() == Status.MANIFEST_PERMISSION_NEEDED )
			{
				final String manifestPermissionWarning = P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.APP_NEEDS_PERMISSION);

				return Please.stop().withDialog(manifestPermissionWarning);
			}
			else
			{
				if( e.nextStage() == Stage.BLUETOOTH )
				{
					return Please.doNext().withImplicitActivityResultHandling();
				}
				else if( e.nextStage() == Stage.LOCATION_PERMISSION )
				{
					final String locationPermissionToastString = P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.LOCATION_PERMISSION_TOAST);

					//--- DRK > If both location stages need enabling then we show one dialog to rule them all,
					//---		otherwise we show just one dialog for permissions.
					final String dialogString =
							false == e.isEnabled(Stage.LOCATION_SERVICES) && false == e.isEnabled(Stage.LOCATION_PERMISSION) ?
                                    P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.REQUIRES_LOCATION_PERMISSION_AND_SERVICES) :
                                    P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.REQUIRES_LOCATION_PERMISSION);

					if( e.bleManager().willLocationPermissionSystemDialogBeShown(e.activity()) )
					{
						return Please.doNext()  .withImplicitActivityResultHandling()  .withDialog(dialogString);
					}
					else
					{
						return Please.doNext()  .withImplicitActivityResultHandling()  .withDialog(dialogString)  .withToast(locationPermissionToastString);
					}
				}
				else if( e.nextStage() == Stage.LOCATION_SERVICES )
				{
					final String locationServicesNeedEnablingString = P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.LOCATION_SERVICES_NEEDS_ENABLING);
					final String locationServicesToastString = P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.LOCATION_SERVICES_TOAST);

					//--- DRK > Here it's a little confusing, but only showing dialog for enabling services if a one dialog to rule them all didn't previously come up.
					if( e.status()/*(for permissions)*/.wasPreviouslyNotEnabled() )
					{
						return Please.doNext()  .withImplicitActivityResultHandling()  .withToast(locationServicesToastString);
					}
					else
					{
						return Please.doNext()  .withImplicitActivityResultHandling()  .withDialog(locationServicesNeedEnablingString)  .withToast(locationServicesToastString);
					}
				}
				else if( e.nextStage() == Stage.NULL )
				{
					//--- DRK > All done...
					return null;
				}
			}

			e.bleManager().ASSERT(false, "Unhandled BluetoothEnabler event!");

			return null;
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

    private /*finalish*/ BluetoothEnablerFilter m_enablerFilter;
    private final WeakReference<Activity> m_defaultActivity;
	private final Application.ActivityLifecycleCallbacks m_lifecycleCallback;

    private BluetoothEnablerFilter.Please m_lastPlease = null;
    private BluetoothEnablerFilter.Stage m_currentStage = null;

    private boolean m_performingSystemCall;
	private boolean m_isForegrounded;

    /**
     * A constructor which taken an activity and a custom implementation of {@link BluetoothEnablerFilter}.
     */
    private BluetoothEnabler(Activity activity, BluetoothEnablerFilter enablerFilter)
    {
        m_defaultActivity = new WeakReference<>(activity);
		m_enablerFilter = enablerFilter;
        m_lifecycleCallback = newLifecycleCallbacks();

        m_defaultActivity.get().getApplication().registerActivityLifecycleCallbacks(m_lifecycleCallback);
		m_isForegrounded = true; // Assume we're foregrounded until told otherwise.
		m_currentStage = BluetoothEnablerFilter.Stage.START;
    }

	private void dispatchEvent(final BluetoothEnablerFilter.Stage currentStage, final BluetoothEnablerFilter.Stage nextStage, final BluetoothEnablerFilter.Status status_currentStage)
	{
        if (!hasActivity())
        {
            Log.e("BluetoothEnabler", "Lost the reference to the Activity, bailing out of enabler process.");
            return;
        }
		final Object appData = m_lastPlease != null ? m_lastPlease.m_appData : null;

		final BluetoothEnablerFilter.BluetoothEnablerEvent e = new BluetoothEnablerFilter.BluetoothEnablerEvent(m_defaultActivity.get(), currentStage, nextStage, status_currentStage, this, appData);

		m_lastPlease = m_enablerFilter.onEvent(e);
        m_lastPlease = m_lastPlease != null ? m_lastPlease : BluetoothEnablerFilter.Please.stop();
        m_currentStage = nextStage;

		if( m_currentStage == BluetoothEnablerFilter.Stage.NULL )
		{
			//--- DRK > We're done - either at the end of the process or user chose to stop it.
		}
		else
		{
			handlePleaseResponse_STEP1_maybeEarlyOutCauseNotNeeded(m_lastPlease);
		}
	}

	private void handlePleaseResponse_STEP1_maybeEarlyOutCauseNotNeeded(final BluetoothEnablerFilter.Please please)
	{
		switch(getStage())
		{
			case BLUETOOTH:
			{
				//--- DRK > All Android versions needed bluetooth enabled, duh.
				handlePleaseResponse_STEP2_maybeEarlyOutCauseAlreadyEnabled(please);

				break;
			}

			case LOCATION_PERMISSION:
			case LOCATION_SERVICES:
			{
				if( Utils.isMarshmallow() )
				{
					handlePleaseResponse_STEP2_maybeEarlyOutCauseAlreadyEnabled(please);
				}
				else
				{
					dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.NOT_NEEDED);
				}

				break;
			}

			case NULL:
			case START:
			{
				bleMngr().ASSERT(false, "Can't determine need for " + getStage());

				return;
			}
		}
	}

    private void updateBleManagerScanState()
    {
        boolean on = isEnabled(BluetoothEnablerFilter.Stage.BLUETOOTH);
        boolean locService = isEnabled(BluetoothEnablerFilter.Stage.LOCATION_SERVICES);
        boolean locPerms = isEnabled(BluetoothEnablerFilter.Stage.LOCATION_PERMISSION);
        if (!Utils.isMarshmallow())
        {
            if (on)
            {
                updateBleScanState();
            }
        }
        else
        {
            if (on && locPerms && locService)
            {
                updateBleScanState();
            }
        }
    }

    private void updateBleScanState()
    {
        try
        {
            Method scanstate = bleMngr().getClass().getDeclaredMethod("setBleScanReady", (Class[]) null);
            scanstate.setAccessible(true);
            scanstate.invoke(bleMngr(), (Object[]) null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

	private void handlePleaseResponse_STEP2_maybeEarlyOutCauseAlreadyEnabled(final BluetoothEnablerFilter.Please please)
	{
		if( isEnabled(getStage()) )
		{
			dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.ALREADY_ENABLED);
		}
		else
		{
			handlePleaseResponse_STEP3_maybeEarlyOutFromPleaseResponse(please);
		}
	}

	private void handlePleaseResponse_STEP3_maybeEarlyOutFromPleaseResponse(final BluetoothEnablerFilter.Please please)
	{
		if( please.m_pleaseOption == BluetoothEnablerFilter.Please.PE_Option__PAUSE )
		{
			handlePleaseResponse_STEP8_maybeShowToast(please);

			//--- DRK > Do nothing, user has to call resume().
		}
		else if( please.m_pleaseOption == BluetoothEnablerFilter.Please.PE_Option__SKIP_NEXT )
		{
			handlePleaseResponse_STEP8_maybeShowToast(please);

			//--- DRK > Recurse back into dispatchEvent().
			dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.SKIPPED);
		}
		else if( please.m_pleaseOption == BluetoothEnablerFilter.Please.PE_Option__END )
		{
			handlePleaseResponse_STEP3a_maybeShowClosingDialog(please);
			handlePleaseResponse_STEP8_maybeShowToast(please);

            updateBleManagerScanState();

			//--- DRK > Recurse back into dispatchEvent().
			dispatchEvent(getStage(), BluetoothEnablerFilter.Stage.NULL, BluetoothEnablerFilter.Status.STOPPED);
		}
		else if( please.m_pleaseOption == BluetoothEnablerFilter.Please.PE_Option__DO_NEXT )
		{
            handlePleaseResponse_STEP4_maybeEarlyOutCausePermissionsNeeded(please);
		}
		else
		{
			bleMngr().ASSERT(false, "Unhandled Please option case " + please.m_pleaseOption + " for " + BluetoothEnabler.class.getSimpleName());
		}
	}

	private void handlePleaseResponse_STEP3a_maybeShowClosingDialog(final BluetoothEnablerFilter.Please please)
	{
		if( please.shouldPopDialog(getStage()) )
		{
            if (!hasActivity())
            {
                Log.e("BluetoothEnabler", "Lost the reference to the Activity, bailing out of enabler process.");
                return;
            }
			final AlertDialog.Builder builder = new AlertDialog.Builder(please.activityOrDefault(m_defaultActivity.get()));

			builder.setMessage(m_lastPlease.m_dialogText);

			builder.setNeutralButton(P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.OK), new DialogInterface.OnClickListener()
			{
				@Override public void onClick(DialogInterface dialog, int which)
				{
				}
			});

			builder.show();
		}
	}

    private void handlePleaseResponse_STEP4_maybeEarlyOutCausePermissionsNeeded(final BluetoothEnablerFilter.Please please)
    {
        if( getStage() == BluetoothEnablerFilter.Stage.LOCATION_PERMISSION )
        {
            if( true == bleMngr().isLocationEnabledForScanning_byManifestPermissions() )
            {
                handlePleaseResponse_STEP5_maybeShowAppDialog(please);
            }
            else
            {
                dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.MANIFEST_PERMISSION_NEEDED);
            }
        }
        else
        {
            handlePleaseResponse_STEP5_maybeShowAppDialog(please);
        }
    }

    private void handlePleaseResponse_STEP5_maybeShowAppDialog(final BluetoothEnablerFilter.Please please)
    {
        if( please.shouldPopDialog(getStage()) )
        {
            if (!hasActivity())
            {
                Log.e("BluetoothEnabler", "Lost the reference to the Activity, bailing out of enabler process.");
                return;
            }
           final AlertDialog.Builder builder = new AlertDialog.Builder(please.activityOrDefault(m_defaultActivity.get()));

			builder.setMessage(m_lastPlease.m_dialogText);

			builder.setNegativeButton(P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.DENY), new DialogInterface.OnClickListener()
			{
				@Override public void onClick(DialogInterface dialog, int which)
				{
					handlePleaseResponse_STEP6_maybeEarlyOutFromDialogResponse(please, /*wasCancelledByDialog=*/true);
				}
			});

			builder.setPositiveButton(P_StringHandler.getString(s_instance.m_defaultActivity.get(), P_StringHandler.ACCEPT), new DialogInterface.OnClickListener()
			{
				@Override public void onClick(DialogInterface dialog, int which)
				{
					handlePleaseResponse_STEP6_maybeEarlyOutFromDialogResponse(please, /*wasCancelledByDialog=*/false);
				}
			});

			builder.show();
        }
        else
        {
			//--- DRK > Skip step 6, no dialog shown.
			handlePleaseResponse_STEP7_launchIntent(please);
        }
    }

	private void handlePleaseResponse_STEP6_maybeEarlyOutFromDialogResponse(final BluetoothEnablerFilter.Please please, final boolean wasCancelledByDialog)
	{
		if( true == wasCancelledByDialog )
		{
			dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG);
		}
		else
		{
			handlePleaseResponse_STEP7_launchIntent(please);
		}
	}

    private void handlePleaseResponse_STEP7_launchIntent(final BluetoothEnablerFilter.Please please)
    {
        if (!hasActivity())
        {
            Log.e("BluetoothEnabler", "Lost the reference to the Activity, bailing out of enabler process.");
            return;
        }
		final Activity callingActivity = please.activityOrDefault(m_defaultActivity.get());

		switch(getStage())
		{
			case BLUETOOTH:
			{
				onSystemCallStart();

				bleMngr().turnOnWithIntent(callingActivity, please.m_requestCode);

				break;
			}

			case LOCATION_PERMISSION:
			{
				onSystemCallStart();

				bleMngr().turnOnLocationWithIntent_forPermissions(callingActivity, please.m_requestCode);

				break;
			}

			case LOCATION_SERVICES:
			{
				onSystemCallStart();

				bleMngr().turnOnLocationWithIntent_forOsServices(callingActivity, please.m_requestCode);

				break;
			}

			case NULL:
			case START:
			{
				bleMngr().ASSERT(false, "Can't launch intent for " + getStage());

				return;
			}
		}

		handlePleaseResponse_STEP8_maybeShowToast(please);
    }

	private void handlePleaseResponse_STEP8_maybeShowToast(final BluetoothEnablerFilter.Please please)
	{
		if( please.shouldShowToast(getStage()) )
		{
            if (!hasActivity())
            {
                Log.e("BluetoothEnabler", "Lost the reference to the Activity, bailing out of enabler process.");
                return;
            }
			Toast.makeText(please.activityOrDefault(m_defaultActivity.get()), please.m_toastText, Toast.LENGTH_LONG).show();
		}

		//--- DRK > Now we wait for onActivityResult or through implicit foreground listening to continue the process.
	}

	private void handlePleaseResponse_STEP9_receiveActivityResult(final BluetoothEnablerFilter.Please please, final int requestCode)
	{
		if( m_lastPlease != null && m_lastPlease.m_requestCode == requestCode )
		{
			if( false == isEnabled(getStage()) )
			{
				dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.CANCELLED_BY_INTENT);
			}
			else
			{
                updateBleManagerScanState();

				dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.ENABLED);
			}
		}
		else
		{
			//--- DRK > Request codes don't match so this is not a result we care about.
		}
	}

    /**
     * A potentially-required method to be placed in your activity's {@link Activity#onRequestPermissionsResult(int, String[], int[])} and {@link Activity#onActivityResult(int, int, Intent)} methods.
     * This method will re-connect the enabler after the app is re-entered. Otherwise, the enabler won't continue unless {@link BluetoothEnablerFilter.Please#withImplicitActivityResultHandling()} is called.
     */
    public void onActivityOrPermissionResult(int requestCode)
    {
		if( m_lastPlease != null )
		{
			if( true == m_lastPlease.m_implicitActivityResultHandling )
			{
				//--- DRK > The filter opted for implicit handling so not passing this in.
			}
			else
			{
                handlePleaseResponse_STEP9_receiveActivityResult(m_lastPlease, requestCode);
			}
		}
    }

    /**
     * Returns whether the passed {@link BluetoothEnablerFilter.Stage} has been enabled.
     */
    public boolean isEnabled(BluetoothEnablerFilter.Stage stage)
    {
        return isEnabled(bleMngr(), stage);
    }

    /**
     * Resume the enabler with the given Please. Enabler will continue where is left off.
     */
    public void resume(BluetoothEnablerFilter.Please please)
    {
        if( isDone() )
        {
            Log.e(BluetoothEnabler.class.getSimpleName(), "Can't resume " + BluetoothEnabler.class.getSimpleName() + " because it's already done.");

            return;
        }
        else
        {
            m_lastPlease = please;

            handlePleaseResponse_STEP2_maybeEarlyOutCauseAlreadyEnabled(please);
        }
    }

    /**
     * Returns the current {@link BluetoothEnablerFilter.Stage} the enabler is on
     */
    public BluetoothEnablerFilter.Stage getStage()
    {
        return m_currentStage;
    }

    public boolean isPerformingSystemCall()
    {
        return m_performingSystemCall && getStage().isNull() == false;
    }

    public boolean isDone()
    {
        return getStage() == BluetoothEnablerFilter.Stage.NULL;
    }





	// REST OF THESE METHODS ARE PRIVATE FLUFF

	private BleManager bleMngr()
	{
		return BleManager.get(m_defaultActivity.get());
	}

	private void onSystemCallStart()
	{
		m_performingSystemCall = true;
	}

	private void onSystemCallEnd()
	{
		m_performingSystemCall = false;
	}

	private Application.ActivityLifecycleCallbacks newLifecycleCallbacks()
	{
		return new Application.ActivityLifecycleCallbacks()
		{
			@Override public void onActivityCreated(Activity activity, Bundle savedInstanceState){}
			@Override public void onActivityStarted(Activity activity){}
			@Override public void onActivityStopped(Activity activity){}
			@Override public void onActivitySaveInstanceState(Activity activity, Bundle outState){}
			@Override public void onActivityDestroyed(Activity activity){}

			@Override public void onActivityPaused(Activity activity)
			{
				m_isForegrounded = false;
			}

			@Override public void onActivityResumed(Activity activity)
			{
				if( false == m_isForegrounded && true == m_performingSystemCall )
				{
					onSystemCallEnd();

					if( m_lastPlease != null && true == m_lastPlease.m_implicitActivityResultHandling )
					{
						handlePleaseResponse_STEP9_receiveActivityResult(m_lastPlease, m_lastPlease.m_requestCode);
					}
				}

				m_isForegrounded = true;
			}
		};
	}

	private void setNewFilter(final BluetoothEnablerFilter filter)
	{
		m_enablerFilter = filter;
	}
}