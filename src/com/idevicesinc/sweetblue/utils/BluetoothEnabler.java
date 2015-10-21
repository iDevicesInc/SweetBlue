package com.idevicesinc.sweetblue.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;

/**
 * BluetoothEnabler is used to handle the new logic that is introduced with {@link android.os.Build.VERSION_CODES#M}.  With {@link android.os.Build.VERSION_CODES#M} you need might need to request
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} at runtime (behavior isn't too consistent)
 * as well as make sure that the user has turned on location services on their device.
 * <br><br>
 * BluetoothEnabler will first make sure that
 *
 */
public class BluetoothEnabler {

    public static interface BluetoothEnablerListener
    {
        public static enum EnablingStage
        {
            START,
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
            NEEDS_ENABLING; //If the current stage needs to be enabled by the user
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
            final Context m_context;
            private final int m_requestCode;
            private final int m_stateCode;
            private String m_dialogText = "";

            private Please(Context context, int stateCode, int requestCode)
            {
                m_context = context;
                m_requestCode = requestCode;
                m_stateCode = stateCode;
            }

            int requestCode()
            {
                return m_requestCode;
            }

            int stateCode()
            {
                return m_stateCode;
            }

            Context context()
            {
                return m_context;
            }

            String dialogText()
            {
                return m_dialogText;
            }

            boolean shouldPopDialog()
            {
                return m_dialogText.equals("") ? false : true;
            }

            public static Please doNext(Context context)
            {
                return new Please(context, DO_NEXT, NULL_REQUEST_CODE);
            }

            public static Please doNextWithRequestCode(Context context, int requestCode)
            {
                return new Please(context, DO_NEXT_WITH_REQUEST_CODE, requestCode);
            }

            public static Please skipNext(Context context)
            {
                return new Please(context, SKIP_NEXT, NULL_REQUEST_CODE);
            }

            public static Please skipNextWithRequestCode(Context context, int requestCode)
            {
                return new Please(context, SKIP_NEXT_WITH_REQUEST_CODE, requestCode);
            }

            public static Please stop(Context context)
            {
                return new Please(context, END, NULL_REQUEST_CODE);
            }

            public Please withDialog(String message)
            {
                m_dialogText = message;
                return this;
            }
        }

        Please onEvent(final BluetoothEnablerEvent e);
    }

    private static final BluetoothEnablerListener DEFAULT_STARTUP_LISTENER = new BluetoothEnablerListener() {
        @Override
        public Please onEvent(BluetoothEnablerEvent e) {
            return  Please.doNext(null);
        }
    };

    private final BleManager m_bleManager;
    private final BluetoothEnablerListener m_startupListener;

    private BluetoothEnablerListener.Please m_lastPlease;
    private BluetoothEnablerListener.EnablingStage m_currentStage;
    private BluetoothEnablerListener.BluetoothEnablerEvent m_currentEvent;

    public BluetoothEnabler(BleManager bleManager, BluetoothEnablerListener startupListener)
    {
        m_bleManager = bleManager;
        m_currentStage = BluetoothEnablerListener.EnablingStage.START;
        m_currentEvent = new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage);
        m_startupListener = startupListener;
        nextStage();
    }

    private void nextStage()
    {
        if(m_currentEvent.stage() == BluetoothEnablerListener.EnablingStage.START)
        {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.NEEDS_ENABLING);
        }
        else if(m_currentStage == BluetoothEnablerListener.EnablingStage.ENABLING_BLUETOOTH)
        {
            //check if bluetooth is on, if it is on pass an event to the library user saying it is already on
            //get a please in return
            //if please says stop then done
            //please holds state, switch on please and do w/e it corresponds to
            if(m_currentEvent.status() == BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG)
            {
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG);
            }
            else if(m_bleManager.isBleSupported() && !m_bleManager.is(BleManagerState.ON))
            {
                m_bleManager.turnOnWithIntent((Activity) m_lastPlease.context(), m_lastPlease.requestCode());
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
                else if(!m_bleManager.isLocationEnabledForScanning_byRuntimePermissions())
                {
                    m_bleManager.turnOnLocationWithIntent_forPermissions((Activity) m_lastPlease.context(), m_lastPlease.requestCode());
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
            else if(!m_bleManager.isLocationEnabledForScanning_byOsServices())
            {
                m_bleManager.turnOnLocationWithIntent_forOsServices((Activity) m_lastPlease.context(), m_lastPlease.requestCode());
            }
            else{
                updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status.ALREADY_ENABLED);
            }
        }
        else if(m_currentStage == BluetoothEnablerListener.EnablingStage.DONE)
        {
            m_currentEvent = new BluetoothEnablerListener.BluetoothEnablerEvent(BluetoothEnablerListener.EnablingStage.DONE);
            m_startupListener.onEvent(m_currentEvent);
        }
    }

    private void handlePleaseResponse()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(m_lastPlease.context());
        if(m_lastPlease.shouldPopDialog() && m_currentStage != BluetoothEnablerListener.EnablingStage.ENABLING_LOCATION_SERVICES)
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
        else if(m_lastPlease.shouldPopDialog() && m_currentStage == BluetoothEnablerListener.EnablingStage.ENABLING_LOCATION_SERVICES)
        {
            if (m_currentEvent.status() == BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG)
            {
                builder.setMessage(m_lastPlease.dialogText());
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
                builder.setMessage(m_lastPlease.dialogText());
                builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
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
        if(m_lastPlease.stateCode() == BluetoothEnablerListener.Please.DO_NEXT || m_lastPlease.stateCode() == BluetoothEnablerListener.Please.DO_NEXT_WITH_REQUEST_CODE)
        {
            m_currentStage = m_currentStage.next();
            m_currentEvent = wasCancelledByDialog ? new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage);
            nextStage();
        }
        else if(m_lastPlease.stateCode() == BluetoothEnablerListener.Please.SKIP_NEXT || m_lastPlease.stateCode() == BluetoothEnablerListener.Please.SKIP_NEXT_WITH_REQUEST_CODE)
        {
            m_currentStage = m_currentStage.next().next();
            m_currentEvent = wasCancelledByDialog ? new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage);
            nextStage();
        }
        else if(m_lastPlease.stateCode() == BluetoothEnablerListener.Please.END)
        {
            m_currentStage = BluetoothEnablerListener.EnablingStage.DONE;
            m_currentEvent = wasCancelledByDialog ? new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage, BluetoothEnablerListener.Status.CANCELLED_BY_DIALOG) : new BluetoothEnablerListener.BluetoothEnablerEvent(m_currentStage);
            nextStage();
        }
    }

    private void updateEventStatusAndPassEventToUser(BluetoothEnablerListener.Status newStatus)
    {
        m_currentEvent.setEventStatus(newStatus);
        m_lastPlease = m_startupListener.onEvent(m_currentEvent);
        handlePleaseResponse();
    }

    public void onActivityOrPermissionResult(int requestCode)
    {
        if(requestCode == m_lastPlease.requestCode());
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

