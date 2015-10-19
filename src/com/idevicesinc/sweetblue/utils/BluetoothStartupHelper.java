package com.idevicesinc.sweetblue.utils;

import android.app.Activity;
import android.content.Context;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;

/**
 * BluetoothStartupHelper is used to handle the new logic that is introduced with {@link android.os.Build.VERSION_CODES#M}.  With {@link android.os.Build.VERSION_CODES#M} you need might need to request
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} at runtime (behavior isn't too consistent)
 * as well as make sure that the user has turned on location services on their device.
 * <br><br>
 * BluetoothStartupHelper will first make sure that
 *
 */
public class BluetoothStartupHelper {

    public static interface BluetoothStartupHelperListener
    {
        public static enum EnablingStage
        {
            ENABLE_BLUETOOTH,
            ENABLE_BLUETOOTH_RESPONSE,
            REQUEST_LOCATION_PERMISSION,
            REQUEST_LOCATION_PERMISSION_RESPONSE,
            ENABLE_LOCATION_SERVICES,
            DONE;

            EnablingStage nextStage()
            {
                return values()[ordinal() + 1];
            }
        }

        public static enum Status
        {
            CANCELLED_BY_DIALOG,
            CANCELLED_BY_INTENT,
            ALREADY_ENABLED,
            NOT_NEEDED,
            NEED_ENABLING,
            NEED_RESPONSE,
            ENABLED;
        }

        public static class BluetoothStartupEvent extends Event
        {
            public EnablingStage stage(){ return m_stage;}
            private final EnablingStage m_stage;

            public Status status(){ return m_status;}
            private Status m_status;

            public BluetoothStartupEvent nextEvent()
            {
                return new BluetoothStartupEvent(m_bleManager, stage().nextStage());
            }

            private BleManager m_bleManager;

//            public EnablingStage nextStage(){return m_stage.nextStage();}


            BluetoothStartupEvent(BleManager bleManager, EnablingStage stage)
            {
                m_bleManager = bleManager;
                m_stage = stage;
                populateStatusForStages(bleManager, stage);
            }

            BluetoothStartupEvent(BleManager bleManager)
            {
                this(bleManager, EnablingStage.ENABLE_BLUETOOTH);
            }

            private void populateStatusForStages(BleManager manager, EnablingStage stage)
            {
                switch (stage){
                    case ENABLE_BLUETOOTH:
                        if(manager.isBleSupported() && !manager.is(BleManagerState.ON))
                        {
                            m_status = Status.NEED_ENABLING;
                        }
                        else
                        {
                            m_status = Status.ALREADY_ENABLED;
                        }
                        break;
                    case REQUEST_LOCATION_PERMISSION:
                        if(!manager.isLocationEnabledForScanning_byRuntimePermissions())
                        {
                            m_status = Status.NEED_ENABLING;
                        }
                        else
                        {
                            m_status = Status.ALREADY_ENABLED;
                        }
                        break;
                    case ENABLE_LOCATION_SERVICES:
                        if(!manager.isLocationEnabledForScanning_byOsServices())
                        {
                            m_status = Status.NEED_ENABLING;
                        }
                        else
                        {
                            m_status = Status.ALREADY_ENABLED;
                        }
                        break;
                    default:
                        m_status = Status.NEED_RESPONSE;
                        break;
                }
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
            private final static int NULL_REQUEST_CODE = 400; //Fix this to a better int
            final Context m_context;
            private final int m_requestCode;

            private Please(Context context, int requestCode)
            {
                m_context = context;
                m_requestCode = requestCode;
            }

            int requestCode()
            {
                return m_requestCode;
            }

            Context context()
            {
                return m_context;
            }

            public static Please doNext(Context context)
            {
                return new Please(context, NULL_REQUEST_CODE);
            }

            public static Please doNextWithRequestCode(Context context, int requestCode)
            {
                return new Please(context, requestCode);
            }

            public static Please doNothing()
            {
                return new Please(null, NULL_REQUEST_CODE);
            }

        }

        Please onEvent(final BluetoothStartupEvent e);
    }

    private static final BluetoothStartupHelperListener DEFAULT_STARTUP_LISTENER = new BluetoothStartupHelperListener() {
        private final int DEFAULT_PERMISSION_REQUEST_CODE = 486016991;
        private final int DEFUALT_SETTING_REQUEST_CODE = 91042469;
        @Override
        public Please onEvent(BluetoothStartupEvent e) {
            return  null;
        }
    };

    private BleManager m_bleManager;
    private BluetoothStartupHelperListener.Please m_currentPlease;
    private BluetoothStartupHelperListener.BluetoothStartupEvent m_currentEvent;
    private final BluetoothStartupHelperListener m_startupListener;

    public BluetoothStartupHelper(BleManager bleManager, BluetoothStartupHelperListener startupListener)
    {
        m_bleManager = bleManager;
        m_currentEvent = new BluetoothStartupHelperListener.BluetoothStartupEvent(bleManager);
        m_startupListener = startupListener;
        m_currentPlease = m_startupListener.onEvent(m_currentEvent);
        eventHandler(m_currentEvent, m_currentPlease);
    }

    private void eventHandler(BluetoothStartupHelperListener.BluetoothStartupEvent e, BluetoothStartupHelperListener.Please please)
    {
        switch(e.stage())
        {
            case ENABLE_BLUETOOTH:
                if(e.status() != BluetoothStartupHelperListener.Status.ALREADY_ENABLED)
                {
                    m_bleManager.turnOnWithIntent((Activity) please.context(), please.requestCode());
                }
                else
                {
                    updateEventAndPleaseAndCallNextEvent();
                }
                break;
            case ENABLE_BLUETOOTH_RESPONSE:
                updateEventAndPleaseAndCallNextEvent();
                break;
            case REQUEST_LOCATION_PERMISSION:
                if(e.status() == BluetoothStartupHelperListener.Status.ALREADY_ENABLED)
                {
                    m_bleManager.turnOnLocationWithIntent_forPermissions((Activity)please.context(), please.requestCode());
                }
                else
                {
                    updateEventAndPleaseAndCallNextEvent();
                }
                break;
            case REQUEST_LOCATION_PERMISSION_RESPONSE:
                updateEventAndPleaseAndCallNextEvent();
                break;
            case ENABLE_LOCATION_SERVICES:
                if(e.status() != BluetoothStartupHelperListener.Status.ALREADY_ENABLED)
                {
                    m_bleManager.turnOnLocationWithIntent_forOsServices((Activity) please.context(), please.requestCode());
                }
                else
                {
                    updateEventAndPleaseAndCallNextEvent();
                }
                break;
            case DONE:
                break;
        }
    }

    public void onActivityOrPermissionResult(int requestCode)
    {
        if(requestCode == m_currentPlease.requestCode())
        {
            updateEventAndPleaseAndCallNextEvent();
        }
    }

    private void updateEventAndPleaseAndCallNextEvent()
    {
        m_currentEvent = m_currentEvent.nextEvent();
        m_currentPlease = m_startupListener.onEvent(m_currentEvent);
        eventHandler(m_currentEvent, m_currentPlease);
    }



}
