package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.idevicesinc.sweetblue.tester.R;

public class BluetoothEnablerActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scan_listitem_layout);
    }

}
