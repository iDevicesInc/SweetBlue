package com.idevicesinc.sweetblue.utils;

import android.app.Activity;
import android.content.pm.PackageManager;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
            NEED_RESPONSE,
            ENABLED;
        }

        public static class BluetoothStartupEvent extends Event
        {
            public EnablingStage stage(){ return m_stage;}
            private final EnablingStage m_stage;

//            public EnablingStage nextStage(){return m_stage.nextStage();}

            private final HashMap<BluetoothStartupEvent, Status> m_events;

            BluetoothStartupEvent(BleManager bleManager, EnablingStage stage)
            {
                m_stage = stage;
                populateStatusForStages(bleManager);
            }

            BluetoothStartupEvent(BleManager bleManager)
            {
                m_stage = EnablingStage.ENABLE_BLUETOOTH;
                populateStatusForStages(bleManager);
            }

            private void populateStatusForStages(BleManager manager)
            {

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
            final Activity m_activity;
            final int m_requestCode;
            final BluetoothStartupHelperListener m_bluetoothStartupListener;
            final EnablingStage m_stage;

            private Please(Activity activity, int requestCode, EnablingStage stage, BluetoothStartupHelperListener startupListener )
            {
                m_activity = activity;
                m_requestCode = requestCode;
                m_stage = stage;
                m_bluetoothStartupListener = startupListener;
            }

            public static Please doNext()
            {
                return new Please(null, NULL_REQUEST_CODE, );
            }

            public static Please doNextWithRequestCode(Activity activity, int requestCode)
            {
                return new Please(activity,, requestCode, null);
            }

        }

        Please onEvent(final BluetoothStartupEvent e);
    }

    private static final BluetoothStartupHelperListener DEFAULT_STARTUP_LISTENER = new BluetoothStartupHelperListener() {
        private final int DEFAULT_PERMISSION_REQUEST_CODE = 486016991;
        private final int DEFUALT_SETTING_REQUEST_CODE = 91042469;
        @Override
        public Please onEvent(BluetoothStartupEvent e) {
            return Please.doNextWithRequestCode();
        }
    };

    public BluetoothStartupHelper(BleManager bleManager, BluetoothStartupHelperListener startupListener)
    {
        BluetoothStartupHelperListener.Please please = BluetoothStartupHelperListener.Please.doNext();
    }


    /**
     * Provide an implementation to
     */
    @com.idevicesinc.sweetblue.annotations.Lambda
    public static interface BluetoothEnabledListener
    {
        void bluetoothWasEnabled();
        void bluetoothStillDisabled();
    }

    public static interface LocationPermissionListener
    {
        void locationPermissionGranted();
        void locationPermissionDenied();
    }

    public static interface LocationServicesListener
    {
        void locationServicesEnabled();
        void locationServicesStillDisabled();
    }

    private final BleManager m_bleManager;
    private final Activity m_providedActivity;
    private BluetoothEnabledListener m_bluetoothEnabledListener;
    private LocationPermissionListener m_locationPermissionListener;
    private LocationServicesListener m_locationServicesListener;
    private List<BluetoothStartupHelperListener.EnablingStage> m_neededStages;

    public BluetoothStartupHelper(BleManager bleManager)
    {
        m_neededStages = new ArrayList<>();
        m_bleManager = bleManager;
        m_providedActivity = null;
    }

    public BluetoothStartupHelper(Activity singletonActivity, BleManager bleManager)
    {
        m_neededStages = new ArrayList<>();
        m_providedActivity = singletonActivity;
        m_bleManager = bleManager;
    }

    private boolean isBleOff()
    {
        return m_bleManager.isBleSupported() && !m_bleManager.is(BleManagerState.ON);
    }

    private boolean isLocationPermissionEnabled()
    {
        return m_bleManager.isLocationEnabledForScanning_byRuntimePermissions();
    }

    private boolean isLocationServicesEnabled() {
        return m_bleManager.isLocationEnabledForScanning_byOsServices();
    }

    private boolean needToEnable(BluetoothStartupHelperListener.EnablingStage stage)
    {
        return m_neededStages.contains(stage);
    }

    private void popStage()
    {
        m_neededStages.remove(0);
    }

    private  void setBleNeededStages()
    {
//        if(isBleOff())
        {
            m_neededStages.add(BluetoothStartupHelperListener.EnablingStage.ENABLE_BLUETOOTH);
        }
//        if(!isLocationPermissionEnabled())
        {
            m_neededStages.add(BluetoothStartupHelperListener.EnablingStage.REQUEST_LOCATION_PERMISSION);
        }
//        if(!isLocationServicesEnabled())
        {
            m_neededStages.add(BluetoothStartupHelperListener.EnablingStage.ENABLE_LOCATION_SERVICES);
        }
    }

    public void enableEverything(BluetoothEnabledListener bluetoothListener, LocationPermissionListener locPermissionListener, LocationServicesListener locServicesListener)
    {
        enableEverything(m_providedActivity, bluetoothListener, locPermissionListener, locServicesListener);
    }

    public void enableEverything(Activity callingActivity, BluetoothEnabledListener bluetoothListener, LocationPermissionListener locPermissionListener, LocationServicesListener locServicesListener)
    {
        setBleNeededStages();
        initListeners(bluetoothListener, locPermissionListener, locServicesListener);
        enableNextRequirement();
    }

    public void enableNextRequirement()
    {
        enableNextRequirement(m_providedActivity);
    }

    public void enableNextRequirement(Activity callingActivity)
    {
        if(needToEnable(BluetoothStartupHelperListener.EnablingStage.ENABLE_BLUETOOTH))
        {
            popStage();
            enableBluetooth(callingActivity);
        }
        else if(needToEnable(BluetoothStartupHelperListener.EnablingStage.REQUEST_LOCATION_PERMISSION))
        {
            popStage();
            requestLocationPermission(callingActivity);
        }
        else if(needToEnable(BluetoothStartupHelperListener.EnablingStage.ENABLE_LOCATION_SERVICES))
        {
            popStage();
            enableLocationServices(callingActivity);
        }
    }

    private void initListeners(BluetoothEnabledListener bluetoothListener, LocationPermissionListener locationPermissionListener, LocationServicesListener locationServicesListener)
    {
        m_bluetoothEnabledListener = bluetoothListener;
        m_locationPermissionListener = locationPermissionListener;
        m_locationServicesListener = locationServicesListener;
    }

    private void enableBluetooth(Activity callingActivity)
    {
        enableBluetooth(callingActivity, m_bluetoothEnabledListener);
    }

    public void enableBluetooth(Activity callingActivity, BluetoothEnabledListener listener)
    {
        m_bluetoothEnabledListener = listener;
        m_bleManager.turnOnWithIntent(callingActivity, BluetoothStartupHelperListener.EnablingStage.ENABLE_BLUETOOTH.getCode());
    }

    public void enableBluetoothResult(int requestCode, int resultCode)
    {
        if(requestCode == BluetoothStartupHelperListener.EnablingStage.ENABLE_BLUETOOTH.getCode())
        {
            if(resultCode == Activity.RESULT_OK)
            {
                m_bluetoothEnabledListener.bluetoothWasEnabled();
            }
            else
            {
                m_bluetoothEnabledListener.bluetoothStillDisabled();
            }
        }
    }

    public void requestLocationPermission(Activity callingActitity)
    {
        requestLocationPermission(callingActitity, m_locationPermissionListener);
    }

    public void requestLocationPermission(Activity callingActivity, LocationPermissionListener locationPermissionListener)
    {
        m_locationPermissionListener = locationPermissionListener;
        m_bleManager.turnOnLocationWithIntent_forPermissions(callingActivity, BluetoothStartupHelperListener.EnablingStage.REQUEST_LOCATION_PERMISSION.getCode());
    }

    public void requestLocationPermissionResult(int requestCode, String[] permissions, int[] grantResult)
    {
        if(requestCode == BluetoothStartupHelperListener.EnablingStage.REQUEST_LOCATION_PERMISSION.getCode())
        {
            if(grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED)
            {
                m_locationPermissionListener.locationPermissionGranted();
            }
            else
            {
                m_locationPermissionListener.locationPermissionDenied();
            }
        }
    }

    public void enableLocationServices(Activity callingActivity)
    {
        enableLocationServices(callingActivity, m_locationServicesListener);
    }

    public void enableLocationServices(Activity callingActivity, LocationServicesListener locationServicesListener)
    {
        m_locationServicesListener = locationServicesListener;
        m_bleManager.turnOnLocationWithIntent_forOsServices(callingActivity, BluetoothStartupHelperListener.EnablingStage.ENABLE_LOCATION_SERVICES.getCode());
    }

    public void enableLocationServicesResult(int requestCode, int resultCode)
    {
        if(requestCode == BluetoothStartupHelperListener.EnablingStage.ENABLE_LOCATION_SERVICES.getCode())
        {
            if(resultCode == Activity.RESULT_OK)
            {
                m_locationServicesListener.locationServicesEnabled();
            }
            else
            {
                m_locationServicesListener.locationServicesStillDisabled();
            }
        }
    }




}
