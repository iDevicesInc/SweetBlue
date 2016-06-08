package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

public class TestActivity extends Activity
{
    private Handler testHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(com.idevicesinc.sweetblue.tester.R.layout.activity_main);

        testHandler = new Handler();
    }

    public Handler getHandler()
    {
        return testHandler;
    }


}
