package com.idevicesinc.sweetblue.toolbox.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.idevicesinc.sweetblue.toolbox.R;


public class SettingsActivity extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.settings_title));
    }

    @Override
    public boolean onNavigateUp()
    {
        finish();
        return true;
    }
}
