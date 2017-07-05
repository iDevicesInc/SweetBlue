package com.idevicesinc.sweetblue.toolbox.activity;


import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.idevicesinc.sweetblue.toolbox.R;


public class BaseActivity extends AppCompatActivity
{

    <T extends View> T find(int id)
    {
        return (T) findViewById(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(getMenuResId(), menu);
        return super.onCreateOptionsMenu(menu);
    }

    public int getMenuResId()
    {
        return R.menu.main;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (!(this instanceof MainActivity))
        {
            if (item.getItemId() == android.R.id.home)
            {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

}
