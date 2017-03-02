package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.os.Bundle;
import com.idevicesinc.sweetblue.integration.R;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;


public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothEnabler.start(this);
    }
}
