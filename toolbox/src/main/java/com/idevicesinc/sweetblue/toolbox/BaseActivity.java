package com.idevicesinc.sweetblue.toolbox;


import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;


public class BaseActivity extends AppCompatActivity
{

    <T extends View> T find(int id)
    {
        return (T) findViewById(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
