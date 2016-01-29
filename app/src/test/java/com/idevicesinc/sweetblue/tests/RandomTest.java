package com.idevicesinc.sweetblue.tests;


import android.app.Activity;
import com.idevicesinc.sweetblue.BleManager;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class RandomTest {

    Activity activity;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void randomTests() {
        BleManager mgr = BleManager.get(activity);
        assertNotNull(mgr);
        assertFalse(mgr.hasDevices());
    }

}
