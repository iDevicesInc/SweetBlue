package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class AdvertisingTest extends BaseBleUnitTest
{

    @Test
    public void startAdvertisingTest() throws Exception
    {
        m_config.loggingEnabled = true;

        m_mgr.setConfig(m_config);

        final BleServer server = m_mgr.getServer();
        server.setListener_Advertising(e ->
        {
            assertTrue(e.wasSuccess());
            assertTrue(server.isAdvertising());
            assertTrue(server.isAdvertising(Uuids.BATTERY_SERVICE_UUID));
            server.stopAdvertising();
            succeed();
        });

        server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);

        startTest();
    }

    @Test
    public void stopAdvertisingTest() throws Exception
    {
        m_config.loggingEnabled = true;

        m_mgr.setConfig(m_config);

        final BleServer server = m_mgr.getServer();
        server.setListener_Advertising(e -> {
            assertTrue(e.wasSuccess());
            server.stopAdvertising();
            assertFalse(server.isAdvertising());
            assertFalse(server.isAdvertising(Uuids.BATTERY_SERVICE_UUID));
            succeed();
        });

        server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);

        startTest();
    }

    @Test
    public void startStopStartAdvertisingTest() throws Exception
    {
        m_config.loggingEnabled = true;

        final AtomicInteger startCount = new AtomicInteger(0);

        m_mgr.setConfig(m_config);

        final BleServer server = m_mgr.getServer();
        server.setListener_Advertising(e -> {
            if (startCount.get() > 3)
            {
                server.stopAdvertising();
                succeed();
            }

            assertTrue(e.wasSuccess());
            startCount.incrementAndGet();

            server.stopAdvertising();
            assertFalse(server.isAdvertising());

            server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);
        });

        server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);

        startTest();
    }

}
