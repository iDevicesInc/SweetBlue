package com.idevicesinc.sweetblue.toolbox;

import android.app.ActionBar;
import android.os.Bundle;

public class DeviceInformationActivity extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.device_information_title));
    }

    @Override
    public boolean onNavigateUp()
    {
        finish();
        return true;
    }
}
