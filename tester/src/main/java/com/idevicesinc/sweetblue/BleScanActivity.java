package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.tester.R;


public class BleScanActivity extends Activity
{
    BleManager bleManager;

    Handler testHandler;

    EventStateInterface eventListener;

    public interface EventStateInterface
    {
        void onEvent(BleManager.StateListener.StateEvent event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BleManagerConfig config = new BleManagerConfig();

        config.loggingEnabled = true;

        bleManager = BleManager.get(this, config);

        setContentView(R.layout.activity_main);

        testHandler = new Handler();

        bleManager.setListener_NativeState(new BleManager.NativeStateListener()
        {
            @Override
            public void onEvent(NativeStateEvent event)
            {
                eventListener.onEvent(event);
//                if(event.didEnter(BleManagerState.SCANNING))
//                {
//                    Log.e("YAY", "WE HAVE ENTERED SCANNING");
//                }
//                else if(event.didExit(BleManagerState.SCANNING))
//                {
//                    Log.e("BOO", "NO LONGER SCANNING");
//                }
            }
        });

    }

    public void startFiveSecondScan()
    {
        bleManager.startScan(Interval.FIVE_SECS);
    }

    public void stopScan()
    {
        bleManager.stopScan();
    }

    public void startInfiniteScan()
    {
        bleManager.startScan();
    }

    public void startPeriodicScan()
    {
        bleManager.startPeriodicScan(Interval.FIVE_SECS, Interval.FIVE_SECS);
    }

    public Handler getHandler()
    {
        return testHandler;
    }

    public BleManager getBleManager()
    {
        return bleManager;
    }

    public void checkState()
    {
        Log.e("BLE_SCANNING", bleManager.is(BleManagerState.SCANNING) + "");
        Log.e("BLE_OFF", bleManager.is(BleManagerState.OFF) + "");
        Log.e("BLE_TURNING_OFF", bleManager.is(BleManagerState.TURNING_OFF) + "");
        Log.e("BLE_STARTING_SCAN", bleManager.is(BleManagerState.STARTING_SCAN) + "");
        Log.e("BLE_RESETTING", bleManager.is(BleManagerState.RESETTING) + "");
        Log.e("BLE_ON", bleManager.is(BleManagerState.ON) + "");
        Log.e("BLE_TURNING_ON", bleManager.is(BleManagerState.TURNING_ON) + "");
    }

    public boolean is(BleManagerState state)
    {
        return bleManager.is(state);
    }
}
