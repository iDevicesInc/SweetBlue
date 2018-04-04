package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;



public class ServiceActivity extends Activity
{

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, MyBleService.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
