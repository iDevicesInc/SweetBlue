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
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} at runtime (behavior isn't too consistent)
 * as well as make sure that the user has turned on location services on their device.
 * <br><br>
 * To use this class create an instance of it in your activity and call the {@link #onActivityOrPermissionResult(int)} on the instance created in the two required methods. Nothing else needs to be done.
 */
public class BluetoothEnabler
{
	/**
	 * Overload of {@link #start(Activity)} that uses an instance {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler.DefaultBluetoothEnablerFilter}.
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
		}

        return s_instance;
	}

	//--- DRK > Lazy singleton not ideal but protects enough against common misuse cases that it's probably justified.
	//---		Can just make constructors public in future if people need it.
	private static BluetoothEnabler s_instance;

    /**
     * Provide an implementation to {@link BluetoothEnabler#BluetoothEnabler(Activity, BluetoothEnablerFilter)} to receive callbacks or simply use the provided class
     * {@link DefaultBluetoothEnablerFilter} by calling {@link #start(Activity)}. This listener will be the main
     * way of handling different enabling events and their results.
     *
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
             * Returns the {@link BluetoothEnablerFilter.Status} of the current Stage.
             */
            public Status status() { return m_status; }
            private final Status m_status;

            /**
             * Returns the {@link Activity} associated with the Event
             */
            public Activity activity() { return m_activity; }
            public final Activity m_activity;

            public BleManager bleManager()  {  return BleManager.get(activity());  }

			public boolean isDone()
			{
				return nextStage() == Stage.NULL;
			}

            private BluetoothEnablerEvent(Activity activity, Stage stage, Stage nextStage, Status status)
            {
                m_stage = stage;
				m_nextStage = nextStage;
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
            final static int PE_Option__DO_NEXT		= 0;
            final static int PE_Option__SKIP_NEXT	= 1;
            final static int PE_Option__END			= 3;
            final static int PE_Option__PAUSE		= 4;

            private final static int NULL_REQUEST_CODE = 51214; //Large random int because we need to make sure that there is low probability that the user is using the same

            private final int m_pleaseOption;

            private  Activity m_activity = null;
            private String m_dialogText = "";
            private String m_toastText = "";
            private int m_requestCode = NULL_REQUEST_CODE;
            private boolean m_implicitActivityResultHandling = false;

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
                return m_dialogText != null && m_dialogText.isEmpty() == false && m_pleaseOption == PE_Option__DO_NEXT && stage != Stage.NULL;
            }

            private boolean shouldShowToast(final Stage stage)
            {
                return m_toastText != null && m_toastText.isEmpty() == false && m_pleaseOption == PE_Option__DO_NEXT && stage != Stage.NULL;
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

    /**
     * A default implementation of BluetoothEnablerListener used in {@link BluetoothEnabler#start(Activity)}. It provides a
     * basic implementation for use/example and can be overridden.
     */
    public static class DefaultBluetoothEnablerFilter implements BluetoothEnablerFilter
    {
		@Override public Please onEvent(BluetoothEnablerEvent e)
        {
			if( e.stage().isLocationRelated() && e.status().isCancelled() )
			{
				//--- DRK > Just making the judgement call here that app user is not keen on enabling any location-related stuff.
				//---		You might want to override this method and pop an "OK, but scanning won't work..." dialog in your app.
				return Please.stop();
			}
			else
			{
				if( e.nextStage() == Stage.BLUETOOTH )
				{
					return Please.doNext().withImplicitActivityResultHandling();
				}
				else if( e.nextStage() == Stage.LOCATION_PERMISSION )
				{
					final String locationPermissionToastString = "Please click the Permissions button, then enable Location, then press back twice.";

					//--- DRK > If both location stages need enabling then we show one dialog to rule them all...
					if( false == e.isEnabled(Stage.LOCATION_SERVICES) && false == e.isEnabled(Stage.LOCATION_PERMISSION) )
					{
						final String locationPermissionAndServicesNeedEnablingString = "Android Marshmallow (6.0+) requires Location Permission to the app to be able to scan for Bluetooth devices.\n\nMarshmallow also requires Location Services to improve Bluetooth device discovery.  While it is not required for use in this app, it is recommended to better discover devices.\n\nPlease accept to allow Location Permission and Services.";

						if( e.bleManager().willLocationPermissionSystemDialogBeShown(e.activity()) )
						{
							return Please.doNext()  .withImplicitActivityResultHandling()  .withDialog(locationPermissionAndServicesNeedEnablingString);
						}
						else
						{
							return Please.doNext()  .withImplicitActivityResultHandling()  .withDialog(locationPermissionAndServicesNeedEnablingString)  .withToast(locationPermissionToastString);
						}
					}

					//--- DRK > Otherwise we show just one dialog for permissions.
					else
					{
						final String locationPermissionNeedEnablingString = "Android Marshmallow (6.0+) requires Location Permission to be able to scan for Bluetooth devices. Please accept to allow Location Permission.";

						if( e.bleManager().willLocationPermissionSystemDialogBeShown(e.activity()) )
						{
							return Please.doNext()  .withImplicitActivityResultHandling()  .withDialog(locationPermissionNeedEnablingString);
						}
						else
						{
							return Please.doNext()  .withImplicitActivityResultHandling()  .withDialog(locationPermissionNeedEnablingString)  .withToast(locationPermissionToastString);
						}
					}
				}
				else if( e.nextStage() == Stage.LOCATION_SERVICES )
				{
					final String locationServicesNeedEnablingString = "Android Marshmallow (6.0+) requires Location Services for improved Bluetooth device scanning. While it is not required, it is recommended that Location Services are turned on to improve device discovery.";
					final String locationServicesToastString = "Please enable Location Services then press back.";

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
    private final Activity m_defaultActivity;
	private final Application.ActivityLifecycleCallbacks m_lifecycleCallback;

    private BluetoothEnablerFilter.Please m_lastPlease = null;
    private BluetoothEnablerFilter.Stage m_currentStage = null;

    private boolean m_performingSystemCall;
	private boolean m_isForegrounded;

    /**
     * A constructor which taken an activity and a custom implementation of {@link BluetoothEnablerFilter}. A BleManager will
     * be obtained from the passed activity.
     */
    private BluetoothEnabler(Activity activity, BluetoothEnablerFilter enablerFilter)
    {
        m_defaultActivity = activity;
		m_enablerFilter = enablerFilter;
        m_lifecycleCallback = newLifecycleCallbacks();

        m_defaultActivity.getApplication().registerActivityLifecycleCallbacks(m_lifecycleCallback);
		m_isForegrounded = true; // Assume we're foregrounded until told otherwise.
		m_currentStage = BluetoothEnablerFilter.Stage.START;

		dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.NULL);
    }

	private void dispatchEvent(final BluetoothEnablerFilter.Stage currentStage, final BluetoothEnablerFilter.Stage nextStage, final BluetoothEnablerFilter.Status status_currentStage)
	{
		final BluetoothEnablerFilter.BluetoothEnablerEvent e = new BluetoothEnablerFilter.BluetoothEnablerEvent(m_defaultActivity, currentStage, nextStage, status_currentStage);

		m_lastPlease = m_enablerFilter.onEvent(e);
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
			//--- DRK > Do nothing, user has to call resume().
		}
		else if( please.m_pleaseOption == BluetoothEnablerFilter.Please.PE_Option__SKIP_NEXT )
		{
			//--- DRK > Recurse back into dispatchEvent().
			dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.SKIPPED);
		}
		else if( please.m_pleaseOption == BluetoothEnablerFilter.Please.PE_Option__END )
		{
			//--- DRK > Recurse back into dispatchEvent().
			dispatchEvent(getStage(), BluetoothEnablerFilter.Stage.NULL, BluetoothEnablerFilter.Status.STOPPED);
		}
		else if( please.m_pleaseOption == BluetoothEnablerFilter.Please.PE_Option__DO_NEXT )
		{
			handlePleaseResponse_STEP4_maybeShowAppDialog(please);
		}
		else
		{
			bleMngr().ASSERT(false, "Unhandled Please option case " + please.m_pleaseOption + " for " + BluetoothEnabler.class.getSimpleName());
		}
	}

    private void handlePleaseResponse_STEP4_maybeShowAppDialog(final BluetoothEnablerFilter.Please please)
    {
        if( please.shouldPopDialog(m_currentStage) )
        {
           final AlertDialog.Builder builder = new AlertDialog.Builder(m_defaultActivity);

			builder.setMessage(m_lastPlease.m_dialogText);

			builder.setNegativeButton("Deny", new DialogInterface.OnClickListener()
			{
				@Override public void onClick(DialogInterface dialog, int which)
				{
					handlePleaseResponse_STEP5_maybeEarlyOutFromDialogResponse(please, /*wasCancelledByDialog=*/true);
				}
			});

			builder.setPositiveButton("Accept", new DialogInterface.OnClickListener()
			{
				@Override public void onClick(DialogInterface dialog, int which)
				{
					handlePleaseResponse_STEP5_maybeEarlyOutFromDialogResponse(please, /*wasCancelledByDialog=*/false);
				}
			});

			builder.show();
        }
        else
        {
			//--- DRK > Skip step 5, no dialog shown.
			handlePleaseResponse_STEP6_launchIntent(please);
        }
    }

	private void handlePleaseResponse_STEP5_maybeEarlyOutFromDialogResponse(final BluetoothEnablerFilter.Please please, final boolean wasCancelledByDialog)
	{
		if( true == wasCancelledByDialog )
		{
			dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.CANCELLED_BY_DIALOG);
		}
		else
		{
			handlePleaseResponse_STEP6_launchIntent(please);
		}
	}

    private void handlePleaseResponse_STEP6_launchIntent(final BluetoothEnablerFilter.Please please)
    {
		final Activity callingActivity = please.activityOrDefault(m_defaultActivity);

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

		handlePleaseResponse_STEP7_maybeShowToast(please);
    }

	private void handlePleaseResponse_STEP7_maybeShowToast(final BluetoothEnablerFilter.Please please)
	{
		if( please.shouldShowToast(getStage()) )
		{
			Toast.makeText(please.activityOrDefault(m_defaultActivity), please.m_toastText, Toast.LENGTH_LONG).show();
		}

		//--- DRK > Now we wait for onActivityResult or through implicit foreground listening to continue the process.
	}

	private void handlePleaseResponse_STEP8_receiveActivityResult(final BluetoothEnablerFilter.Please please, final int requestCode)
	{
		if( m_lastPlease != null && m_lastPlease.m_requestCode == requestCode )
		{
			if( false == isEnabled(getStage()) )
			{
				dispatchEvent(getStage(), getStage().next(), BluetoothEnablerFilter.Status.CANCELLED_BY_INTENT);
			}
			else
			{
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
				handlePleaseResponse_STEP8_receiveActivityResult(m_lastPlease, requestCode);
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
        m_lastPlease = please;

		handlePleaseResponse_STEP2_maybeEarlyOutCauseAlreadyEnabled(please);
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
		return BleManager.get(m_defaultActivity);
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
						handlePleaseResponse_STEP8_receiveActivityResult(m_lastPlease, m_lastPlease.m_requestCode);
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