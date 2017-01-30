package com.idevicesinc.sweetblue;


import android.app.Activity;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public abstract class BaseTester<T extends Activity>
{

    T activity;
    BleManager mgr;


    @Rule
    public ActivityTestRule<T> mRule = new ActivityTestRule<>(getActivityClass());

    abstract Class<T> getActivityClass();
    abstract BleManagerConfig getInitialConfig();

    @Before
    public void setup()
    {
        activity = mRule.getActivity();
        mgr = BleManager.get(activity, getConfig());
        mgr.onResume();
        additionalSetup();
    }

    @After
    public void shutdown()
    {
        mgr.shutdown();
    }

    private BleManagerConfig getConfig()
    {
        BleManagerConfig config = getInitialConfig();
        if (config == null)
        {
            config = new BleManagerConfig();
        }
        return config;
    }

    public void additionalSetup()
    {
    }


}
