package com.idevicesinc.sweetblue.tests;

import android.app.Activity;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.BleManagerConfigScanTest;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class ScanTest
{

    @Test
    public void singleScanWithInterval() throws Exception
    {
        Activity activity = Robolectric.setupActivity(Activity.class);
        BleManagerConfigScanTest sConfig = new BleManagerConfigScanTest();
        BleManager mgr = BleManager.get(activity, sConfig);
        assertNotNull(mgr);
    }

}
