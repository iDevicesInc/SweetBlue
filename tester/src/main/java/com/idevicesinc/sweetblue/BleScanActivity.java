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

    }


    public Handler getHandler()
    {
        return testHandler;
    }

    public BleManager getBleManager()
    {
        return bleManager;
    }

}
