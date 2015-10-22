package com.idevicesinc.sweetblue.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;

/**
 * BluetoothEnabler is used to handle the new logic for getting BLE scan results that is introduced with {@link android.os.Build.VERSION_CODES#M}.  With {@link android.os.Build.VERSION_CODES#M} you need might need to request
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} at runtime (behavior isn't too consistent)
 * as well as make sure that the user has turned on location services on their device.
 * <br><br>
 * BluetoothEnabler will first make sure that
 *
 */
public class BluetoothEnabler {

    /**
     * Provide an implementation to {@link BluetoothEnabler#BluetoothEnabler(BleManager, BluetoothEnablerListener)} to receive callbacks FINISH THIS
     *
     */
    public static interface BluetoothEnablerListener
    {
        /**
         * Enumerates changes in the "enabling" stage before a
         * Bluetooth LE scan is started. Used at {@link BluetoothEnablerEvent#stage()}.
         */
        public static enum EnablingStage
        {
            /**
             * Used when the enabling cycle will begin. A dialog to the user can
             * be shown here to explain the reason for showing system dialogs in
             * the next stage(s).
             */
            START,

            /**
             * Used when checking if the device needs Bluetooth turned on and
             */
            ENABLING_BLUETOOTH,
            REQUESTING_LOCATION_PERMISSION,
            ENABLING_LOCATION_SERVICES,
            DONE;

            EnablingStage next()
            {
                return ordinal() + 1 < values().length ? values()[ordinal() + 1] : DONE;
            }
        }

        public static enum Status
        {
            ALREADY_ENABLED, //If whatever current stage is already enabled
            ENABLED, //If the current stage was enabled by the user
            CANCELLED_BY_DIALOG, //If the current stage was cancelled by the user selecting the dialog
            CANCELLED_BY_INTENT, //If the current stage was cancelled by the user declining the actual request
            NOT_NEEDED, //If the current stage isn't needed (occurs with requesting permission on pre-Marshmallow devices)
            NEEDS_ENABLING, //If the current stage needs to be enabled by the user
            SKIPPED;
        }

        public static class BluetoothEnablerEvent extends Event
        {
            public EnablingStage stage(){ return m_stage;}
            private final EnablingStage m_stage;

            public Status status(){ return m_status;}
            private Status m_status;

            BluetoothEnablerEvent(EnablingStage stage)
            {
                this(stage, Status.NEEDS_ENABLING);
            }

            BluetoothEnablerEvent(EnablingStage stage, Status status)
            {
                m_stage = stage;
                m_status = status;
            }

            void setEventStatus(Status status)
            {
                m_status = status;
            }

            @Override public String toString()
            {
                return Utils_String.toString
                (
                        this.getClass(),
                        "stage", stage()
                );
            }
        }

        public static class Please
        {
            final static int DO_NEXT = 0;
            final static int SKIP_NEXT = 1;
            final static int DO_NEXT_WITH_REQUEST_CODE = 2;
            final static int SKIP_NEXT_WITH_REQUEST_CODE = 3;
            final static int END = 4;

            private final static int NULL_REQUEST_CODE = 400; //Fix this to a better int
            private final int m_requestCode;
            private final int m_stateCode;

            private  Activity m_activity = null;
            private String m_dialogText = "";


            private Please(int stateCode, int requestCode)
            {
                m_requestCode = requestCode;
                m_stateCode = stateCode;
            }

            Activity activity()
            {
                return m_activity;
            }


            int requestCode()
            {
                return m_requestCode;
            }

            int stateCode()
            {
                return m_stateCode;
            }

            String dialogText()
            {
                return m_dialogText;
            }

            boolean wasSkipped()
            {
                return m_stateCode == SKIP_NEXT_WITH_REQUEST_CODE || m_stateCode == SKIP_NEXT;
            }

            boolean shouldPopDialog()
            {
                return m_dialogText.equals("") ? false : true;
            }

            public static Please doNext()
            {
                return new Please(DO_NEXT, NULL_REQUEST_CODE);
            }

            public static Please doNextWithRequestCode(int requestCode)
            {
                return new Please(DO_NEXT_WITH_REQUEST_CODE, requestCode);
            }

            public static Please skipNext()
            {
                return new Please(SKIP_NEXT, NULL_REQUEST_CODE);
            }

            public static Please skipNextWithRequestCode(int requestCode)
            {
                return new Please(SKIP_NEXT_WITH_REQUEST_CODE, requestCode);
            }

            public static Please stop()
            {
                return new Please(END, NULL_REQUEST_CODE);
            }

            public Please withDialog(String message)
            {
                m_dialogText = message;
                return this;
            }

            public Please withActivity(Activity activity)
            {
                m_activity = activity;
                return this;
            }
        }

        Please onEvent(final BluetoothEnablerEvent e);
    }

    private final BluetoothEnablerListener DEFAULT_STARTUP_LISTENER = new BluetoothEnablerListener() {
        @Override
        public Please onEvent(BluetoothEnablerEvent e) {
            if(e.stage() == EnablingStage.START)
            {
                if(e.status() == Status.NEEDS_ENABLING)
                {
                    return Please.doNext().withDialog("This app requires Bluetooth to function properly but it is currently turned off. Please turn Bluetooth on.");
                }
            }
            else if(e.stage() == EnablingStage.ENABLING_BLUETOOTH)
            {
                if(e.status() == Status.ALREADY_ENABLED)
                {
                    return Please.doNext().withDialog("Location permissions are needed for Bluetooth LE scan to show available devices. If you would like to see your devices please allow the Location permission.");
                }
                else if(e.status() == Status.ENABLED)
                {
                    return Please.doNext().withDialog("Location permissions are needed for Bluetooth LE scan to show available devices. If you would like to see your devices please allow the Location permission.");
                }
                else if(e.status() == Status.CANCELLED_BY_DIALOG || e.status() == Status.CANCELLED_BY_INTENT)
                {
                    return Please.stop();
                }
                else if(e.status() == Status.SKIPPED) {
                    return Please.doNext();
                }
            }
            else if(e.stage() == EnablingStage.REQUESTING_LOCATION_PERMISSION)
            {
                if(e.status() == Status.ALREADY_ENABLED)
                {
                    return Please.doNext().withDialog("Location Services are needed for Bluetooth LE scan to show available devices. If you would like to see your devices please turn on Location Services.");
                }
                else if( e.status() == Status.ENABLED)
                {
                    return Please.doNext().withDialog("Location Services are needed for Bluetooth LE scan to show available devices. If you would like to see your devices please turn on Location Services.");
                }
                else if(e.status() == Status.CANCELLED_BY_DIALOG || e.status() == Status.CANCELLED_BY_INTENT)
                {
                    return Please.stop();
                }
                else if(e.status() == Status.SKIPPED) {
                    return Please.doNext();
                }
            }
            else if(e.stage() == EnablingStage.ENABLING_LOCATION_SERVICES)
            {
                if(e.status() == Status.ALREADY_ENABLED || e.status() == Status.ENABLED)
                {
                    return Please.doNext();
                }
                else if(e.status() == Status.CANCELLED_BY_DIALOG || e.status() == Status.CANCELLED_BY_INTENT)
                {
                    return Please.stop();
                }
                else if(e.status() == Status.SKIPPED) {
                    return Please.doNext();
                }
            }
            else if(e.stage() == EnablingStage.DONE)
            {
                return Please.stop();
            }
            return Please.doNext();
        }
    };


    private final BleManager m_bleManager;
    private final BluetoothEnablerListener m_startupListener;

    private Activity m_passedActivity;

    private BluetoothEnablerListener.Please m_lastPlease;
    private BluetoothEnablerListener.EnablingStage m_currentStage;
    private BluetoothEnablerListener.BluetoothEnablerEvent m_currentEvent;


    public BluetoothEnabler(Activity activity)
    {
        this(activity, BleManager.get(activity));
    }

    public BluetoothEnabler(Activity activity, BleManager bleManager)
    {
        m_bleManager = bleManager;
        m_passedActivity = activity;

        m_currentStage = BluetoothEnablerListener.EnablingStage.START;
        m_currentEvent = new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage);
        m_startupListener = DEFAULT_STARTUP_LISTENER;
        nextStage();
    }

    public BluetoothEnabler(Activity activity, BluetoothEnablerListener startupListener)
    {
        this(activity, BleManager.get(activity), startupListener);
    }

    public BluetoothEnabler(Activity activity, BleManager bleManager, BluetoothEnablerListener startupListener)
    {
        m_bleManager = bleManager;
        m_passedActivity = activity;

        m_currentStage = BluetoothEnablerListener.EnablingStage.START;
        m_currentEvent = new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage);
        m_startupListener = startupListener;
        nextStage();
    }

    private void nextStage()
    {
        if(m_currentEvent.stage() == BluetoothEnablerListener.EnablingStage.START)
        {
            if(m_bleManager.isBleSupported() && !m_bleManager.is(BleManagerState.ON)){
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.NEEDS_ENABLING);
            }
            else {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ALREADY_ENABLED);
            }
        }
        else if(m_currentStage == BluetoothEnablerListener.EnablingStage.ENABLING_BLUETOOTH)
        {
            if(m_currentEvent.status() == BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG);
            }
            else if(m_currentEvent.status() == BluetoothEnablerListener.Status.SKIPPED)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.SKIPPED);
            }
            else if(m_bleManager.isBleSupported() && !m_bleManager.is(BleManagerState.ON))
            {
                Activity resultActivity = m_lastPlease.activity() != null ? m_lastPlease.activity() : m_passedActivity;
                m_bleManager.turnOnWithIntent(resultActivity, m_lastPlease.requestCode());
            }
            else
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ALREADY_ENABLED);
            }
        }
        else if(m_currentStage == BluetoothEnablerListener.EnablingStage.REQUESTING_LOCATION_PERMISSION)
        {
            if(!Utils.isMarshmallow())
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.NOT_NEEDED);
            }
            else
            {
                if(m_currentEvent.status() == BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG)
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG);
                }
                else if(m_currentEvent.status() == BluetoothEnablerListener.Status.SKIPPED)
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.SKIPPED);
                }
                else if(!m_bleManager.isLocationEnabledForScanning_byRuntimePermissions()) {
                    Activity resultActivity = m_lastPlease.activity() != null ? m_lastPlease.activity() : m_passedActivity;
                    m_bleManager.turnOnLocationWithIntent_forPermissions(resultActivity, m_lastPlease.requestCode());
                }
                else
                {
                    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ALREADY_ENABLED);
                }
            }
        }
        else if(m_currentStage == BluetoothEnablerListener.EnablingStage.ENABLING_LOCATION_SERVICES)
        {
            if(m_currentEvent.status() == BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG);
            }
            else if(m_currentEvent.status() == BluetoothEnablerListener.Status.SKIPPED)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.SKIPPED);
            }
            else if(!m_bleManager.isLocationEnabledForScanning_byOsServices())
            {
                Activity resultActivity = m_lastPlease.activity() != null ? m_lastPlease.activity() : m_passedActivity;
                m_bleManager.turnOnLocationWithIntent_forOsServices(resultActivity, m_lastPlease.requestCode());
            }
            else{
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ALREADY_ENABLED);
            }
        }
        else if(m_currentStage == BluetoothEnablerListener.EnablingStage.DONE)
        {
            m_currentEvent = new BluetoothEnablerListener.BluetoothEnablerEvent(BluetoothEnablerListener.EnablingStage.DONE);
            m_lastPlease = m_startupListener.onEvent(m_currentEvent);
            handlePleaseResponse();
        }
    }

    private boolean isNextStageAlreadyEnabled(BluetoothEnablerListener.EnablingStage stage)
    {
        if(stage == BluetoothEnablerListener.EnablingStage.ENABLING_BLUETOOTH)
        {
            return m_bleManager.isBleSupported() && m_bleManager.is(BleManagerState.ON);
        }
        else if(stage == BluetoothEnablerListener.EnablingStage.REQUESTING_LOCATION_PERMISSION)
        {
            return m_bleManager.isLocationEnabledForScanning_byRuntimePermissions();
        }
        else if(stage == BluetoothEnablerListener.EnablingStage.ENABLING_LOCATION_SERVICES)
        {
            return m_bleManager.isLocationEnabledForScanning_byOsServices();
        }
        return true;
    }
    private void handlePleaseResponse()
    {
        if(m_lastPlease.activity() != null)
        {
            m_lastPlease.activity().startActivity(new Intent());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(m_passedActivity);
        if(m_lastPlease.shouldPopDialog() && m_currentStage != BluetoothEnablerListener.EnablingStage.DONE && !m_lastPlease.wasSkipped() && !isNextStageAlreadyEnabled(m_currentStage.next()))
        {
            if(m_lastPlease.stateCode() == BluetoothEnablerListener.Please.END)
            {
                builder.setMessage(m_lastPlease.dialogText());
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
                builder.setMessage(m_lastPlease.dialogText());
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
        else if(m_lastPlease.shouldPopDialog() && m_currentStage == BluetoothEnablerListener.EnablingStage.DONE && !m_lastPlease.wasSkipped())
        {
            builder.setMessage(m_lastPlease.dialogText());
            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
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
        if(m_lastPlease.stateCode() == BluetoothEnablerListener.Please.DO_NEXT || m_lastPlease.stateCode() == BluetoothEnablerListener.Please.DO_NEXT_WITH_REQUEST_CODE)
        {
            m_currentStage = m_currentStage.next();
            m_currentEvent = wasCancelledByDialog ? new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage);
            nextStage();
        }
        else if(m_lastPlease.stateCode() == BluetoothEnablerListener.Please.SKIP_NEXT || m_lastPlease.stateCode() == BluetoothEnablerListener.Please.SKIP_NEXT_WITH_REQUEST_CODE)
        {
            m_currentStage = m_currentStage.next();
            m_currentEvent = wasCancelledByDialog ? new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.SKIPPED);
            nextStage();
        }
        else if(m_lastPlease.stateCode() == BluetoothEnablerListener.Please.END)
        {
            m_currentStage = BluetoothEnablerListener.EnablingStage.DONE;
            m_currentEvent = wasCancelledByDialog ? new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage);
        }
    }

    private void
    updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status newStatus)
    {
        m_currentEvent.setEventStatus(newStatus);
        m_lastPlease = m_startupListener.onEvent(m_currentEvent);
        handlePleaseResponse();
    }


    /**
     *
     * @param requestCode
     */
    public void onActivityOrPermissionResult(int requestCode)
    {
        if(requestCode == m_lastPlease.requestCode())
        {
            if(m_currentStage == BluetoothEnablerListener.EnablingStage.ENABLING_BLUETOOTH)
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
            else if(m_currentStage == BluetoothEnablerListener.EnablingStage.REQUESTING_LOCATION_PERMISSION)
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
            else if(m_currentStage == BluetoothEnablerListener.EnablingStage.ENABLING_LOCATION_SERVICES)
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
}

