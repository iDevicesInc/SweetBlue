package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ScanPowerTest extends BaseBleUnitTest
{

    @Test
    public void scanPowerVeryLow() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.VERY_LOW_POWER;
        m_mgr.setConfig(m_config);
        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                assertTrue("Scan Power: " + getScanPower().name(), getScanPower() == BleScanPower.VERY_LOW_POWER);
                m_mgr.stopScan();
                succeed();
            }
        });
        m_mgr.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test
    public void scanPowerLow() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.LOW_POWER;
        m_mgr.setConfig(m_config);
        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                assertTrue("Scan Power: " + getScanPower().name(), getScanPower() == BleScanPower.LOW_POWER);
                m_mgr.stopScan();
                succeed();
            }
        });
        m_mgr.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test
    public void scanPowerMedium() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.MEDIUM_POWER;
        m_mgr.setConfig(m_config);
        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                assertTrue("Scan Power: " + getScanPower().name(), getScanPower() == BleScanPower.MEDIUM_POWER);
                m_mgr.stopScan();
                succeed();
            }
        });
        m_mgr.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test
    public void scanPowerHigh() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.HIGH_POWER;
        m_mgr.setConfig(m_config);
        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                assertTrue("Scan Power: " + getScanPower().name(), getScanPower() == BleScanPower.HIGH_POWER);
                m_mgr.stopScan();
                succeed();
            }
        });
        m_mgr.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test
    public void scanPowerAutoForeground() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.AUTO;
        m_mgr.setConfig(m_config);
        m_mgr.onResume();
        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                // We're in the foreground, and NOT running an infinite scan, so this should be High power here
                assertTrue("Scan Power: " + getScanPower().name(), getScanPower() == BleScanPower.HIGH_POWER);
                m_mgr.stopScan();
                succeed();
            }
        });
        m_mgr.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test
    public void scanPowerAutoInfinite() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.AUTO;
        m_mgr.setConfig(m_config);
        m_mgr.onResume();
        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                // We're in the foreground, and running an infinite scan, so this should be Medium power here
                assertTrue("Scan Power: " + getScanPower().name(), getScanPower() == BleScanPower.MEDIUM_POWER);
                m_mgr.stopScan();
                succeed();
            }
        });
        m_mgr.startScan();
        startAsyncTest();
    }

    @Test
    public void scanPowerAutoBackground() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.AUTO;
        m_mgr.setConfig(m_config);
        m_mgr.onPause();
        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                // We're in the background, so this should be low power here
                assertTrue("Scan Power: " + getScanPower().name(), getScanPower() == BleScanPower.LOW_POWER);
                m_mgr.stopScan();
                m_mgr.onResume();
                succeed();
            }
        });
        m_mgr.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

}
